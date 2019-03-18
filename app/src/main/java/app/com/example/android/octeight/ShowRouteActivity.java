package app.com.example.android.octeight;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.button.MaterialButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jaygoo.widget.OnRangeChangedListener;
import com.jaygoo.widget.RangeSeekBar;

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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static app.com.example.android.octeight.Utils.checkForAnnotation;
import static app.com.example.android.octeight.Utils.fileExists;
import static app.com.example.android.octeight.Utils.getAppVersionNumber;
import static app.com.example.android.octeight.Utils.lookUpBooleanSharedPrefs;
import static app.com.example.android.octeight.Utils.lookUpIntSharedPrefs;
import static app.com.example.android.octeight.Utils.overWriteFile;
import static app.com.example.android.octeight.Utils.writeBooleanToSharedPrefs;
import static app.com.example.android.octeight.Utils.writeIntToSharedPrefs;

public class ShowRouteActivity extends BaseActivity {

    ImageButton backBtn;
    TextView toolbarTxt;

    String startTime = "";
    String timeStamp = "";

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Map stuff, Overlays
    private MapView mMapView;
    private RelativeLayout addIncBttn;
    private RelativeLayout exitAddIncBttn;

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

    MapEventsOverlay overlayEvents;

    // Marker-icons for different types/states of events:
    // Automatically detected/custom; to be annotated/already annotated

    Drawable editMarkerDefault;
    Drawable editCustMarker;
    Drawable editDoneDefault;
    Drawable editDoneCust;
    boolean addCustomMarkerMode;
    MarkerFunct myMarkerFunct;
    AlertDialog alertDialog;

    int bike;
    int child;
    int trailer;
    int pLoc;

    int state;
    String duration;
    File gpsFile;
    Polyline route;

    Ride tempRide;
    File tempGpsFile;
    String tempAccGpsPath;
    String tempAccEventsPath;
    Polyline tempRoute;
    boolean showWarning = true;
    boolean continueWithRefresh = true;

    int lastLeft;
    int lastRight;

    RangeSeekBar privacySlider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() started");
        setContentView(R.layout.activity_show_route);
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Toolbar
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        toolbarTxt = findViewById(R.id.toolbar_title);
        toolbarTxt.setText(R.string.title_activity_showRoute);

        backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Map configuration
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        mMapView = findViewById(R.id.showRouteMap);
        mMapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mMapView.setMultiTouchControls(true); // gesture zooming
        mMapView.setFlingEnabled(true);
        TextView copyrightTxt = (TextView) findViewById(R.id.copyright_text);
        copyrightTxt.setMovementMethod(LinkMovementMethod.getInstance());

        addIncBttn = findViewById(R.id.addIncident);
        addIncBttn.setVisibility(View.VISIBLE);
        exitAddIncBttn = findViewById(R.id.exitAddIncident);
        exitAddIncBttn.setVisibility(View.INVISIBLE);
        RelativeLayout saveButton = findViewById(R.id.saveIncident);
        saveButton.setVisibility(View.VISIBLE);

        // scales tiles to dpi of current display
        mMapView.setTilesScaledToDpi(true);

        String pathToAccGpsFile = getIntent().getStringExtra("PathToAccGpsFile");
        startTime = getIntent().getStringExtra("StartTime");
        // Log.d(TAG, "onCreate() date: " + date);
        state = getIntent().getIntExtra("State", 0);
        // Log.d(TAG, "onCreate() PathToAccGpsFile:" + pathToAccGpsFile);
        duration = getIntent().getStringExtra("Duration");

        gpsFile = getFileStreamPath(pathToAccGpsFile);

        Log.d(TAG, "creating ride objects");
        bike = lookUpIntSharedPrefs("Settings-BikeType", 0, "simraPrefs", this);
        child = lookUpIntSharedPrefs("Settings-Child", 0, "simraPrefs", this);
        trailer = lookUpIntSharedPrefs("Settings-Trailer", 0, "simraPrefs", this);
        pLoc = lookUpIntSharedPrefs("Settings-PhoneLocation", 0, "simraPrefs", this);

        showCustomViewAlertDialog();


        Log.d(TAG, "onCreate() continues.");

