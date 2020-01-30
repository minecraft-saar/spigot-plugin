package de.saar.minecraft.communication;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class DefaultPlugin extends JavaPlugin {
    Client client;
    MinecraftListener listener;
    private static Logger logger = LogManager.getLogger(DefaultPlugin.class);

    /**
     * Gets the locations of all players on the server and sends StatusMessages to the broker.
     */
    public void getAllPlayerPositions() {
        logger.debug(client.getActiveGames().toString());
        for (Player player: getServer().getOnlinePlayers()) {
            String playerName = player.getName();
            int gameId = client.getGameIdForPlayer(playerName);
            Location playerLocation = player.getLocation();
            int xPos = (int)Math.round(playerLocation.getX());
            int yPos = (int)Math.round(playerLocation.getY());
            int zPos = (int)Math.round(playerLocation.getZ());
            Vector direction = player.getEyeLocation().getDirection();
            double xDir = direction.getX();
            double yDir = direction.getY();
            double zDir = direction.getZ();
            System.out.println(direction);
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
        } else {
            logger.info("Player {} logged out before receiving message {}", playerName, message);
        }
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
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
