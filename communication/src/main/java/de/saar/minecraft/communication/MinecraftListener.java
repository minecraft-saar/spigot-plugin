package de.saar.minecraft.communication;

import org.bukkit.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.HashMap;

public class MinecraftListener implements Listener {

    MinecraftClient client;
    WorldCreator creator;
    World nextWorld;  // Preloaded world for the next joining player

    HashMap<String, World> activeWorlds = new HashMap<String, World>();

    MinecraftListener(MinecraftClient client) {
        super();
        if (client == null){
            throw new RuntimeException("No client was passed to the Listener");
        }
        this.client = client;

        creator = new WorldCreator("playerworld_0");
        creator.generator(new FlatChunkGenerator());
        creator.generateStructures(false);
        nextWorld = creator.createWorld();
        prepareWorld(nextWorld);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getDisplayName();
        client.registerGame(playerName);
        Bukkit.broadcastMessage("Welcome to the server, " + playerName);

        // Teleport player to own world
        Location teleportLocation = nextWorld.getSpawnLocation();
        boolean worked = event.getPlayer().teleport(teleportLocation);
        System.out.format("Teleportation worked %b", worked);
        System.out.println("Second world " + event.getPlayer().getWorld().getName());
        Bukkit.broadcastMessage("Welcome to the server, " + playerName);

        // Add world to active worlds
        activeWorlds.put(nextWorld.getName(), nextWorld);

        // Create new preloaded world for the next player
        String worldName = "playerworld_" + activeWorlds.size();
        creator = new WorldCreator(worldName);
        creator.generator(new FlatChunkGenerator());
        creator.generateStructures(false);
        nextWorld = creator.createWorld();
    }

    /**
     * Sets all world settings to peaceful
     * @param world
     */
    private void prepareWorld(World world){
        world.setThundering(false);
        world.setSpawnFlags(false, false);
        world.setDifficulty(Difficulty.PEACEFUL);
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
    public void onWorldLoadEvent​(WorldLoadEvent event){
        World world = event.getWorld();
        prepareWorld(world);
        Bukkit.broadcastMessage("World loaded " + world.getName());
        System.out.println("World was loaded " + world.getName());
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event){
        if (event.toWeatherState()) {  // would change to raining, thunder is already disabled
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
