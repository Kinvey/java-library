/** 
 * Copyright (c) 2013, Kinvey, Inc. All rights reserved.
 *
 * This software contains valuable confidential and proprietary information of
 * KINVEY, INC and is subject to applicable licensing agreements.
 * Unauthorized reproduction, transmission or distribution of this file and its
 * contents is a violation of applicable laws.
 * 
 */
package com.kinvey.android.offline;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.google.api.client.http.UriTemplate;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonGenerator;
import com.kinvey.android.Client;
import com.kinvey.android.callback.KinveyDeleteCallback;
import com.kinvey.android.callback.KinveyUserCallback;
import com.kinvey.java.Query;
import com.kinvey.java.User;
import com.kinvey.java.core.KinveyClientCallback;
import com.kinvey.java.model.KinveyDeleteResponse;
import com.kinvey.java.offline.*;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * This Android Service listens for intents and uses the {@link com.kinvey.android.AsyncAppData} API to execute requests
 *
 * @author edwardf
 * @since 2.0
 */
public class KinveySyncService extends IntentService {

    public static final String ACTION_OFFLINE_SYNC = "com.kinvey.android.ACTION_OFFLINE_SYNC";

    //allows clients to bind
    private final IBinder mBinder = new KBinder();
    private final String TAG = "Kinvey - SyncService";

    private Client client;

    public KinveySyncService(String name) {
        super(name);

    }

    public KinveySyncService() {
        super("Kinvey Sync Service");

    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void ping() {
        Log.i(TAG, "ping success!");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, startId, startId);
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        onHandleIntent(intent);

        return START_STICKY;
    }

