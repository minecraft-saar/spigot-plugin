package de.saar.minecraft.worldtest;

import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Collection;

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
                resetTime();
            }
        }, 0L, 1000L);
    }

    // Fired when plugin is disabled
    @Override
    public void onDisable() {

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
