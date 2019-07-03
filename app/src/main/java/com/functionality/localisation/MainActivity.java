package com.functionality.localisation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.database.DatabaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity implements FetchAddressTask.OnTaskCompleted {


    /**********************Global Variable and Attributes Declaration**************************/

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final String TRACKING_LOCATION_KEY = "LOCATION_KEY";
    private static final double DISTANCE_THRESHOLD = 53.0;
    private static final int LOCATION_PERIOD = 1000;
    private static final int FASTEST_INTERVAL = 500;
    private Location[] adrGroup = new Location[1024];
    private int tabIndex = 0;
    private Button BtnStart;
    private Button BtnDebug;
    private Button BtnViewData;
    private Location mLastLocation;
    private boolean mTrackingLocation = false;
    private boolean changeAdr = true;
    private int groupResult = 0;
    private long timeBase = System.currentTimeMillis();
    private int time = 0;
    private String adr = "";
    private Location old_location = null;
    private TextView mLocationTextView;
    private TextView Timer;
    private TextView Longitude;
    private TextView Latitude;
    private TextView Log;
    private double longitude = 0;
    private double latitude = 0;
    private TextView Status;
    private LocationCallback mLocationCallback;
    private FusedLocationProviderClient mFusedLocationClient;
    private DatabaseHelper recordDB;


    /***************Location Tracker's Methods*************************/

    /**
     * Method that demands user's permission for location tracking, if
     * there is permission then start tracking location
     */
    private void startTrackingLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            mFusedLocationClient.requestLocationUpdates
                    (getLocationRequest(), mLocationCallback,
                            null /* Looper */);
            mLocationTextView.setText("Loading...");
        }
    }

    /**
     * Stop the location tracking by removing the thread on location updates.
     * Also reverses the Stop button back to Start.
     */
    private void stopTrackingLocation() {
        if (mTrackingLocation) {
            mTrackingLocation = false;
            BtnStart.setText("Start");
            Status.setText("Tracking Location Stopped !");
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    /**
     * Configure a LocationRequest in term of Period, Fastest Interval
     * and Priority. Currently it is configured to request every 1 sec.
     * @return a configured location request
     */
    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(LOCATION_PERIOD);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    /**********************Private conversion method***********************/

    /**
     * Convert a given time in seconds to a format of hours-minutes-seconds
     * @param time given time in seconds
     * @return a String containing time in format hours-minutes-seconds
     */
    private String time_conv(int time) {
        String res = "";
        int s = time;
        int m = 0;
        int h = 0;
        if (s < 60) {
            res += s + "s";
        } else {
            if (s >= 60 && s < 3600) {
                m = s / 60;
                s = s % 60;
                res += m + "m" + s + "s";
            } else {
                h = s / 3600;
                m = (s % 3600) / 60;
                s = (s % 3600) % 60;
                res += h + "h" + m + "m" + s + "s";
            }
        }
        return res;
    }

    /************************Database displaying methods*************************/

    /**
     * Implements the View Data button's functionality. It queries the
     * database, appends all data needed for display in a String Buffer then display it.
     */
    public void viewData() {
        BtnViewData.setOnClickListener(view -> {
            Cursor data = recordDB.showData();

            if (data.getCount() == 0) {
                display("Error", "No Data Found!");
                return;
            }
            StringBuffer buffer = new StringBuffer();
            while(data.moveToNext()) {
                buffer.append("GroupAddress: " + data.getString(0) + "\n");
                buffer.append("Address: " + data.getString(3) + "\n");
                buffer.append("Duration: " + time_conv(data.getInt(4)) + "\n");
                buffer.append("--------------------------------------\n");
            }
            display("All Stored Data:", buffer.toString());
        });
    }


     /**
     * An auxiliary method used in viewData(). This method creates an alert dialog to show database' stored data.
     * @param title the title of the alert dialog
     * @param message the message to be displayed - in our case, the String Buffer containing data.
     */
    public void display(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }

    /*********************************Address Group Table's manipulation**************************************/

    /**
     * Initializes the table with the address group and the location already stored in the database
     */
    private void initTab() {
        Cursor data = recordDB.showData();

        if (data.getCount() > 0) {
            Toast.makeText(MainActivity.this, "Group table initialized!", Toast.LENGTH_LONG).show();
            while(data.moveToNext()) {
                Location location = new Location("");
                location.setLongitude(Double.parseDouble(data.getString(1)));
                location.setLatitude(Double.parseDouble(data.getString(2)));
                adrGroup[tabIndex] = location;
                tabIndex++;
            }
        }
    }

    /**
     * Add a new address group linked with the central location in the table.
     * The table index is served as group's number.
     * @param adr the central location to be added in a new address group
     */
    private void updateAdrGroup(Location adr) {
        adrGroup[tabIndex] = adr;
        tabIndex++;
    }

    /**
     * When detected a new location, check if this location belongs to
     * an address group already existed in the table.
     * @param adr the location to be checked
     * @return the group's number if the examined location belongs to that group, else return -1
     */
    private int groupSearch(Location adr) {
        if (tabIndex == 0) {
            Toast.makeText(MainActivity.this, "Group Address Tab empty, first insert at " + (tabIndex + 1), Toast.LENGTH_LONG).show();
            return -1;
        }
        else {
            double min = 999999;
            int res = -1;
            for (int i = 0; i < tabIndex; i++) {
                if (min > adr.distanceTo(adrGroup[i])) {
                    min = adr.distanceTo(adrGroup[i]);
                    res = i;
                }
            }
            if (min < DISTANCE_THRESHOLD) {
                Toast.makeText(MainActivity.this, "This address belongs to group " + res, Toast.LENGTH_LONG).show();
                return res;
            } else {
                Toast.makeText(MainActivity.this, "This address doesnt belongs to any group, new insert at " + (tabIndex+ 1), Toast.LENGTH_LONG).show();
                return -1;
            }
        }
    }


    /****************************************Android Application Overridden Methods************************************/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION:
                // If the permission is granted, get the location,
                // otherwise, show a Toast
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startTrackingLocation();
                } else {
                    Toast.makeText(this,
                            "Location permission denied!",
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Instantiate the database
        recordDB = new DatabaseHelper(this);

        Status = findViewById(R.id.status);
        Longitude = findViewById(R.id.longitude);
        Latitude = findViewById(R.id.latitude);
        Timer = findViewById(R.id.timer);
        mLocationTextView = findViewById(R.id.address);
        Log = findViewById(R.id.log);

        //Initialize address group table
        initTab();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        BtnStart = findViewById(R.id.btn_start);
        BtnDebug = findViewById(R.id.btn_debug);
        BtnViewData = findViewById(R.id.btn_db);

        viewData();

        BtnStart.setOnClickListener(view -> {
            if (!mTrackingLocation) {
                startTrackingLocation();
                Status.setText("Tracking Location Started !");
                BtnStart.setText("Stop");
                mTrackingLocation = true;
            }
            else
                stopTrackingLocation();

        });

        BtnDebug.setOnClickListener(view -> {
            String msg = "";
            if (tabIndex > 1) {
                for (int i = 0; i < tabIndex; i++) {
                    for (int j = i; j < tabIndex; j++) {
                        if (i != j)
                            msg += "Distance between group " + (i+1) + " and group " + (j+1) + ": " + adrGroup[i].distanceTo(adrGroup[j]) + "\n";
                    }
                }
            }
            display("debugging", msg);
        });
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                mLastLocation = locationResult.getLastLocation();
                if (old_location == null) {
                    old_location = mLastLocation;
                }
                if (mLastLocation.distanceTo(old_location) < DISTANCE_THRESHOLD) {
                    mLastLocation = old_location;
                }
                else {
                    if (changeAdr) {
                        int grp = groupSearch(old_location);
                        if (grp == -1) {
                            Toast.makeText(MainActivity.this, "Trying to add location here", Toast.LENGTH_LONG).show();
                            updateAdrGroup(old_location);
                            groupResult = tabIndex;
                        } else {
                            groupResult = grp;
                        }
                        old_location = mLastLocation;
                        changeAdr = false;
                    }
                }
                longitude = mLastLocation.getLongitude();
                latitude = mLastLocation.getLatitude();
                Log.setText("Current index : " + (tabIndex + 1));
                if (mLastLocation != null) {
                    Longitude.setText("Longitude : " + longitude);
                    Latitude.setText("Latitude : " + latitude);
                    time = (int)((System.currentTimeMillis() - timeBase)/1000f);
                    Timer.setText("Time : " +time_conv(time));
                    new FetchAddressTask(MainActivity.this, MainActivity.this)
                            .execute(mLastLocation);
                }
                else {
                    Longitude.setText("No location detected");
                    Latitude.setText("No location detected");
                }
            }
        };
        if (savedInstanceState != null) {
            mTrackingLocation = savedInstanceState.getBoolean(
                    TRACKING_LOCATION_KEY);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(TRACKING_LOCATION_KEY, mTrackingLocation);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onTaskCompleted(String result) {
        if (adr.equals("")) {
            adr = result;
        }
        mLocationTextView.setText(getString(R.string.address_text,
                result));
        if (!adr.equals(result)) {
            changeAdr = true;
            if (recordDB.isEmpty()) {
                Toast.makeText(MainActivity.this, "Database empty, first insert here." + adr, Toast.LENGTH_LONG).show();
                boolean res = recordDB.addData(adr, time, old_location);
                if (res) {
                    Toast.makeText(MainActivity.this, "New Data Inserted!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to insert new data", Toast.LENGTH_LONG).show();
                }
            }
            else {
                Toast.makeText(MainActivity.this, "Looking for ID by address " + adr, Toast.LENGTH_LONG).show();
                Cursor data = recordDB.findByAdr(groupResult);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    boolean res = recordDB.addData(adr, time, old_location);
                    Toast.makeText(MainActivity.this, "New address group detected ! " + groupResult + " associated with " + adr, Toast.LENGTH_LONG).show();
                    if (res) {
                        Toast.makeText(MainActivity.this, "New Data Inserted!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to insert new data", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Update Duration for " + groupResult, Toast.LENGTH_LONG).show();
                    recordDB.updateData(data.getInt(0), data.getString(3), data.getInt(4) + time, data.getString(1), data.getString(2));
                }
            }
            adr = result;
            timeBase = System.currentTimeMillis();
        }
    }
}
