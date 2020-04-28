package de.saar.minecraft.communication;


import org.bukkit.scheduler.BukkitScheduler;


public class WorldTestPlugin extends DefaultPlugin {

    // Fired when plugin is first enabled
    @Override
    public void onEnable() {
        super.onEnable();
        long updateFrequency = config.getLong("updateFrequency", 4L);
        logger.info("Update Frequency {}", updateFrequency);
        client = new DummyMinecraftClient(this);
        listener = new MinecraftListener(client, this);
        getServer().getPluginManager().registerEvents(listener, this);
        // to get player position
        BukkitScheduler positionScheduler = getServer().getScheduler();
        positionScheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                getAllPlayerPositions();
            }
        }, 0L, updateFrequency);  // One tick happens usually every 0.05 seconds
    }

}

