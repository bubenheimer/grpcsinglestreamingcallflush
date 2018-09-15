package org.bubenheimer.grpctest;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import io.grpc.ManagedChannel;
import io.grpc.android.AndroidChannelBuilder;
import io.grpc.stub.StreamObserver;

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
    }

    @Override
    protected void onDestroy() {
        channel.shutdownNow();

        super.onDestroy();
    }

    public void on11ButtonClick(final View view) {
        view.setEnabled(false);

        new Thread(() -> {
            final StreamDataGrpc.StreamDataStub stub = StreamDataGrpc.newStub(channel);

            final StreamObserver<Item> streamObserver = stub.streamData(
                    new StreamObserver<Item>() {
                        private int counter = 0;

                        @Override
                        public void onNext(final Item value) {
                            // Safe even from multiple threads as called in a synchronized fashion
                            ++counter;
                        }

                        @Override
                        public void onError(final Throwable t) {
                            Log.w(TAG, t);
                        }

                        @Override
                        public void onCompleted() {
                            Log.i(TAG, "Call completed after " + counter + " Items");
                        }
                    });

            for (int i = 0; i < 10_000; ++i) {
                streamObserver.onNext(Item.newBuilder().setValue(Integer.toString(i)).build());
            }

            streamObserver.onCompleted();

            runOnUiThread(() -> view.setEnabled(true));
        }).start();
    }
}
