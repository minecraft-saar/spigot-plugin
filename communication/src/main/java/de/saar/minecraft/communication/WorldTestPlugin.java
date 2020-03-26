package de.saar.minecraft.communication;


import org.bukkit.scheduler.BukkitScheduler;


public class WorldTestPlugin extends DefaultPlugin {

    // Fired when plugin is first enabled
    @Override
    public void onEnable() {
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
        }, 0L, 200L);  // One tick happens usually every 0.05 seconds, set later to 2L
    }

}

