package com.straphq.wear_sdk;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.a;
import com.google.android.gms.wearable.*;
import com.google.android.gms.common.api.*;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import android.util.Log;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import android.content.Context;

public class MyActivity extends Activity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);

                GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                        .addConnectionCallbacks(new ConnectionCallbacks() {
                            @Override
                            public void onConnected(Bundle connectionHint) {
                                Log.d("TAG", "onConnected: " + connectionHint);
                                // Now you can use the data layer API
                            }
                            @Override
                            public void onConnectionSuspended(int cause) {
                                Log.d("TAG", "onConnectionSuspended: " + cause);
                            }
                        })
                        .addOnConnectionFailedListener(new OnConnectionFailedListener() {
                            @Override
                            public void onConnectionFailed(ConnectionResult result) {
                                Log.d("TAG", "onConnectionFailed: " + result);
                            }
                        })
                        .addApi(Wearable.API)
                        .build();

                mGoogleApiClient.connect();

                Strap strap = new Strap(mGoogleApiClient);
                strap.logEvent("blah");
            }
        });
    }
}