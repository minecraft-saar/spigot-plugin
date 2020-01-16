package de.saar.minecraft.woz;

import de.saar.minecraft.architect.Architect;
import de.saar.minecraft.shared.*;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Material;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class WOZArchitect implements Architect {
    private int waitTime;
    private static Logger logger = LogManager.getLogger(WOZArchitect.class);
    private WOZListener listener;

    public WOZArchitect(int waitTime, WOZListener listener) {
        this.waitTime = waitTime;
        this.listener = listener;
    }

    @Override
    public void initialize(WorldSelectMessage request) {
        String worldName = request.getName();
        logger.info("Got world " + worldName);
        listener.loadWorld(worldName);
        listener.player.sendMessage("You can begin giving instructions.");
    }

    @Override
    public void handleStatusInformation(StatusMessage request, StreamObserver<TextMessage> responseObserver) {
        int gameId = request.getGameId();
        int x = request.getX();
        int y = request.getY();
        int z = request.getZ();
        double xDir = request.getXDirection();
        double yDir = request.getYDirection();
        double zDir = request.getZDirection();

        // spawn a thread for a long-running computation
        new Thread() {
            @Override
            public void run() {
                //TODO
                String text = "ok";
                TextMessage mText = TextMessage.newBuilder().setGameId(gameId).setText(text).build();

                // delay for a bit
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // send the text message back to the client
                responseObserver.onNext(mText);
                responseObserver.onCompleted();
            }
        }.start();
    }

    @Override
    public void handleBlockPlaced(BlockPlacedMessage request, StreamObserver<TextMessage> responseObserver) {
        int gameId = request.getGameId();
        int x = request.getX();
        int y = request.getY();
        int z = request.getZ();
        int type = request.getType();

        // spawn a thread for a long-running computation
        new Thread() {
            @Override
            public void run() {
                Material material = Material.values()[type];
                listener.displayWorld.getBlockAt(x,y,z).setType(material);
                listener.player.sendMessage("Next instruction: ");

                // delay for a bit, so the wizard has time to type
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // TODO: wait for the wizard to hit enter, then continue. If they don't hit enter in x seconds send empty message

                String text = String.join(". ", listener.savedMessages);
                listener.savedMessages.clear();

//                String text = String.format("A block was just placed at %d-%d-%d :%d", x, y, z, type);
                TextMessage mText = TextMessage.newBuilder().setGameId(gameId).setText(text).build();

                // send the text message back to the client
                responseObserver.onNext(mText);
                responseObserver.onCompleted();
            }
        }.start();
    }

    @Override
    public void handleBlockDestroyed(BlockDestroyedMessage request,
                                     StreamObserver<TextMessage> responseObserver) {
        int gameId = request.getGameId();
        int x = request.getX();
        int y = request.getY();
        int z = request.getZ();
        int type = request.getType();

        // spawn a thread for a long-running computation
        new Thread() {
            @Override
            public void run() {
                String text = String.format("A block was just destroyed at %d-%d-%d :%d", x, y, z, type);
                TextMessage mText = TextMessage.newBuilder().setGameId(gameId).setText(text).build();

                // delay for a bit
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // send the text message back to the client
                responseObserver.onNext(mText);
                responseObserver.onCompleted();
            }
        }.start();
    }

    @Override
    public String getArchitectInformation() {
        return "WOZArchitect";
    }
}
