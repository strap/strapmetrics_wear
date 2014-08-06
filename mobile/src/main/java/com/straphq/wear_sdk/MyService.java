package com.straphq.wear_sdk;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.*;
import android.util.Log;

import com.google.android.gms.wearable.*;
import com.google.android.gms.common.api.*;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.*;
import android.net.Uri;

import org.json.JSONException;

public class MyService extends WearableListenerService {

    private static final String TAG = "DataLayerSample";
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";

    private final StrapMetrics sm = new StrapMetrics();


    public void onPeerConnected(com.google.android.gms.wearable.Node peer) {
        super.onPeerConnected(peer);
        String id = peer.getId();
        String name = peer.getDisplayName();

        Log.d("Wear_connect", "Connected peer name & ID: " + name + "|" + id);
    }


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onDataChanged: " + dataEvents);
        }


        // Loop through the events and send a message
        // to the node that created the data item.
        for (DataEvent event : dataEvents) {
            Uri uri = event.getDataItem().getUri();
            DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
            DataMap map = dataMapItem.getDataMap();
            if (sm.canHandleMsg(event)) {
                Log.d("DataEvent","Received new strapmetrics event!! " + map.toString());
                try {
                    sm.processReceiveData(event);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d("DataEvent","NOT a strapmetrics event!! " + map.toString());
            }


//
//
//
//            Log.d("DataEvent","The URI is " + dataMapItem.getUri());
//            Log.d("DataEvent","The URI segments " + dataMapItem.getUri().getPathSegments());
//            Log.d("DataEvent","Received new Data Event from wearable! " + map.toString());
            //TODO sync with Strap Metrics
            //Basically, this should check if strap can handle the data map. If so, strap should
            //handle the event, otherwise the user can handle it however they want.



        }
    }

}
