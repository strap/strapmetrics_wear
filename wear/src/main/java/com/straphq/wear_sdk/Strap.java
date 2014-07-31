package com.straphq.wear_sdk;

import android.app.Activity;
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

public class Strap {

    private GoogleApiClient mGoogleApiClient = null;

    Strap(GoogleApiClient apiClient) {
        mGoogleApiClient = apiClient;
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
}
