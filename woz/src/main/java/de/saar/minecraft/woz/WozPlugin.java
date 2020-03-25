package de.saar.minecraft.woz;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
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
        architectServer = new WozArchitectServer(10001, listener);
        try {
            architectServer.start();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    // Fired when plugin is disabled
    @Override
    public void onDisable() {
        String worldName = "display_world";
        Bukkit.unloadWorld(worldName, false);
        File f = new File(worldName);
        logger.info("Path {}", f.getAbsolutePath());
        try {
            FileUtils.deleteDirectory(f);
            logger.info("deleted");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

}
