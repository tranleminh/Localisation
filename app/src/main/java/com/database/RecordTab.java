package com.database;

import android.provider.BaseColumns;

public final class RecordTab {
    private RecordTab() {}

    public static class Record implements BaseColumns {
        public static final String TABLE_NAME = "record";
        public static final String _ID = BaseColumns._ID;
        public static final String COL2 = "Longitude";
        public static final String COL3 = "Latitude";
        public static final String COL4 = "Address";
        public static final String COL5 = "Duration";
        /*public static final String COL3 = "StartTime";
        public static final String COL4 = "EndTime";
        public static final String COL5 = "Steps";
        public static final String COL6 = "Distance";
        public static final String COL7 = "Duration";
        public static final String COL8 = "AvgSpeed";*/
    }
}
