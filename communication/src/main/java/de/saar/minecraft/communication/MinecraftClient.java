package de.saar.minecraft.communication;

import de.saar.minecraft.broker.BrokerGrpc;
import de.saar.minecraft.broker.GameData;
import de.saar.minecraft.shared.BlockDestroyedMessage;
import de.saar.minecraft.shared.BlockPlacedMessage;
import de.saar.minecraft.shared.GameId;
import de.saar.minecraft.shared.MinecraftServerError;
import de.saar.minecraft.shared.StatusMessage;
import de.saar.minecraft.shared.TextMessage;
import de.saar.minecraft.shared.WorldFileError;
import de.saar.minecraft.shared.WorldSelectMessage;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class MinecraftClient implements Client {

    private final ManagedChannel channel;
    private BrokerGrpc.BrokerBlockingStub blockingStub;
    private BrokerGrpc.BrokerStub nonblockingStub;

    private static Logger logger = LogManager.getLogger(MinecraftClient.class);

    private HashMap<String, Integer> activeGames;

    /**
     * Construct client connecting to Broker at {@code host:port}.
     */
    public MinecraftClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
        // TODO: .build() here or change signature of next method to public
        //  RouteGuideClient(ManagedChannelBuilder<?> channelBuilder)
        activeGames = new HashMap<>();
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
    public String registerGame(String playerName) throws UnknownHostException {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.error("Hostname not found: " + e.getMessage());
            throw e;
        }

        GameData gameInfo = GameData.newBuilder()
            .setClientAddress(hostname)
            .setPlayerName(playerName)
            .build();

        WorldSelectMessage worldSelect;
        try {
            worldSelect = blockingStub.startGame(gameInfo);
        } catch (StatusRuntimeException e) {
            logger.error("RPC failed: " + e.getStatus());
            throw e;
        }

        // remember active games
        int gameId = worldSelect.getGameId();
        activeGames.put(playerName, gameId);

        return worldSelect.getName();
    }

    /**
     * Unregisters a game with the broker.
     */
    public void finishGame(int gameId) {
        activeGames.values().remove(gameId);
        logger.info(String.format("Removed player %d", gameId));
        logger.info(activeGames.toString());
        GameId gameIdMessage = GameId.newBuilder().setId(gameId).build();
        blockingStub.endGame(gameIdMessage);  // TODO: what to do with the void return
    }

    /**
     * Sends a player's location and direction to the broker.
     * @return the text message the broker send back
     */
    public String sendPlayerPosition(int gameId, int x, int y, int z, double xDir, double yDir,
                                     double zDir) {
        // sendStatusMessage(gameId, x, y, z, xdir, ydir, zdir, new TextStreamObserver(gameId)
        StatusMessage position = StatusMessage.newBuilder()
            .setGameId(gameId)
            .setX(x)
            .setY(y)
            .setZ(z)
            .setXDirection(xDir)
            .setYDirection(yDir)
            .setZDirection(zDir)
            .build();
        Iterator<TextMessage> messageStream = blockingStub.handleStatusInformation(position);
        StringBuilder result = new StringBuilder();
        for (; messageStream.hasNext(); ) {
            TextMessage m = messageStream.next();
            logger.debug(m.getGameId());
            logger.debug(m.getText());
            result.append(m.getText());
        }
        return result.toString();
    }

    public int getGameIdForPlayer(String playerName) {
        return this.activeGames.get(playerName);
    }

    public HashMap<String, Integer> getActiveGames() {
        return this.activeGames;
    }

    /**
     * Sends a BlockPlacedMessage to the broker.
     * @param type an integer encoding the block material
     * @return the text message the broker sends back
     */
    public String sendBlockPlaced(int gameId, int x, int y, int z, int type) {
        BlockPlacedMessage message = BlockPlacedMessage.newBuilder()
            .setGameId(gameId)
            .setX(x)
            .setY(y)
            .setZ(z)
            .setType(type)
            .build();
        Iterator<TextMessage> messageStream = blockingStub.handleBlockPlaced(message);
        StringBuilder result = new StringBuilder();
        for (; messageStream.hasNext(); ) {
            TextMessage m = messageStream.next();
            result.append(m.getText());
        }
        return result.toString();
    }

    /**
     * Sends a BlockDestroyedMessage to the broker.
     * @param type an integer encoding the block material
     * @return the text message the broker sends back
     */
    public String sendBlockDestroyed(int gameId, int x, int y, int z, int type) {
        BlockDestroyedMessage message = BlockDestroyedMessage.newBuilder()
            .setGameId(gameId)
            .setX(x)
            .setY(y)
            .setZ(z)
            .setType(type)
            .build();
        Iterator<TextMessage> messageStream = blockingStub.handleBlockDestroyed(message);
        StringBuilder result = new StringBuilder();
        for (; messageStream.hasNext(); ) {
            TextMessage m = messageStream.next();
            result.append(m.getText());
        }
        return result.toString();
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
        blockingStub.handleMinecraftServerError(request);
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
        blockingStub.handleWorldFileError(request);
    }


}
