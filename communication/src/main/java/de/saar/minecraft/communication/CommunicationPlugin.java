package de.saar.minecraft.communication;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;


public class CommunicationPlugin extends JavaPlugin {

    MinecraftClient client;
    MinecraftListener listener;
    private static Logger logger = LogManager.getLogger(CommunicationPlugin.class);

    // Fired when plugin is first enabled
    @Override
    public void onEnable() {
        client = new MinecraftClient("localhost", 2802, this);
        listener = new MinecraftListener(client);
        getServer().getPluginManager().registerEvents(listener, this);

        // to get player position
        BukkitScheduler positionScheduler = getServer().getScheduler();
        positionScheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                getAllPlayerPositions();
            }
        }, 0L, 200L);  // One tick happens usually every 0.05 seconds, set later to 2L
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
    }

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
//            getServer().getPlayer(playerName).sendMessage(returnMessage);
        }
    }


    public void sendTextMessage(String playerName, String message) {
        // TODO: check that player is still online
        getServer().getPlayer(playerName).sendMessage(message);
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new FlatChunkGenerator();
    }

}
