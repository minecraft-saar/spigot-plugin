package de.saar.minecraft.woz;

import de.saar.minecraft.architect.Architect;
import de.saar.minecraft.shared.BlockDestroyedMessage;
import de.saar.minecraft.shared.BlockPlacedMessage;
import de.saar.minecraft.shared.StatusMessage;
import de.saar.minecraft.shared.TextMessage;
import de.saar.minecraft.shared.WorldSelectMessage;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Material;


public class WozArchitect implements Architect {
    private int waitTime;
    private WozListener listener;
    private static Logger logger = LogManager.getLogger(WozArchitect.class);

    /**
     * Initializes a Wizard of Oz Architect.
     * @param waitTime time in seconds that the wizards gets to type an answer
     * @param listener the WOZListener
     */
    public WozArchitect(int waitTime, WozListener listener) {
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
    public void handleStatusInformation(StatusMessage request,
                                        StreamObserver<TextMessage> responseObserver) {
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
                listener.movePlayer(x, y, z, xDir, yDir, zDir);
                listener.player.sendMessage("Next instruction: ");

                //  wait for the wizard to hit enter, then continue.
                //  If they don't hit enter in <waitTime> seconds send empty message
                listener.wizardGaveInstructionLatch = new CountDownLatch(1);
                try {
                    boolean wizardGaveInstruction = listener.wizardGaveInstructionLatch.await(
                        waitTime, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }

                String text = String.join(". ", listener.savedMessages);
                listener.savedMessages.clear();
                TextMessage message = TextMessage.newBuilder().setGameId(gameId).setText(text).build();

                responseObserver.onNext(message);
                responseObserver.onCompleted();
            }
        }.start();
    }

    @Override
    public void handleBlockPlaced(BlockPlacedMessage request,
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
                Material material = Material.values()[type];
                listener.placeBlock(x,y,z,material);
                logger.info("{} block placed at {}-{}-{}", material, x, y, z);
                listener.player.sendMessage("Next instruction: ");
                //  wait for the wizard to hit enter, then continue.
                //  If they don't hit enter in <waitTime> seconds send empty message
                listener.wizardGaveInstructionLatch = new CountDownLatch(1);
                try {
                    boolean wizardGaveInstruction = listener.wizardGaveInstructionLatch.await(
                        waitTime, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }

                String text = String.join(". ", listener.savedMessages);
                listener.savedMessages.clear();

                TextMessage message = TextMessage.newBuilder().setGameId(gameId).setText(text).build();
                logger.info("Send message: " + text);

                // send the text message back to the client
                responseObserver.onNext(message);
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

        // spawn a thread for a long-running computation
        new Thread() {
            @Override
            public void run() {
                listener.breakBlock(x, y, z);

                listener.player.sendMessage("Next instruction: ");

                //  wait for the wizard to hit enter, then continue.
                //  If they don't hit enter in <waitTime> seconds send empty message
                listener.wizardGaveInstructionLatch = new CountDownLatch(1);
                try {
                    boolean wizardGaveInstruction = listener.wizardGaveInstructionLatch.await(
                        waitTime, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }

                String text = String.join(". ", listener.savedMessages);
                listener.savedMessages.clear();

                TextMessage message = TextMessage.newBuilder().setGameId(gameId).setText(text).build();

                responseObserver.onNext(message);
                responseObserver.onCompleted();
            }
        }.start();
    }

    @Override
    public String getArchitectInformation() {
        return "WOZArchitect";
    }
}
