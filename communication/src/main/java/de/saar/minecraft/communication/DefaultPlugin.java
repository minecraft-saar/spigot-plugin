package de.saar.minecraft.communication;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class DefaultPlugin extends JavaPlugin {
    Client client;
    MinecraftListener listener;
    static Logger logger = LogManager.getLogger(DefaultPlugin.class);
    FileConfiguration config;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.reloadConfig();
        config = getConfig();
        logger.info("Config file {} {}", config.getName(), config.getCurrentPath());
        logger.info("Config {}", config.saveToString());
    }

    /**
     * Gets the locations of all players on the server and sends StatusMessages to the broker.
     */
    public void getAllPlayerPositions() {
        logger.debug(client.getActiveGames().toString());
        for (Player player: getServer().getOnlinePlayers()) {
            String playerName = player.getName();
            int gameId = client.getGameIdForPlayer(playerName);
            // skip players that are not in a game (yet)
            if (gameId == -1) {
                continue;
            }
            Location playerLocation = player.getLocation();
            int xPos = (int)Math.round(playerLocation.getX());
            int yPos = (int)Math.round(playerLocation.getY());
            int zPos = (int)Math.round(playerLocation.getZ());
            Vector direction = player.getEyeLocation().getDirection();
            double xDir = direction.getX();
            double yDir = direction.getY();
            double zDir = direction.getZ();
            client.sendPlayerPosition(gameId, xPos, yPos, zPos, xDir, yDir, zDir);
        }
    }

    /**
     * Displays a given message to a player.
     * @param playerName the name of the player that should receive the message
     * @param message a string message
     */
    public void sendTextMessage(String playerName, String message) {
        Player player = getServer().getPlayer(playerName);
        if (player != null) {
            player.sendMessage(message);
            logger.info("Text message for {}: {}", playerName, message);
        } else {
            logger.warn("Player {} logged out before receiving message {}", playerName, message);
        }
    }

    /**
     * Will kick a player from the server in ca. 10 minutes.
     * @param playerName the username of the player
     */
    public void delayedKickPlayer(String playerName) {
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                Player player = getServer().getPlayer(playerName);
                if (player != null) {
                    player.kickPlayer("You have completed the experiment.");
                }
            }
        }, 12000L);  // = 20 (ticks per second) * 60 (seconds per minute) * 10 (minutes)

    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, String id) {
        return new FlatChunkGenerator();
    }

    // Fired when plugin is disabled
    @Override
    public void onDisable() {
        // Unload remaining worlds
        List<World> remainingWorlds = getServer().getWorlds();
        for (World world: remainingWorlds) {
            listener.deleteWorld(world);
        }
        // Finish all remaining games
        for (int gameId: client.getActiveGames().values()) {
            client.finishGame(gameId);
        }

        // Shut down client
        try {
            client.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
