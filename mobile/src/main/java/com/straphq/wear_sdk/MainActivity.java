package com.straphq.wear_sdk;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.straphq.wear_sdk.R;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.a;
import com.google.android.gms.wearable.*;
import com.google.android.gms.common.api.*;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import android.util.Log;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import android.content.Context;
import android.widget.Toast;
import android.content.Intent;

import org.json.JSONException;

import java.io.IOException;


public class MainActivity extends Activity {

    public static GoogleApiClient mGoogleApiClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toast.makeText(getApplicationContext(), "this", Toast.LENGTH_SHORT);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d("TAG", "onConnected: " + connectionHint);
                        // Now you can use the data layer API
                        //startService(new Intent(this, MyService.class));
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d("", "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d("", "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        StrapMetrics strap = new StrapMetrics();

        /*
        try {
            strap.processReceiveData();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        //strap.processReceiveData();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
