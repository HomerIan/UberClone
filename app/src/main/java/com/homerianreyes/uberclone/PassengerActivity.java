package com.homerianreyes.uberclone;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.homerianreyes.uberclone.databinding.ActivityPassengerBinding;
import com.parse.DeleteCallback;
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
import java.util.Timer;


public class PassengerActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Boolean isUberCancelled = true;
    private ActivityPassengerBinding binding;
    private View viewRoot;
    private Boolean isCarReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setup data binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_passenger);
        viewRoot = binding.getRoot();
        setContentView(viewRoot);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        binding.requestButton.setOnClickListener(this);
        binding.beepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                getDriverUpdates();
            }
        });

        //check if user request a car
        ParseQuery<ParseObject> carRequestQuery = ParseQuery.getQuery("RequestCar");
        carRequestQuery.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        carRequestQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> requests, ParseException e) {

                if (requests.size() > 0 && e == null) {

                    isUberCancelled = false;
                    binding.requestButton.setText("Cancel booked uber");
                    getDriverUpdates();
                }
            }
        });

        //logout button
        binding.logoutFromPassengerActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                transitionToMainActivity();
                finish();
            }
        });
    }

    private void transitionToMainActivity() {

        ParseUser.logOutInBackground(new LogOutCallback() {
            @Override
            public void done(ParseException e) {

                if (e == null) {

                    FancyToast.makeText(PassengerActivity.this, "You\'ve been logged out.", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                }
            }
        });

        Intent intent = new Intent(PassengerActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                updateCameraPassengerLocation(location);
            }
        };
        //check device version
        if (Build.VERSION.SDK_INT < 23) {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        } else if (Build.VERSION.SDK_INT >= 23){

            if (ContextCompat.checkSelfPermission(PassengerActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(PassengerActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            } else {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location currentPassengerLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateCameraPassengerLocation(currentPassengerLocation);

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1000 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //if user give us permission to access location
            if (ContextCompat.checkSelfPermission(PassengerActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            Location currentPassengerLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            updateCameraPassengerLocation(currentPassengerLocation);
            }
        }
    }

     private void updateCameraPassengerLocation(Location passengerlocation) {

        if (!isCarReady) {

            //get current location
            LatLng passengerLocation = new LatLng(passengerlocation.getLatitude(), passengerlocation.getLongitude());
            mMap.clear();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(passengerLocation, 10));
            mMap.addMarker(new MarkerOptions().position(passengerLocation).title("You are here!").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)));
        }
     }
    //request car
    @Override
    public void onClick(View view) {

        if (isUberCancelled == true) {

            //if user give us permission to access location
            if (ContextCompat.checkSelfPermission(PassengerActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location currentPassengerLocation = locationManager.getLastKnownLocation(locationManager.GPS_PROVIDER);

                if (currentPassengerLocation != null) {

                    ParseObject requestCar = new ParseObject("RequestCar");
                    requestCar.put("username", ParseUser.getCurrentUser().getUsername());

                    ParseGeoPoint userLocation = new ParseGeoPoint(currentPassengerLocation.getLatitude(), currentPassengerLocation.getLongitude());
                    requestCar.put("passengerLocation", userLocation);

                    requestCar.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {

                            if (e == null) {

                                FancyToast.makeText(PassengerActivity.this, "Request sent", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                                binding.requestButton.setText("Cancel booked uber");
                                isUberCancelled = false;
                            }
                        }
                    });
                } else {
                    FancyToast.makeText(PassengerActivity.this, "Unkown Error. Something went wrong.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                }
            }
        } else {
            //find user who booked uber
            ParseQuery<ParseObject> carRequestQuery = ParseQuery.getQuery("RequestCar");
            carRequestQuery.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
            carRequestQuery.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> requests, ParseException e) {

                    if (requests.size() > 0 && e == null) {

                        isUberCancelled = true;
                        binding.requestButton.setText("Request a new car");

                        for (ParseObject uberRequest : requests) {

                            uberRequest.deleteInBackground(new DeleteCallback() {
                                @Override
                                public void done(ParseException e) {

                                    if (e == null) {
                                        FancyToast.makeText(PassengerActivity.this, "Book cancelled", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                                    }
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    private void getDriverUpdates() {

                ParseQuery<ParseObject> uberRequestQuery = ParseQuery.getQuery("RequestCar");
                //current passenger's account
                uberRequestQuery.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
                //driver accepted the request
                uberRequestQuery.whereEqualTo("requestAccepted", true);
                //there is an actual driver
                uberRequestQuery.whereExists("driverOfUser");

                uberRequestQuery.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> requests, ParseException e) {

                        if (requests.size() > 0 && e == null) {

                            isCarReady = true;

                            for (final ParseObject request : requests) {
                                //calculate distance between passenger and driver
                                ParseQuery<ParseUser> driverQuery = ParseUser.getQuery();
                                driverQuery.whereEqualTo("username", request.getString("driverOfUser"));
                                driverQuery.findInBackground(new FindCallback<ParseUser>() {
                                    @Override
                                    public void done(List<ParseUser> objects, ParseException e) {

                                        if (objects.size() > 0 && e == null) {

                                            for (ParseObject object : objects) {

                                                ParseGeoPoint driverRequestLocation = object.getParseGeoPoint("driverLocation");

                                                if (ContextCompat.checkSelfPermission(PassengerActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                                                    Location passengerLocation = locationManager.getLastKnownLocation(locationManager.GPS_PROVIDER);

                                                    ParseGeoPoint pLocationAsParseGeoPoint = new ParseGeoPoint(passengerLocation.getLatitude(), passengerLocation.getLongitude());

                                                    double milesDistance = driverRequestLocation.distanceInMilesTo(pLocationAsParseGeoPoint);

                                                    if (milesDistance < 0.3) {

                                                        //delete request
                                                        request.deleteInBackground(new DeleteCallback() {
                                                            @Override
                                                            public void done(ParseException e) {

                                                                if (e == null) {

                                                                    FancyToast.makeText(PassengerActivity.this, "Uber has arrived", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                                                                    isCarReady = false;
                                                                    isUberCancelled = true;
                                                                    binding.requestButton.setText("Request a car");
                                                                }
                                                            }
                                                        });
                                                    } else {

                                                        float roundedDistance = Math.round(milesDistance * 10) / 10;

                                                        FancyToast.makeText(PassengerActivity.this, request.getString("driverOfUser") + " is " + roundedDistance + " miles away from you! Please wait.", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();

                                                        // locate driver's position
                                                        LatLng dLocation = new LatLng(driverRequestLocation.getLatitude(), driverRequestLocation.getLongitude());

                                                        // locate passenger's position
                                                        LatLng pLocation = new LatLng(passengerLocation.getLatitude(), passengerLocation.getLongitude());

                                                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                                        Marker driverMarker = mMap.addMarker(new MarkerOptions().position(dLocation).title("Driver Location"));
                                                        Marker passengerMarker = mMap.addMarker(new MarkerOptions().position(pLocation).title("Passenger Location"));

                                                        ArrayList<Marker> myMarkers = new ArrayList<>();
                                                        myMarkers.add(driverMarker);
                                                        myMarkers.add(passengerMarker);
                                                        //iterate value of the marker
                                                        for (Marker marker : myMarkers) {

                                                            builder.include(marker.getPosition());
                                                        }

                                                        LatLngBounds bounds = builder.build();
                                                        //specify camera
                                                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 10);
                                                        mMap.animateCamera(cameraUpdate);
                                                    }
                                                }
                                            }

                                        } else {

                                            isCarReady = false;
                                        }
                                    }
                                });

                            }
                        }
                    }
                });
    }
}