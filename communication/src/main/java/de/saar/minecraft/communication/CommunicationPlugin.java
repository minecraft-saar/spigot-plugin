package de.saar.minecraft.communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
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
        }, 0L, 200L);  // One tick happens usually every 0.05 seconds, set later to 2L
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

    public void askEvaluation(int gameId) {
        String playerName = client.getPlayernameFromGameId(gameId);
        Player player = getServer().getPlayer(playerName);

        // Get first question from broker
        client.beginEvaluation(gameId);

//        while (true) {// TODO: while Phase is still evaluation
//            // wait until player answered
//            listener.newMessageLatch = new CountDownLatch(1);
//            try {
//                boolean newMessage = listener.newMessageLatch.await(
//                    20, TimeUnit.SECONDS);  // TODO: how long should the players have time to answer?
//            } catch (InterruptedException e) {
//                logger.error(e.getMessage());
//            }
//
//            String answer = listener.currentMessage;
//            listener.currentMessage = "";
//
//
//            client.sendEvaluationAnswer(gameId, answer);
//            // TODO: If answer is valid, ask next question, else repeat question
//        }

    }

}
