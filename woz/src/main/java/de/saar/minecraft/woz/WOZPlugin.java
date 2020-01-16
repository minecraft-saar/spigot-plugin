package de.saar.minecraft.woz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.IOException;
import java.util.List;


public class WOZPlugin extends JavaPlugin {
    private static Logger logger = LogManager.getLogger(WOZPlugin.class);
    private WOZListener listener;
//    private WOZArchitect architect;
    private WOZArchitectServer architectServer;

    // Fired when plugin is first enabled
    @Override
    public void onEnable() {
//        client = new WOZClient("localhost", 2802);
        listener = new WOZListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        architectServer = new WOZArchitectServer(10000, listener);
        try {
            architectServer.start();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
//        architect = new WOZArchitect(5000, listener);


        BukkitScheduler positionScheduler = getServer().getScheduler();
        positionScheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                getUpdate();
            }
        }, 0L, 200L);  // One tick happens usually every 0.05 seconds, set later to 2L
    }

    // Fired when plugin is disabled
    @Override
    public void onDisable() {
        Bukkit.unloadWorld("display_world", false);
    }

    private void getUpdate() {
    }
}
