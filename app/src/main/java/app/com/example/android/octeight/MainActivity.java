package app.com.example.android.octeight;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
<<<<<<< HEAD
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
=======
>>>>>>> 2fa3540a022f2370b17f27566dea5db54031cc47
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
<<<<<<< HEAD
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
=======
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
>>>>>>> 2fa3540a022f2370b17f27566dea5db54031cc47
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.view.*;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
<<<<<<< HEAD
import org.osmdroid.tileprovider.cachemanager.CacheManager;
import org.osmdroid.tileprovider.tilesource.HEREWeGoTileSource;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.MapBoxTileSource;
import org.osmdroid.tileprovider.tilesource.MapQuestTileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
=======
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
>>>>>>> 2fa3540a022f2370b17f27566dea5db54031cc47
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayItem;
<<<<<<< HEAD
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
=======
>>>>>>> 2fa3540a022f2370b17f27566dea5db54031cc47
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;

import static java.security.AccessController.getContext;

<<<<<<< HEAD
public class MainActivity extends AppCompatActivity {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Map stuff, Overlays

    private MapView mMapView;
    private MapController mMapController;
    private Location lastLocation;
    private MyLocationNewOverlay mLocationOverlay;
    private CompassOverlay mCompassOverlay;
    private ScaleBarOverlay mScaleBarOverlay;
    private RotationGestureOverlay mRotationGestureOverlay;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // CLICKABLES --> INTENTS

    ImageButton menuButton;
    ImageButton helmetButton;
    ImageButton centerMap;
    RelativeLayout neuRoute;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
=======
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Basic map stuff

    private final GeoPoint tuBerlin =
            new GeoPoint(52.51101, 13.3226082);
    private MapView mMapView;
    private MapController mMapController;
    private Location lastLocation;
    private boolean which = false;
    private boolean whichTwo = false;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // CLICKABLES --> INTENTS

    //ImageButton menuButton;
    ImageButton helmetButton;

    RelativeLayout neuRoute;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
>>>>>>> 2fa3540a022f2370b17f27566dea5db54031cc47
    // For permission request

    private final int LOCATION_ACCESS_CODE = 1;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
<<<<<<< HEAD
    // Case-Switch for onPermissionResult

    private static int myCase;

    // Log tag:

    private static final String TAG = "MainActivity";

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html

    LocationManager locationManager;

    public static final OnlineTileSourceBase HTTP_MAPNIK = new XYTileSource("HttpMapnik",
            0, 19, 256, ".png", new String[] {
            "http://a.tile.openstreetmap.org/",
            "http://b.tile.openstreetmap.org/",
            "http://c.tile.openstreetmap.org/" },
            "© OpenStreetMap contributors");
=======
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html

    String PROVIDER = LocationManager.GPS_PROVIDER;
    //String PROVIDER = LocationManager.NETWORK_PROVIDER;

    LocationManager locationManager;
    double myLatitude, myLongitude;

    //TextView textLatitude, textLongitude, textLog;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Location updates: http://android-er.blogspot.com/2012/05/location-updates-from-gpsprovider-and.html
    LinkedList<Location> locList;
    final static int LOG_SIZE = 5;
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
>>>>>>> 2fa3540a022f2370b17f27566dea5db54031cc47

    @Override
    protected void onCreate(Bundle savedInstanceState) {

<<<<<<< HEAD
        Configuration.getInstance().setUserAgentValue(getPackageName());

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Case-Code required for onRequestPermissionResult-Method which executes functionality
        // based on activity lifecycle
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        myCase = 1;
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        Log.i(TAG,"On Create called");

        super.onCreate(savedInstanceState);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Set some params (context, DisplayMetrics, Config, ContentView)
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        final Context ctx = getApplicationContext();
        final DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_main);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        //Map configuration
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        mMapView = findViewById(R.id.map);

        mMapView.setTileSource(HTTP_MAPNIK);

        //**************************************************************************************
        // ALTERNATIVE MAP TILE PROVIDERS
        /**final MapBoxTileSource tileSource = new MapBoxTileSource();
        tileSource.retrieveAccessToken(ctx);
        tileSource.retrieveMapBoxMapId(ctx);
        mMapView.setTileSource(tileSource);*/

        /**final ITileSource tileSource = new HEREWeGoTileSource(ctx);
        mMapView.setTileSource(tileSource);*/
        //**************************************************************************************

