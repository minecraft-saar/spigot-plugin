package de.saar.minecraft.communication;

import de.saar.minecraft.broker.BrokerGrpc;
import de.saar.minecraft.broker.GameData;
import de.saar.minecraft.shared.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MinecraftClient {

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
        // TODO: .build() here or change signature of next method to public RouteGuideClient(ManagedChannelBuilder<?> channelBuilder)
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
     * Registers a game with the broker. Returns a unique game ID for this game.
     */
    public String registerGame(String playerName) throws UnknownHostException {
        String hostname = "localhost";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.error("Hostname not found: " + e.getMessage());
            throw e;
        }

        GameData mGameInfo = GameData.newBuilder().setClientAddress(hostname).setPlayerName(playerName)
                .build();

        WorldSelectMessage mWorldSelect;
        try {
            mWorldSelect = blockingStub.startGame(mGameInfo);
        } catch (StatusRuntimeException e) {
            logger.error("RPC failed: " + e.getStatus());
            throw e;
        }

        // remember active games
        int gameId = mWorldSelect.getGameId();
        activeGames.put(playerName, gameId);

        // todo load world type
        return mWorldSelect.getName();
    }

    public void finishGame(int gameId) {
        activeGames.values().remove(gameId);
        logger.info(String.format("Removed player %d", gameId));
        logger.info(activeGames.toString());
        GameId mGameId = GameId.newBuilder().setId(gameId).build();
        blockingStub.endGame(mGameId);  // TODO: what to do with the void return
    }

    public String sendPlayerPosition(int gameId, int x, int y, int z){
        StatusMessage position = StatusMessage.newBuilder().setGameId(gameId).setX(x).setY(y).setZ(z).build();
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

    int getGameIdForPlayer(String playerName){
        return this.activeGames.get(playerName);
    }

    HashMap<String, Integer> getActiveGames(){
        return this.activeGames;
    }

    public String sendBlockPlaced(int gameId, int x, int y, int z, int type){
        BlockPlacedMessage message = BlockPlacedMessage.newBuilder().setGameId(gameId).setX(x).setY(y).setZ(z).setType(type).build();
        Iterator<TextMessage> messageStream = blockingStub.handleBlockPlaced(message);
        StringBuilder result = new StringBuilder();
        for (; messageStream.hasNext(); ) {
            TextMessage m = messageStream.next();
            result.append(m.getText());
        }
        return result.toString();
    }

    public String sendBlockDestroyed(int gameId, int x, int y, int z, int type){
        BlockDestroyedMessage message = BlockDestroyedMessage.newBuilder().setGameId(gameId).setX(x).setY(y).setZ(z).setType(type).build();
        Iterator<TextMessage> messageStream = blockingStub.handleBlockDestroyed(message);
        StringBuilder result = new StringBuilder();
        for (; messageStream.hasNext(); ) {
            TextMessage m = messageStream.next();
            result.append(m.getText());
        }
        return result.toString();
    }


}
