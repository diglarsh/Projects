package com.example.didig.finalproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.*;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.widget.LinearLayout;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.location.*;

import java.util.LinkedList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;
    private boolean mLocationPermissionGranted;
    private Location mLastKnownLocation;
    private final float DEFAULT_ZOOM = 120;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Marker marker;
    private LocationManager lManager;
    private Criteria lmCriteria;
    private MyLocationListener myListener;

    private GestureDetectorCompat gDetector;
    private boolean tracking = false;
    private LinkedList<LatLng> pathPoints;
    private SensorManager sm;
    private StepListener stepListener;
    private float steps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.didig.main.R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(com.example.didig.main.R.id.fragment);
        mapFragment.getMapAsync(this);
        getLocationPermission();
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        lManager = getSystemService(LocationManager.class);
        lmCriteria = new Criteria();
        lmCriteria.setAccuracy(Criteria.ACCURACY_FINE);

        LinearLayout linearLayout = findViewById(com.example.didig.main.R.id.myLinearLayout);
        linearLayout.setOnTouchListener(new TrackerListener());
        gDetector = new GestureDetectorCompat(this, new mySimpleOnGestureListener());

        stepListener = new StepListener();
        sm = getSystemService(SensorManager.class);
    }

    @Override
    protected void onResume() {
        super.onResume();

        getLocationPermission();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            getLocationPermission();
        }
        if (mLocationPermissionGranted) {
            if (myListener != null) lManager.removeUpdates(myListener);
            myListener = new MyLocationListener();
            lManager.requestLocationUpdates(1000, 1, lmCriteria, myListener, getMainLooper());
        } else {
            Log.e("ME", "no permission for updates");
        }
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (tracking) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                getLocationPermission();
            }
            lManager.requestLocationUpdates(15000, 10, lmCriteria, myListener, getMainLooper());
        } else if (myListener != null) lManager.removeUpdates(myListener);
    }

    private void getLocationPermission() {
    /*
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.e("ME", "have permission");
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            Log.e("ME", "asked permission");
        }
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
        getLocationPermission();
        getDeviceLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    Log.e("ME", "permission granted");
                }
            }
        }
    }

    private void getDeviceLocation() {

    /*
     * Get the best and most recent location of the device, which may be null in rare
     * cases when a location is not available.
     */
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = (Location) task.getResult();
                            if (mLastKnownLocation != null) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(mLastKnownLocation.getLatitude(),
                                                mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                                placeMarker(mLastKnownLocation);
                            }
                        } else {
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);

                        }
                    }
                });
            } else {
                Log.e("ME", "failed to get location");
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void placeMarker(Location location) {
        //Log.e("ME", "Placing marker");
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        if (marker != null) {
            marker.remove();
        }
        MarkerOptions options = new MarkerOptions()
                .position(new LatLng(lat, lng));
        marker = mMap.addMarker(options);
    }

    private class MyLocationListener implements android.location.LocationListener {
        MyLocationListener() {
            Log.e("ME", "tracking movements");
        }

        @Override
        public void onLocationChanged(Location location) {
            mLastKnownLocation = location;
            if (tracking) pathPoints.add(new LatLng(location.getLatitude(), location.getLongitude()));
            placeMarker(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    private class TrackerListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            gDetector.onTouchEvent(event);
            return true;
        }
    }

    private class mySimpleOnGestureListener implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.e("ME", "double tap");
            if (tracking) {
                sm.unregisterListener(stepListener);

                makePath();
                // TODO add tracking
                tracking = false;
                getDeviceLocation();
                TextView tv = findViewById(com.example.didig.main.R.id.stepsText);
                tv.setText(String.format("Steps in last walk: %s", steps));
            }
            else startTracking();
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.e("ME", "single tap");
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }

    private void startTracking() {
        if (mLastKnownLocation == null) {
            Toast.makeText(getApplicationContext(), "Unable to find current position", Toast.LENGTH_SHORT).show();
            return;
        }
        steps = 0;
        tracking = true;
        pathPoints = new LinkedList<>();
        pathPoints.add(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()));
        try {
            sm.registerListener(stepListener, sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        catch (NullPointerException e) {
            Toast.makeText(this, "Phone does not have a step tracker", Toast.LENGTH_SHORT).show();
        }
    }

    private void makePath() {
        mMap.clear();
        placeMarker(mLastKnownLocation);
        PolylineOptions polyLineOptions = new PolylineOptions();
        polyLineOptions.addAll(pathPoints);
        polyLineOptions.visible(true);
        mMap.addPolyline(polyLineOptions);
    }

    private class StepListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            steps++;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }
}
