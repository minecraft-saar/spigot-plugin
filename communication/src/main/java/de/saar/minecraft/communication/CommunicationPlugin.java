package de.saar.minecraft.communication;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.Location;

import java.util.Map;

public class CommunicationPlugin extends JavaPlugin{

    MinecraftClient client;

    // Fired when plugin is first enabled
    @Override
    public void onEnable() {
        System.out.println("Trying to enable CommunicationPlugin");
        client = new MinecraftClient("localhost", 2802);
        getServer().getPluginManager().registerEvents(new MinecraftListener(client), this);

        // to get player position
        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                getAllPlayerPositions();
            }
        }, 0L, 20L);  // One tick happens usually every 0.05 seconds, set later to 2L
    }

    // Fired when plugin is disabled
    @Override
    public void onDisable() {

    }

    public Location getPlayerPosition(String playerName){
        Location playerLocation = getServer().getPlayer(playerName).getLocation();
        return playerLocation;
    }

    public void getAllPlayerPositions(){
        for (Map.Entry<String, Integer> entry : client.activeGames.entrySet()){
            String playerName = entry.getKey();
            int gameId = entry.getValue();
            Location playerLocation = getServer().getPlayer(playerName).getLocation();
            int xPos = (int)Math.round(playerLocation.getX());
            int yPos = (int)Math.round(playerLocation.getY());
            int zPos = (int)Math.round(playerLocation.getZ());
            //System.out.format("Player at position %d - %d - %d ", xPos, yPos, zPos );
            String returnMessage = client.sendPlayerPosition(gameId, xPos, yPos, zPos);
            getServer().getPlayer(playerName).sendMessage(returnMessage);  //alternativ: sendRawMessage
        }
    }


}
