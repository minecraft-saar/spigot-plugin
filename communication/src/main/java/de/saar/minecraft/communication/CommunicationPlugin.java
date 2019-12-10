package de.saar.minecraft.communication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.Location;


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
