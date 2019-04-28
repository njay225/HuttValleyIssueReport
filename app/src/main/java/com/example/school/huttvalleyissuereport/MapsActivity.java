package com.example.school.huttvalleyissuereport;

import android.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    //Variable for logging into the console
    private static final String TAG = "MapsActivity";

    //Default Location for App - Queensgate Lower Hutt
    private static final Double DEFAULT_LATITUDE = -41.208703224540145;
    private static final Double DEFAULT_LONGITUDE = 174.90603447210447;
    private static final LatLng DEFAULT_COORDINATES = new LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);

    //Initialising Location Popup Variables
    private GoogleMap gMaps;
    private LocationManager locationManager;
    private Location currentLocation;
    public static LatLng issueLocation = DEFAULT_COORDINATES;

    //Initialising Search Variables
    PlaceAutocompleteFragment placeAutocompleteFragment;
    AutocompleteFilter searchAutocompleteFilter;
    private List<Address> addresses;
    public static Address currentAddress;

    //Initialising Buttons
    private Button confirmButton;
    private FloatingActionButton currentLocationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Setting up the Search Box
        placeAutocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_auto_complete_fragment);
        //Initialises a filter to limit search box results to New Zealand
        searchAutocompleteFilter = new AutocompleteFilter.Builder()
                .setCountry("NZ")
                .build();

        //Applies the filter to the search box
        placeAutocompleteFragment.setFilter(searchAutocompleteFilter);

        //Sets a listener to the search box for when a place has been selected
        placeAutocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                //Checks if the location is in Lower Hutt
                Boolean isLowerHutt = checkLocationIsLowerHutt(place.getLatLng());

                if (isLowerHutt) {
                    //If location is in Lower Hutt, Update the map
                    updateMap(place.getLatLng());
                } else {
                    //Otherwise Message is shown to user to inform them that location is not in Lower Hutt
                    Toast.makeText(MapsActivity.this, "Please select a location in Lower Hutt", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Status status) {
                Log.d(TAG, status.toString());
            }
        });

        //Links confirm button to widget
        confirmButton = (Button) findViewById(R.id.confirmButton);
        //Adds an onclick listener to confirm button
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //When the confirm button has been clicked, then close the maps activity
                finish();
            }
        });

        //Links the current location button to the widget
        currentLocationButton = (FloatingActionButton) findViewById(R.id.currentLocationButton);
        //Set an onclick listener to the current location button
        currentLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Gets the current location of the user
                getCurrentLocation();
            }
        });


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        //Checks if the user has not previously entered a location
        if (issueLocation == DEFAULT_COORDINATES && !checkConnection()) {
            getCurrentLocation();
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     */
    //Once the map is ready, then this function is called
    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Initialises the map
        gMaps = googleMap;

        //Checks if permission exists
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //Checks if the user has not previously entered a location
        if (issueLocation == DEFAULT_COORDINATES) {
            getCurrentLocation();
        }else{
            //If a previous location was entered
            //Add a marker of that location
            gMaps.addMarker(new MarkerOptions().position(issueLocation));
            //Move the camera to that location
            gMaps.moveCamera(CameraUpdateFactory.newLatLng(issueLocation));
            //Zoom into that location
            gMaps.moveCamera(CameraUpdateFactory.newLatLngZoom(issueLocation, 16.0f));
            //Check if that location is in the Hutt Valley
            checkLocationIsLowerHutt(issueLocation);
        }



        //Set an onclick listener to the map
        gMaps.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                //Checks if clicked location is in Lower Hutt
                Boolean isHuttValley = checkLocationIsLowerHutt(latLng);
                //If the location is in Lower Hutt
                if (isHuttValley) {
                    //Set the issue location to the current coordinates
                    issueLocation = latLng;
                    //Update the map
                    updateMap(latLng);
                } else {
                    //If the location isn't in Lower Hutt, then an error message is sent to the user
                    Toast.makeText(MapsActivity.this, "Please select a location in Lower Hutt", Toast.LENGTH_SHORT).show();
                    currentAddress = null;
                }
            }
        });
    }

    //This function checks if the location is in Lower Hutt
    private Boolean checkLocationIsLowerHutt(LatLng latLng) {
        //isHuttValley boolean is initialised to false
        Boolean isLowerHutt = false;

        try {
            //Gets the address based on the coordinates (array is returned)
            addresses = new Geocoder(MapsActivity.this).getFromLocation(latLng.latitude, latLng.longitude, 1);
            //Sets the current address to the first and only object in the addresses array
            currentAddress = addresses.get(0);

            Log.d(TAG, "checkLocationIsHuttValley: " + currentAddress);

            //Checks if the locality of the address is in Lower Hutt
            if(Objects.equals(currentAddress.getLocality(), "Lower Hutt")){
                //If it is then isLowerHutt is set to true
                isLowerHutt = true;
                //The search box text is set to the address
                placeAutocompleteFragment.setText(addresses.get(0).getAddressLine(0));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        //isLowerHutt is returned to where the function was called
        return isLowerHutt;
    }

    //Gets the current location of the user
    private void getCurrentLocation() {
        //Checks if user has permissions
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //Get the current location of user based on network provider
        currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        //Checks if location was returned
        if(currentLocation == null){
            //If not, then current location is checked based on GPS
            currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        //Checks if a location exists
        if(currentLocation != null){
            Log.d(TAG, "getCurrentLocation: " + currentLocation);
            //If so, location coordinates are returned
            LatLng currentCoordinates = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            //Location is checked if it is in the Hutt Valley
            checkLocationIsLowerHutt(currentCoordinates);
            //Sets the issue location to passed coordinates
            issueLocation = currentCoordinates;
            //Checks if the location is in Lower Hutt
            if(checkLocationIsLowerHutt(currentCoordinates) && checkConnection()){
                //If it is, then the map is updated
                updateMap(currentCoordinates);
            }
        }else{
            //If no location was provided
            //then map is updated with default coordinates
            updateMap(DEFAULT_COORDINATES);
            //Location is checked to make sure it is in Lower Hutt
            checkLocationIsLowerHutt(DEFAULT_COORDINATES);
        }
    }

    //Function updates the map when called
    private void updateMap(LatLng latLng){

        //if(currentLocation != null || issueLocation != null){
        //Clears any existing marker from the map
        gMaps.clear();
        //Marker is added at passed coordinates
        gMaps.addMarker(new MarkerOptions().position(latLng));
        //Camera is moved to coordinates
        gMaps.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        //Zooms into coordinates
        gMaps.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0f));
        //}

    }

    private boolean checkConnection(){
        //Variables for checking network status
        ConnectivityManager cm;
        NetworkInfo activeNetwork;

        //Checks if device is connected to a network
        cm = (ConnectivityManager) getBaseContext().getSystemService(CONNECTIVITY_SERVICE);
        activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.d(TAG, "Paused");

    }

}
