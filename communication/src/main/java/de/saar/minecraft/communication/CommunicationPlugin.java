package de.saar.minecraft.communication;

import java.util.List;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitScheduler;


public class CommunicationPlugin extends DefaultPlugin {
    MinecraftListener listener;

    // Fired when plugin is first enabled
    @Override
    public void onEnable() {
        client = new MinecraftClient("localhost", 2802, this);
        listener = new MinecraftListener(client);
        getServer().getPluginManager().registerEvents(listener, this);

        // to get player position
        BukkitScheduler positionScheduler = getServer().getScheduler();
        positionScheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                getAllPlayerPositions();
            }
        }, 0L, 4L);  // One tick happens usually every 0.05 seconds, set later to 2L
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
        for (int gameId: client.getActiveGames().values()) {
            client.finishGame(gameId);
        }
    }

}
