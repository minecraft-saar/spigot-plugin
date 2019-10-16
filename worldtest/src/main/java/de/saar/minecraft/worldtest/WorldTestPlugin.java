package de.saar.minecraft.worldtest;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class WorldTestPlugin extends JavaPlugin{

    // Fired when plugin is first enabled
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new MinecraftListener(), this);
        // to get player position
        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
//                    getAllPlayerPositions();
            }
        }, 0L, 20L);  // One tick happens usually every 0.05 seconds, set later to 2L
    }

    // Fired when plugin is disabled
    @Override
    public void onDisable() {

    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new FlatChunkGenerator();
    }


    }
