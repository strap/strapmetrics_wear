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
import android.os.Handler;
import java.util.logging.LogRecord;

import android.content.Context;
import android.view.Display;
import android.graphics.Point;

import org.json.JSONObject;


/**
 * The StrapMetrics Platform for Android Wear. <Insert better text here></Insert>
 */
public class Strap {


    //members
    private GoogleApiClient mGoogleApiClient = null;
    private SensorManager mSensorManager = null;
    private Sensor mAccelerometer = null;
    private String mStrapAppID = null;
    private Point mDisplayResolution = null;
    private ArrayList<DataMap> mAccelDataMapList = null;
    private RecordAccelerometerTask recordTask = null;
    private SystemInfoTask systemTask = null;


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

    private Handler accelHandler;
    private Handler systemHandler;


    //TODO finish singleton implementation
    /*public static Strap getInstance() {
        return strapManager;
    }*/


    /**
     * Starts StrapMetrics on the device
     * @param apiClient A connected GoogleApiClient to be used for sending data between your Android Wear device and your Android phone.
     * @param applicationContext The context of your application.
     * @param strapAppID The Strap application ID to be used
     */
    public Strap(GoogleApiClient apiClient, Context applicationContext, String strapAppID) {

        accelHandler = new Handler();
        systemHandler = new Handler();
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
        mSensorManager.registerListener( new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                setLastAccelData(buildAccelData(sensorEvent.values));
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        }, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);


        recordTask = new RecordAccelerometerTask();
        systemTask = new SystemInfoTask();
        systemTask.setApplicationContext(applicationContext);

        systemHandler.postDelayed(systemTask, kSystemDataFrequencyInMS);
        accelHandler.postDelayed(recordTask, kAccelerometerFrequencyInMS);
    }

    private DataMap buildBasicRequest (DataMap mapToBuild) {
        mapToBuild.putString("appId",mStrapAppID);
        mapToBuild.putInt("display_width", mDisplayResolution.x);
        mapToBuild.putInt("display_height", mDisplayResolution.y);

        return mapToBuild;
    }

    public void setShouldLogAccel(Boolean shouldLog) {
        if(!shouldLog) {
            accelHandler.removeCallbacks(recordTask);
        } else {
            accelHandler.removeCallbacks(recordTask);
            accelHandler.postDelayed(recordTask, kAccelerometerFrequencyInMS);
        }
    }

    public void setShouldLogDiagnostics(Boolean shouldLog) {
        if(!shouldLog) {
            systemHandler.removeCallbacks(systemTask);
        } else {
            systemHandler.removeCallbacks(systemTask);
            systemHandler.postDelayed(systemTask, kAccelerometerFrequencyInMS);
        }
    }

    /**
     * Logs the specified event with Strap Metrics.
     * <p>
     * This method always returns immediately, whether or not the
     * message was immediately sent to the watch.
     *
     * @param  eventName  The name of the Strap event being logged.
     * @param  jsonData The custom JSON associated with this event
     */
    public void logEvent(String eventName, JSONObject jsonData) {

        //create a new data map entry for this event and load it with data
        PutDataMapRequest dataMap = PutDataMapRequest.create("/strap/" + new Date().toString());

        buildBasicRequest(dataMap.getDataMap());
        dataMap.getDataMap().putString("type", "logEvent");

        if(!eventName.equals("") ) {
            dataMap.getDataMap().putString("eventName", eventName);
            dataMap.getDataMap().putString("type", kLogEventType);

            if(jsonData != null) {
                dataMap.getDataMap().putString("cvar", jsonData.toString());
            }

        } else {
            dataMap.getDataMap().putDataMapArrayList("accelData", mAccelDataMapList);
            dataMap.getDataMap().putString("type", kAcclType);
        }

        //sync the data
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult result) {
                if(result.getStatus().isSuccess()) {
                    Log.d("Callback", "Data item set: " + result.getDataItem());
                } else {
                    Log.e("Callback", "Error setting data item! :" + result.toString());
                }
            }
        });

    }



    /**
     * Logs the specified event with Strap Metrics.
     * <p>
     * This method always returns immediately, whether or not the
     * message was immediately sent to the watch.
     *
     * @param  eventName  The name of the Strap event being logged.
     */
    public void logEvent(String eventName) {
        logEvent(eventName, null);
    }

    private DataMap buildAccelData(float [] coords) {
        DataMap accelDataMap = new DataMap();
        accelDataMap.putFloatArray("coordinates", coords);

        return accelDataMap;
    }


    //Setter for accel data
    private void  setLastAccelData(DataMap accelData) {
        synchronized(lock) {
            lastAccelData = accelData;
        }
    }

    private DataMap getLastAccelData() {
        synchronized(lock) {
            return lastAccelData;
        }
    }

    //Small task implementation for periodically recording accel data.
    private class RecordAccelerometerTask implements Runnable {
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

            accelHandler.postDelayed(this, kAccelerometerFrequencyInMS);

        }
    }

    private class SystemInfoTask implements Runnable {
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

            systemHandler.postDelayed(this, kSystemDataFrequencyInMS);


        }
    }
}
