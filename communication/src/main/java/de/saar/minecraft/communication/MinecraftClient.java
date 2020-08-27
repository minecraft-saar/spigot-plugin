package de.saar.minecraft.communication;

import de.saar.minecraft.broker.BrokerGrpc;
import de.saar.minecraft.broker.GameData;
import de.saar.minecraft.shared.BlockDestroyedMessage;
import de.saar.minecraft.shared.BlockPlacedMessage;
import de.saar.minecraft.shared.GameId;
import de.saar.minecraft.shared.MinecraftServerError;
import de.saar.minecraft.shared.NewGameState;
import de.saar.minecraft.shared.None;
import de.saar.minecraft.shared.StatusMessage;
import de.saar.minecraft.shared.TextMessage;
import de.saar.minecraft.shared.WorldFileError;
import de.saar.minecraft.shared.WorldSelectMessage;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class MinecraftClient implements Client {

    private static final NoneObserver noneObserver = new NoneObserver();
    private final ManagedChannel channel;
    private final BrokerGrpc.BrokerBlockingStub blockingStub;
    private final BrokerGrpc.BrokerStub nonblockingStub;

    private static final Logger logger = LogManager.getLogger(MinecraftClient.class);

    private BidiMap<String, Integer> activeGames;

    // Games in which the player is already in their own world
    private Set<Integer> readyGames = new HashSet<>();
    private static CommunicationPlugin plugin;

    /**
     * Construct client connecting to Broker at {@code host:port}.
     */
    public MinecraftClient(String host, int port, CommunicationPlugin plugin) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
        activeGames = new DualHashBidiMap<>();
        MinecraftClient.plugin = plugin;
    }

    /**
     * Construct client for accessing Broker using the existing channel.
     */
    MinecraftClient(ManagedChannel channel) {
        logger.debug("In Channel constructor of Minecraft client");
        logger.debug(channel.toString());
        this.channel = channel;
        blockingStub = BrokerGrpc.newBlockingStub(channel);
        nonblockingStub = BrokerGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * Registers a game with the broker. Returns a world name.
     */
    public String registerGame(String playerName, String playerIp) {
        GameData gameInfo = GameData.newBuilder()
            .setClientAddress(playerIp)
            .setPlayerName(playerName)
            .build();

        WorldSelectMessage worldSelect;
        try {
            synchronized (this) {
                worldSelect = blockingStub.startGame(gameInfo);
            }
        } catch (StatusRuntimeException e) {
            logger.error("RPC failed: " + e.getStatus());
            throw e;
        }

        // remember active games
        int gameId = worldSelect.getGameId();
        TextStreamObserver tso = new TextStreamObserver(gameId);
        synchronized (this) {
            nonblockingStub.getMessageChannel(GameId.newBuilder().setId(gameId).build(), tso);
        }
        System.err.println("!!!!! obtained message channel");
        activeGames.put(playerName, gameId);
        logger.info("obtained message channel");
        return worldSelect.getName();
    }

    /**
     * Notifies the broker that the player is ready for instructions.
     */
    public void playerReady(int gameId) {
        synchronized (this) {
            blockingStub.playerReady(GameId.newBuilder().setId(gameId).build());
        }
        readyGames.add(gameId);
    }

    /**
     * Unregisters a game with the broker.
     */
    public void finishGame(int gameId) {
        readyGames.remove(gameId);
        activeGames.values().remove(gameId);
        logger.info(String.format("Removed player %d", gameId));
        logger.info(activeGames.toString());
        GameId gameIdMessage = GameId.newBuilder().setId(gameId).build();
        synchronized (this) {
            blockingStub.endGame(gameIdMessage);  // TODO: what to do with the void return
        }
    }

    /**
     * Sends a player's location and direction to the broker.
     */
    public void sendPlayerPosition(int gameId, int x, int y, int z, double xDir, double yDir,
                                     double zDir) {
        if (!readyGames.contains(gameId)) {
            // only send updates once the player is in their own world.
            return;
        }
        StatusMessage position = StatusMessage.newBuilder()
            .setGameId(gameId)
            .setX(x)
            .setY(y)
            .setZ(z)
            .setXDirection(xDir)
            .setYDirection(yDir)
            .setZDirection(zDir)
            .build();
        synchronized (this) {
            nonblockingStub.handleStatusInformation(position, noneObserver);
        }
    }

    public static class NoneObserver implements StreamObserver<None> {
        @Override
        public void onNext(None value) {
        }

        @Override
        public void onError(Throwable t) {
            logger.error(t);
        }

        @Override
        public void onCompleted() {
        }
    }

    private class TextStreamObserver implements StreamObserver<TextMessage> {
        private final int gameId;

        public TextStreamObserver(int gameId) {
            this.gameId = gameId;
        }

        @Override
        public void onNext(TextMessage value) {
            // verify that message is sent to correct player
            assert gameId == value.getGameId();
            String playerName = activeGames.getKey(gameId);
            plugin.sendTextMessage(playerName, value.getText());
            if (value.getNewGameState() == NewGameState.QuestionnaireFinished) {
                plugin.delayedKickPlayer(playerName);
            }
        }

        @Override
        public void onError(Throwable t) {
            logger.error(t.toString());
        }

        @Override
        public void onCompleted() {
        }
    }

    /**
     * Returns the game ID or -1 if the player is currently not
     * associated with a game.
     */
    public int getGameIdForPlayer(String playerName) {
        Integer result = this.activeGames.get(playerName);
        if (result == null) {
            return -1;
        }
        return result;
    }

    public String getPlayernameFromGameId(int gameId) {
        return this.activeGames.getKey(gameId);
    }

    public BidiMap<String, Integer> getActiveGames() {
        return this.activeGames;
    }

    /**
     * Sends a BlockPlacedMessage to the broker.
     * @param type an integer encoding the block material
     */
    public void sendBlockPlaced(int gameId, int x, int y, int z, int type) {
        if (!readyGames.contains(gameId)) {
            // only send updates once the player is in their own world.
            return;
        }
        BlockPlacedMessage message = BlockPlacedMessage.newBuilder()
            .setGameId(gameId)
            .setX(x)
            .setY(y)
            .setZ(z)
            .setType(type)
            .build();
        synchronized (this) {
            nonblockingStub.handleBlockPlaced(message, noneObserver);
        }
    }

    /**
     * Sends a BlockDestroyedMessage to the broker.
     * @param type an integer encoding the block material
     */
    public void sendBlockDestroyed(int gameId, int x, int y, int z, int type) {
        if (!readyGames.contains(gameId)) {
            // only send updates once the player is in their own world.
            return;
        }
        BlockDestroyedMessage message = BlockDestroyedMessage.newBuilder()
            .setGameId(gameId)
            .setX(x)
            .setY(y)
            .setZ(z)
            .setType(type)
            .build();
        synchronized (this) {
            nonblockingStub.handleBlockDestroyed(message, noneObserver);
        }
    }

    /**
     * Sends a MinecraftServerError to the broker.
     * @param gameId the id of the game where the error occurred
     * @param message the specific error message
     */
    public void sendMinecraftServerError(int gameId, String message) {
        MinecraftServerError request = MinecraftServerError.newBuilder()
            .setGameId(gameId)
            .setMessage(message)
            .build();
        synchronized (this) {
            blockingStub.handleMinecraftServerError(request);
        }
    }

    /**
     * Sends a WorldFileError to the broker.
     * @param gameId the id of the game where the error occurred
     * @param message the specific error message
     */
    public void sendWorldFileError(int gameId, String message) {
        WorldFileError request = WorldFileError.newBuilder()
            .setGameId(gameId)
            .setMessage(message)
            .build();
        synchronized (this) {
            blockingStub.handleWorldFileError(request);
        }
    }

    /**
     * Sends a TextMessage to the broker.
     */
    public void sendTextMessage(int gameId, String message) {
        if (!readyGames.contains(gameId)) {
            // only send updates once the player is in their own world.
            return;
        }
        TextMessage request = TextMessage.newBuilder()
            .setGameId(gameId)
            .setText(message)
            .build();
        synchronized (this) {
            nonblockingStub.handleTextMessage(request, noneObserver);
        }
    }

}
