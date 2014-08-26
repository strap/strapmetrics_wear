package com.straphq.wear_sdk;

import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;



public class StrapMetrics {

    private static void setDefaultUncaughtExceptionHandler() {
        try {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    Log.e("THREAD_EXCEPTION","Uncaught Exception detected in thread " + t, e);
                }
            });
        } catch (SecurityException e) {
            Log.e("SECURITY_EXCEPTION","Could not set the Default Uncaught Exception Handler", e);
        }
    }

    private String strapURL = "https://api.straphq.com/create/visit/with/";

    private static final String userAgent = "WEAR/1.0";
    private static final String eventKey = "eventName";
    //    private String strapURL = "http://192.168.2.8:8000/create/visit/with/";

    private static final String kLogEventType = "logEvent";
    private static final String kAcclType = "logAccl";
    private static final String kDiagnosticType = "logDiagnostic";

    private static final float kConversionFactor = 101.971621298f;

    Calendar mCalendar = new GregorianCalendar();
    TimeZone mTimeZone = mCalendar.getTimeZone();
    int mGMTOffset = mTimeZone.getRawOffset();
    long tz_offset = TimeUnit.HOURS.convert(mGMTOffset, TimeUnit.MILLISECONDS);


    private static JSONArray tmpstore = new JSONArray();


    private static StrapMetrics instance = null;


    /**
     * Starts StrapMetrics on the phone.
     */
    public StrapMetrics() {

    }


    private void concatJSONArrays(JSONArray tmp) throws JSONException {
        // append the values in tmp to the result JSONArray
        for (int i = 0; i < tmp.length(); i++) {
            tmpstore.put(tmp.getJSONObject(i));
        }
    }

    private String getBaseQuery(Properties lp, String serial) {
        return "app_id=" + lp.getProperty("appId")
                + "&resolution=" + ((lp.getProperty("resolution").length() > 0) ?  lp.getProperty("resolution") : "")
                + "&useragent=" + ((lp.getProperty("useragent").length() > 0) ?  lp.getProperty("useragent") : "")
                + "&visitor_id=" + serial
                + "&visitor_timeoffset=" + tz_offset;
    }

    private String getEventQuery(String eventName, Properties lp, String serial) {
        String query = null;
        query = getBaseQuery(lp, serial)

                + "&action_url=" + eventName;

        return query;
    }

    private String getAcclQuery(Properties lp, String serial) throws JSONException, IOException{
        String query = getBaseQuery(lp, serial);

        query = query
                + "&action_url=" + "STRAP_API_ACCL"
                + "&accl=" + URLEncoder.encode(tmpstore.toString(),"UTF-8")
                + "&act=UNKNOWN";

        return query;
    }

    private String getDiagnosticQuery(Properties lp, String serial, JSONObject diagnostics) throws JSONException, IOException{
        String query = getBaseQuery(lp, serial);

        query = query
                + "&action_url=" + "STRAP_DIAG"
                + "&cvar=" + URLEncoder.encode(diagnostics.toString(), "UTF-8");

        return query;
    }

    /**
     * Checks to see if StrapMetrics can handle the data event from Wear.
     * @param data The data event received from the android wear device
     * @return Whether or not StrapMetrics can handle the data event
     */
    public Boolean canHandleMsg(DataEvent data) {
        return data.getDataItem().getUri().getPathSegments().get(0).equals("strap");
    }


    /**
     * Processes a StrapMetrics DataMap.
     * @param map The data map from an event that StrapMetrics can handle.
     */
    public void processReceiveData(DataMap map) throws JSONException, IOException {
        String query = "";
        boolean bWasAcclRequest = false;

        String serial = null;


        //DataMapItem dataMapItem = DataMapItem.fromDataItem(data.getDataItem());
       // DataMap map = dataMapItem.getDataMap();

        final Properties lp = new Properties();
        String resolution = "UNK";
        if(map.containsKey("display_width") && map.containsKey("display_height")) {
            resolution = map.getInt("display_width") + "x" + map.getInt("display_height");
        }
        lp.put("resolution", resolution);
        lp.put("useragent", userAgent);
        lp.put("appId",map.getString("appID"));
        int min_readings = 200;

        // using Android phone's serial no for the visitor id right now

        Class<?> c = null;
        try {
            c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            serial = (String) get.invoke(c, "ro.serialno");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if(map.getString("type").equals(kAcclType)) {
            JSONArray convData = StrapMetrics.convAcclData(map);
            concatJSONArrays(convData);

            if(tmpstore.length() >= min_readings) {
                query = getAcclQuery(lp, serial);
                bWasAcclRequest = true;

                try {
                    Runnable r = new PostLog(strapURL,query);
                    new Thread(r).start();

                } catch (Exception e) {
                    Log.e("POST_ERROR","ERROR with PostLog Thread: " + e.toString());
                    e.printStackTrace();

                }
                tmpstore = new JSONArray();
            }
        }
        else if(map.getString("type").equals(kLogEventType)){
            query = getEventQuery(map.getString(eventKey), lp, serial);

            try {
                Runnable r = new PostLog(strapURL,query);
                new Thread(r).start();

            } catch (Exception e) {
                Log.e("POST_ERROR","ERROR with PostLog Thread: " + e.toString());
                e.printStackTrace();
            }
        }
        else if(map.getString("type").equals(kDiagnosticType)) {
            JSONObject diagnostics = new JSONObject();
            diagnostics.put("brightness", map.getInt("brightness"));
            diagnostics.put("battery", map.getInt("battery"));
            diagnostics.put("time", map.getLong("time"));

            query = getDiagnosticQuery(lp, serial, diagnostics);

            try {
                Runnable r = new PostLog(strapURL,query);
                new Thread(r).start();

            } catch (Exception e) {
                Log.e("POST_ERROR","ERROR with PostLog Thread: " + e.toString());
                e.printStackTrace();
            }
        }

    }

    private static JSONArray convAcclData(DataMap data) throws JSONException {
        JSONArray convData = new JSONArray();

        ArrayList<DataMap> accelDataEvents = data.getDataMapArrayList("accelData");

        for(int i = 0; i < accelDataEvents.size(); i++) {
            DataMap accelEvent = accelDataEvents.get(i);
            JSONObject ad = new JSONObject();
            float[] coords = accelEvent.getFloatArray("coordinates");

            float x = coords[0] * kConversionFactor;
            float y = coords[1] * kConversionFactor;
            float z = coords[2] * kConversionFactor;

            ad.put("x", Math.round(x));
            ad.put("y", Math.round(y));
            ad.put("z", Math.round(z));
            ad.put("ts", accelEvent.getLong("time"));

            convData.put(ad);

        }

        return convData;
    }
}