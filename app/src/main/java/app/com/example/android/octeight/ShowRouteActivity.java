package app.com.example.android.octeight;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.ImageButton;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static app.com.example.android.octeight.Utils.checkForAnnotation;

public class ShowRouteActivity extends BaseActivity {

    String startTime = "";
    String timeStamp = "";

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Map stuff, Overlays
    private MapView mMapView;
    private TextView copyrightTxt;
    private RelativeLayout addIncBttn;
    private RelativeLayout exitAddIncBttn;
    private RelativeLayout saveButton;

    ////~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Our ride
    Ride ride;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Log tag
    private static final String TAG = "ShowRouteActivity_LOG";

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // GEOCODER --> obtain GeoPoint from address

    ExecutorService pool;

    public MapView getmMapView() {
        return mMapView;
    }

    public void setmMapView(MapView mMapView) {
        this.mMapView = mMapView;
    }

    MapEventsOverlay overlayEvents;

    // Marker-icons for different types/states of events:
    // Automatically detected/custom; to be annotated/already annotated

    Drawable editMarkerDefault;

    Drawable editCustMarker;

    Drawable editDoneDefault;

    Drawable editDoneCust;

    boolean addCustomMarkerMode;

    MarkerFunct myMarkerFunct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() started");
        setContentView(R.layout.activity_show_route);
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Map configuration
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        mMapView = findViewById(R.id.showRouteMap);
        mMapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mMapView.setMultiTouchControls(true); // gesture zooming
        mMapView.setFlingEnabled(true);
        copyrightTxt = (TextView) findViewById(R.id.copyright_text);
        copyrightTxt.setMovementMethod(LinkMovementMethod.getInstance());

        addIncBttn = findViewById(R.id.addIncident);
        addIncBttn.setVisibility(View.VISIBLE);
        exitAddIncBttn = findViewById(R.id.exitAddIncident);
        exitAddIncBttn.setVisibility(View.INVISIBLE);
        saveButton = findViewById(R.id.saveIncident);
        saveButton.setVisibility(View.VISIBLE);

        // scales tiles to dpi of current display
        mMapView.setTilesScaledToDpi(true);

        String pathToAccGpsFile = getIntent().getStringExtra("PathToAccGpsFile");
        startTime = getIntent().getStringExtra("StartTime");
        // Log.d(TAG, "onCreate() date: " + date);
        int state = getIntent().getIntExtra("State",0);
        // Log.d(TAG, "onCreate() PathToAccGpsFile:" + pathToAccGpsFile);
        String duration = getIntent().getStringExtra("Duration");

        File gpsFile = getFileStreamPath(pathToAccGpsFile);

        Log.d(TAG, "creating ride objects");


        // Create a ride object with the accelerometer, gps and time data
        try {
            ride = new Ride(gpsFile, duration, startTime,/*date,*/ state, this);
        } catch (Exception e){
            e.printStackTrace();
        }

        // Get the Route as a Polyline to be displayed on the map
        Polyline route = ride.getRoute();
        // Get a bounding box of the route so the view can be moved to it and the zoom can be
        // set accordingly
        BoundingBox bBox = getBoundingBox(route);

        mMapView.getOverlayManager().add(route);

        mMapView.invalidate();
        zoomToBBox(bBox);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // (3): CenterMap
        ImageButton centerMap = findViewById(R.id.bounding_box_center_button);

        centerMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "boundingBoxCenterMap clicked ");
                zoomToBBox(bBox);
            }
        });

        // Set the icons for event markers

        // (1) Automatically recognized, not yet annotated

        editMarkerDefault = getResources().getDrawable(R.drawable.edit_event_bunt, null);

        // (2) Custom, not yet annotated

        editCustMarker = getResources().getDrawable(R.drawable.edit_event_grun, null);

        // (3) Automatically recognized, annotated

        editDoneDefault = getResources().getDrawable(R.drawable.event_annotated_bunt, null);

        // (4) Custom, not yet annotated

        editDoneCust = getResources().getDrawable(R.drawable.event_annotated_grun, null);

        // Call function for drawing markers for all AccEvents in ride, now encapsulated in
        // MarkerFunct class for better readability

        Log.d(TAG, "setting up Executors");
        pool = Executors.newFixedThreadPool(4);

        // Create an instance of MarkerFunct-class which provides all functionality related to
        // incident markers
        Log.d(TAG, "creating MarkerFunct object");
        myMarkerFunct = new MarkerFunct(this);

        // Show all the incidents present in our ride object
        Log.d(TAG, "showing all incidents");
        myMarkerFunct.showIncidents();

        addCustomMarkerMode = false;

        Log.d(TAG, "setting up mReceive");

        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {

                if (addCustomMarkerMode){
                    // Call custom marker adding functionality which enables the user to put
                    // markers on the map

                    myMarkerFunct.addCustMarker(p);
                    exitAddIncBttn.performClick();
                    return true;
                } else {
                    // myMarkerFunct.closeAllInfoWindows();
                    InfoWindow.closeAllInfoWindowsOn(mMapView);
                    return false;
                }

            }

            @Override
            public boolean longPressHelper(GeoPoint p) {

                if (addCustomMarkerMode){

                    // Call custom marker adding functionality which enables the user to put
                    // markers on the map
                    myMarkerFunct.addCustMarker(p);
                    exitAddIncBttn.performClick();
                    return true;

                }
                return false;
            }
        };

        overlayEvents = new MapEventsOverlay(getBaseContext(), mReceive);
        mMapView.getOverlays().add(overlayEvents);
        mMapView.invalidate();

        Log.d(TAG, "setting up clickListeners");
        // Functionality for 'edit mode', i.e. the mode in which users can put their own incidents
        // onto the map

        addIncBttn.setOnClickListener((View v) -> {

            addIncBttn.setVisibility(View.INVISIBLE);
            exitAddIncBttn.setVisibility(View.VISIBLE);
            ShowRouteActivity.this.addCustomMarkerMode = true;

            // overlayEvents = new MapEventsOverlay(getBaseContext(), mReceive);
            // mMapView.getOverlays().add(overlayEvents);
            // mMapView.invalidate();

        });

        exitAddIncBttn.setOnClickListener((View v) -> {

            addIncBttn.setVisibility(View.VISIBLE);
            exitAddIncBttn.setVisibility(View.INVISIBLE);
            ShowRouteActivity.this.addCustomMarkerMode = false;
            // mMapView.getOverlays().remove(overlayEvents);

        });

        saveButton.setOnClickListener((View v) -> {

            File[] dirFiles = getFilesDir().listFiles();
            if (dirFiles.length != 0) {
                for (int i = 0; i < dirFiles.length; i++) {
                    String nameOfFileToBeRenamed = dirFiles[i].getName();
                    String newNameOfFile = nameOfFileToBeRenamed.replace(".csv","_1.csv");
                    String path = Constants.APP_PATH + "files/";
                    Log.d(TAG, "nameOfFileToBeRenamed: " + nameOfFileToBeRenamed + " newNameOfFile: " + newNameOfFile);
                    if (nameOfFileToBeRenamed.startsWith(ride.getId()+"_") && !nameOfFileToBeRenamed.endsWith("_1.csv")) {
                        Log.d(TAG, "Renaming");
                        dirFiles[i].renameTo(new File(path+newNameOfFile));
                    }
                }
            }
            /*
            Intent intent = new Intent(this, UploadService.class);
            intent.putExtra("Ride_Key", ride.getId());
            startService(intent);
            */
            Toast.makeText(this, getString(R.string.savedRide), Toast.LENGTH_SHORT).show();
            finish();
            // mMapView.getOverlays().remove(overlayEvents);

        });

        Log.d(TAG, "onCreate() finished");

    }

    // If the user clicks on an InfoWindow and IncidentPopUpActivity for that
    // event opens, upon closing it again onActivityResult is called and the
    // displaying of markers is updated if necessary.

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {

            if (resultCode == Activity.RESULT_OK) {

               String result = data.getStringExtra("result");

                String[] incidentProps = result.split(",");

                boolean annotated = checkForAnnotation(incidentProps);

                myMarkerFunct.setMarker(new AccEvent(Integer.valueOf(incidentProps[0]),
                        Double.parseDouble(incidentProps[1]), Double.parseDouble(incidentProps[2]),
                        Long.parseLong(incidentProps[3]),
                        annotated));

            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        //*****************************************************************
        // Shutdown pool and await termination to make sure the program
        // doesn't continue without the relevant work being completed

        pool.shutdown();

        try {
            pool.awaitTermination(2, TimeUnit.SECONDS);
        } catch(InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    // Returns the longitudes of the southern- and northernmost points
    // as well as the latitudes of the western- and easternmost points
    // in a double Array {South, North, West, East}
    static BoundingBox getBoundingBox(Polyline pl){

        // {North, East, South, West}
        ArrayList<GeoPoint> geoPoints = pl.getPoints();

        double[] border = {geoPoints.get(0).getLatitude(), geoPoints.get(0).getLongitude(), geoPoints.get(0).getLatitude(), geoPoints.get(0).getLongitude()};

        for (int i = 0; i < geoPoints.size(); i++) {
            // Check for south/north
            if (geoPoints.get(i).getLatitude() < border[2]){
                border[2] = geoPoints.get(i).getLatitude();
            } if (geoPoints.get(i).getLatitude() > border[0]){
                border[0] = geoPoints.get(i).getLatitude();
            }
            // Check for west/east
            if (geoPoints.get(i).getLongitude() < border[3]){
                border[3] = geoPoints.get(i).getLongitude();
            } if (geoPoints.get(i).getLongitude() > border[1]){
                border[1] = geoPoints.get(i).getLongitude();
            }

        }

        return new BoundingBox(border[0]+0.001,border[1]+0.001,border[2]-0.001,border[3]-0.001);
    }

    public void zoomToBBox(BoundingBox bBox){
        // zoom automatically to the bounding box. Usually the command in the if body should suffice
        // but osmdroid is buggy and we need the else part to fix it.
        if((mMapView.getIntrinsicScreenRect(null).bottom-mMapView.getIntrinsicScreenRect(null).top) > 0){
            mMapView.zoomToBoundingBox(bBox, false);
        } else {
            ViewTreeObserver vto = mMapView.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                @Override
                public void onGlobalLayout() {
                    mMapView.zoomToBoundingBox(bBox, false);
                    ViewTreeObserver vto2 = mMapView.getViewTreeObserver();
                    vto2.removeOnGlobalLayoutListener(this);
                }
            });
        }

        mMapView.setMinZoomLevel(7.0);
        if(mMapView.getMaxZoomLevel() > 19.0){
            mMapView.setMaxZoomLevel(19.0);
        }
    }


}
