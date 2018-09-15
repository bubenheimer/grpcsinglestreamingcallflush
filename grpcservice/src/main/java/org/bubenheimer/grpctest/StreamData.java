/*
 * Copyright (c) 2015-2016 Uli Bubenheimer. All rights reserved.
 */

package org.bubenheimer.grpctest;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.grpc.stub.StreamObserver;

final class StreamData extends StreamDataGrpc.StreamDataImplBase {
    private static final Logger LOG = Logger.getLogger(StreamData.class.getName());

    @Override
    public StreamObserver<Item> streamData(final StreamObserver<Item> responseObserver) {
        return new StreamObserver<Item>() {
            private int counter = 0;

            @Override
            public void onNext(final Item value) {
                responseObserver.onNext(value);
                // Safe even from multiple threads because we're called in a synchronized fashion
                ++counter;
            }

            @Override
            public void onError(final Throwable t) {
                LOG.log(Level.WARNING, "StreamData error: ", t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();

                LOG.info("Call completed after " + counter + " Items");
            }
        };
    }
}
