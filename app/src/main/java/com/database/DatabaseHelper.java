package com.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;


import static com.database.RecordTab.Record.COL2;
import static com.database.RecordTab.Record.COL3;
import static com.database.RecordTab.Record.COL4;
import static com.database.RecordTab.Record.COL5;
import static com.database.RecordTab.Record.TABLE_NAME;
import static com.database.RecordTab.Record._ID;


public class DatabaseHelper extends SQLiteOpenHelper {

    /******************Attribute and global variables declaration**********************/

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "record_location.db";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + RecordTab.Record.TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY," +
                    RecordTab.Record.COL2 + " TEXT," +
                    RecordTab.Record.COL3 + " TEXT," +
                    RecordTab.Record.COL4 + " TEXT," +
                    RecordTab.Record.COL5 + " INTEGER);";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + RecordTab.Record.TABLE_NAME;


    /**
     * Constructor
     * @param context context of the database
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**********************Database's Overridden Methods*********************/
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /**********************Database's Data Manipulation Methods*****************/

    /**
     * Add new data to the database
     * @param adr an address
     * @param duration the duration where user stays in that address
     * @param location coordinates of that address
     * @return true if successfully added data, false otherwise
     */
    public boolean addData(String adr, int duration, Location location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL2, String.valueOf(location.getLongitude()));
        contentValues.put(RecordTab.Record.COL3, String.valueOf(location.getLatitude()));
        contentValues.put(RecordTab.Record.COL4, adr);
        contentValues.put(RecordTab.Record.COL5, duration);

        long result = db.insert(RecordTab.Record.TABLE_NAME, null, contentValues);

        if (result == -1) {
            return false;
        }
        else {
            return true;
        }
    }

    /**
     * Update an existed data base on its ID
     * @param id the ID of the data to be updated
     * @param adr updated address
     * @param duration updated duration
     * @param longi updated longitude
     * @param lati updated latitude
     * @return true when data successfully updated
     * Note : in our case, only the duration is updated, others keep their value.
     */
    public boolean updateData(int id, String adr, int duration, String longi, String lati) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(_ID, id);
        contentValues.put(COL2, longi);
        contentValues.put(RecordTab.Record.COL3, lati);
        contentValues.put(RecordTab.Record.COL4, adr);
        contentValues.put(RecordTab.Record.COL5, duration);
        db.update(TABLE_NAME, contentValues, _ID + " = ?", new String[] {Integer.toString(id)});
        return true;
    }

    /**
     * A method querying all data from the database
     * @return a cursor containing all the data
     */
    public Cursor showData() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor data = db.rawQuery("SELECT * FROM " + RecordTab.Record.TABLE_NAME, null);
        return data;
    }

    /**
     * A method that looks for data by address group number
     * @param grp the address group number
     * @return the cursor containing the data corresponding to that group number
     */
    public Cursor findByAdr(int grp) {
        //String select = "Select * from " + TABLE_NAME + " Where ( Address = " + adr + " )";
        SQLiteDatabase db = this.getReadableDatabase();
        //Cursor data = db.rawQuery(select, null);
        Cursor data = db.query(TABLE_NAME, new String[] {_ID, COL2, COL3, COL4, COL5}, _ID + " = ?", new String[] {Integer.toString(grp)}, null, null, null);
        return data;
    }

    /**
     * Check whether the database is empty or not
     * @return true if database is empty, else false
     */
    public boolean isEmpty() {
        String q = "Select count(*) from " + TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor data = db.rawQuery(q, null);
        data.moveToFirst();
        int n = data.getInt(0);
        if (n == 0) {
            return true;
        }
        else {
            return false;
        }
    }


}
