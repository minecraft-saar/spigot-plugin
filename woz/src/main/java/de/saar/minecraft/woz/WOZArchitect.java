package de.saar.minecraft.woz;

import de.saar.minecraft.architect.Architect;
import de.saar.minecraft.shared.*;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Material;


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
                listener.movePlayer(x,y,z,xDir,yDir,zDir);
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
                TextMessage mText = TextMessage.newBuilder().setGameId(gameId).setText(text).build();

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
                listener.placeBlock(x,y,z,material);
                logger.info("{} block placed at {}-{}-{}", material, x, y, z);
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

                TextMessage mText = TextMessage.newBuilder().setGameId(gameId).setText(text).build();
                logger.info("Send message: " + text);

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
                listener.breakBlock(x, y, z);

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

                TextMessage mText = TextMessage.newBuilder().setGameId(gameId).setText(text).build();

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
