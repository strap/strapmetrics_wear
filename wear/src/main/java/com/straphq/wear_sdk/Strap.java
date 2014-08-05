package com.straphq.wear_sdk;

import android.app.Activity;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.os.Bundle;
import android.os.Looper;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.a;
import com.google.android.gms.wearable.*;
import com.google.android.gms.common.api.*;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import android.util.Log;
import java.util.Date;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import java.util.concurrent.TimeUnit;

import android.content.Context;

public class Strap implements SensorEventListener {

    private GoogleApiClient mGoogleApiClient = null;
    private SensorManager mSensorManager = null;
    private Sensor mAccelerometer = null;
    private static Strap strapManager = null;

    private float mXAxis;
    private float mYAxis;
    private float mZAxis;


    //TODO finish singleton implementation
    public static Strap getInstance() {
        return strapManager;
    }

    Strap(GoogleApiClient apiClient, SensorManager sensorManager) {

        mGoogleApiClient = apiClient;
        mSensorManager = sensorManager;

        strapManager = this;

        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


    }

    public void logEvent(String eventName) {


        //create a new data map entry for this event
        PutDataMapRequest dataMap = PutDataMapRequest.create("/strap/" + new Date().toString());
        dataMap.getDataMap().putString("eventName", eventName);


        //sync the data
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);

        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(final DataApi.DataItemResult result) {
                if(result.getStatus().isSuccess()) {
                    Log.d("Callback", "Data item set: " + result.getDataItem().getUri());
                }
            }
        });

    }

    //Sensor listener override methods

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //TODO handling of accuracy changes
    }

    //Basic reading of the accelerometer. Just updates the member variables to the new values.
    //If collecting a trend, something like a list/vector could be used to get deltas.
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        mXAxis = sensorEvent.values[0];
        mYAxis = sensorEvent.values[1];
        mZAxis = sensorEvent.values[2];
    }
}
