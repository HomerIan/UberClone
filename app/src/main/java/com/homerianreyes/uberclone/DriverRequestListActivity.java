package com.homerianreyes.uberclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.homerianreyes.uberclone.databinding.ActivityDriverRequestListBinding;
import com.parse.FindCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.shashank.sony.fancytoastlib.FancyToast;

import java.util.ArrayList;
import java.util.List;

public class DriverRequestListActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private ActivityDriverRequestListBinding binding;
    private View viewRoot;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private ArrayAdapter adapter;
    private ArrayList<String> nearbyDriverRequests;
    private long mLastClickTime = 0;
    private ArrayList<Double> passengersLatitudes;
    private ArrayList<Double> passengersLongtitudes;
    private ArrayList<String> requestCarUsernames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_request_list);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_driver_request_list);
        viewRoot = binding.getRoot();
        setContentView(viewRoot);

        binding.getNearbyRequestButton.setOnClickListener(this);

        //set up in a listViews
        nearbyDriverRequests = new ArrayList<>();
        passengersLongtitudes = new ArrayList<>();
        passengersLatitudes = new ArrayList<>();
        requestCarUsernames = new ArrayList<>();

        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, nearbyDriverRequests);

        binding.requestListView.setAdapter(adapter);

        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

        if (Build.VERSION.SDK_INT > 23 || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            initializeLocationListener();
        }

        //setup item click listener to listView
        binding.requestListView.setOnItemClickListener(this);
    }

    private void initializeLocationListener() {

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {

                locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.driver_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.driverLogout) {

            transitionToMainActivity();

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {

        // Preventing multiple clicks, using threshold of 1 second
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();

        //Ask location permission
        //check device version
        if (Build.VERSION.SDK_INT < 23) {

            getLocation();

        } else if (Build.VERSION.SDK_INT >= 23) {

            if (ContextCompat.checkSelfPermission(DriverRequestListActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //get permission to the user
                ActivityCompat.requestPermissions(DriverRequestListActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            } else {

                getLocation();

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1000 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //if user give us permission to access location
            if (ContextCompat.checkSelfPermission(DriverRequestListActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                initializeLocationListener();

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                getLocation();
            }
        }
    }

    private void updateRequestsListView(Location driverLocation) {

        if (driverLocation != null) {

            saveDriverLocationParse(driverLocation);
            //get Driver current location
            final ParseGeoPoint driverCurrentLocation = new ParseGeoPoint(driverLocation.getLatitude(), driverLocation.getLongitude());

            ParseQuery<ParseObject> requestCarQuery = ParseQuery.getQuery("RequestCar");
            requestCarQuery.whereNear("passengerLocation", driverCurrentLocation);
            requestCarQuery.whereDoesNotExist("driverOfUser");
            requestCarQuery.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> requests, ParseException e) {

                    if (e == null) {

                        if (requests.size() > 0) {

                            if (nearbyDriverRequests.size() > 0) {
                                //display list once
                                nearbyDriverRequests.clear();
                            }
                            if (passengersLatitudes.size() > 0) {
                                //display list once
                                passengersLatitudes.clear();
                            }
                            if (passengersLongtitudes.size() > 0) {
                                //display list once
                                passengersLongtitudes.clear();
                            }
                            if (requestCarUsernames.size() > 0) {
                                requestCarUsernames.clear();
                            }

                            for (ParseObject nearRequest : requests) {

                                ParseGeoPoint pLocation = (ParseGeoPoint) nearRequest.get("passengerLocation");
                                double milesDistanceToPassenger = driverCurrentLocation.distanceInMilesTo(pLocation);

                                float roundedDistanceValue = Math.round(milesDistanceToPassenger * 10) / 10;

                                nearbyDriverRequests.add("There are " + roundedDistanceValue + " miles to " + nearRequest.get("username"));

                                //get passengers location
                                passengersLatitudes.add(pLocation.getLatitude());
                                passengersLongtitudes.add(pLocation.getLongitude());
                                //get username
                                requestCarUsernames.add(nearRequest.get("username") + "");
                            }
                            //update listView
                            adapter.notifyDataSetChanged();
                        } else {
                            FancyToast.makeText(DriverRequestListActivity.this, "There is no car request yet", FancyToast.LENGTH_SHORT, FancyToast.INFO, false).show();
                        }
                    }
                }
            });
        }

    }

    public void getLocation() {
        Location currentDriverLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        updateRequestsListView(currentDriverLocation);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Location cdLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (cdLocation != null){

                Intent intent = new Intent(this, ViewLocationMapActivity.class);
                intent.putExtra("driverLatitude", cdLocation.getLatitude());
                intent.putExtra("driverLongtitude", cdLocation.getLongitude());
                intent.putExtra("passengerLongtitude", passengersLongtitudes.get(position));
                intent.putExtra("passengerLatitude", passengersLatitudes.get(position));
                intent.putExtra("username", requestCarUsernames.get(position));

                startActivity(intent);
            }

        }

    }

    private void saveDriverLocationParse(Location location){

        ParseUser parseDriver = ParseUser.getCurrentUser();
        ParseGeoPoint driverLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        parseDriver.put("driverLocation", driverLocation);

        parseDriver.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {

                if (e == null) {
                    FancyToast.makeText(DriverRequestListActivity.this, "Location saved!", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                }
            }
        });
    }

    private void transitionToMainActivity() {

        ParseUser.logOutInBackground(new LogOutCallback() {
            @Override
            public void done(ParseException e) {

                if (e == null) {

                    FancyToast.makeText(DriverRequestListActivity.this, "Successfully logged out", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                    Intent intent = new Intent(DriverRequestListActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }
}