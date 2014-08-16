package com.straphq.wear_sdk;

import android.app.Activity;
import android.content.res.Resources;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.os.Bundle;
import android.os.Looper;
import android.support.wearable.view.WatchViewStub;
import android.text.format.Time;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.a;
import com.google.android.gms.wearable.*;
import com.google.android.gms.common.api.*;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.ArrayList;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import android.content.Context;


public class Strap implements SensorEventListener {


    //members
    private GoogleApiClient mGoogleApiClient = null;
    private SensorManager mSensorManager = null;
    private Sensor mAccelerometer = null;
    private String mStrapAppID = null;
    private ArrayList<DataMap> accelDataMapList = null;

    private static Strap strapManager = null;
    private DataMap lastAccelData;

    //stub object for locking accelerometer data;
    private Object lock = new Object();

    //constants
    private int kMaxAccelLength = 100;
    private int kAccelerometerFrequencyInMS = 5000;


    //TODO finish singleton implementation
    public static Strap getInstance() {
        return strapManager;
    }

    Strap(GoogleApiClient apiClient, SensorManager sensorManager, String strapAppID) {

        mGoogleApiClient = apiClient;
        mSensorManager = sensorManager;
        mStrapAppID = strapAppID;

        strapManager = this;

        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelDataMapList = new ArrayList<DataMap>();

        Timer accelTimer = new Timer();
        RecordAccelerometerTask recordTask = new RecordAccelerometerTask();

        accelTimer.schedule(recordTask, kAccelerometerFrequencyInMS, kAccelerometerFrequencyInMS);

    }

    public void logEvent(String eventName) {


        //create a new data map entry for this event
        PutDataMapRequest dataMap = PutDataMapRequest.create("/strap/" + new Date().toString());
        dataMap.getDataMap().putString("appID",mStrapAppID);
        dataMap.getDataMap().putString("eventName", eventName);
        if(accelDataMapList.size() > kMaxAccelLength ) {
            dataMap.getDataMap().putDataMapArrayList("accelData", accelDataMapList);
        }


        //sync the data
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);

        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(final DataApi.DataItemResult result) {
                if(result.getStatus().isSuccess()) {
                    Log.d("Callback", "Data item set: " + result.getDataItem());
                } else {
                    Log.e("Callback", "Error setting data item! :" + result.toString());
                }
            }
        });

    }

    private DataMap buildAccelData(float [] coords) {
        DataMap accelDataMap = new DataMap();
        accelDataMap.putFloatArray("coordinates", coords);

        Time currentTime = new Time();
        currentTime.setToNow();

        accelDataMap.putString("time", currentTime.toString());

        return accelDataMap;
    }

    //Sensor listener override methods

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //TODO handling of accuracy changes
    }

    //Setter for
    public void  setLastAccelData(DataMap accelData) {
        synchronized(lock) {
            lastAccelData = accelData;
        }
    }

    public DataMap getLastAccelData() {
        synchronized(lock) {
            return lastAccelData;
        }
    }

    //Basic reading of the accelerometer. Just updates the member variables to the new values.
    //If collecting a trend, something like a list/vector could be used to get deltas.
    @Override
    public void  onSensorChanged(SensorEvent sensorEvent) {
        setLastAccelData(buildAccelData(sensorEvent.values));
    }


    //Small task implementation for periodically recording accel data.
    class RecordAccelerometerTask extends TimerTask {
        public void run() {
            accelDataMapList.add(getLastAccelData());
        }
    }
}
