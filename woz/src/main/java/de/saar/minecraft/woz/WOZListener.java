package de.saar.minecraft.woz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class WOZListener implements Listener {

    private static Logger logger = LogManager.getLogger(WOZListener.class);
//    WOZClient client;
    World displayWorld;
    Player player;
    boolean active = false;
    ArrayList<String> savedMessages = new ArrayList<>();


    WOZListener() {
        WorldCreator creator = new WorldCreator("display_world");
        creator.generator(new FlatChunkGenerator());
        creator.generateStructures(false);
        displayWorld = creator.createWorld();
        prepareWorld(displayWorld);
    }

    // TODO add dependency on communication
    public void prepareWorld(World world){
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
    public void onPlayerJoin(PlayerJoinEvent event){
        // TODO notify when cancelling
        if (active) {
            event.getPlayer().kickPlayer("There is already a player on this server");
        }
        player = event.getPlayer();
        Location teleportLocation = displayWorld.getSpawnLocation();
        if (player.teleport(teleportLocation)){
            player.sendMessage("Start");
        } else {
            player.sendMessage("Teleportation failed");
        }
        active = true;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        active = false;
    }

    @EventHandler
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        event.getPlayer().sendMessage("You cannot place blocks");
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockDamageEvent(BlockDamageEvent event) {
        event.getPlayer().sendMessage("You cannot damage blocks");
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreakEvent(BlockBreakEvent event) {
        event.getPlayer().sendMessage("You cannot break blocks");
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        event.getPlayer().sendMessage("You cannot walked around");
        event.setCancelled(true);
    } //TODO: can a player turn their head without moving?

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event){
        String message = event.getMessage();
        savedMessages.add(message);
    }

    boolean loadWorld(String worldName){
        String filename = String.format("/de/saar/minecraft/worlds/%s.csv", worldName);
        InputStream in = WOZArchitect.class.getResourceAsStream(filename);
        if (in != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            try {
                loadPrebuiltStructure(reader, displayWorld);
                logger.info("Loaded structure: {}", filename);
            } catch (IOException e){
                logger.error("World file could not be loaded: {} {}", filename, e);
//                player.sendMessage("World file could not be loaded");
                return false;
            }
        } else {
            logger.error("World file is not found: {}", filename);
//            player.sendMessage("World file is not found");
            return false;
        }
        return true;
    }

    /**
     * Reads blocks from a file and creates them in the given world.
     * @param reader: BufferedReader for a csv-file of the line structure: x,y,z,block type name
     * @param world: the world where the structure should be built
     * @throws IOException
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

                Location location = new Location(world, x, y, z);
                Material newMaterial = Material.getMaterial(typeName);
                if (newMaterial == null) {
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
}