        refreshRoute(false);


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
        });

        privacySlider = findViewById(R.id.privacySlider);
        int routeSize = route.getPoints().size();
        if (routeSize < 2) {
            routeSize = 2;
        }
        privacySlider.setRange(0, routeSize);
        privacySlider.setValue(0, routeSize);
        Log.d(TAG, "route.size(): " + routeSize);
        // String originalAccGpsContent = readContentFromFile(ride.accGpsFile.getName(),this);
        // String tempAccGpsPath = originalAccGpsContent;

        lastLeft = 0;
        lastRight = routeSize;
        final int[] left = {0};
        final int[] right = {0};
        privacySlider.setOnRangeChangedListener(new OnRangeChangedListener() {

            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                left[0] = (int) leftValue;
                right[0] = (int) rightValue;
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
                //start tracking touch
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
                showWarning = lookUpBooleanSharedPrefs("ShowRoute-Warning", true, "simraPrefs", ShowRouteActivity.this);
                if (showWarning) {
                    getDialogValueBack(left[0], right[0]);
                } else {
                    //stop tracking touch
                    Log.d(TAG, "left: " + left[0] + " right: " + right[0]);
                    tempAccEventsPath = "TempaccEvents" + ride.getId() + ".csv";
                    tempAccGpsPath = "Temp" + gpsFile.getName();
                    tempGpsFile = updateRoute(left[0], right[0], tempAccGpsPath);
                    refreshRoute(true);
                    Toast.makeText(ShowRouteActivity.this, getString(R.string.newIncidents), Toast.LENGTH_LONG).show();
                    lastLeft = left[0];
                    lastRight = right[0];
                }

            }
        });

        saveButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    saveButton.setElevation(0.0f);
                    saveButton.setBackground(getDrawable(R.drawable.button_pressed));
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    saveButton.setElevation(2 * ShowRouteActivity.this.getResources().getDisplayMetrics().density);
                    saveButton.setBackground(getDrawable(R.drawable.button_unpressed));
                }
                return false;
            }

        });

        saveButton.setOnClickListener((View v) -> {

            String content = "";
            int appVersion = getAppVersionNumber(ShowRouteActivity.this);
            String fileVersion = "";
            try (BufferedReader br = new BufferedReader(new FileReader(getFileStreamPath("metaData.csv")))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("#")) {
                        String[] fileInfoArray = line.split("#");
                        fileVersion = fileInfoArray[1];
                        continue;
                    }
                    String[] metaDataLine = line.split(",", -1);
                    String metaDataRide = line;
                    if (metaDataLine[0].equals(ride.getId())) {
                        metaDataLine[3] = "1";
                        metaDataRide = (metaDataLine[0] + "," + metaDataLine[1] + "," + metaDataLine[2] + "," + metaDataLine[3]);
                    }
                    content += metaDataRide += System.lineSeparator();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            String fileInfoLine = appVersion + "#" + fileVersion + System.lineSeparator();
            overWriteFile((fileInfoLine + content), "metaData.csv", this);


            // tempAccEventsPath
            // tempAccGpsPath
            if (tempGpsFile != null && fileExists(tempGpsFile.getName(), this)) {
                Log.d(TAG, "path of tempGpsFile: " + tempGpsFile.getPath());
                deleteFile(pathToAccGpsFile);
                String path = Constants.APP_PATH + "files/";
                boolean success = tempGpsFile.renameTo(new File(path + pathToAccGpsFile));
                Log.d(TAG, "tempGpsFile successfully renamed: " + success);
            }
            String pathToAccEventsFile = "accEvents" + ride.getId() + ".csv";
            if (tempAccEventsPath != null) {
                deleteFile(pathToAccEventsFile);
                String path = Constants.APP_PATH + "files/";
                File tempAccEventsFile = new File(path + tempAccEventsPath);
                Log.d(TAG, "path of tempAccEventsFile: " + tempAccEventsFile.getPath());
                boolean success = tempAccEventsFile.renameTo(new File(path + pathToAccEventsFile));
                Log.d(TAG, "tempAccEventsFile successfully renamed: " + success);
            }

            Toast.makeText(this, getString(R.string.savedRide), Toast.LENGTH_SHORT).show();
            finish();
        });

        Log.d(TAG, "onCreate() finished");

    }

    private void refreshRoute(boolean temp) {

        // Create a ride object with the accelerometer, gps and time data
        if (temp) {
            try {
                tempRide = new Ride(tempGpsFile, duration, startTime,/*date,*/ state, bike, child, trailer, pLoc, true, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                ride = new Ride(gpsFile, duration, startTime,/*date,*/ state, bike, child, trailer, pLoc, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Get the Route as a Polyline to be displayed on the map
        if (temp) {
            mMapView.getOverlayManager().remove(route);
            if (tempRoute != null) {
                mMapView.getOverlayManager().remove(tempRoute);
            }
            tempRoute = tempRide.getRoute();
            Log.d(TAG, "temp route size: " + tempRoute.getPoints().size());
            mMapView.getOverlayManager().add(tempRoute);

        } else {
            route = ride.getRoute();
            Log.d(TAG, "route size: " + route.getPoints().size());
            mMapView.getOverlayManager().add(route);

        }

        // Get a bounding box of the route so the view can be moved to it and the zoom can be
        // set accordingly
        BoundingBox bBox;
        if (temp) {
            bBox = getBoundingBox(tempRoute);
        } else {
            bBox = getBoundingBox(route);
        }


        mMapView.invalidate();
        zoomToBBox(bBox);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // (3): CenterMap
        ImageButton centerMap = findViewById(R.id.bounding_box_center_button);

        BoundingBox finalBBox = bBox;
        centerMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "boundingBoxCenterMap clicked ");
                zoomToBBox(finalBBox);
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
        if (temp) {
            if (myMarkerFunct != null) {
                myMarkerFunct.deleteAllMarkers();
            }
            myMarkerFunct = new MarkerFunct(this, true);
        } else {
            myMarkerFunct = new MarkerFunct(this, false);
        }

        // Show all the incidents present in our ride object
        Log.d(TAG, "showing all incidents");
        myMarkerFunct.showIncidents();

        addCustomMarkerMode = false;

        Log.d(TAG, "setting up mReceive");

        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {

                if (addCustomMarkerMode) {
                    // Call custom marker adding functionality which enables the user to put
                    // markers on the map

                    myMarkerFunct.addCustMarker(p);
                    exitAddIncBttn.performClick();
                    return true;
                } else {
                    InfoWindow.closeAllInfoWindowsOn(mMapView);
                    return false;
                }

            }

            @Override
            public boolean longPressHelper(GeoPoint p) {

                if (addCustomMarkerMode) {
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
    }

    private File updateRoute(int left, int right, String pathToAccGpsFile) {
        String content = "";
        FileOutputStream writer = null;
        try {
            writer = openFileOutput(pathToAccGpsFile, MODE_PRIVATE);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(ride.accGpsFile)))) {
                String line;
                content += (br.readLine() + System.lineSeparator()); // fileInfo
                content += (br.readLine() + System.lineSeparator()); // csv header
                writer.write(content.getBytes());
                writer.flush();
                int partOfRideNumber = 0;
                while ((line = br.readLine()) != null) {
                    if (!line.startsWith(",,")) {
                        partOfRideNumber++;
                    }
                    if ((partOfRideNumber >= left) && (partOfRideNumber <= right)) {
                        content += (br.readLine() + System.lineSeparator());
                        writer.write((line + System.lineSeparator()).getBytes());
                        writer.flush();
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return getFileStreamPath(pathToAccGpsFile);
    }

    // If the user clicks on an InfoWindow and IncidentPopUpActivity for that
    // event opens, upon closing it again onActivityResult is called and the
    // displaying of markers is updated if necessary.

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {

            if (resultCode == Activity.RESULT_OK) {

                String result = data.getStringExtra("result");

                String[] incidentProps = result.split(",", -1);

                boolean annotated = checkForAnnotation(incidentProps);

                myMarkerFunct.setMarker(new AccEvent(Integer.valueOf(incidentProps[0]),
                        Double.parseDouble(incidentProps[1]), Double.parseDouble(incidentProps[2]),
                        Long.parseLong(incidentProps[3]),
                        annotated), Integer.valueOf(incidentProps[0]));

            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        Log.d(TAG, "tempAccEventsPath: " + tempAccEventsPath);

        if (tempAccEventsPath != null && fileExists(tempAccEventsPath, this)) {
            deleteFile(tempAccEventsPath);
        }
        if (tempAccGpsPath != null && fileExists(tempAccGpsPath, this)) {
            deleteFile(tempAccGpsPath);
        }

        //*****************************************************************
        // Shutdown pool and await termination to make sure the program
        // doesn't continue without the relevant work being completed

        pool.shutdown();

        try {
            pool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    // Returns the longitudes of the southern- and northernmost points
    // as well as the latitudes of the western- and easternmost points
    // in a double Array {South, North, West, East}
    static BoundingBox getBoundingBox(Polyline pl) {

        // {North, East, South, West}
        ArrayList<GeoPoint> geoPoints = pl.getPoints();

        double[] border = {geoPoints.get(0).getLatitude(), geoPoints.get(0).getLongitude(), geoPoints.get(0).getLatitude(), geoPoints.get(0).getLongitude()};

        for (int i = 0; i < geoPoints.size(); i++) {
            // Check for south/north
            if (geoPoints.get(i).getLatitude() < border[2]) {
                border[2] = geoPoints.get(i).getLatitude();
            }
            if (geoPoints.get(i).getLatitude() > border[0]) {
                border[0] = geoPoints.get(i).getLatitude();
            }
            // Check for west/east
            if (geoPoints.get(i).getLongitude() < border[3]) {
                border[3] = geoPoints.get(i).getLongitude();
            }
            if (geoPoints.get(i).getLongitude() > border[1]) {
                border[1] = geoPoints.get(i).getLongitude();
            }

        }

        return new BoundingBox(border[0] + 0.001, border[1] + 0.001, border[2] - 0.001, border[3] - 0.001);
    }

    // zoom automatically to the bounding box. Usually the command in the if body should suffice
    // but osmdroid is buggy and we need the else part to fix it.
    public void zoomToBBox(BoundingBox bBox) {
        if ((mMapView.getIntrinsicScreenRect(null).bottom - mMapView.getIntrinsicScreenRect(null).top) > 0) {
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
        if (mMapView.getMaxZoomLevel() > 19.0) {
            mMapView.setMaxZoomLevel(19.0);
        }
    }

    // Show how to add custom view in android alert dialog.
    private void showCustomViewAlertDialog() {

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message mesg) {
                throw new RuntimeException();
            }
        };

        // Store the created AlertDialog instance.
        // Because only AlertDialog has cancel method.
        alertDialog = null;

        // Create a alert dialog builder.
        final AlertDialog.Builder builder = new AlertDialog.Builder(ShowRouteActivity.this);

        // Get custom login form view.
        View settingsView = getLayoutInflater().inflate(R.layout.activity_ride_settings, null);

        // Set above view in alert dialog.
        builder.setView(settingsView);

        // Bike Type and Phone Location Spinners

        Spinner bikeTypeSpinner = settingsView.findViewById(R.id.bikeTypeSpinnerRideSettings);
        Spinner phoneLocationSpinner = settingsView.findViewById(R.id.locationTypeSpinnerRideSettings);

        CheckBox childCheckBoxButton = settingsView.findViewById(R.id.childCheckBoxRideSettings);
        CheckBox trailerCheckBoxButton = settingsView.findViewById(R.id.trailerCheckBoxRideSettings);

        CheckBox rememberMyChoiceCheckBox = settingsView.findViewById(R.id.rememberMyChoice);

        bikeTypeSpinner.setSelection(bike);
        phoneLocationSpinner.setSelection(pLoc);

        // Load previous child and trailer settings

        if (child == 1) {
            childCheckBoxButton.setChecked(true);
        }

        childCheckBoxButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    child = 1;
                } else {
                    child = 0;
                }
            }
        });

        if (trailer == 1) {
            trailerCheckBoxButton.setChecked(true);
        }
        trailerCheckBoxButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    trailer = 1;
                } else {
                    trailer = 0;
                }
            }
        });
        // doneButton click listener.
        MaterialButton doneButton = (MaterialButton) settingsView.findViewById(R.id.done_button);

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    bike = bikeTypeSpinner.getSelectedItemPosition();
                    pLoc = phoneLocationSpinner.getSelectedItemPosition();
                    if (rememberMyChoiceCheckBox.isChecked()) {
                        writeIntToSharedPrefs("Settings-BikeType", bike, "simraPrefs", ShowRouteActivity.this);
                        writeIntToSharedPrefs("Settings-PhoneLocation", pLoc, "simraPrefs", ShowRouteActivity.this);
                        writeIntToSharedPrefs("Settings-Child", child, "simraPrefs", ShowRouteActivity.this);
                        writeIntToSharedPrefs("Settings-Trailer", trailer, "simraPrefs", ShowRouteActivity.this);
                    }
                    // Close Alert Dialog.
                    handler.sendMessage(handler.obtainMessage());
                    alertDialog.cancel();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        builder.setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();

        try {
            Looper.loop();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public boolean getDialogValueBack(int left, int right) {

        View checkBoxView = View.inflate(this, R.layout.checkbox, null);
        CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                writeBooleanToSharedPrefs("ShowRoute-Warning", !checkBox.isChecked(), "simraPrefs", ShowRouteActivity.this);
            }
        });
        checkBox.setText(getString(R.string.doNotShowAgain));
        AlertDialog.Builder alert = new AlertDialog.Builder(ShowRouteActivity.this);
        alert.setTitle(getString(R.string.warningRefreshTitle));
        alert.setMessage(getString(R.string.warningRefresMessage));
        alert.setView(checkBoxView);
        alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                continueWithRefresh = true;
                //stop tracking touch
                Log.d(TAG, "left: " + left + " right: " + right);
                tempAccEventsPath = "TempaccEvents" + ride.getId() + ".csv";
                tempAccGpsPath = "Temp" + gpsFile.getName();
                tempGpsFile = updateRoute(left, right, tempAccGpsPath);
                refreshRoute(true);
                // Toast.makeText(ShowRouteActivity.this, getString(R.string.newIncidents), Toast.LENGTH_LONG).show();
                lastLeft = left;
                lastRight = right;
            }
        });
        alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                continueWithRefresh = false;
                privacySlider.setValue(lastLeft, lastRight);
            }
        });
        alert.show();


        return continueWithRefresh;


    }

}
