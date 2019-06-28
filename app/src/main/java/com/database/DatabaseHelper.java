package com.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

import static com.database.RecordTab.Record.COL2;
import static com.database.RecordTab.Record.COL3;
import static com.database.RecordTab.Record.COL4;
import static com.database.RecordTab.Record.TABLE_NAME;
import static com.database.RecordTab.Record._ID;


public class DatabaseHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "record_location.db";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + RecordTab.Record.TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY," +
                    RecordTab.Record.COL2 + " BLOB," +
                    RecordTab.Record.COL3 + " TEXT," +
                    RecordTab.Record.COL4 + " INTEGER);";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + RecordTab.Record.TABLE_NAME;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public Cursor showData() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor data = db.rawQuery("SELECT * FROM " + RecordTab.Record.TABLE_NAME, null);
        return data;
    }

    public Cursor findByAdr(int grp) {
        //String select = "Select * from " + TABLE_NAME + " Where ( Address = " + adr + " )";
        SQLiteDatabase db = this.getReadableDatabase();
        //Cursor data = db.rawQuery(select, null);
        Cursor data = db.query(TABLE_NAME, new String[] {_ID, COL2, COL3, COL4}, _ID + " = ?", new String[] {Integer.toString(grp)}, null, null, null);
        return data;
    }

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

    public boolean addData(String adr, int duration, Location location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL2, SerializationUtils.serialize((Serializable)location));
        contentValues.put(RecordTab.Record.COL3, adr);
        contentValues.put(RecordTab.Record.COL4, duration);

        long result = db.insert(RecordTab.Record.TABLE_NAME, null, contentValues);

        if (result == -1) {
            return false;
        }
        else {
            return true;
        }
    }

    public boolean updateData(int id, String adr, int duration, byte[] location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(_ID, id);
        contentValues.put(COL2, SerializationUtils.serialize((Serializable)location));
        contentValues.put(COL3, adr);
        contentValues.put(COL4, duration);
        db.update(TABLE_NAME, contentValues, _ID + " = ?", new String[] {Integer.toString(id)});
        return true;
    }


}
