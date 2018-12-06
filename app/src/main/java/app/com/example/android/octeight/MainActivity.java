package app.com.example.android.octeight;

import android.Manifest;
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

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;

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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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


import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
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

    // Instance of class encapsulating accelerometer sensor functionality

    //AccelerometerService myAccService;
    AccelerometerFunct accFunct = null;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // Data structures for saving GPS information
    private ArrayList<MyGeoPoint> geoDat = new ArrayList<>();

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG,"OnCreate called");
        super.onCreate(savedInstanceState);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Set some params (context, DisplayMetrics, Config, ContentView)
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_main);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Instantiate AccelerometerFunct-instance for accelerometer data recording
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        accFunct = new AccelerometerFunct(this);
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // set up location manager to get location updates
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Check whether FINE_LOCATION permission is not granted
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission for FINE_LOCATION is not granted. Show rationale why location permission is needed
            // in an AlertDialog and request access to FINE_LOCATION

            // The message to be shown in the AlertDialog
            String rationaleMessage = "Diese App benötigt den Zugriff auf deine Standortdaten, um dich auf der Karte anzuzeigen" +
                    "können und deine Fahrt zu speichern.";

            // The OK-Button fires a requestPermissions
            DialogInterface.OnClickListener rationaleOnClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_ACCESS_CODE);
                }
            };
            showMessageOK(rationaleMessage, rationaleOnClickListener);
        }

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

        // move map to the last known location
        try {
            mMapController.animateTo(new GeoPoint((locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude()),
                    (locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude())));
        } catch (RuntimeException re) {
            Log.d(TAG, re.getMessage());
        }

        // move map to current location
        mMapController.animateTo(mLocationOverlay.getMyLocation());

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

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // (4): NEUE ROUTE / START BUTTON

        startBtn = findViewById(R.id.start_button);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // show stop button, hide start button
                showStop();

                // hand start time of recording over to AccelerometerFunct-instance to enable
                // recording at the intended intervals
                accFunct.myRecordingTimeVar = System.currentTimeMillis();

                // start recording accelerometer data
                accFunct.recording = true;

                // set recording to true to enable the appropriate showing/hiding of buttons
                // in other phases of the lifecycle
                recording = true;

                // call method for recording GPS data
                if(PermissionHandler.permissionGrantCheck(ctx)) {
                    recordGPSData(System.currentTimeMillis());
                }
            }
        });

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // (5): AUFZEICHNUNG STOPPEN / STOP-BUTTON

        stopBtn = findViewById(R.id.stop_button);
        stopBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // set recording to false to enable the appropriate showing/hiding of buttons
                // in all phases of the lifecycle
                recording = false;

                // stop recording accelerometer data
                accFunct.recording = false;

                    // write accelerometer data recorded during route
                    // into csv file in internal storage
                    accFunct.saveRouteData();

                // call method for saving GPS data
                saveGPSData();

                // unregister accelerometer sensor listener
                // @TODO (is this necessary? where else to unregister? - unregistering the
                // listener in onPause as demonstrated in most examples is not an option
                // as we want to keep recording when screen is turned off!)
                accFunct.unregister();

                // show start button, hide stop button
                showStart();

            }
        });

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        Log.i(TAG,"OnCreate finished");

    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // Switching between buttons:

    // (1) start button visible, stop button invisible

    public void showStart() {

        stopBtn.setVisibility(View.INVISIBLE);
        startBtn.setVisibility(View.VISIBLE);

    }

    // (2) stop button visible, start button invisible

    public void showStop() {

        stopBtn.setVisibility(View.VISIBLE);
        startBtn.setVisibility(View.INVISIBLE);

    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public void onResume(){

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        Log.i(TAG,"OnResume called");

        super.onResume();

        // Ensure the button that matches current state is presented.
        // @TODO für MARK: doesn't seem to work yet, when display is rotated "Neue Route" is always presented
        if(recording) {
            showStop();
        } else {
            showStart();
        }

        // Load Configuration with changes from onCreate
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Configuration.getInstance().load(this, prefs);

        // register listener for accelerometer functionality
        accFunct.register();

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
                    0, this);
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

        // Ensure the button that matches current state is presented.
        // @TODO für MARK: doesn't seem to work yet, when display is rotated "Neue Route" is always presented
        if(recording) {
            showStop();
        } else {
            showStart();
        }

        // register listener for accelerometer functionality
        accFunct.register();

        // Load Configuration with changes from onCreate
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Configuration.getInstance().save(this, prefs);

        // Refresh the osmdroid configuration on pausing.
        mMapView.onPause(); //needed for compass and icons

        Log.i(TAG,"OnPause finished");
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
            startActivity(intent);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // Create an AlertDialog with an OK Button displaying a message
    private void showMessageOK(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", okListener)
                .create()
                .show();
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public void recordGPSData(long startTime) throws SecurityException {

        // save the time we start recording permanently for calculating diffTime (time between
        // start of recording and time of recording a specific location)
        // ---> THIS VARIABLE STAYS THE SAME THROUGHOUT THE RECORDING OF ONE ROUTE.
        long recordingStartTime = startTime;

        // save the time we start recording one for ensuring we record location once specified
        // time intervals have elapsed;
        // ---> THIS VARIABLE IS UPDATED AFTER EVERY RECORDING
        long timeVar = startTime;

        // we keep recording until the stop button has been pressed.
        while(recording) {

            // CASE 1: one minute has elapsed; we record
            //          a) latitude,
            //          b) longitude,
            //          c) time since we first started recording,
            //          d) TimeStamp.
            // ..... for last known location.
            if (System.currentTimeMillis() == (timeVar + Constants.GPS_TIME_FREQUENCY)) {

                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                MyGeoPoint mgp = new MyGeoPoint(location.getLatitude(),
                                                location.getLongitude(),
                                                (location.getTime() - recordingStartTime),
                                                new Timestamp(location.getTime()));

                geoDat.add(mgp);

                // update time variable
                timeVar = System.currentTimeMillis();
            }

            // CASE 2: three seconds have elapsed; we record
            //          a) latitude,
            //          b) longitude,
            //          c) time since we first started recording.
            // ..... for last known location.
            else if (System.currentTimeMillis() == (startTime + Constants.GPS_FREQUENCY)) {

                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                MyGeoPoint mgp = new MyGeoPoint(location.getLatitude(),
                        location.getLongitude(),
                        (location.getTime() - recordingStartTime));

                geoDat.add(mgp);

                // update time variable
                timeVar = System.currentTimeMillis();
            }

        }

    }


    public void saveGPSData() {

        FileOutputStream fos = null;

        try {

            fos = ctx.openFileOutput("accData.csv", Context.MODE_PRIVATE);

        } catch (FileNotFoundException fnfe) {

            fnfe.printStackTrace();

        }

        try (OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            CSVPrinter csvPrinter = new CSVPrinter(osw, CSVFormat.DEFAULT.withHeader("longitude", "latitude",
                    "time since start of recording", "timestamp"));
            for (int i = 0; i < geoDat.size(); i++) {
                csvPrinter.printRecord(geoDat.get(i).lat,
                        geoDat.get(i).lon,
                        geoDat.get(i).timeDiff,
                        geoDat.get(i).timeStamp);
            }
            csvPrinter.flush();
            csvPrinter.close();

        } catch (IOException ioe) {

            ioe.printStackTrace();
        }
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public void onLocationChanged(Location location) { }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }

}
