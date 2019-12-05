package de.saar.minecraft.worldtest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private static Logger logger = LogManager.getLogger(MinecraftListener.class);
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
        logger.info("World was created and prepared {}", nextWorld.getName());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getDisplayName();
        player.sendMessage("Welcome to the server, " + playerName);

        String structureFile = "artengis";
        String filename = String.format("/de/saar/minecraft/worlds/%s.csv", structureFile);
        InputStream in = MinecraftListener.class.getResourceAsStream(filename);
        if (in != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            try {
                loadPrebuiltStructure(reader, nextWorld);
                logger.info("Loaded structure: {}", filename);
            } catch (IOException e){
                logger.error("World file could not be loaded: {} {}", filename, e);
                player.sendMessage("World file could not be loaded");
                // TODO: notify broker that set-up is wrong
            }
        } else {
            logger.error("World file is not found: {}", filename);
            player.sendMessage("World file is not found");
            // TODO: notify broker that set-up is wrong
        }

        // Teleport player to own world
        Location teleportLocation = nextWorld.getSpawnLocation();
        boolean worked = player.teleport(teleportLocation);
        if (!worked){
            logger.error("Teleportation failed");
            player.sendMessage("Teleportation failed");
            // TODO: notify broker that the player is in the wrong world
        }
        logger.info("Now in world {}", player.getWorld().getName());
        logger.debug("Now at block type: {}", teleportLocation.getBlock().getType());

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
        // Only positive coordinates with chunk size 16
        world.getWorldBorder().setCenter(world.getSpawnLocation());
        world.getWorldBorder().setSize(32);
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
    private void onChunkLoad(ChunkLoadEvent event){
        Chunk chunk = event.getChunk();
        World world = event.getWorld();
        int x = chunk.getX();
        int z = chunk.getZ();
        Location chunkLocation = new Location(world, x, 0, z);
        WorldBorder border = world.getWorldBorder();
        if (!border.isInside(chunkLocation)){
            logger.info("ChunkLoadEvent cancelled {} {}", x, z);
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
        logger.info("Block was placed with type {} {}", block.getType().name(), block.getType().ordinal());

        logger.info("Biome is {}", event.getPlayer().getWorld().getBiome(0,0));
    }

    @EventHandler
    public void onBlockDestroyed(BlockBreakEvent event){
        Block block = event.getBlock();
        Player player = event.getPlayer();
        // Don't destroy the bedrock layer
        if (block.getY() <= 1){  // TODO: add check for type=BEDROCK ?
            event.setCancelled(true);
            player.sendMessage("You cannot destroy this");
            return;
        }
        logger.info("Block was destroyed with type {} {}", block.getType().name(), block.getType().ordinal());
    }

    @EventHandler
    public void onBlockDamaged(BlockDamageEvent event){
        event.setCancelled(true);
    }

    @EventHandler
    public void onWorldLoadEvent(WorldLoadEvent event){
        World world = event.getWorld();
        prepareWorld(world);
        logger.info("World was loaded {}", world.getName());
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event){
        logger.info("Attempted Weather Change to {}", event.toWeatherState());
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



//    @EventHandler  // TODO: events must have a static getHandlerList method to be able to be listened to
//    public void onAllEvents(BlockEvent event){
//        logger.info("There was an " + event.getEventName() + event.toString());
//    }

    /**
     * Reads blocks from a file and creates them in the given world.
     * @param reader: BufferedReader for a csv-file of the line structure: x,y,z,block type name
     * @param world: the world where the structure should be built
     */
    private void loadPrebuiltStructure(BufferedReader reader, World world) throws IOException {
        try (reader) {
            String line;
            while ((line = reader.readLine()) != null) {
                // skip comments
                if (line.startsWith("#")) {
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
                if (newMaterial == null) {
                    logger.error(typeName + " is not a valid Material.");
                    throw new IOException(typeName + " is not a valid Material.");
                } else {
                    location.getBlock().setType(newMaterial);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();  // TODO: log with logger not to standard error
            throw e;
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
                        logger.debug("Adding to save set {}", currentBlock);
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
            logger.info("Saved: {}", line);
        }
        pw.flush();
    }
}