        //Set compass (from OSMdroid sample project: https://github.com/osmdroid/osmdroid/blob/master/OpenStreetMapViewer/src/main/
        //                       java/org/osmdroid/samplefragments/location/SampleFollowMe.java)
        this.mCompassOverlay = new CompassOverlay(ctx, new InternalCompassOrientationProvider(ctx),
                mMapView);

        // MyLocationNewOverlay constitutes an alternative to definition of  a custom resource
        // proxy (DefaultResourceProxyImpl is deprecated)
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
        mLocationOverlay.enableFollowLocation();
        mLocationOverlay.setEnableAutoStop(true);

        // Call function for setting custom icons for current location person marker + navigation
        // arrow
        setLocationMarker();

        mRotationGestureOverlay = new RotationGestureOverlay(mMapView);
        mRotationGestureOverlay.setEnabled(true);

        /**
        // ScaleBar (from OSMdroid sample project: https://github.com/osmdroid/osmdroid/blob/master/OpenStreetMapViewer/src/main/
        //            java/org/osmdroid/samplefragments/location/SampleFollowMe.java)
        mScaleBarOverlay = new ScaleBarOverlay(mMapView);
        mScaleBarOverlay.setCentred(true);
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);
         */

        // self-explanatory
        mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(15);
        // Disable zoom buttons
        mMapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        // Gesture zooming
        mMapView.setMultiTouchControls(true);
        mMapView.setFlingEnabled(true);

        // self-explanatory
        mMapView.setTilesScaledToDpi(true);

        // Add overlays
        mMapView.getOverlays().add(this.mLocationOverlay);
        mMapView.getOverlays().add(this.mCompassOverlay);
        //mMapView.getOverlays().add(this.mScaleBarOverlay);
        mMapView.getOverlays().add(this.mRotationGestureOverlay);

        mLocationOverlay.setOptionsMenuEnabled(true);
        mCompassOverlay.enableCompass();
        mRotationGestureOverlay.setEnabled(true);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // Coordinates currently only optimized for Theresa's device --> needs work
        //mCompassOverlay.setCompassCenter(((dm.widthPixels/5)*2),((dm.heightPixels)/8)*5);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
=======
        super.onCreate(savedInstanceState);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Context, Config, ContentView
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_main);


        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Location updates: http://android-er.blogspot.com/2012/05/location-updates-from-gpsprovider-and.html

        locList = new LinkedList<Location>();

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // If user wants to record new route, check location tracking permission
        // --> if permission hasn't been granted, request permission
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        getLocationWrapper();
>>>>>>> 2fa3540a022f2370b17f27566dea5db54031cc47

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html

<<<<<<< HEAD
        if(PermissionHandler.permissionGrantCheck(this)) {
            try {
                lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } catch (SecurityException se) {
                se.printStackTrace();
            }
        } else {
            PermissionHandler.askPermission(MainActivity.this);
        }
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html

        if (lastLocation != null)
            updateLoc(lastLocation);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // CLICKABLES

        // (1): Burger Menu

        menuButton = findViewById(R.id.burger_menu);
         menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launchActivityIntent = new Intent(MainActivity.this,
                        MenuActivity.class);
                startActivity(launchActivityIntent);
            }
        });

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
                if (lastLocation != null) {
                    GeoPoint myPosition = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());
                    mMapView.getController().animateTo(myPosition);
                }
            }
        });

        // (4): Neue Route
=======
        try {
            updateLoc(lastLocation);
        } catch(NullPointerException npe) {
            npe.printStackTrace();
        }

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        //Map configuration
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        mMapView = findViewById(R.id.map);

        // Get Tiles from MAPNIK (DEFAULT_TILE_SOURCE = MAPNIK)
        mMapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);

        // Disable zoom buttons
        mMapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        // Enable touch controls
        mMapView.setMultiTouchControls(true);

        // Set user agent for OSM maps. Gets banned if abused.
        Configuration.getInstance().setUserAgentValue(getPackageName());
        
        mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(15);
        GeoPoint gPt = tuBerlin;
        mMapController.setCenter(gPt);


        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // CLICKABLES

        // (1): Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // (2): Start Neue Route
>>>>>>> 2fa3540a022f2370b17f27566dea5db54031cc47

        neuRoute = findViewById(R.id.route_button);
        neuRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launchActivityIntent = new Intent(MainActivity.this,
                        RouteActivity.class);
                startActivity(launchActivityIntent);
            }
        });

