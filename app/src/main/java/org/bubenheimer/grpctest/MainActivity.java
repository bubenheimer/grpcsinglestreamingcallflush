package org.bubenheimer.grpctest;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.concurrent.CountDownLatch;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.android.AndroidChannelBuilder;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ManagedChannel channel;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_layout);

        channel = AndroidChannelBuilder
                .forAddress("10.0.2.2", 8082)
                .usePlaintext()
                .context(getApplicationContext())
                .build();

        new Runnable() {
            @Override
            public void run() {
                final ConnectivityState state = channel.getState(false);
                channel.notifyWhenStateChanged(state, this);
                Log.d(TAG, "Changed channel state: " + state);
            }
        }.run();
    }

    @Override
    protected void onDestroy() {
        channel.shutdownNow();

        super.onDestroy();
    }

    public void on11ButtonClick(final View view) {
        view.setEnabled(false);

        new Thread(() -> {
            final CountDownLatch latch = new CountDownLatch(1);

            //noinspection ResultOfMethodCallIgnored
            StreamDataGrpc.newStub(channel).streamData(
                    new ClientResponseObserver<Item, Item>() {
                        private int outCounter = 0;
                        private int inCounter = 0;

                        @Override
                        public void beforeStart(
                                final ClientCallStreamObserver<Item> requestStream) {
                            requestStream.setOnReadyHandler(() -> {
                                while (requestStream.isReady()) {
                                    final Item item = Item.newBuilder()
                                            .setValue(Integer.toString(outCounter++)).build();
                                    requestStream.onNext(item);
                                    if (outCounter == 50_000) {
                                        requestStream.onCompleted();
                                        break;
                                    }
                                }
                            });
                        }
                        @Override
                        public void onNext(final Item value) {
                            // Safe even from multiple threads as called in a synchronized fashion
                            ++inCounter;
                        }

                        @Override
                        public void onError(final Throwable t) {
                            Log.w(TAG, t);
                            latch.countDown();
                        }

                        @Override
                        public void onCompleted() {
                            Log.i(TAG, "Call completed after " + inCounter + " Items");
                            latch.countDown();
                        }
                    });

            try {
                latch.await();
            } catch (final InterruptedException e) {
                Log.w(TAG, "Interrupted while waiting for stream completion", e);
            }

            Log.i(TAG, "Call finished");

            runOnUiThread(() -> view.setEnabled(true));
        }).start();
    }
}
