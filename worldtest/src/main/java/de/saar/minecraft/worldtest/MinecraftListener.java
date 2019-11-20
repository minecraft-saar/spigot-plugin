package de.saar.minecraft.worldtest;

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
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;


import java.io.*;
import java.util.HashMap;


public class MinecraftListener implements Listener {
    WorldCreator creator;
    World nextWorld;  // Preloaded world for the next joining player

    HashMap<String, World> activeWorlds = new HashMap<String, World>();

    public MinecraftListener(){
        super();

        creator = new WorldCreator("playerworld_0");
        creator.generator(new FlatChunkGenerator());
        creator.generateStructures(false);
        nextWorld = creator.createWorld();
        prepareWorld(nextWorld);

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getDisplayName();
        System.out.println("First world " + event.getPlayer().getWorld().getName());

        Location teleportLocation = nextWorld.getSpawnLocation();
        boolean worked = event.getPlayer().teleport(teleportLocation);
        System.out.format("Teleportation worked %b", worked);
        System.out.println("Second world " + event.getPlayer().getWorld().getName());

        // Add world to active worlds
        activeWorlds.put(nextWorld.getName(), nextWorld);

        // Create new preloaded world for the next player
        String worldName = "playerworld_" + activeWorlds.size();
        creator = new WorldCreator(worldName);
//        World templateWorld = event.getPlayer().getServer().getWorld("playerworld_1");
//        System.out.println("template world " + templateWorld.toString());
//        creator.copy(templateWorld);
        creator.generator(new FlatChunkGenerator());
        creator.generateStructures(false);
        nextWorld = creator.createWorld();

        System.out.println(Material.BLUE_WOOL);
        System.out.println(teleportLocation.getBlock().getType());
    }

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
        buildCube(new Location(world, 2, 2, 2));
        loadPrebuiltStructure("/home/ca/Documents/Hiwi_Minecraft/spigot-plugin/worldtest/src/main/resources/prebuild_structures/orange_cube.csv", world);
    }

    @EventHandler
    private void onChunkLoad(ChunkLoadEvent event){
        Chunk chunk = event.getChunk();
        World world = event.getWorld();
        int x = chunk.getX();
        int z = chunk.getZ();
        Location chunkLocation = new Location(world, x, 0, z);
        WorldBorder border = world.getWorldBorder();
        if (!border.isInside(chunkLocation)){
            System.out.println("ChunkLoadEvent cancelled " + x + z);
            chunk.unload();
        }
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        event.getQuitMessage();
    }

    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent event){
        Block b = event.getBlock();
        Material m = event.getBlock().getType();
        String name = m.name();
        int number = m.ordinal();
        System.out.println("Block was placed with name " + name);
        System.out.format("Block was placed with number %d", number);
        System.out.println("b.getState().getData().toString() " + b.getState().getData().toString());
        System.out.print("m.toString() " + m.toString());
        System.out.println("m.data " + m.data);

        System.out.println("Biome is " + event.getPlayer().getWorld().getBiome(0,0));
    }

    @EventHandler
    public void onBlockDestroyed(BlockBreakEvent event){
        Block block = event.getBlock();
        Player player = event.getPlayer();
        // Don't destroy the bedrock layer
        if (block.getY() <= 1){
            event.setCancelled(true);
            player.sendMessage("You cannot destroy this");
            return;
        }
        System.out.println("Block was destroyed with name " + block.getType().name() + " " + block.getType().ordinal());
    }

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
        if (event.toWeatherState()) {  // would change to raining, TODO: does thunder count as raining?
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

    @EventHandler
    public void onWorldSave(WorldSaveEvent event){
        if (event.getWorld().getName().startsWith("playerworld_")){
            // TODO cancel
            System.out.println(event.toString() + " should be cancelled");
        }
    }


//    @EventHandler  // TODO: events must have a static getHandlerList method to be able to be listened to
//    public void onAllEvents(BlockEvent event){
//        System.out.println("There was an " + event.getEventName() + event.toString());
//    }

    private void buildCube(Location location){
        location.getBlock().setType(Material.BLUE_WOOL);
    }

    private void loadPrebuiltStructure(String filename, World world){
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null) {
                // skip comments
                if (line.startsWith("#")){
                    continue;
                }
                // use comma as separator
                String[] blockInfo = line.split(",");
                int x = Integer.parseInt(blockInfo[0]);
                int y = Integer.parseInt(blockInfo[1]);
                int z = Integer.parseInt(blockInfo[2]);
                String typeName = blockInfo[3];
                // int type = Integer.parseInt(blockInfo[3]);

                Location location = new Location(world, x, y, z);
                location.getBlock().setType(Material.getMaterial(typeName));

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}

