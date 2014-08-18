package com.straphq.wear_sdk;

import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;

import org.json.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
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
    //    private String strapURL = "http://192.168.2.8:8000/create/visit/with/";
    private String appID;

    Calendar mCalendar = new GregorianCalendar();
    TimeZone mTimeZone = mCalendar.getTimeZone();
    int mGMTOffset = mTimeZone.getRawOffset();
    long tz_offset = TimeUnit.HOURS.convert(mGMTOffset, TimeUnit.MILLISECONDS);


    private JSONArray tmpstore = new JSONArray();

    public StrapMetrics(String url, String appid) {
        appID = appid;
        strapURL = url;
    }

    public StrapMetrics(String appid) {
        appID = appid;
    }

    public StrapMetrics() {
    }

    private void concatJSONArrays(JSONArray result, JSONArray tmp) throws JSONException {
        // append the values in tmp to the result JSONArray
        for (int i = 0; i < tmp.length(); i++) {
            result.put(tmp.getJSONObject(i));
        }
    }

    public Boolean canHandleMsg(DataEvent data) {

//        Log.d("SM_MSG_HANDLER", data.getDataItem().getUri().getPathSegments().get(0));

        return data.getDataItem().getUri().getPathSegments().get(0).equals("strap");
    }


    public void processReceiveData(DataMap map) throws JSONException, IOException {
        String query;

        String serial = null;

        //DataMapItem dataMapItem = DataMapItem.fromDataItem(data.getDataItem());
       // DataMap map = dataMapItem.getDataMap();

        final Properties lp = new Properties();
        String resolution = "UNK";
        if(map.containsKey("display_width") && map.containsKey("display_height")) {
            resolution = map.getInt("display_width") + "x" + map.getInt("display_height");
        }
        lp.put("resolution", resolution);
        lp.put("useragent", "WEAR/1.0");

        int min_readings = 200;
        /*
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            serial = (String) get.invoke(c, "ro.serialno");
        } catch (Exception ignored) {
        }*/



        String key = "eventName";
        if(!map.containsKey(key)) {
            //JSONArray convData = StrapMetrics.convAcclData(data);

            //concatJSONArrays(tmpstore, convData);

            if(tmpstore.length() > min_readings || true) {


                query = "app_id=" + appID
                        + "&resolution=" + ((lp.getProperty("resolution").length() > 0) ?  lp.getProperty("resolution") : "")
                        + "&useragent=" + ((lp.getProperty("useragent").length() > 0) ?  lp.getProperty("useragent") : "")
                        + "&action_url=" + "STRAP_API_ACCL"
                        + "&visitor_id=" + serial
                        + "&visitor_timeoffset=" + tz_offset
                        + "&accl=" + URLEncoder.encode(tmpstore.toString(),"UTF-8")
//                   + "&act=" + ((tmpstore.length() > 0)?tmpstore[0].act:"UNKNOWN");
                        + "&act=UNKNOWN";

                //console.log('query: ' + query);



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
        else {

            query = "app_id=" +appID
                    + "&resolution=" + ((lp.getProperty("resolution").length() > 0) ?  lp.getProperty("resolution") : "")
                    + "&useragent=" + ((lp.getProperty("useragent").length() > 0) ?  lp.getProperty("useragent") : "")
                    + "&visitor_id=" + serial
                    + "&visitor_timeoffset=" + tz_offset
                    + "&action_url=" + map.getString(key);

            try {
                Runnable r = new PostLog(strapURL,query);
                new Thread(r).start();

            } catch (Exception e) {
                Log.e("POST_ERROR","ERROR with PostLog Thread: " + e.toString());
                e.printStackTrace();
            }
        }
    }

    public static JSONArray convAcclData(DataEvent data) throws JSONException {
        JSONArray convData = new JSONArray();

//        int key = KEY_OFFSET + T_TIME_BASE;
//        long time_base = Long.parseLong(data.getString(key));
//        data.remove(key);
//
//        for(int i = 0; i < strap_api_num_samples; i++) {
//            int point = KEY_OFFSET + (10 * i);
//
//            JSONObject ad = new JSONObject();
//            // ts key
//            key = point + T_TS;
//            ad.put("ts", (data.getInteger(key) + time_base));
//            data.remove(key);
//
//            // x key
//            key = point + T_X;
//            ad.put("x", data.getInteger(key));
//            data.remove(key);
//
//            // y key
//            key = point + T_Y;
//            ad.put("y", data.getInteger(key));
//            data.remove(key);
//
//            // z key
//            key = point + T_Z;
//            ad.put("z", data.getInteger(key));
//            data.remove(key);
//
//            // did_vibrate key
//            key = point + T_DID_VIBRATE;
//            ad.put("vib", (data.getString(key) == "1")?true:false);
//            data.remove(key);
//
//            ad.put("act", data.getString(KEY_OFFSET + T_ACTIVITY));
//            data.remove(key);
//
//            convData.put(ad);
//        }

        return convData;
    }


//    public void postLog() throws IOException {
//
//    }
//
}