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
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.a;
import com.google.android.gms.wearable.*;
import com.google.android.gms.common.api.*;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import android.util.Log;

//System data
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.provider.*;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.ArrayList;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.view.Display;
import android.graphics.Point;

public class Strap implements SensorEventListener, ResultCallback<DataApi.DataItemResult> {


    //members
    private GoogleApiClient mGoogleApiClient = null;
    private SensorManager mSensorManager = null;
    private Sensor mAccelerometer = null;
    private String mStrapAppID = null;
    private Point mDisplayResolution = null;
    private ArrayList<DataMap> mAccelDataMapList = null;

    private static Strap strapManager = null;
    private DataMap lastAccelData;

    //stub object for locking accelerometer data;
    private Object lock = new Object();

    //constants
    private int kMaxAccelLength = 100;
    private int kAccelerometerFrequencyInMS = 100;

    private int kSystemDataFrequencyInMS = 1000 * 60 * 5;

    private static final String kLogEventType = "logEvent";
    private static final String kAcclType = "logAccl";
    private static final String kDiagnosticType = "logDiagnostic";


    //TODO finish singleton implementation
    public static Strap getInstance() {
        return strapManager;
    }

    Strap(GoogleApiClient apiClient, Context applicationContext, String strapAppID) {

        //Singleton reference TODO
        strapManager = this;

        //Initialize members
        mGoogleApiClient = apiClient;
        mSensorManager = (SensorManager) applicationContext.getSystemService(applicationContext.SENSOR_SERVICE);
        mStrapAppID = strapAppID;
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccelDataMapList = new ArrayList<DataMap>();

        //Grab screen data
        WindowManager windowManager = (WindowManager) applicationContext.getSystemService(applicationContext.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        mDisplayResolution = new Point();
        display.getSize(mDisplayResolution);

        //Setup accelerometer pinging.
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        Timer accelTimer = new Timer();
        Timer systemTimer = new Timer();
        RecordAccelerometerTask recordTask = new RecordAccelerometerTask();
        SystemInfoTask systemTask = new SystemInfoTask();
        systemTask.setApplicationContext(applicationContext);

        accelTimer.scheduleAtFixedRate(recordTask, new Date(), kAccelerometerFrequencyInMS);
        systemTimer.scheduleAtFixedRate(systemTask, new Date(), kSystemDataFrequencyInMS);
    }

    public void onResult(final DataApi.DataItemResult result) {
        if(result.getStatus().isSuccess()) {
            Log.d("Callback", "Data item set: " + result.getDataItem());
        } else {
            Log.e("Callback", "Error setting data item! :" + result.toString());
        }
    }

    private DataMap buildBasicRequest (DataMap mapToBuild) {
        mapToBuild.putString("appID",mStrapAppID);
        mapToBuild.putInt("display_width", mDisplayResolution.x);
        mapToBuild.putInt("display_height", mDisplayResolution.y);

        return mapToBuild;
    }

    public void logEvent(String eventName) {


        //create a new data map entry for this event and load it with data
        PutDataMapRequest dataMap = PutDataMapRequest.create("/strap/" + new Date().toString());

        buildBasicRequest(dataMap.getDataMap());
        dataMap.getDataMap().putString("type", "logEvent");

        if(eventName != "") {
            dataMap.getDataMap().putString("eventName", eventName);
            dataMap.getDataMap().putString("type", kLogEventType);
        } else {
            dataMap.getDataMap().putDataMapArrayList("accelData", mAccelDataMapList);
            dataMap.getDataMap().putString("type", kAcclType);
        }

        //sync the data
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);
        pendingResult.setResultCallback(this);

    }

    private DataMap buildAccelData(float [] coords) {
        DataMap accelDataMap = new DataMap();
        accelDataMap.putFloatArray("coordinates", coords);

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

            long time = System.currentTimeMillis();
            DataMap lastAccelData = getLastAccelData();
            if(lastAccelData != null) {

                DataMap newMap = new DataMap();
                newMap.putLong("time", time);
                newMap.putFloatArray("coordinates", lastAccelData.getFloatArray("coordinates"));
                mAccelDataMapList.add(newMap);

                if(mAccelDataMapList.size() >= kMaxAccelLength) {
                    logEvent("");
                    mAccelDataMapList.clear();
                }
            }

        }
    }

    class SystemInfoTask extends TimerTask {
        Context context = null;

        public void setApplicationContext(Context applicationContext) {
            context = applicationContext;
        }

        public void run() {

            PutDataMapRequest dataMap = PutDataMapRequest.create("/strap/" + new Date().toString());

            Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            dataMap.getDataMap().putString("type", kDiagnosticType);
            dataMap.getDataMap().putInt("battery", level);
            dataMap.getDataMap().putLong("time", System.currentTimeMillis());

            buildBasicRequest(dataMap.getDataMap());

            try {

                int brightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                dataMap.getDataMap().putInt("brightness", brightness);
            } catch (Settings.SettingNotFoundException e) {
                Log.d("SettingsNotFound",e.getMessage());
            }

            PutDataRequest request = dataMap.asPutDataRequest();
            PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                    .putDataItem(mGoogleApiClient, request);


        }
    }
}
