package de.saar.minecraft.communication;

import de.saar.minecraft.broker.BrokerGrpc;
import de.saar.minecraft.broker.GameData;
import de.saar.minecraft.shared.GameId;
import de.saar.minecraft.shared.StatusMessage;
import de.saar.minecraft.shared.TextMessage;
import de.saar.minecraft.shared.BlockDestroyedMessage;
import de.saar.minecraft.shared.BlockPlacedMessage;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

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

    private Random random = new Random();
    HashMap<String, Integer> activeGames;

    /**
     * Construct client connecting to Broker at {@code host:port}.
     */
    public MinecraftClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
        // TODO: .build() here or change signature of next method to public RouteGuideClient(ManagedChannelBuilder<?> channelBuilder)
        activeGames = new HashMap();
    }

    /**
     * Construct client for accessing Broker using the existing channel.
     */
    MinecraftClient(ManagedChannel channel) {
        System.out.println("In Channel constructor of Minecraft client");
        System.out.println(channel.toString());
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
    public int registerGame(String playerName) {

        // TODO: what is the correct host? localhost
        String hostname = "localhost";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
        }

        GameData mGameInfo = GameData.newBuilder().setClientAddress(hostname).setPlayerName(playerName)
                .build();

        GameId mGameId;
        try {
            mGameId = blockingStub.startGame(mGameInfo);
        } catch (StatusRuntimeException e) {
            System.err.println("RPC failed: " + e.getStatus());
            return -1;
        }

        // remember active games
        activeGames.put(playerName, mGameId.getId());
        return mGameId.getId();
    }

    public void finishGame(int gameId) {
        activeGames.values().remove(gameId);
        System.out.format("Removed player %d", gameId);
        System.out.println(activeGames.toString());
        GameId mGameId = GameId.newBuilder().setId(gameId).build();
        blockingStub.endGame(mGameId);  // TODO: what to do with the void return
    }

    public String sendPlayerPosition(int gameId, int x, int y, int z){
        GameId mGameId = GameId.newBuilder().setId(gameId).build();  // TODO: why construct id?
        StatusMessage position = StatusMessage.newBuilder().setGameId(gameId).setX(x).setY(y).setZ(z).build();
        Iterator<TextMessage> messageStream = blockingStub.handleStatusInformation(position);
        String result = "";
        for (; messageStream.hasNext(); ) {
            TextMessage m = messageStream.next();
            System.out.println(m.getGameId());
            System.out.println(m.getText());
            result += m.getText();
        }
        return result;
    }

    int getGameIdForPlayer(String playerName){
        return this.activeGames.get(playerName);
    }

    HashMap<String, Integer> getActiveGames(){
        return this.activeGames;
    }

//    public void receiveTextMessage(){
//        blockingStub.
//    }

    public String sendBlockPlaced(int gameId, int x, int y, int z, int type){
        BlockPlacedMessage message = BlockPlacedMessage.newBuilder().setGameId(gameId).setX(x).setZ(z).setType(type).build();
        Iterator<TextMessage> messageStream = blockingStub.handleBlockPlaced(message);
        String result = "";
        for (Iterator<TextMessage> it = messageStream; it.hasNext(); ) {
            TextMessage m = it.next();
            result += m.getText();
        }
        return result;
    }

    public String sendBlockDestroyed(int gameId, int x, int y, int z, int type){
        BlockDestroyedMessage message = BlockDestroyedMessage.newBuilder().setGameId(gameId).setX(x).setZ(z).setType(type).build();
        Iterator<TextMessage> messageStream = blockingStub.handleBlockDestroyed(message);
        String result = "";
        for (Iterator<TextMessage> it = messageStream; it.hasNext(); ) {
            TextMessage m = it.next();
            result += m.getText();
        }
        return result;
    }



}
