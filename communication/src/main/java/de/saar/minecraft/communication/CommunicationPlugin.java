package de.saar.minecraft.communication;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.Location;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class CommunicationPlugin extends JavaPlugin{

    MinecraftClient client;
    private static Logger logger = LogManager.getLogger(CommunicationPlugin.class);

    // Fired when plugin is first enabled
    @Override
    public void onEnable() {
        client = new MinecraftClient("localhost", 2802);
        getServer().getPluginManager().registerEvents(new MinecraftListener(client), this);

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
            boolean isUnloaded = Bukkit.unloadWorld(world, false);  // onWorldUnload is not called because Listener is already disabled
            // Remove only unloaded worlds
            if (isUnloaded) {  // Don't delete files for main world "world"
                // Delete files from disk
                String dirName = world.getName();
                logger.info("world dir {}", dirName);
                File f = new File(dirName);
                logger.info("Path {}", f.getAbsolutePath());
                try {
                    FileUtils.deleteDirectory(f);
                    logger.info("deleted");
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
            logger.info("{} is unloaded: {}", world.getName(), isUnloaded);
        }
    }

    public void getAllPlayerPositions(){
        logger.debug(client.getActiveGames().toString());
        for (Player player: getServer().getOnlinePlayers()){
            String playerName = player.getName();
            int gameId = client.getGameIdForPlayer(playerName);
            Location playerLocation = player.getLocation();
            int xPos = (int)Math.round(playerLocation.getX());
            int yPos = (int)Math.round(playerLocation.getY());
            int zPos = (int)Math.round(playerLocation.getZ());
            String returnMessage = client.sendPlayerPosition(gameId, xPos, yPos, zPos);
            getServer().getPlayer(playerName).sendMessage(returnMessage);  //alternativ: sendRawMessage
        }
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new FlatChunkGenerator();
    }

}
