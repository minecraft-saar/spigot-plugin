package de.saar.minecraft.replay;

import de.saar.minecraft.communication.FlatChunkGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.io.File;
import java.io.IOException;

public class ReplayListener implements Listener {
    private static Logger logger = LogManager.getLogger(ReplayListener.class);
    private boolean movementLocked = false;
    private ReplayPlugin plugin;

    ReplayListener(ReplayPlugin plugin) {
        this.plugin = plugin;
        World world = Bukkit.getWorld("world");
        world.setThundering(false);
        world.setSpawnFlags(false, false);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setTime(1200);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.NATURAL_REGENERATION, false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        Location location = world.getSpawnLocation();
        world.setBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ(), Biome.PLAINS);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage("Start a replay with '/select <gameid>'");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        deleteReplayWorld();
        this.plugin.currentReplay.cancel();
    }

    public void deleteReplayWorld() {
        String name = "replay_world";
        World world = Bukkit.getWorld(name);

        if (world == null) {
            logger.warn("replay_world not on server");
            return;
        }
        for (Entity entity: world.getEntities()) {
            entity.teleport(Bukkit.getWorld("world").getSpawnLocation());
        }
        boolean isUnloaded = Bukkit.unloadWorld(world, false);
        logger.info("World {} is unloaded: {}", world.getName(), isUnloaded);
        if (!isUnloaded) {
            logger.warn("Could not unload replay world");
            return;
        }

        // Delete files from disk
        File f = new File(name);
        logger.info("Path {}", f.getAbsolutePath());
        try {
            FileUtils.deleteDirectory(f);
            logger.info("deleted");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (movementLocked) {
            event.setCancelled(true);
        }
    }

    public void setMovementLocked(boolean value) {
        movementLocked = value;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        world.setThundering(false);
        world.setSpawnFlags(false, false);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setTime(1200);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.NATURAL_REGENERATION, false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        Location location = world.getSpawnLocation();
        world.setBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ(), Biome.PLAINS);
    }
}
