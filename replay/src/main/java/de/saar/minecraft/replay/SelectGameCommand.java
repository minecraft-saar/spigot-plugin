package de.saar.minecraft.replay;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.github.agomezmoron.multimedia.recorder.VideoRecorder;
import de.saar.minecraft.broker.db.GameLogsDirection;
import de.saar.minecraft.broker.db.tables.records.GameLogsRecord;
import de.saar.minecraft.broker.db.tables.records.GamesRecord;
import de.saar.minecraft.communication.FlatChunkGenerator;
import de.saar.minecraft.communication.MinecraftListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jooq.Result;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.MILLIS;

public class SelectGameCommand implements CommandExecutor {
    final ReplayPlugin plugin;
    private static final Logger logger = LogManager.getLogger(SelectGameCommand.class);
    private LocalDateTime replayStartTime;
    private LocalDateTime gameStartTime;
    private LocalDateTime currentGameTime;
    private final int updateFrequency;
    private final double speed;

    public SelectGameCommand(ReplayPlugin plugin) {
        this.plugin = plugin;
        this.updateFrequency = plugin.getConfig().getInt("updateFrequency");
        this.speed = plugin.getConfig().getDouble("speed");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (! (sender instanceof Player)) {
            // TODO: find player or reject command
            return false;
        }
        Player player = (Player) sender;
        // stop old replays
        if (plugin.currentReplay != null) {
            Bukkit.getServer().getScheduler().cancelTask(plugin.currentReplay.getTaskId());
            logger.info("task is cancelled {}", plugin.currentReplay.isCancelled());
        }
        if (args.length < 1) {
            sender.sendMessage("Missing game id");
            return false;
        }
        int gameId = Integer.parseInt(args[0]);
        return runCommand(gameId, player);
    }

    public boolean runCommand(int gameId, Player player) {
        GamesRecord game = plugin.getGame(gameId);
        Result<GameLogsRecord> gameLog = plugin.getGameLog(gameId);
        if ((gameLog == null) || (game == null)) {
            player.sendMessage(String.format("Game with id %d is not in database", gameId));
            return false;
        }
        prepareReplay(game, player);
        replayStartTime = LocalDateTime.now();
        gameStartTime = gameLog.get(0).getTimestamp();
        currentGameTime = gameStartTime;
        BukkitRunnable current = new BukkitRunnable() {
            @Override
            public void run() {
                runReplayRound(gameLog, player);
            }
        };
        plugin.currentReplay = current.runTaskTimer(plugin, 0, updateFrequency);
        return true;
    }

    public void prepareReplay(GamesRecord game, Player player) {
        player.teleport(Bukkit.getWorld("world").getSpawnLocation());
        plugin.listener.deleteReplayWorld();

        // Prepare a new world  TODO: copy a base replay world?
        String worldName = "replay_world";
        WorldCreator creator = new WorldCreator(worldName);
        FlatChunkGenerator chunkGenerator = new FlatChunkGenerator();
        creator.generator(chunkGenerator);
        creator.generateStructures(false);
        World replayWorld = creator.createWorld();

        // load world file
        String scenario = game.getScenario();
        String filename = Paths.get("/de/saar/minecraft/worlds/", scenario + ".csv").toString();
        InputStream in = MinecraftListener.class.getResourceAsStream(filename);

        if (in != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            // First, populate the world
            try {
                chunkGenerator.loadPrebuiltStructure(reader, replayWorld);
                logger.info("Loaded structure: {}", filename);
            } catch (IOException e) {
                logger.error("World file could not be loaded: {} {}", filename, e);
                player.sendMessage("World file could not be loaded");
            }
        } else {
            logger.error("World file could not be found: {}", filename);
            player.sendMessage("World file could not be found");
        }

        // Teleport
        player.teleport(replayWorld.getSpawnLocation());
        plugin.listener.setMovementLocked(true);
    }