<<<<<<< HEAD
        Log.i(TAG,"On Create finished");

    }

   /** public void cacheTiles() {

        CacheManager cacheManager = new CacheManager(mMapView);
        BoundingBox boundingBox = new BoundingBox(52.4344418, 13.2827911,
                latitude+0.5, longitude+0.5);

        cacheManager.downloadAreaAsync(MainActivity.this, boundingBox, 15, 15);

    }*/

    public void setLocationMarker() {


        // Set current location marker icon to custom icon

        Drawable currentDraw = ResourcesCompat.getDrawable(getResources(), R.drawable.bicycle5, null);
        Bitmap currentIcon = null;
        if (currentDraw != null) {
            currentIcon = ((BitmapDrawable) currentDraw).getBitmap();
        }

        // Set navigation arrow icon to custom icon

        /**Drawable currentArrowDraw = ResourcesCompat.getDrawable(getResources(), R.drawable.bicycle5, null);
        Bitmap currentArrowIcon = null;
        if (currentArrowDraw != null) {
            currentArrowIcon = ((BitmapDrawable) currentArrowDraw).getBitmap();
        }

        mLocationOverlay.setDirectionArrow(currentIcon, currentArrowIcon);*/

        mLocationOverlay.setPersonIcon(currentIcon);

        mLocationOverlay.setDrawAccuracyEnabled(true);

        mMapView.getOverlays().add(mLocationOverlay);

    }

    public void onStart() {

        super.onStart();

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // MyLocationONewOverlayParameters.
        // --> enableMyLocation: Enable receiving location updates from the provided
        //                          IMyLocationProvider and show your location on the maps.
        // --> enableFollowLocation: Enables "follow" functionality.
        mLocationOverlay.enableMyLocation();
        mLocationOverlay.enableFollowLocation();
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // Call function for setting custom icons for current location person marker + navigation
        // arrow
        setLocationMarker();

    }

    public void onResume(){

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Case-Code required for onRequestPermissionResult-Method which executes functionality
        // based on activity lifecycle
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        myCase = 2;
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        Log.i(TAG,"On Resume called");

        super.onResume();

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // MyLocationONewOverlayParameters.
        // --> enableMyLocation: Enable receiving location updates from the provided
        //                          IMyLocationProvider and show your location on the maps.
        // --> enableFollowLocation: Enables "follow" functionality.
        mLocationOverlay.enableMyLocation();
        mLocationOverlay.enableFollowLocation();
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // Call function for setting custom icons for current location person marker
        setLocationMarker();

        //cacheTiles(lastLocation.getLatitude(), lastLocation.getLongitude());

        if(PermissionHandler.permissionGrantCheck(this)) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLocationListener);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, myLocationListener);
            } catch (SecurityException se) {
                se.printStackTrace();
            }
        }

        Log.i(TAG,"On Resume finished");

        }

    public void onPause(){

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Case-Code required for onRequestPermissionResult-Method which executes functionality
        // based on activity lifecycle
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        myCase = 3;
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        Log.i(TAG,"On Pause called");

        super.onPause();

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // MyLocationONewOverlayParameters.
        // --> enableMyLocation: Enable receiving location updates from the provided
        //                          IMyLocationProvider and show your location on the maps.
        // --> enableFollowLocation: Enables "follow" functionality.
        mLocationOverlay.enableMyLocation();
        mLocationOverlay.enableFollowLocation();
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // Call function for setting custom icons for current location person marker + navigation
        // arrow
        setLocationMarker();

        if(PermissionHandler.permissionGrantCheck(this)) {
            try {
                locationManager.removeUpdates(myLocationListener);
            } catch (SecurityException se) {
                se.printStackTrace();
            }
        }

        Log.i(TAG,"On Pause finished");

=======
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Wrapper for location functionality called in onCreate()
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private void getLocationWrapper() {

        int hasFineLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        if(hasFineLocationPermission != PackageManager.PERMISSION_GRANTED) {

            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showMessageOKCancel(getString(R.string.permissionFineLocation), new DialogInterface.OnClickListener() {
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
        lastLocation = locationManager.getLastKnownLocation(PROVIDER);
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        }


    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Wrapper for location functionality called in onResume()
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private void updateLocationWrapper() {

       int hasFineLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        if(hasFineLocationPermission != PackageManager.PERMISSION_GRANTED) {

            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showMessageOKCancel(getString(R.string.permissionFineLocation), new DialogInterface.OnClickListener() {
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
        locationManager.requestLocationUpdates(PROVIDER, 0, 0, myLocationListener);
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
                showMessageOKCancel(getString(R.string.permissionFineLocation), new DialogInterface.OnClickListener() {
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
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
>>>>>>> 2fa3540a022f2370b17f27566dea5db54031cc47
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case LOCATION_ACCESS_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
<<<<<<< HEAD
                    // Check where we're at: case 1 = onCreate, case 2 = onResume, case 3 = onPause
                    switch(myCase) {
                        case 1:
                            try {
                                lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            } catch (SecurityException se) {
                                se.printStackTrace();
                            }
                            break;
                        case 2:
                            try {
                                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLocationListener);
                                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, myLocationListener);
                            } catch (SecurityException se) {
                                se.printStackTrace();
                            }
                            break;
                        case 3:
                            try {
                                locationManager.removeUpdates(myLocationListener);
                            } catch (SecurityException se) {
                                se.printStackTrace();
                            }
                            break;
=======
                    // Permission Granted

                    // super non-elegant case distinction
                    if (!which & !whichTwo) {
                        getLocationWrapper();
                        which = true;
                    } else if (which & !whichTwo){
                        updateLocationWrapper();
                        whichTwo = true;
                    } else {
                        removeUpdatesWrapper();
>>>>>>> 2fa3540a022f2370b17f27566dea5db54031cc47
                    }

                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "Zugriff auf Standortdaten " +
                            "wurde abgelehnt.", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

<<<<<<< HEAD
=======
    public void onResume(){
        super.onResume();

        updateLocationWrapper();

        }

    public void onPause(){

        super.onPause();

        removeUpdatesWrapper();

    }

>>>>>>> 2fa3540a022f2370b17f27566dea5db54031cc47
    // Writes longitude & latitude values into text views

    private void updateLoc(Location loc){

<<<<<<< HEAD
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Update location: http://android-er.blogspot.com/2012/05/update-location-on-openstreetmap.html
        GeoPoint locGeoPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
        mMapController.setCenter(locGeoPoint);
        mMapView.invalidate();
=======
        //textLatitude.setText("Latitude: " + loc.getLatitude());
        //textLongitude.setText("Longitude: " + loc.getLongitude());

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Location updates: http://android-er.blogspot.com/2012/05/location-updates-from-gpsprovider-and.html

        locList.add(loc);

        //maintain the LOG_SIZE
        if (locList.size() > LOG_SIZE) {
            locList.remove();
        }

        String locLog = "\n LOCATION LOG (last " + LOG_SIZE + " logs)\n";
        for(int i = 0; i < locList.size(); i++){
            if(locList.get(i) != null){

                String formatedTime = (new SimpleDateFormat("mm:ss:SSS")).format(locList.get(i).getTime());

                locLog += "\n--- " + i + " ---\n"
                        + "@ " + formatedTime + "\n"
                        + "Latitude: " + locList.get(i).getLatitude() + "\n"
                        + "Longitude: " + locList.get(i).getLongitude() + "\n"
                        + "Time: " +  String.valueOf(locList.get(i).getTime()) + "\n"
                        + "Provider: " + locList.get(i).getProvider() + "\n";

                if(locList.get(i).hasAltitude()){
                    locLog += "Altitude: " + locList.get(i).getAltitude() + "\n";
                }

                if(locList.get(i).hasAccuracy()){
                    locLog += "Accuracy: " + locList.get(i).getAccuracy() + "(m)\n";
                }

                if(locList.get(i).hasBearing()){
                    locLog += "Bearing: " + locList.get(i).getBearing() + "(m)\n";
                }

                if(locList.get(i).hasSpeed()){
                    locLog += "Speed: " + locList.get(i).getSpeed() + "(m)\n";
                }

            }

        }

        //textLog.setText(locLog);

>>>>>>> 2fa3540a022f2370b17f27566dea5db54031cc47
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

<<<<<<< HEAD
=======
    // Navigation Drawer

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

   /* @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // rechtes Toolbar icon
   /* public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/

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

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

>>>>>>> 2fa3540a022f2370b17f27566dea5db54031cc47
}
