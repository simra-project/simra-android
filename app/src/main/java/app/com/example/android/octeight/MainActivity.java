package app.com.example.android.octeight;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Basic map stuff

    private MapView mMapView;
    private MapController mMapController;
    private Location lastLocation;

    boolean checkInProgress = false;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // CLICKABLES --> INTENTS

    ImageButton menuButton;
    ImageButton helmetButton;

    RelativeLayout neuRoute;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // For permission request

    private final int LOCATION_ACCESS_CODE = 1;

    // Log tag:

    private static final String TAG = "MainActivity";

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html

    String PROVIDER = LocationManager.GPS_PROVIDER;

    LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG,"On Create called");

        super.onCreate(savedInstanceState);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Context, Config, ContentView
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_main);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        //Map configuration
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        mMapView = findViewById(R.id.map);
        mMapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        mMapView.setBuiltInZoomControls(true);
        mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(15);
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // If user wants to record new route, check location tracking permission
        // --> if permission hasn't been granted, request permission
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        getLocationWrapper();

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html

        if (lastLocation != null)
            updateLoc(lastLocation);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        //--- Create Overlay --> upcoming tutorial on Osmdroid

       /** overlayItemArray = new ArrayList<OverlayItem>();

        DefaultResourceProxyImpl defaultResourceProxyImpl
                = new DefaultResourceProxyImpl(this);
        MyItemizedIconOverlay myItemizedIconOverlay
                = new MyItemizedIconOverlay(
                overlayItemArray, null, defaultResourceProxyImpl);
        mMapView.getOverlays().add(myItemizedIconOverlay);
        //---  */

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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


        // (3): Neue Route

        neuRoute = findViewById(R.id.route_button);
        neuRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launchActivityIntent = new Intent(MainActivity.this,
                        RouteActivity.class);
                startActivity(launchActivityIntent);
            }
        });

        Log.i(TAG,"On Create finished");

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
        new AlertDialog.Builder(MainActivity.this)
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
                    getLocationWrapper();
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
        checkInProgress = false;
    }

    public void onResume(){

        Log.i(TAG,"On Resume called");

        super.onResume();
        updateLocationWrapper();

        Log.i(TAG,"On Resume finished");

        }

    public void onPause(){

        Log.i(TAG,"On Pause called");

        super.onPause();

        removeUpdatesWrapper();

        Log.i(TAG,"On Pause finished");

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
