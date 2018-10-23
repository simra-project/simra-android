package app.com.example.android.octeight;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;

import java.text.SimpleDateFormat;
import java.util.LinkedList;

public class RouteActivity extends AppCompatActivity implements SensorEventListener {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Basic map stuff

    private MapView mMapView;
    private MapController mMapController;
    private Location lastLocation;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Sensor stuff
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private SensorManager mSensorManager;

    private Sensor myAcc;

    TextView accDat;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // For permission request

    private final int LOCATION_ACCESS_CODE = 1;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html

    String PROVIDER = LocationManager.GPS_PROVIDER;

    LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Context, Config, ContentView
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_route);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Map configuration
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        mMapView = findViewById(R.id.map);
        mMapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        mMapView.setBuiltInZoomControls(true);
        mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(15);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Sensor-related configuration
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        myAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        accDat = findViewById(R.id.acc_dat);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        getLocationWrapper();

        try {
            updateLoc(lastLocation);
        } catch(NullPointerException npe) {
            npe.printStackTrace();
        }

    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Wrapper for location functionality called in onCreate()
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private void getLocationWrapper() {

        int hasFineLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        if(hasFineLocationPermission != PackageManager.PERMISSION_GRANTED) {

            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showMessageOKCancel("Um eine neue Route anzulegen, ist der Zugriff auf Deinen Standort" +
                                " vonnöten.",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                                        LOCATION_ACCESS_CODE);
                            }
                        });
                return;
            }
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_ACCESS_CODE);
            return;

        } /**Toast.makeText(MainActivity.this, "Du hast " +
         "die nötige Erlaubnis bereits erteilt (1).", Toast.LENGTH_SHORT).show();*/

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html
        lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    }


    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Wrapper for location functionality called in onResume()
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private void updateLocationWrapper() {

        int hasFineLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        if(hasFineLocationPermission != PackageManager.PERMISSION_GRANTED) {

            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showMessageOKCancel("Um fortzufahren, erlaube bitte den Zugriff auf Deine " +
                                "Standortdaten.",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                                        LOCATION_ACCESS_CODE);
                            }
                        });
                return;
            }
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_ACCESS_CODE);
            return;

        }

        /**Toast.makeText(MainActivity.this, "Du hast " +
         "die nötige Erlaubnis bereits erteilt. (2)", Toast.LENGTH_SHORT).show();*/

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLocationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, myLocationListener);
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Wrapper for location functionality called in onPause()
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private void removeUpdatesWrapper() {

        int hasFineLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        if(hasFineLocationPermission != PackageManager.PERMISSION_GRANTED) {

            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showMessageOKCancel("Um fortzufahren, erlaube bitte den Zugriff auf" +
                                " Deine Standortdaten. (2)",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                                        LOCATION_ACCESS_CODE);
                            }
                        });
                return;
            }
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_ACCESS_CODE);
            return;

        }

        /**Toast.makeText(MainActivity.this, "Du hast " +
         "die nötige Erlaubnis bereits erteilt. (2)", Toast.LENGTH_SHORT).show();*/

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html
        locationManager.removeUpdates(myLocationListener);
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(RouteActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case LOCATION_ACCESS_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateLocationWrapper();
                } else {
                    // Permission Denied
                    Toast.makeText(RouteActivity.this, "Zugriff auf Standortdaten " +
                            "wurde abgelehnt.", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        // The light sensor returns a single value.
        // Many sensors return 3 values, one for each axis.
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        accDat.setText("x: " + x + "y: " + y + "z: " + z);
    }


    public void onResume(){

        super.onResume();

        updateLocationWrapper();

        mSensorManager.registerListener(this, myAcc, SensorManager.SENSOR_DELAY_NORMAL);

    }

    public void onPause(){

        super.onPause();

        removeUpdatesWrapper();

        mSensorManager.unregisterListener(this);

    }

    // Writes longitude & latitude values into text views

    private void updateLoc(Location loc){

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Update location: http://android-er.blogspot.com/2012/05/update-location-on-openstreetmap.html
        GeoPoint locGeoPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
        mMapController.setCenter(locGeoPoint);
        mMapView.invalidate();
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    }

    private LocationListener myLocationListener
            = new LocationListener(){

        @Override
        public void onLocationChanged(Location location) {
            // TODO Auto-generated method stub
            updateLoc(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub

        }

    };

}
