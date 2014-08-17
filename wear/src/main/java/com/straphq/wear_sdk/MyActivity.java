package com.straphq.wear_sdk;

import android.app.Activity;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.Button;
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

    // TODO: Dynamically pull this from strings.xml. I couldn't get that to work. -@scald
//  String strapAppID = this.getString(R.string.strap_app_id);
    String strapAppID = "rdjYKgrfeAPeMSjQ4";
    Strap strap = null;

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
                SensorManager sensor = (SensorManager)getSystemService(SENSOR_SERVICE);

                strap = new Strap(mGoogleApiClient, getApplicationContext(), strapAppID);

                strap.logEvent("blah");


            }

        });
    }
    public void handleButtonClick(View view) {
        strap.logEvent("button-click");
    }
}
