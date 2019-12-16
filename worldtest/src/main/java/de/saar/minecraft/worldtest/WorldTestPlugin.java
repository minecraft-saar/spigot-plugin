package de.saar.minecraft.worldtest;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class WorldTestPlugin extends JavaPlugin{
    private static Logger logger = LogManager.getLogger(WorldTestPlugin.class);

    // Fired when plugin is first enabled
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new MinecraftListener(), this);
        // to get player position
        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                resetTime();
            }
        }, 0L, 1000L);
    }

    // Fired when plugin is disabled
    @Override
    public void onDisable() {
        // Unload remaining worlds
        List<World> remainingWorlds = getServer().getWorlds();
        for (World world: remainingWorlds){
            boolean isUnloaded = Bukkit.unloadWorld(world, false);  // onWorldUnload is not called because Listener is already disabled
            // Remove only unloaded worlds
            if (isUnloaded) {
                // Delete files from disk
                String dirName = world.getName();
                logger.info("world dir {}", dirName);
                File f = new File(dirName);
                logger.info("Path {}", f.getAbsolutePath());
                try {
//                FileUtils.cleanDirectory(f);
                    FileUtils.deleteDirectory(f);
                    logger.info("deleted");
                } catch (IOException e){
                    logger.error(e.getMessage());
                }
            }

            logger.info("{} is unloaded: {}", world.getName(), isUnloaded);
        }

    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new FlatChunkGenerator();
    }


    private void resetTime(){
        Collection<Player> players = (Collection<Player>) getServer().getOnlinePlayers();
        for (Player p: players){
            long time = p.getWorld().getTime();
            if (time < 7000 || time > 17000) {  // sunset begins at 17:37
                p.getWorld().setTime(7000);
            }
        }
    }
    }
