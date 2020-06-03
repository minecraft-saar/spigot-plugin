package de.saar.minecraft.replay;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class ReplayListener implements Listener {
    static Logger logger = LogManager.getLogger(ReplayListener.class);

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // start replay
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        event.setCancelled(true);
    }

}
