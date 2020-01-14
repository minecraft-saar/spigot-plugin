package de.saar.minecraft.woz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.List;


public class WOZPlugin extends JavaPlugin {
    private static Logger logger = LogManager.getLogger(WOZPlugin.class);
//    private WOZClient client;
    private WOZListener listener;
    private WOZArchitect architect;

    // Fired when plugin is first enabled
    @Override
    public void onEnable() {
//        client = new WOZClient("localhost", 2802);
        listener = new WOZListener();
        getServer().getPluginManager().registerEvents(listener, this);
        architect = new WOZArchitect(5000, listener);


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

    }

    private void getUpdate() {
    }
}
