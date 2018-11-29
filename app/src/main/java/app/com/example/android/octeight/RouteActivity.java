package app.com.example.android.octeight;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;

public class RouteActivity extends AppCompatActivity implements SensorEventListener {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Basic map stuff

    private MapView mMapView;
    private MapController mMapController;
    private Location lastLocation;
    private MyLocationNewOverlay mLocationOverlay;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Sensor stuff
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private SensorManager mSensorManager;

    private Sensor myAcc;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Views
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    TextView accDat;

    RelativeLayout stopButton;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Logging
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    final String ROUTE_ACT = "RouteActiviy";

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Save sensor data

    ArrayList<Float> xList = new ArrayList<>();

    ArrayList<Float> yList = new ArrayList<>();

    ArrayList<Float> zList = new ArrayList<>();

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // For permission request

    private final int LOCATION_ACCESS_CODE = 1;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html

    String PROVIDER = LocationManager.GPS_PROVIDER;

    LocationManager locationManager;

    public static final OnlineTileSourceBase HTTP_MAPNIK = new XYTileSource("HttpMapnik",
            0, 19, 256, ".png", new String[]{
            "http://a.tile.openstreetmap.org/",
            "http://b.tile.openstreetmap.org/",
            "http://c.tile.openstreetmap.org/"},
            "© OpenStreetMap contributors");

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Configuration.getInstance().setUserAgentValue(getPackageName());

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
        mMapView.setTileSource(HTTP_MAPNIK);
        // Disable zoom buttons
        mMapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(15);
        mMapView.setMultiTouchControls(true); // gesture zooming

        // MyLocationNewOverlay constitutes an alternative to definition of  a custom resource
        // proxy (DefaultResourceProxyImpl is deprecated)
        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mMapView);
        mLocationOverlay.enableFollowLocation();
        mLocationOverlay.enableMyLocation();

        // Call function for setting custom icons for current location person marker + navigation
        // arrow
        setLocationMarker();

        /**Enable compass (currently probably hidden behind upper bar)
         CompassOverlay compassOverlay = new CompassOverlay(this, mMapView);
         compassOverlay.enableCompass();
         mMapView.getOverlays().add(compassOverlay);*/

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

        try {
            if (PermissionHandler.permissionGrantCheck(this)) {
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html
                lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

            }
        } catch (SecurityException se) {

            se.printStackTrace();

        }

        try {
            updateLoc(lastLocation);
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Sensor-related configuration
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        stopButton = findViewById(R.id.stop_button);

        // When stopButton is pressed, route data is saved to a JSON file & SaveActivity is
        // triggered.
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveRouteData();
                Intent launchActivityIntent = new Intent(RouteActivity.this,
                        SaveActivity.class);
                startActivity(launchActivityIntent);
            }
        });


    }

    public void setLocationMarker() {

        // Set current location marker icon to custom icon

        Drawable currentDraw = ResourcesCompat.getDrawable(getResources(), R.drawable.bicycle5, null);
        Bitmap currentIcon = null;
        if (currentDraw != null) {
            currentIcon = ((BitmapDrawable) currentDraw).getBitmap();
        }

        // Set navigation arrow icon to custom icon

        Drawable currentArrowDraw = ResourcesCompat.getDrawable(getResources(), R.drawable.bicycle5, null);
        Bitmap currentArrowIcon = null;
        if (currentArrowDraw != null) {
            currentArrowIcon = ((BitmapDrawable) currentArrowDraw).getBitmap();
        }

        mLocationOverlay.setPersonIcon(currentIcon);

        mLocationOverlay.setDirectionArrow(currentIcon, currentArrowIcon);

        mLocationOverlay.setDrawAccuracyEnabled(true);

        mMapView.getOverlays().add(mLocationOverlay);

    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        // The acceleratometer returns 3 values, one for each axis.
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // Add the acceleratormeter data to the respective ArrayLists.
        xList.add(x);

        yList.add(y);

        zList.add(z);

        // Show the values on screen (for demonstration purposes only)
        accDat.setText("x: " + x + "\ny: " + y + "\nz: " + z);
    }

    public void saveRouteData() {

        String xString = xList.toString();
        create(this, "x_acceleratometer.csv", xString);
        isFilePresent(this, "x_acceleratometer.csv");

        String yString = yList.toString();
        create(this, "y_acceleratometer.csv", yString);
        isFilePresent(this, "y_acceleratometer.csv");

        String zString = zList.toString();
        create(this, "z_acceleratometer.csv", zString);
        isFilePresent(this, "y_acceleratometer.csv");

    }

    private boolean create(Context context, String fileName, String jsonString) {

        try {
            FileOutputStream fos = openFileOutput(fileName, Context.MODE_PRIVATE);
            if (jsonString != null) {
                fos.write(jsonString.getBytes());
            }
            fos.close();
            return true;
        } catch (FileNotFoundException fileNotFound) {
            return false;
        } catch (IOException ioException) {
            return false;
        }

    }

    public boolean isFilePresent(Context context, String fileName) {
        String path = context.getFilesDir().getAbsolutePath() + "/" + fileName;
        File file = new File(path);
        Log.i(ROUTE_ACT, path);
        return file.exists();
    }

    public void onStart() {

        super.onStart();

        // Call function for setting custom icons for current location person marker + navigation
        // arrow
        setLocationMarker();

    }

    public void onResume() {

        super.onResume();

        try {
            if (PermissionHandler.permissionGrantCheck(this)) {
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLocationListener);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, myLocationListener);
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            }
        } catch (SecurityException se) {
            se.printStackTrace();
        }

        // Call function for setting custom icons for current location person marker + navigation
        // arrow
        setLocationMarker();

        mSensorManager.registerListener(this, myAcc, SensorManager.SENSOR_DELAY_NORMAL);

    }

    public void onPause() {

        super.onPause();

        try {
            if (PermissionHandler.permissionGrantCheck(this)) {
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html
                locationManager.removeUpdates(myLocationListener);
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            }
        } catch (SecurityException se) {
            se.printStackTrace();
        }

        // Call function for setting custom icons for current location person marker + navigation
        // arrow
        setLocationMarker();

    }

    // Writes longitude & latitude values into text views

    private void updateLoc(Location loc) {

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Update location: http://android-er.blogspot.com/2012/05/update-location-on-openstreetmap.html
        GeoPoint locGeoPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
        mMapController.setCenter(locGeoPoint);
        mMapView.invalidate();
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    }

    private LocationListener myLocationListener
            = new LocationListener() {

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