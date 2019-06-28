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
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.database.DatabaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.sql.Time;

public class MainActivity extends AppCompatActivity implements FetchAddressTask.OnTaskCompleted {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final String TAG = "";
    private static final String TRACKING_LOCATION_KEY = "LOCATION_KEY";
    private static final double DISTANCE_THRESHOLD = 53.0;
    private Location[] adrGroup = new Location[1024];
    private int tabIndex = 0;
    private Button BtnStart;
    private Button BtnDebug;
    private Button BtnViewData;
    private Location mLastLocation;
    private boolean mTrackingLocation = false;
    private boolean changeAdr = true;
    //private float max = -999999;
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

    private void stopTrackingLocation() {
        if (mTrackingLocation) {
            mTrackingLocation = false;
            BtnStart.setText("Start");
            Status.setText("Tracking Location Stopped !");
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    /*private double checkLongitude(double newLong) {
        if (Math.abs(longitude - newLong) < 0.001) {
            return longitude;
        }
        else {
            return newLong;
        }
    }

    private double checkLatitude(double newLat) {
        if (Math.abs(latitude - newLat) < 0.001) {
            return latitude;
        }
        else {
            return newLat;
        }
    }*/

    private String time_conv(int time) {
        String res = "";
        int s = time;
        int m = 0;
        int h = 0;
        /*if (s == 0) {
            res = "1s";
        }
        else {*/
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
        //}
        return res;
    }

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


    /****************************************************************************************************************************
     METHOD display() : an auxiliary method used in viewData(). This method creates an alert dialog to show database' stored data.
     ****************************************************************************************************************************/
    public void display(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }

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

    private void updateAdrGroup(Location adr) {
        adrGroup[tabIndex] = adr;
        tabIndex++;
    }

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
                    //changeAdr = false;
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
                    //changeAdr = true;
                }
                /*if (old_location != null) {
                    if (mLastLocation.distanceTo(old_location) > max)
                        max = mLastLocation.distanceTo(old_location);
                    Log.setText("Maximal distance from same point = " + max);
                }*/
                longitude = mLastLocation.getLongitude();
                latitude = mLastLocation.getLatitude();
                Log.setText("Current index : " + (tabIndex + 1));
                if (mLastLocation != null) {
                    Longitude.setText("Longitude : " + longitude);
                    Latitude.setText("Latitude : " + latitude);
                    time = (int)((locationResult.getLastLocation().getTime() - timeBase)/1000f);
                    Timer.setText("Time : " +time_conv(time));
                    new FetchAddressTask(MainActivity.this, MainActivity.this)
                            .execute(mLastLocation);
                }
                else {
                    Longitude.setText("No location detected");
                    Latitude.setText("No location detected");
                }
                /*if (old_location == null || mLastLocation.distanceTo(old_location) < DISTANCE_THRESHOLD) {
                    old_location = mLastLocation;
                    changeAdr = true;
                }*/
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
                    //Toast.makeText(MainActivity.this, "This address has id " + data.getString(0) + " and duration of " + data.getInt(2), Toast.LENGTH_LONG).show();
                    //Toast.makeText(MainActivity.this, "This data has id : " + data.getString(0), Toast.LENGTH_LONG).show();
                }
            }
            //Log.setText("Address changed from " + adr + " to " + result + " during " + time_conv(time));
            adr = result;
            timeBase = System.currentTimeMillis();
        }
    }
}
