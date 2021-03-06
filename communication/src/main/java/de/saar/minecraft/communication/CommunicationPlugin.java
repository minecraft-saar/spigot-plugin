package de.saar.minecraft.communication;

import java.util.List;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitScheduler;


public class CommunicationPlugin extends DefaultPlugin {
    MinecraftListener listener;

    // Fired when plugin is first enabled
    @Override
    public void onEnable() {
        super.onEnable();
        // Get config values
        final long updateFrequency = config.getLong("updateFrequency", 4L);
        int clientPort = config.getInt("clientPort", 2802);

        client = new MinecraftClient("localhost", clientPort, this);
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

    // Fired when plugin is disabled
    @Override
    public void onDisable() {
        // Unload remaining worlds
        List<World> remainingWorlds = getServer().getWorlds();
        for (World world: remainingWorlds) {
            listener.deleteWorld(world);
        }
        // Finish all remaining games
        Integer[] runningGames = client.getActiveGames().values().toArray(new Integer[]{});
        for (int gameId: runningGames) {
            client.finishGame(gameId);
        }
    }

}