    /**
     * Runs one iteration of the game replay
     * @param gameLog all gameLogsRecords for one game
     * @param player the player watching the replay
     */
    public void runReplayRound(Result<GameLogsRecord> gameLog, Player player) {
        LocalDateTime newGameTime =
                gameStartTime.plus((long) (replayStartTime.until(LocalDateTime.now(),
                        MILLIS) * speed), MILLIS);

        // Get all records between currentGameTime and newGameTime
        List<GameLogsRecord> filtered = gameLog.stream()
                .filter((x) -> x.getTimestamp().isAfter(currentGameTime)
                        && x.getTimestamp().isBefore(newGameTime))
                .collect(Collectors.toList());
        currentGameTime = newGameTime;

        // replay set of records in Minecraft
        for (GameLogsRecord record: filtered) {
            String type = record.getMessageType();
            String message = record.getMessage();
            GameLogsDirection direction = record.getDirection();

            World world = player.getWorld();
            switch (type) {
                case "ERROR":
                case "LOG": {
                    player.sendMessage(ChatColor.AQUA + message);
                    if (message.contains("Finished")) {
                        plugin.listener.setMovementLocked(false);
                        Bukkit.getScheduler().cancelTask(plugin.currentReplay.getTaskId());
                        plugin.currentReplay = null;
                        try {
                            String videoPath = VideoRecorder.stop();
                            player.sendMessage("Finished recording: " + videoPath);
                        } catch (MalformedURLException e) {
                            logger.error(e.getMessage());
                        }
                    }
                    // TODO: also check for player logging out (e.g. game 45)
                    break;
                }
                case "StatusMessage": {
                    // TODO: stop being teleported into existing blocks
                    int[] coords = getCoordinates(message);
                    float[] dirs = getDirections(message);
                    Location nextLocation = new Location(world, coords[0], coords[1], coords[2]);
                    nextLocation.setDirection(new Vector(dirs[0], dirs[1], dirs[2]));
                    player.teleport(nextLocation);
                    break;
                }
                case "BlockPlacedMessage": {
                    int[] coords = getCoordinates(message);
                    int materialType = Json.parse(message).asObject().get("type").asInt();
                    Material material = Material.values()[materialType];
                    world.getBlockAt(coords[0], coords[1], coords[2]).setType(material);
                    break;
                }
                case "BlockDestroyedMessage": {
                    int[] coords = getCoordinates(message);
                    world.getBlockAt(coords[0], coords[1], coords[2]).setType(Material.AIR);
                    break;
                }
                case "TextMessage": {
                    JsonObject object = Json.parse(message).asObject();
                    String text = object.get("text").asString();
                    if (text.startsWith("{")) {
                        text = Json.parse(text).asObject().get("message").asString();
                    }
                    if (direction.equals(GameLogsDirection.PassToClient)) {
                        player.sendMessage(ChatColor.WHITE + text);
                    } else {
                        player.sendMessage(ChatColor.YELLOW + text);
                    }
                    break;
                }
                // Ignore Architect logs
                case "BlocksCurrentObjectLeft":
                case "CurrentWorld":
                case "CurrentObject":
                case "NewOrientation":
                case "InitialPlan":
                case "GameId":
                    break;
                default: {
                    logger.warn("Unidentified message {}", type);
                }
            }
        }
    }

    private int[] getCoordinates(String message) {
        JsonObject object = Json.parse(message).asObject();
        int x, y, z;
        if (object.get("x") != null) {
            x = object.get("x").asInt();
        } else {
            x = 0;
        }
        if (object.get("y") != null) {
            y = object.get("y").asInt();
        } else {
            y = 0;
        }
        if (object.get("z") != null) {
            z = object.get("z").asInt();
        } else {
            z = 0;
        }
        return new int[]{x,y,z};
    }

    private float[] getDirections(String message) {
        JsonObject object = Json.parse(message).asObject();
        float x, y, z;
        if (object.get("xDirection") != null) {
            x = object.get("xDirection").asFloat();
        } else {
            x = 0;
        }
        if (object.get("yDirection") != null) {
            y = object.get("yDirection").asFloat();
        } else {
            y = 0;
        }
        if (object.get("zDirection") != null) {
            z = object.get("zDirection").asFloat();
        } else {
            z = 0;
        }
        return new float[]{x,y,z};
    }
}