    /**
     * This class is called by the Android OS, when a registered intent is fired off.
     * <p/>
     * Check <intent-filter> elements in the Manifest.xml to see which intents are handled by this method.
     *
     * @param intent the intent to act upon.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(Client.TAG, "Offline Executor Intent received ->" + intent.getAction());
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        if (isOnline()) {
            initClientAndKickOffSync();
        }

        //all intents do the same thing (as of now) so no distinction is necessary
        /**
         if (action.equals(ACTION_OFFLINE_SYNC)) {
         Log.i(TAG, "offline sync");
         if (isOnline()) {
         initClientAndKickOffSync();
         }
         } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
         Log.i(TAG, "connectivity actions");

         if (isOnline()) {
         initClientAndKickOffSync();
         }
         } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
         Log.i(TAG, "network state change");
         if (isOnline()) {
         initClientAndKickOffSync();
         }
         }

         */

    }

    public boolean isOnline() {

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            Log.v(Client.TAG, "Kinvey Sync Execution is gonna happen!  Device connected.");
            return true;
        }
        Log.v(Client.TAG, "Kinvey Sync Execution is not happening, device is offline.");

        return false;


    }

    private void initClientAndKickOffSync() {

        if (client == null) {
            client = new Client.Builder(getApplicationContext()).setRetrieveUserCallback(new KinveyUserCallback() {
                @Override
                public void onSuccess(User result) {
                    getFromStoreAndExecute();
                }

                @Override
                public void onFailure(Throwable error) {
                    Log.e(TAG, "Unable to login from Kinvey Sync Service! -> " + error);
                }
            }).build();
            client.enableDebugLogging();


        } else {
            getFromStoreAndExecute();
        }

    }

    public void getFromStoreAndExecute() {
        OfflineHelper dbHelper = new OfflineHelper(getApplicationContext());
        List<String> collectionNames = dbHelper.getCollectionTables();


        for (String s : collectionNames) {
            boolean done = false;

            while (!done){
                OfflineRequestInfo req = dbHelper.getTable(s).popSingleQueue(dbHelper);
                if (req == null){
                    done = true;
                }else{
                    executeRequest(dbHelper, req, s);
                }
            }
        }
    }

    private void executeRequest(final OfflineHelper dbHelper, final OfflineRequestInfo cur, final String collectionName) {


        if (cur.getHttpVerb().equals("PUT") || cur.getHttpVerb().equals(("POST"))) {
            client.appData(collectionName, GenericJson.class).save(dbHelper.getEntity(client, client.appData(collectionName, GenericJson.class), cur.getEntityID()), new KinveyClientCallback<GenericJson>() {
                @Override
                public void onSuccess(GenericJson result) {
                    KinveySyncService.this.storeCompletedRequestInfo(collectionName, true, cur, result);
                }

                @Override
                public void onFailure(Throwable error) {
                    KinveySyncService.this.storeCompletedRequestInfo(collectionName, false, cur, error.getMessage());
                }
            });
        } else if (cur.getHttpVerb().equals("GET")){
            client.appData(collectionName, GenericJson.class).getEntity(cur.getEntityID(), new KinveyClientCallback<GenericJson>() {
                @Override
                public void onSuccess(GenericJson result) {
                    KinveySyncService.this.storeCompletedRequestInfo(collectionName, true, cur, result);
                    //update datastore with response
                    dbHelper.getTable(collectionName).insertEntity(dbHelper, client, result);
                }

                @Override
                public void onFailure(Throwable error) {
                    KinveySyncService.this.storeCompletedRequestInfo(collectionName, false, cur, error.getMessage());
                }
            });
        } else if (cur.getHttpVerb().equals("DELETE")){

            client.appData(collectionName, GenericJson.class).delete(cur.getEntityID(), new KinveyDeleteCallback() {
                @Override
                public void onSuccess(KinveyDeleteResponse result) {
                    KinveySyncService.this.storeCompletedRequestInfo(collectionName, true, cur, result);
                    dbHelper.getTable(collectionName).deleteEntity(dbHelper, cur.getEntityID());
                }

                @Override
                public void onFailure(Throwable error) {
                    KinveySyncService.this.storeCompletedRequestInfo(collectionName, false, cur, error.getMessage());
                }
            });
        }else if (cur.getHttpVerb().equals("QUERY")){
            client.appData(collectionName, GenericJson[].class).getEntity(cur.getEntityID(), new KinveyClientCallback<GenericJson[]>() {
                @Override
                public void onSuccess(GenericJson[] result) {
                    List<String> resultIds = new ArrayList<String>();

                    for (GenericJson res : result){
                        KinveySyncService.this.storeCompletedRequestInfo(collectionName, true, cur, res);
                        //update datastore with response
                        dbHelper.getTable(collectionName).insertEntity(dbHelper, client, res);
                        resultIds.add(res.get("_id").toString());
                    }

                    dbHelper.getTable(collectionName).storeQueryResults(dbHelper, cur.getEntityID(),resultIds);
                }

                @Override
                public void onFailure(Throwable error) {
                    KinveySyncService.this.storeCompletedRequestInfo(collectionName, false, cur, error.getMessage());
                }
            });
        }
    }

    private void storeCompletedRequestInfo(String collectionName, boolean success, OfflineRequestInfo info, String returnValue) {
        OfflineHelper dbHelper = new OfflineHelper(getApplicationContext());
        dbHelper.getTable(collectionName).storeCompletedRequestInfo(dbHelper, collectionName, success, info, returnValue);

        //if request failed, re-queue it
        if (!success){
            dbHelper.getTable(collectionName).enqueueRequest(dbHelper, info.getHttpVerb(), info.getEntityID());
        }

    }

    private void storeCompletedRequestInfo(String collectionName, boolean success, OfflineRequestInfo info, GenericJson returnValue) {

        String jsonResult = "";
        StringWriter writer = new StringWriter();
        try {
            JsonGenerator generator = client.getJsonFactory().createJsonGenerator(writer);
            generator.serialize(returnValue);
            generator.flush();
            jsonResult = writer.toString();
        } catch (Exception ex) {
            Log.e(TAG, "unable to serialize JSON! -> " + ex);
        }

        storeCompletedRequestInfo(collectionName, success, info, jsonResult);


    }


    /**
     * Binder coupled with this Service
     *
     */
    public class KBinder extends Binder{
        public KinveySyncService getService(){
            return KinveySyncService.this;
        }

    }

}