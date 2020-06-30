package de.saar.minecraft.replay;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
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
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.MILLIS;

public class SelectGameCommand implements CommandExecutor {
    private final ReplayPlugin plugin;
    private static final Logger logger = LogManager.getLogger(SelectGameCommand.class);
    private BukkitRunnable current;

    public SelectGameCommand(ReplayPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (! (sender instanceof Player)) {
            // TODO: find player or reject command
            return false;
        }
        Player player = (Player) sender;
        // stop old replays
        if (current != null) {
            current.cancel();
            logger.info("is cancelled {}", current.isCancelled());
            int taskid = current.getTaskId();
            Bukkit.getServer().getScheduler().cancelTask(taskid);
            logger.info("is cancelled {}", current.isCancelled());
        }

        int gameId = Integer.parseInt(args[0]);
        GamesRecord game = plugin.getGame(gameId);
        Result<GameLogsRecord> gameLog = plugin.getGameLog(gameId);
        if ((gameLog == null) || (game == null)) {
            sender.sendMessage(String.format("Game with id %d is not in database", gameId));
            return false;
        }
        prepareReplay(game, player);

        current = new BukkitRunnable() {
            @Override
            public void run() {
                startReplay(gameLog, player);
            }
        };
        current.runTaskAsynchronously(plugin);
        return true;
    }

    public void prepareReplay(GamesRecord game, Player player) {
        player.teleport(Bukkit.getWorld("world").getSpawnLocation());
        plugin.listener.deleteReplayWorld();
        // Prepare a new world
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

    private void execLater(Runnable task) {
        var scheduler = plugin.getServer().getScheduler();
        scheduler.scheduleSyncDelayedTask(plugin, task, 0);
    }

    public void startReplay(Result<GameLogsRecord> gameLog, Player player) {
        LocalDateTime oldTimestamp = gameLog.get(0).getTimestamp();
        for (GameLogsRecord record: gameLog) {
            String type = record.getMessageType();
            String message = record.getMessage();
            LocalDateTime timestamp = record.getTimestamp();
            GameLogsDirection direction = record.getDirection();

            // TODO: switch between mode automatic and only next message on click
            if (type.equals("StatusMessage")) {
                // wait for the timestamp difference TODO: check difference
                long difference = oldTimestamp.until(timestamp, MILLIS);
                oldTimestamp = timestamp;
//                difference = Long.max(difference - 20, 0);  // Each tick has 20 ms
                difference = Long.max(difference, 0);  // Each tick has 20 ms
                try {
                    Thread.sleep(difference);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }
            }

            execLater(() -> {
                World world = player.getWorld();
                switch (type) {
                    case "LOG": {
                        player.sendMessage(ChatColor.AQUA + message);
                        break;
                    }
                    case "StatusMessage": {
                        // TODO: stop being teleported into existing blocks
                        JsonObject object = Json.parse(message).asObject();
                        int x;
                        if (object.get("x") != null) {
                            x = object.get("x").asInt();
                        } else {
                            x = 0;
                        }
                        int y = object.get("y").asInt();
                        int z = object.get("z").asInt();
                        float xDirection;
                        if (object.get("xDirection") != null) {
                            xDirection = object.get("xDirection").asFloat();
                        } else {
                            xDirection = 0;
                        }
                        float yDirection;
                        if (object.get("yDirection") != null) {
                            yDirection = object.get("yDirection").asFloat();
                        } else {
                            // In the beginning after game is created, some yDirection values are missing
                            yDirection = 0;
                        }
                        float zDirection = object.get("zDirection").asFloat();
                        Location nextLocation = new Location(world, x, y, z);
                        nextLocation.setDirection(new Vector(xDirection, yDirection, zDirection));
                        player.teleport(nextLocation);
                        break;
                    }
                    case "BlockPlacedMessage": {
                        JsonObject object = Json.parse(message).asObject();
                        int x = object.get("x").asInt();
                        int y = object.get("y").asInt();
                        int z = object.get("z").asInt();
                        Material material = Material.values()[object.get("type").asInt()];
                        world.getBlockAt(x, y, z).setType(material);
                        break;
                    }
                    case "BlockDestroyedMessage": {
                        JsonObject object = Json.parse(message).asObject();
                        int x = object.get("x").asInt();
                        int y = object.get("y").asInt();
                        int z = object.get("z").asInt();
                        world.getBlockAt(x, y, z).setType(Material.AIR);
                        break;
                    }
                    case "TextMessage": {
                        // TODO: distinguish between direction
                        JsonObject object = Json.parse(message).asObject();
                        String text = object.get("text").asString();
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
            });
        }
        plugin.listener.setMovementLocked(false);
    }
}
