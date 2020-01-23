package de.saar.minecraft.woz;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;


public class WOZPlugin extends JavaPlugin {
    private static Logger logger = LogManager.getLogger(WOZPlugin.class);
    private WOZListener listener;
    private WOZArchitectServer architectServer;

    // Fired when plugin is first enabled
    @Override
    public void onEnable() {
        listener = new WOZListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        architectServer = new WOZArchitectServer(10000, listener);
        try {
            architectServer.start();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    // Fired when plugin is disabled
    @Override
    public void onDisable() {
        Bukkit.unloadWorld("display_world", false);
    }

}
