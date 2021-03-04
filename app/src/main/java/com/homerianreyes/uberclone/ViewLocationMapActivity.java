package com.homerianreyes.uberclone;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.homerianreyes.uberclone.databinding.ActivityViewLocationMapBinding;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class ViewLocationMapActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private GoogleMap mMap;
    private ActivityViewLocationMapBinding binding;
    private View viewRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_view_location_map);
        viewRoot = binding.getRoot();
        setContentView(viewRoot);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //ride request button
        binding.rideButton.setText("Accept " + getIntent().getStringExtra("username") + " Ride Request!");
        binding.rideButton.setOnClickListener(this);

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

        // locate driver's position
        LatLng dLocation = new LatLng(getIntent().getDoubleExtra("driverLatitude", 0), getIntent().getDoubleExtra("driverLongtitude", 0));
//        mMap.addMarker(new MarkerOptions().position(dLocation).title("Driver Location"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(dLocation));

        // locate passenger's position
        LatLng pLocation = new LatLng(getIntent().getDoubleExtra("passengerLatitude", 0), getIntent().getDoubleExtra("passengerLongtitude", 0));
//        mMap.addMarker(new MarkerOptions().position(pLocation).title("Passenger Location"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(pLocation));

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
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 0);
        mMap.animateCamera(cameraUpdate);
    }

    @Override
    public void onClick(View view) {

        ParseQuery<ParseObject> carRequestQuery = ParseQuery.getQuery("RequestCar");
        carRequestQuery.whereEqualTo("username", getIntent().getStringExtra("username"));

        carRequestQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {

                if (objects.size() > 0 && e == null) {

                    for (ParseObject uberRequest : objects) {

                        uberRequest.put("requestAccepted", true);

                        uberRequest.put("driverOfUser", ParseUser.getCurrentUser().getUsername());
                        uberRequest.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {

                                if (e == null) {

                                    Intent googleIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?saddr=" +
                                            getIntent().getDoubleExtra("driverLatitude", 0) + "," +
                                            getIntent().getDoubleExtra("driverLongtitude", 0) + "&"+ "daddr=" +
                                            getIntent().getDoubleExtra("passengerLatitude", 0) + "," +
                                            getIntent().getDoubleExtra("passengerLongtitude", 0)));

                                    startActivity(googleIntent);
                                }
                            }
                        });
                    }
                }
            }
        });

    }
}