package app.com.example.android.octeight;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnNavigationItemSelectedListener, LocationListener {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Map stuff, Overlays

    private MapView mMapView;
    private MapController mMapController;
    private final int ZOOM_LEVEL = 19;
    private MyLocationNewOverlay mLocationOverlay;
    private CompassOverlay mCompassOverlay;
    private RotationGestureOverlay mRotationGestureOverlay;
    private LocationManager locationManager;
    private static final OnlineTileSourceBase HTTP_MAPNIK = new XYTileSource("HttpMapnik",
            0, 19, 256, ".png", new String[] {
            "http://a.tile.openstreetmap.org/",
            "http://b.tile.openstreetmap.org/",
            "http://c.tile.openstreetmap.org/" },
            "© OpenStreetMap contributors");

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // SharedPreferences for storing last location, editor for editing sharedPrefs

    SharedPreferences sharedPrefs;

    SharedPreferences.Editor editor;

    // Custom method for saving double values in sharedPreferences (not possible by default)

    SharedPreferences.Editor putDouble(final SharedPreferences.Editor edit,
                                       final String key, final double value) {
        return edit.putLong(key, Double.doubleToRawLongBits(value));
    }

    // Custom method for retrieving double values from sharedPreferences (not possible by default)

    double getDouble(final SharedPreferences prefs, final String key, final double defaultValue) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // Instance of class encapsulating accelerometer sensor functionality

    AccelerometerService myAccService = new AccelerometerService();

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // Data structures for saving GPS information
    private ArrayList<Float> xCoord = new ArrayList<>();
    private ArrayList<Float> yCoord = new ArrayList<>();

    private Boolean recording = false;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // CLICKABLES --> INTENTS

    private ImageButton helmetButton;
    private ImageButton centerMap;
    private RelativeLayout startBtn;
    private RelativeLayout stopBtn;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // For permission request

    private final int LOCATION_ACCESS_CODE = 1;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Context of application environment
    private Context ctx;

    // Log tag
    private static final String TAG = "MainActivity_LOG";

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG,"OnCreate called");
        super.onCreate(savedInstanceState);

        // set up location manager to get location updates
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Set some params (context, DisplayMetrics, Config, ContentView)
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_main);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        //Map configuration
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        mMapView = findViewById(R.id.map);
        mMapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mMapView.setMultiTouchControls(true); // gesture zooming
        mMapView.setFlingEnabled(true);
        mMapView.setTileSource(HTTP_MAPNIK);
        mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(ZOOM_LEVEL);

        //**************************************************************************************
        // ALTERNATIVE MAP TILE PROVIDERS

        // (1) MapBox
        /**final MapBoxTileSource tileSource = new MapBoxTileSource();
         tileSource.retrieveAccessToken(ctx);
         tileSource.retrieveMapBoxMapId(ctx);
         mMapView.setTileSource(tileSource);*/

        // (2) HERE we go
        /**final ITileSource tileSource = new HEREWeGoTileSource(ctx);
         mMapView.setTileSource(tileSource);*/
        //**************************************************************************************

        //Set compass (from OSMdroid sample project: https://github.com/osmdroid/osmdroid/blob/master/OpenStreetMapViewer/src/main/
        //                       java/org/osmdroid/samplefragments/location/SampleFollowMe.java)
        this.mCompassOverlay = new CompassOverlay(ctx, new InternalCompassOrientationProvider(ctx),
                mMapView);

        // Sets the icon to device location.
        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mMapView);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // MyLocationONewOverlayParameters.
        // --> enableMyLocation: Enable receiving location updates from the provided
        //                          IMyLocationProvider and show your location on the maps.
        // --> enableFollowLocation: Enables "follow" functionality.
        // --> setEnableAutoStop: if true, when the user pans the map, follow my location will
        //                          automatically disable if false, when the user pans the map,
        //                           the map will continue to follow current location
        mLocationOverlay.enableMyLocation();

        // If app has been used before and therefore a last known location is available in sharedPrefs,
        // animate the map to that location.
        // Move map to last location known by locationManager if app is started for the first time.

        if(sharedPrefs.contains("lastLoc_latitude") & sharedPrefs.contains("lastLoc_latitude")) {

            GeoPoint lastLoc = new GeoPoint(
                    Double.parseDouble(sharedPrefs.getString("lastLoc_latitude", "")),
                    Double.parseDouble(sharedPrefs.getString("lastLoc_latitude", "")));

            mMapController.animateTo(lastLoc);

        }  else {

            try {
                mMapController.animateTo(new GeoPoint((locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude()),
                        (locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude())));
            } catch (RuntimeException re) {
                Log.d(TAG, re.getMessage());
                // move map to current location
                mMapController.animateTo(mLocationOverlay.getMyLocation());
            }
        }

        // the map will follow the user until the user scrolls in the UI
        mLocationOverlay.enableFollowLocation();

        // enables map rotation with gestures
        mRotationGestureOverlay = new RotationGestureOverlay(mMapView);
        mRotationGestureOverlay.setEnabled(true);

        // scales tiles to dpi of current display
        mMapView.setTilesScaledToDpi(true);

        // Add overlays
        mMapView.getOverlays().add(this.mLocationOverlay);
        mMapView.getOverlays().add(this.mCompassOverlay);
        mMapView.getOverlays().add(this.mRotationGestureOverlay);

        mLocationOverlay.setOptionsMenuEnabled(true);
        mCompassOverlay.enableCompass();

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // Initialize sharedPreferences

        sharedPrefs = getSharedPreferences("simraPrefs", Context.MODE_PRIVATE);

        editor = sharedPrefs.edit();

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // CLICKABLES

        // (1): Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // (2): Helmet

        helmetButton = findViewById(R.id.helmet_icon);
        helmetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launchActivityIntent = new Intent(MainActivity.this,
                        HelmetActivity.class);
                startActivity(launchActivityIntent);
            }
        });

        // (3): CenterMap

        centerMap = findViewById(R.id.center_button);

        centerMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "centerMap clicked ");
                mLocationOverlay.enableFollowLocation();
            }
        });

        // (4): Neue Route / Start Button

        startBtn = findViewById(R.id.start_button);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopBtn.setVisibility(View.VISIBLE);
                startBtn.setVisibility(View.INVISIBLE);
                recording = true;
                routeFunctionality();
            }
        });

        // (5): Aufzeichnung stoppen / Stop Button
        stopBtn = findViewById(R.id.stop_button);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveRouteData();
                startBtn.setVisibility(View.VISIBLE);
                stopBtn.setVisibility(View.INVISIBLE);
                recording = false;
            }
        });

        Log.i(TAG,"OnCreate finished");

    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public void onResume(){

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        Log.i(TAG,"OnResume called");

        super.onResume();

        // Load Configuration with changes from onCreate
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Configuration.getInstance().load(this, prefs);

        if (recording) {
            myAccService.accSensorManager.registerListener((SensorEventListener) this,
                    myAccService.myAccSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            // locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        } catch ( SecurityException se ) {
            Log.d(TAG, "onStart() permission not granted yet");
        }

        // Refresh the osmdroid configuration on resuming.
        mMapView.onResume(); //needed for compass and icons

    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public void onPause(){

        Log.i(TAG,"OnPause called");

        super.onPause();

        // Load Configuration with changes from onCreate
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Configuration.getInstance().save(this, prefs);

        // Refresh the osmdroid configuration on pausing.
        mMapView.onPause(); //needed for compass and icons

        Log.i(TAG,"OnPause finished");
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public void onStop(){

        Log.i(TAG,"OnStop called");

        super.onStop();

        try {
            editor.putString("lastLoc_latitude", String.valueOf(locationManager
                    .getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude()));
            editor.putString("lastLoc_longitude", String.valueOf(locationManager
                    .getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude()));
        } catch (SecurityException se) {
            se.printStackTrace();
        }

        Log.i(TAG,"OnStop finished");
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // Navigation Drawer
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_history) {
            // Handle the camera action
        } else if (id == R.id.nav_democraphic_data) {
            // src: https://stackoverflow.com/questions/2197741/how-can-i-send-emails-from-my-android-application
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL  , new String[]{getString(R.string.feedbackReceiver)});
            i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.demoDataHeader));
            i.putExtra(Intent.EXTRA_TEXT, getString(R.string.demoDataMail));
            try {
                startActivity(Intent.createChooser(i, "Send Data..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
            }

        } else if (id == R.id.nav_feedback) {
            // src: https://stackoverflow.com/questions/2197741/how-can-i-send-emails-from-my-android-application
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL  , new String[]{getString(R.string.feedbackReceiver)});
            i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedbackHeader));
            i.putExtra(Intent.EXTRA_TEXT, getString(R.string.feedbackMail));
            try {
                startActivity(Intent.createChooser(i, "Send mail..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
            }

        } else if (id == R.id.nav_setting) {

        } else if (id == R.id.nav_infoMCC){
            Intent intent = new Intent (MainActivity.this, WebActivity.class);
            startActivity(intent);
            /*WebView webview = new WebView(this);
            setContentView(webview);
            webview.loadUrl(getString(R.string.mccPageDE));
            /*Intent intent = new Intent (MainActivity.this, HelmetActivity.class);
            startActivity(intent);*/
        } else if (id == R.id.nav_infoSimRa){
            Intent intent = new Intent (MainActivity.this, StartActivity.class);
            intent.putExtra("caller", "MainActivity");
            startActivity(intent);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    private void routeFunctionality (){
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Sensor-related configuration
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        myAccService.accSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        myAccService.myAccSensor = myAccService.accSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

    }

    public void saveRouteData() {

        String xString = myAccService.xList.toString();
        create(this, "x_accelerometer.csv", xString);

        String yString = myAccService.yList.toString();
        create(this, "y_accelerometer.csv", yString);

        String zString = myAccService.zList.toString();
        create(this, "z_accelerometer.csv", zString);

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
        Log.i(TAG, path);
        return file.exists();
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

    @Override
    public void onLocationChanged(Location location) { }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }

}