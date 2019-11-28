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
import java.util.HashSet;


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
        Player player = event.getPlayer();
        String playerName = player.getDisplayName();
        player.sendMessage("Welcome to the server, " + playerName);

        String structureFile = "bridge";
//        String filename = "/prebuilt_structures/" + structureFile + ".csv";
        String filename = "/resources/prebuilt_structures/" + structureFile + ".csv";
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);

        try {
            System.out.println(getClass().getResource(filename).toURI());
            File file = new File(getClass().getResource(filename).toURI());
            System.out.println(file);

        } catch (Exception e){
            System.out.println(e);
        }
        //InputStream in = getClass().getResourceAsStream(filename);
        System.out.println("Inputstream " + in);
        InputStream in2 = getClass().getClassLoader().getResourceAsStream(filename);
        System.out.println("Inputstream 2 " + in2);
        if (in != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            loadPrebuiltStructure(reader, nextWorld);
        }

        // Teleport player to own world
        Location teleportLocation = nextWorld.getSpawnLocation();
        boolean worked = event.getPlayer().teleport(teleportLocation);
        System.out.format("Teleportation worked %b", worked);
        System.out.println("Now in world " + player.getWorld().getName());
        System.out.println("Now at block type: " + teleportLocation.getBlock().getType());

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

        // Set initial blue block as orientation for planner and NLG
        Location anchor = new Location(world,2,2,2);
        anchor.getBlock().setType(Material.BLUE_WOOL);



        //loadPrebuiltStructure("/home/ca/Documents/Hiwi_Minecraft/spigot-plugin/worldtest/src/main/resources/prebuilt_structures/orange_cube.csv", world);
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
        String filename = "saved_structure_" + event.getPlayer().getName() + System.currentTimeMillis() + ".csv";
        try {
            saveBuiltStructure(filename, event.getPlayer().getWorld());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent event){
        Block block = event.getBlock();
        System.out.println("Block was placed with name " + block.getType().name() + " " + block.getType().ordinal());

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

//    @EventHandler
//    public void onWorldSave(WorldSaveEvent event){
//        if (event.getWorld().getName().startsWith("playerworld_")){
//            // TODO cancel
//            System.out.println(event.toString() + " should be cancelled");
//        }
//    }


//    @EventHandler  // TODO: events must have a static getHandlerList method to be able to be listened to
//    public void onAllEvents(BlockEvent event){
//        System.out.println("There was an " + event.getEventName() + event.toString());
//    }

    private void buildCube(Location location){
        location.getBlock().setType(Material.BLUE_WOOL);
    }

    /**
     * Reads blocks from a file and creates them in the given world.
     * @param reader: BufferedReader for a csv-file of the line structure: x,y,z,block type name
     * @param world: the world where the structure should be build
     */
    private void loadPrebuiltStructure(BufferedReader reader, World world){
        try {
            String line;
            while ((line = reader.readLine()) != null) {
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
                Material newMaterial = Material.getMaterial(typeName);
                if (newMaterial == null){
                    System.out.println(typeName + " is not a valid Material. Skipped.");
                } else {
                    location.getBlock().setType(newMaterial);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Reads blocks from a file and creates them in the given world.
     * @param filename: csv-file of the line structure: x,y,z,block type name
     * @param world: the world where the structure should be build
     */
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
                Material newMaterial = Material.getMaterial(typeName);
                if (newMaterial == null){
                    System.out.println(typeName + " is not a valid Material. Skipped.");
                } else {
                    location.getBlock().setType(newMaterial);
                }
            }

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

    /**
     * Saves all non-air blocks above the ground from a world to a csv-file.
     * @param filename: csv-file where the blocks should be saved
     * @param world:
     * @throws FileNotFoundException
     */
    private void saveBuiltStructure(String filename, World world) throws FileNotFoundException {
        WorldBorder border = world.getWorldBorder();
        HashSet<Block> toSave = new HashSet<>();
        Location center = border.getCenter();
        int radius = ((Double)border.getSize()).intValue();  // TODO how to round here?

        // Loop over height until there are only air blocks
        boolean foundSolid = true;
        int y = 1; // Upmost ground layer
        while (foundSolid){
            foundSolid = false;
            y++;
            // Loop over every block in this plain
            for (int x = center.getBlockX() - radius; x <= center.getBlockX() + radius; x++){
                for (int z = center.getBlockZ() - radius; z <= center.getBlockZ() + radius; z++){
                    Block currentBlock = world.getBlockAt(x,y,z);
                    if (!currentBlock.getType().isAir()){
                        toSave.add(currentBlock);
                        foundSolid = true;
                        System.out.println(currentBlock);
                    }
                }
            }
        }
        // Save blocks
        File csvOutputFile = new File(filename);
        PrintWriter pw = new PrintWriter(csvOutputFile);
        for (Block block:toSave){
            String line = String.format("%d,%d,%d,", block.getX(), block.getY(), block.getZ()) + block.getType().name();
            pw.println(line);
            System.out.println(line);
        }
        pw.flush();
    }
}

