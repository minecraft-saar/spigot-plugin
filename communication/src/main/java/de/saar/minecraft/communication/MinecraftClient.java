package de.saar.minecraft.communication;

import de.saar.minecraft.broker.BrokerGrpc;
import de.saar.minecraft.broker.GameData;
import de.saar.minecraft.shared.BlockDestroyedMessage;
import de.saar.minecraft.shared.BlockPlacedMessage;
import de.saar.minecraft.shared.GameId;
import de.saar.minecraft.shared.GamePhase;
import de.saar.minecraft.shared.MinecraftServerError;
import de.saar.minecraft.shared.StatusMessage;
import de.saar.minecraft.shared.TextMessage;
import de.saar.minecraft.shared.WorldFileError;
import de.saar.minecraft.shared.WorldSelectMessage;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.entity.Player;


public class MinecraftClient implements Client {

    private final ManagedChannel channel;
    private BrokerGrpc.BrokerBlockingStub blockingStub;
    private BrokerGrpc.BrokerStub nonblockingStub;

    private static Logger logger = LogManager.getLogger(MinecraftClient.class);

    private BidiMap<String, Integer> activeGames;

    private static CommunicationPlugin plugin;

    /**
     * Construct client connecting to Broker at {@code host:port}.
     */
    public MinecraftClient(String host, int port, CommunicationPlugin plugin) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
        // TODO: .build() here or change signature of next method to public
        //  RouteGuideClient(ManagedChannelBuilder<?> channelBuilder)
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
            worldSelect = blockingStub.startGame(gameInfo);
        } catch (StatusRuntimeException e) {
            logger.error("RPC failed: " + e.getStatus());
            throw e;
        }



        // remember active games
        int gameId = worldSelect.getGameId();
        activeGames.put(playerName, gameId);

        // Keep track of game phase
        GameId id = GameId.newBuilder().setId(gameId).build();
        nonblockingStub.observeGamePhase(id, new GamePhaseStreamObserver(gameId));

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
     */
    public void sendPlayerPosition(int gameId, int x, int y, int z, double xDir, double yDir,
                                     double zDir) {
        StatusMessage position = StatusMessage.newBuilder()
            .setGameId(gameId)
            .setX(x)
            .setY(y)
            .setZ(z)
            .setXDirection(xDir)
            .setYDirection(yDir)
            .setZDirection(zDir)
            .build();
        nonblockingStub.handleStatusInformation(position, new TextStreamObserver(gameId));
    }

    private class TextStreamObserver implements StreamObserver<TextMessage> {
        private int gameId;

        public TextStreamObserver(int gameId) {
            this.gameId = gameId;
        }

        @Override
        public void onNext(TextMessage value) {
            // verify that message is sent to correct player
            assert gameId == value.getGameId();
            String playerName = activeGames.getKey(gameId);
            plugin.sendTextMessage(playerName, value.getText());
        }

        @Override
        public void onError(Throwable t) {
            logger.error(t.toString());
        }

        @Override
        public void onCompleted() {
        }
    }

    private class EvaluationTextStreamObserver implements StreamObserver<TextMessage> {
        private int gameId;

        public EvaluationTextStreamObserver(int gameId) {
            this.gameId = gameId;
        }

        @Override
        public void onNext(TextMessage value) {
            // verify that message is sent to correct player
            assert gameId == value.getGameId();
            String playerName = activeGames.getKey(gameId);
            plugin.sendTextMessage(playerName, value.getText());

            // wait until player answered
            plugin.listener.MessageLatches.put(gameId, new CountDownLatch(1));
            try {
                boolean newMessage = plugin.listener.MessageLatches.get(gameId).await(
                    20, TimeUnit.SECONDS);  // TODO: how long should the players have time to answer?
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }

            // extract answer
            String answer = plugin.listener.currentMessages.get(gameId);
            plugin.listener.currentMessages.remove(gameId);

            sendEvaluationAnswer(gameId, answer);
        }

        @Override
        public void onError(Throwable t) {
            logger.error(t.toString());
        }

        @Override
        public void onCompleted() {
        }
    }

    private class GamePhaseStreamObserver implements StreamObserver<GamePhase> {
        private int gameId;

        public GamePhaseStreamObserver(int gameId) {
            this.gameId = gameId;
        }
        @Override
        public void onNext(GamePhase value) {
            switch (value.getCurrentPhase()){
                case BUILDING: {
                    // TODO
                }
                case EVALUATION: {
                    // Get first question from broker
                    beginEvaluation(gameId);

                }
                case END: {
                    // TODO: send player a message that they are finished and can log out.
                }

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




    public int getGameIdForPlayer(String playerName) {
        return this.activeGames.get(playerName);
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
        BlockPlacedMessage message = BlockPlacedMessage.newBuilder()
            .setGameId(gameId)
            .setX(x)
            .setY(y)
            .setZ(z)
            .setType(type)
            .build();
        nonblockingStub.handleBlockPlaced(message, new TextStreamObserver(gameId));
    }

    /**
     * Sends a BlockDestroyedMessage to the broker.
     * @param type an integer encoding the block material
     */
    public void sendBlockDestroyed(int gameId, int x, int y, int z, int type) {
        BlockDestroyedMessage message = BlockDestroyedMessage.newBuilder()
            .setGameId(gameId)
            .setX(x)
            .setY(y)
            .setZ(z)
            .setType(type)
            .build();
        nonblockingStub.handleBlockDestroyed(message, new TextStreamObserver(gameId));
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

    public void sendEvaluationAnswer(int gameId, String message) {
        TextMessage request = TextMessage.newBuilder()
            .setGameId(gameId)
            .setText(message)
            .build();

        nonblockingStub.handleEvaluationAnswer(request, new EvaluationTextStreamObserver(gameId));
    }

    public void beginEvaluation(int gameId) {
        GameId request = GameId.newBuilder().setId(gameId).build();
        nonblockingStub.beginEvaluation(request, new EvaluationTextStreamObserver(gameId));
    }




}
