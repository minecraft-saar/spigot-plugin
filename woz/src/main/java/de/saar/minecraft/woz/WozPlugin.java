package de.saar.minecraft.woz;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;


public class WozPlugin extends JavaPlugin {
    private static Logger logger = LogManager.getLogger(WozPlugin.class);
    private WozListener listener;
    private WozArchitectServer architectServer;

    // Fired when plugin is first enabled
    @Override
    public void onEnable() {
        listener = new WozListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        architectServer = new WozArchitectServer(10000, listener);
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
