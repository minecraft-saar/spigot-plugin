package de.saar.minecraft.woz;

import com.google.rpc.Code;
import com.google.rpc.Status;
import de.saar.minecraft.architect.ArchitectGrpc;
import de.saar.minecraft.architect.ArchitectInformation;
import de.saar.minecraft.shared.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



/**
 * A server that provides access to one WOZArchitect.
 */
public class WozArchitectServer {

    private Server server;
    private int port;
    private WozListener listener;
    private WozArchitect arch;

    private static Logger logger = LogManager.getLogger(WozArchitectServer.class);

    public WozArchitectServer(int port, WozListener listener) {
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
                WozArchitectServer.this.stop();
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

        public void hello(None request, StreamObserver<ArchitectInformation> responseObserver) {
            WozArchitect arch = new WozArchitect(1, listener);

            responseObserver.onNext(
                ArchitectInformation.newBuilder().setInfo(arch.getArchitectInformation()).build());
            responseObserver.onCompleted();
        }


        public void startGame(WorldSelectMessage request, StreamObserver<None> responseObserver) {
            if (arch == null) {
                arch = new WozArchitect(10, listener);
            }
            arch.initialize(request);

            responseObserver.onNext(None.getDefaultInstance());
            responseObserver.onCompleted();

            logger.info("architect for id {}: {}", request.getGameId(), arch);
        }

        @Override
        public void playerReady(GameId request, StreamObserver<None> responseObserver) {
            if (arch != null) {
                arch.playerReady();
                responseObserver.onNext(None.getDefaultInstance());
                responseObserver.onCompleted();
            } else {
                Status status = Status.newBuilder()
                        .setCode(Code.INVALID_ARGUMENT.getNumber())
                        .setMessage("No architect running")
                        .build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                logger.warn("could not find architect for message channel");
            }
        }

        @Override
        public void getMessageChannel(GameId request,
                                      StreamObserver<TextMessage> responseObserver) {
            logger.info("architectServer getMessageChannel");
            if (arch == null) {
                Status status = Status.newBuilder()
                        .setCode(Code.INVALID_ARGUMENT.getNumber())
                        .setMessage("No architect running")
                        .build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                logger.warn("could not find architect for message channel");
                return;
            }

            arch.setMessageChannel(responseObserver);
            logger.info("set the message channel");
        }


        public void endGame(GameId request, StreamObserver<None> responseObserver) {
            logger.info("architect for id {} finished", request.getId());
            if (arch != null) {
                arch.shutdown();
            }
            arch = null;
            responseObserver.onNext(None.newBuilder().build());
            responseObserver.onCompleted();
        }

        @Override
        public void endAllGames(None request, StreamObserver<None> responseObserver) {
            if (arch != null) {
                arch.shutdown();
            }
            arch = null;
            responseObserver.onNext(None.getDefaultInstance());
            responseObserver.onCompleted();
        }

        public void handleStatusInformation(StatusMessage request, StreamObserver<None> responseObserver) {
            arch.handleStatusInformation(request);
        }


        public void handleBlockPlaced(BlockPlacedMessage request, StreamObserver<None> responseObserver) {
            arch.handleBlockPlaced(request);
        }

        public void handleBlockDestroyed(BlockDestroyedMessage request, StreamObserver<None> responseObserver) {
            arch.handleBlockDestroyed(request);
        }
    }
}
