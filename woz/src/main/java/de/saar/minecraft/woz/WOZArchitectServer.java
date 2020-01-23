package de.saar.minecraft.woz;

import de.saar.minecraft.architect.ArchitectGrpc;
import de.saar.minecraft.architect.ArchitectInformation;
import de.saar.minecraft.shared.BlockDestroyedMessage;
import de.saar.minecraft.shared.BlockPlacedMessage;
import de.saar.minecraft.shared.GameId;
import de.saar.minecraft.shared.StatusMessage;
import de.saar.minecraft.shared.TextMessage;
import de.saar.minecraft.shared.Void;
import de.saar.minecraft.shared.WorldSelectMessage;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



/**
 * A server that provides access to one WOZArchitect.
 */
public class WOZArchitectServer {

    private Server server;
    private int port;
    private WOZListener listener;
    private WOZArchitect arch;

    private static Logger logger = LogManager.getLogger(WOZArchitectServer.class);

    public WOZArchitectServer(int port, WOZListener listener) {
        this.listener = listener;
        this.port = port;
    }

    /**
     * Starts the server.
     * @throws IOException if the server cannot be started on the specified port
     */
    public void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new ArchitectImpl())
                .build()
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                WOZArchitectServer.this.stop();
            }
        });

        logger.info("Architect server running on port {}", port);
    }

    /**
     * Stops the server if it was running.
     */
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }


    private class ArchitectImpl extends ArchitectGrpc.ArchitectImplBase {

        public void hello(Void request, StreamObserver<ArchitectInformation> responseObserver) {
            WOZArchitect arch = new WOZArchitect(1, listener);

            responseObserver.onNext(
                ArchitectInformation.newBuilder().setInfo(arch.getArchitectInformation()).build());
            responseObserver.onCompleted();
        }


        public void startGame(WorldSelectMessage request, StreamObserver<Void> responseObserver) {
            if (arch == null) {
                arch = new WOZArchitect(10, listener);
            }
            arch.initialize(request);

            responseObserver.onNext(Void.newBuilder().build());
            responseObserver.onCompleted();

            logger.info("architect for id {}: {}", request.getGameId(),
                arch.getArchitectInformation());
        }


        public void endGame(GameId request, StreamObserver<Void> responseObserver) {
            logger.info("architect for id {} finished", request.getId());
            arch = null;
            responseObserver.onNext(Void.newBuilder().build());
            responseObserver.onCompleted();
        }

        public void handleStatusInformation(StatusMessage request,
                                            StreamObserver<TextMessage> responseObserver) {
            arch.handleStatusInformation(request, responseObserver);
        }


        public void handleBlockPlaced(BlockPlacedMessage request,
                                      StreamObserver<TextMessage> responseObserver) {
            arch.handleBlockPlaced(request, responseObserver);
        }

        public void handleBlockDestroyed(BlockDestroyedMessage request,
                                         StreamObserver<TextMessage> responseObserver) {
            arch.handleBlockDestroyed(request, responseObserver);
        }
    }
}
