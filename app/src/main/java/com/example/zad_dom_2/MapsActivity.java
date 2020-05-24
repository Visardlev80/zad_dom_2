package com.example.zad_dom_2;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;

//OD TYPA
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.example.lab_7.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

class Pozycje
{
    public double xPos;
    public double yPos;

    public Pozycje()
    {
        xPos = 0;
        yPos = 0;
    }
    public Pozycje(double x, double y)
    {
        xPos = x;
        yPos = y;
    }
}

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
    GoogleMap.OnMapLoadedCallback,
    GoogleMap.OnMarkerClickListener,
    GoogleMap.OnMapLongClickListener,
    SensorEventListener{

    private final String POINTS_JSON_FILE = "marker.json";

//animation
    ViewGroup tContainer;
    FloatingActionButton hide;
    FloatingActionButton start;
    TextView sensor;
    boolean visible;
    boolean onTop;
    boolean record = false;

    private TextView xText, yText;
    private Sensor mySensor;
    private SensorManager SM;

    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    Marker gpsMarker = null;
    List<Marker> markerList;
    List<Pozycje> pozycjaMarkera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //restoreFromJson();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        markerList = new ArrayList<>();
        //pozycjaMarkera = new ArrayList<>();

//animation
        tContainer = findViewById(R.id.mainView);
        hide = findViewById(R.id.Hide);
        start = findViewById(R.id.StartStop);
        sensor = findViewById(R.id.xText);

        SM = (SensorManager)getSystemService(SENSOR_SERVICE);
        mySensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        SM.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);
        xText = (TextView)findViewById(R.id.xText);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        xText.setText("Acceleration: \n");
        xText.append("X: " + event.values[0]);
        xText.append(" Y: " + event.values[1]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        /*// Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }

    private void createLocationRequest()
    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates()
    {
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);
    }

    private void createLocationCallback()
    {
        locationCallback = new LocationCallback()
        {
            @Override
            public void onLocationResult(LocationResult locationResult)
            {
                if(locationResult != null)
                {
                    if(gpsMarker != null)
                        gpsMarker.remove();

                    Location location = locationResult.getLastLocation();
                    gpsMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(location.getLatitude(), location.getLongitude()))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                            .alpha(0.8f)
                            .title("Current Location"));
                }
            }
        };
    }

    @Override
    public void onMapLoaded() {
        Log.i(MapsActivity.class.getSimpleName(), "MapLoaded");
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
        Task<Location> lastLocation = fusedLocationClient.getLastLocation();

        lastLocation.addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null && mMap != null)
                {
                    mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            .title(getString(R.string.last_known_loc_msg)));
                }
            }
        });

        createLocationRequest();
        createLocationCallback();
        startLocationUpdates();
    }

    public void zoomInClick(View v)
    {
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View v)
    {
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    private void stopLocationUpdates()
    {
        if(locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
    }
    @Override
    public void onMapLongClick(LatLng latLng) {
        float distance = 0f;

        if(markerList.size() > 0)
        {
            Marker lastMarker = markerList.get(markerList.size() - 1);
            float [] tmpDis = new float[3];

            Location.distanceBetween(lastMarker.getPosition().latitude, lastMarker.getPosition().longitude,
                    latLng.latitude, latLng.latitude, tmpDis);
            distance = tmpDis[0];

            PolylineOptions rectOptions = new PolylineOptions()
                    .add(lastMarker.getPosition())
                    .add(latLng)
                    .width(10)
                    .color(Color.BLUE);
            mMap.addPolyline(rectOptions);
        }
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude, latLng.longitude))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker2))
                .alpha(0.8f)
                .title(String.format("Position: (%.2f, %.2f) Distance: %.2f", latLng.latitude, latLng.longitude, distance)));
        markerList.add(marker);
        //double pomocnicza = marker.getPosition().latitude;
        //double pomocnicza2 = marker.getPosition().longitude;
        //pozycjaMarkera.add(new Pozycje(pomocnicza, pomocnicza2));
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
//animation
        TransitionManager.beginDelayedTransition(tContainer);
        visible = !visible;

        if(onTop == false)
        {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            hide.startAnimation(animation);
            start.startAnimation(animation);

            hide.setVisibility(View.VISIBLE);
            start.setVisibility(View.VISIBLE);

            onTop = true;
        }
        return false;
    }

    public void hideAll(View view)
    {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        hide.startAnimation(animation);
        start.startAnimation(animation);
        sensor.startAnimation(animation);
        visible = !visible;
        hide.setVisibility(View.GONE);
        start.setVisibility(View.GONE);
        sensor.setVisibility(View.GONE);

        onTop = false;
    }

    public void clearMap(View view)
    {
        mMap.clear();
        markerList.removeAll(markerList);
        if(onTop == true) {
            hideAll(tContainer);
        }
    }

    public void startStop(View view)
    {
        if(record == false) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            sensor.startAnimation(animation);
            sensor.setVisibility(View.VISIBLE);
            record = true;
        }
        else
        {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            sensor.startAnimation(animation);
            sensor.setVisibility(View.GONE);
            record = false;
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       savePointsToJson();
    }

    private void savePointsToJson() {
        Gson gson = new Gson();
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(POINTS_JSON_FILE,MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            gson.toJson(pozycjaMarkera,writer);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restoreFromJson() {
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 10000;
        Gson gson = new Gson();
        String readJson;
        try {
            inputStream = openFileInput(POINTS_JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();
            while ((n = reader.read(buf)) >= 0) {
                String tmp = String.valueOf(buf);
                String substring = (n<DEFAULT_BUFFER_SIZE) ? tmp.substring(0,n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type to_restore_type = new TypeToken<List<Pozycje>>(){}.getType();
            List<Pozycje> to_restore = gson.fromJson(readJson,to_restore_type);
            if (to_restore != null) {
                for (int i = 0; i < to_restore.size(); i++ ){
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(to_restore.get(i).xPos,to_restore.get(i).yPos))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker2))
                            .alpha(0.8f)
                            .title(String.format("Position: (%.2f, %.2f", to_restore.get(i).xPos,to_restore.get(i).yPos)));
                    pozycjaMarkera.add(new Pozycje(to_restore.get(i).xPos,to_restore.get(i).yPos));
                    markerList.add(marker);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

