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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.google.api.client.json.GenericJson;
import com.kinvey.android.AsyncAppData;
import com.kinvey.java.AbstractClient;
import com.kinvey.java.AppData;
import com.kinvey.java.offline.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class manages a set of {@link OfflineTable}s.
 *
 * This class is used by the OS to access sqllite, and delegates control to the appropriate OfflineTable.
 *
 *
 * @author edwardf
 * @since 2.0
 */
public class OfflineHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    private String collectionName;

    private static final String DB_NAME = "kinveyOffline.db";

    private static final String COLLECTION_TABLE = "collections";
    private static final String COLUMN_NAME = "name";

    /**
     * Used for creating new database tables, collection name is used for generating table name
     *
     * @param context
     * @param collectionName
     */
    public OfflineHelper(Context context, String collectionName) {
        super(context, DB_NAME, null, DATABASE_VERSION);
        this.collectionName = collectionName;
    }

    /**
     * used for accessing ALREADY existing tables
     *
     * @param context
     */
    public OfflineHelper(Context context){
        super(context, DB_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called by operating system when a new database is created
     *
     * @param database
     */
    @Override
    public void onCreate(SQLiteDatabase database) {
       OfflineTable table = new OfflineTable(collectionName);
       table.onCreate(database);

    }

    /**
     * Called by operating system when a database needs to be upgraded
     *
     * @param database
     * @param oldVersion
     * @param newVersion
     */
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        OfflineTable table = new OfflineTable(collectionName);
        table.onUpgrade(database, oldVersion, newVersion);
    }

    /**
     * Creates a new collection table, adds it to the metadata table, and returns it
     *
     * @param collectionName - the collection to create a new table for
     * @return
     */
    public OfflineTable getTable(String collectionName){
        createCollectionTable();
        addCollection(collectionName);
        return new OfflineTable(collectionName);
    }

    /**
     * Add a collection to the metadata table if it doesn't already exist
     *
     * @param collectionName
     */
    private void addCollection(String collectionName){

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, collectionName);

        int change = db.updateWithOnConflict(COLLECTION_TABLE, values, null, null, db.CONFLICT_REPLACE);
        if (change == 0){
            db.insert(COLLECTION_TABLE, null, values);
        }

    }

    /**
     * query the metdata table for a list of all collection tables, returning them as a list
     *
     * @return a list of collection table names
     */
    public List<String> getCollectionTables(){
        ArrayList<String> ret = new ArrayList<String>();

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(COLLECTION_TABLE, new String[]{COLUMN_NAME}, null, null, null, null, null);
        while (c.moveToNext()){
            ret.add(c.getString(0));
        }
        c.close();
        db.close();
        return ret;



    }


    /**
     * Get an entity directly from an offline table
     *
     * @param client - an instance of the client
     * @param appData - an instance of appdata
     * @param id - the id of the entity to return
     * @return
     */

    public GenericJson getEntity(AbstractClient client, AppData appData, String id) {
        //ensure table exists, if not, create it   <- done by constructor of offlinehelper (oncreate will delegate)
        SQLiteDatabase db = getWritableDatabase();

        GenericJson ret;

        ret = getTable(appData.getCollectionName()).getEntity(this, client, id, appData.getCurrentClass());

        return ret;
    }


    /**
     * this method does not close the DB, it is assumed an entry will be added immediately after (and that operation will close it)
     */
    private void createCollectionTable(){
        SQLiteDatabase db = getWritableDatabase();

        String createCommand = "CREATE TABLE IF NOT EXISTS "
                + COLLECTION_TABLE
                + "("
                + COLUMN_NAME + " TEXT not null "
                + ");";

        OfflineTable.runCommand(db, createCommand);

    }
}