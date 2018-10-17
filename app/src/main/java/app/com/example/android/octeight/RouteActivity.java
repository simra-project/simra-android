package app.com.example.android.octeight;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;

import java.text.SimpleDateFormat;
import java.util.LinkedList;

public class RouteActivity extends AppCompatActivity {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Basic map stuff

    private final GeoPoint tuBerlin =
            new GeoPoint(52.51101, 13.3226082);
    private MapView mMapView;
    private MapController mMapController;
    private Location lastLocation;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html

    String PROVIDER = LocationManager.GPS_PROVIDER;
    //String PROVIDER = LocationManager.NETWORK_PROVIDER;

    LocationManager locationManager;
    double myLatitude, myLongitude;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Location updates: http://android-er.blogspot.com/2012/05/location-updates-from-gpsprovider-and.html
    LinkedList<Location> locList;
    final static int LOG_SIZE = 5;
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Context, Config, ContentView
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_route);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Location updates: http://android-er.blogspot.com/2012/05/location-updates-from-gpsprovider-and.html

        locList = new LinkedList<Location>();

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        if (ContextCompat.checkSelfPermission(RouteActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(RouteActivity.this, "Leider fehlt die notwendige" +
                    "Berechtigung.", Toast.LENGTH_SHORT).show();

        } else {
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html
            lastLocation = locationManager.getLastKnownLocation(PROVIDER);
        }

        try {
            updateLoc(lastLocation);
        } catch(NullPointerException npe) {
            npe.printStackTrace();
        }

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        //Map configuration
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        mMapView.setBuiltInZoomControls(true);
        mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(15);
        GeoPoint gPt = tuBerlin;
        mMapController.setCenter(gPt);

    }

    public void onResume(){

        super.onResume();

        if (ContextCompat.checkSelfPermission(RouteActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(RouteActivity.this, "Leider fehlt die notwendige" +
                    "Berechtigung.", Toast.LENGTH_SHORT).show();

        } else {
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html
            locationManager.requestLocationUpdates(PROVIDER, 0, 0, myLocationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,
                    myLocationListener);
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }

    }

    public void onPause(){

        super.onPause();

        if (ContextCompat.checkSelfPermission(RouteActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(RouteActivity.this, "Leider fehlt die notwendige" +
                    "Berechtigung.", Toast.LENGTH_SHORT).show();

        } else {
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // Obtaining location: http://android-er.blogspot.com/2012/05/obtaining-user-location.html
            locationManager.removeUpdates(myLocationListener);
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }

    }

    // Writes longitude & latitude values into text views

    private void updateLoc(Location loc){

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
