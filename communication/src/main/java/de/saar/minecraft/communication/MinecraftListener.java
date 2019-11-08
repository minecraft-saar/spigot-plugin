package de.saar.minecraft.communication;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
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
        Player player = event.getPlayer();
        String playerName = player.getDisplayName();
        client.registerGame(playerName);
        player.sendMessage("Welcome to the server, " + playerName);

        // Teleport player to own world
        Location teleportLocation = nextWorld.getSpawnLocation();
        boolean worked = event.getPlayer().teleport(teleportLocation);
        System.out.format("Teleportation worked %b", worked);
        System.out.println("Now in world " + event.getPlayer().getWorld().getName());
        System.out.println("Now at block type: " + teleportLocation.getBlock().getType());

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
        world.setTime(8000);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.NATURAL_REGENERATION, false);
        world.setBiome(0,0, Biome.PLAINS);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        event.getQuitMessage();
        int gameId = client.getGameIdForPlayer(event.getPlayer().getName());  // TODO: which of the player names?
        client.finishGame(gameId);
    }

    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent event){
        Block block = event.getBlock();
        System.out.println("Block was placed with name " + block.getType().name() + " " + block.getType().ordinal());

        Player player = event.getPlayer();
        int gameId = client.getGameIdForPlayer(player.getName());
        String message = client.sendBlockPlaced(gameId, block.getX(), block.getY(), block.getZ(), block.getType().ordinal());
        String[] parts = message.split(":");
        int id = Integer.parseInt(parts[1]);
        Material m = Material.values()[id];
        player.sendMessage(parts[0] + m.toString());
    }

    @EventHandler
    public void onBlockDestroyed(BlockBreakEvent event){
        Block block = event.getBlock();
        System.out.println("Block was destroyed with name " + block.getType().name() + " " + block.getType().ordinal());

        Player player = event.getPlayer();
        int gameId = client.getGameIdForPlayer(player.getName());
        String message = client.sendBlockDestroyed(gameId, block.getX(), block.getY(), block.getZ(), block.getType().ordinal());
        System.out.println(message);
        String[] parts = message.split(":");
        int id = Integer.parseInt(parts[1]);
        Material m = Material.values()[id];
        player.sendMessage(parts[0] + m.toString());
    }

    // TODO: what if block is not broken but just damaged?
    @EventHandler
    public void onBlockDamaged(BlockDamageEvent event){
        event.setCancelled(true);
    }

    @EventHandler
    public void onWorldLoadEvent(WorldLoadEvent event){
        World world = event.getWorld();
        prepareWorld(world);
        System.out.println("World was loaded " + world.getName());
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event){
        System.out.println("Attempted Weather Change to " + event.toWeatherState());
        if (event.toWeatherState()) {  // would change to raining, thunder is already disabled
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onStructureGrow(StructureGrowEvent event){
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerGameModeChangeEvent (PlayerGameModeChangeEvent event){
        event.getPlayer().setGameMode(GameMode.CREATIVE);
    }
}
