package org.bubenheimer.grpctest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.netty.channel.ChannelOption;

//TODO workaround: direct executor lets RuntimeExceptions pass through the call stack: https://github.com/grpc/grpc-java/issues/636
//TODO consider enabling compression at some point
public class Main {
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    private final int port;
    private final Server server;

    private Main(final int port) {
        this.port = port;
        server = NettyServerBuilder.forPort(port)
//                .withChildOption(ChannelOption.TCP_NODELAY, false)
                .addService(new StreamData())
                .build();
    }

    /** Start serving requests. */
    private void start() throws IOException {
        server.start();
        LOG.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            stop();
        }));
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
            try {
                final boolean isTerminated = server.awaitTermination(10L, TimeUnit.SECONDS);
                if (isTerminated) {
                    System.err.println("*** server shut down");
                } else {
                    System.err.println("*** server failed to shut down - potentially unclean termination pending?");
                }

            } catch (final InterruptedException e) {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                e.printStackTrace();
            }
        }
    }

    /**
     * Await termination on the main thread since the grpc/netty library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(final String[] args) throws IOException, InterruptedException {
        final Main server = new Main(8082);
        server.start();
        server.blockUntilShutdown();
    }
}
