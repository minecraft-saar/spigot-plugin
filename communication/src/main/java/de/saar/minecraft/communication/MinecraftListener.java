package de.saar.minecraft.communication;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldInitEvent;

public class MinecraftListener implements Listener {

    MinecraftClient client;

    MinecraftListener(MinecraftClient client) {
        super();
        if (client == null){
            throw new RuntimeException("No client was passed to the Listener");
        }
        this.client = client;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getDisplayName();
        client.registerGame(playerName);
        Bukkit.broadcastMessage("Welcome to the server, " + playerName);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        event.getQuitMessage();
        int gameId = client.getGameIdForPlayer(event.getPlayer().getName());  // TODO: which of the player names?
        client.finishGame(gameId);
    }

    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent event){

    }

    @EventHandler
    public void onBlockDestroyed(BlockDamageEvent event){

    }

    @EventHandler
    public void onWorldInitEvent​(WorldInitEvent event){
        Bukkit.broadcastMessage("World was initialized");
        // disable thunder
        event.getWorld().setThundering(false);
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event){
        if (event.toWeatherState()) {  // would change to raining, TODO: does thunder count as raining
            event.setCancelled(true);
        }

        //event.getWorld().setWeatherDuration(0);
        //
    }

    @EventHandler
    public void onStructureGrow(StructureGrowEvent event){
        event.setCancelled(true);
    }
}
