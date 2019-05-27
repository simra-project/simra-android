package de.tuberlin.mcc.simra.app.annotation;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.LinearLayout;
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
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.util.BaseActivity;

import static de.tuberlin.mcc.simra.app.util.Constants.ZOOM_LEVEL;
import static de.tuberlin.mcc.simra.app.util.Utils.checkForAnnotation;
import static de.tuberlin.mcc.simra.app.util.Utils.fileExists;
import static de.tuberlin.mcc.simra.app.util.Utils.getAppVersionNumber;
import static de.tuberlin.mcc.simra.app.util.Utils.lookUpBooleanSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.lookUpIntSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.overWriteFile;
import static de.tuberlin.mcc.simra.app.util.Utils.writeBooleanToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.writeIntToSharedPrefs;

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
    RelativeLayout saveButton;
    RelativeLayout exitButton;

    ////~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Our ride
    public Ride ride;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Log tag
    private static final String TAG = "ShowRouteActivity_LOG";

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // GEOCODER --> obtain GeoPoint from address

    public ExecutorService pool;

    public MapView getmMapView() {
        return mMapView;
    }

    MapEventsOverlay overlayEvents;

    // Marker-icons for different types/states of events:
    // Automatically detected/custom; to be annotated/already annotated

    public Drawable editMarkerDefault;
    public Drawable editCustMarker;
    public Drawable editDoneDefault;
    public Drawable editDoneCust;
    boolean addCustomMarkerMode;
    MarkerFunct myMarkerFunct;
    AlertDialog alertDialog;

    int bike;
    int child;
    int trailer;
    int pLoc;

    public int state;
    String duration;
    String pathToAccGpsFile;
    File gpsFile;
    Polyline route;
    Polyline editableRoute;

    public Ride tempRide;
    File tempGpsFile;
    String tempAccGpsPath;
    String tempAccEventsPath;
    Polyline tempRoute;
    boolean showWarning = true;
    boolean continueWithRefresh = true;

    int lastLeft;
    int lastRight;

    RangeSeekBar privacySlider;

    final int[] left = {0};
    final int[] right = {0};

    int routeSize = 3;
    private View progressBarRelativeLayout;
    BoundingBox bBox;

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
        MapController mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(ZOOM_LEVEL);
        TextView copyrightTxt = (TextView) findViewById(R.id.copyright_text);
        copyrightTxt.setMovementMethod(LinkMovementMethod.getInstance());



        pathToAccGpsFile = getIntent().getStringExtra("PathToAccGpsFile");
        startTime = getIntent().getStringExtra("StartTime");
        state = getIntent().getIntExtra("State", 0);
        Log.d(TAG, "state: " + state);
        duration = getIntent().getStringExtra("Duration");

        addIncBttn = findViewById(R.id.addIncident);

        exitAddIncBttn = findViewById(R.id.exitAddIncident);
        exitAddIncBttn.setVisibility(View.GONE);
        progressBarRelativeLayout = findViewById(R.id.progressBarRelativeLayout);

        // scales tiles to dpi of current display
        mMapView.setTilesScaledToDpi(true);

        gpsFile = getFileStreamPath(pathToAccGpsFile);

        Log.d(TAG, "creating ride objects");
        bike = lookUpIntSharedPrefs("Settings-BikeType", 0, "simraPrefs", this);
        child = lookUpIntSharedPrefs("Settings-Child", 0, "simraPrefs", this);
        trailer = lookUpIntSharedPrefs("Settings-Trailer", 0, "simraPrefs", this);
        pLoc = lookUpIntSharedPrefs("Settings-PhoneLocation", 0, "simraPrefs", this);

        privacySlider = findViewById(R.id.privacySlider);
        TextView privacySliderDescription = findViewById(R.id.privacySliderDescription);
        LinearLayout privacySliderLinearLayout = findViewById(R.id.privacySliderLinearLayout);
        saveButton = findViewById(R.id.saveIncident);
        exitButton = findViewById(R.id.exitShowRoute);

        if (state < 2) {
            addIncBttn.setVisibility(View.VISIBLE);
            exitButton.setVisibility(View.INVISIBLE);
        } else {
            addIncBttn.setVisibility(View.GONE);
            privacySliderLinearLayout.setVisibility(View.INVISIBLE);
            privacySliderDescription.setVisibility(View.INVISIBLE);
            saveButton.setVisibility(View.INVISIBLE);
            exitButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        exitButton.setElevation(0.0f);
                        exitButton.setBackground(getDrawable(R.drawable.button_pressed));
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        exitButton.setElevation(2 * ShowRouteActivity.this.getResources().getDisplayMetrics().density);
                        exitButton.setBackground(getDrawable(R.drawable.button_unpressed));
                    }
                    return false;
                }

            });
            exitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        Log.d(TAG, "setting up clickListeners");

        // Functionality for 'edit mode', i.e. the mode in which users can put their own incidents
        // onto the map
        addIncBttn.setOnClickListener((View v) -> {
            addIncBttn.setVisibility(View.INVISIBLE);
            exitAddIncBttn.setVisibility(View.VISIBLE);
            ShowRouteActivity.this.addCustomMarkerMode = true;
        });

        exitAddIncBttn.setOnClickListener((View v) -> {
            addIncBttn.setVisibility(View.VISIBLE);
            exitAddIncBttn.setVisibility(View.INVISIBLE);
            ShowRouteActivity.this.addCustomMarkerMode = false;
        });

        fireRideSettingsDialog();

        Log.d(TAG, "onCreate() finished");

    }

    private class RideUpdateTask extends AsyncTask<String, String, String> {

        private boolean temp;


        private RideUpdateTask(Boolean temp) {
            this.temp = temp;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        protected String doInBackground(String... urls) {
            Log.d(TAG, "doInBackground()");
            try {
                refreshRoute(temp);
            } catch (IOException e) {
                e.printStackTrace();
                cancel(true);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            Log.d(TAG, "onPostExecute()");
            super.onPostExecute(s);
            progressBarRelativeLayout.setVisibility(View.GONE);
            if (temp) {
                Toast.makeText(ShowRouteActivity.this, getString(R.string.newIncidents), Toast.LENGTH_LONG).show();
                InfoWindow.closeAllInfoWindowsOn(mMapView);
            }

        }
    }

    private void refreshRoute(boolean temp) throws IOException {

        // Create a ride object with the accelerometer, gps and time data
        if (temp) {
            tempAccEventsPath = "TempaccEvents" + ride.getId() + ".csv";
            tempAccGpsPath = "Temp" + gpsFile.getName();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBarRelativeLayout.setVisibility(View.VISIBLE);
                }
            });
            tempGpsFile = updateRoute(left[0], right[0], tempAccGpsPath);

            tempRide = new Ride(tempGpsFile, duration, startTime,/*date,*/ state, bike, child, trailer, pLoc, true, this);

        } else {

            ride = new Ride(gpsFile, duration, startTime,/*date,*/ state, bike, child, trailer, pLoc, this);

        }

        // Get the Route as a Polyline to be displayed on the map
        if (temp) {
            mMapView.getOverlayManager().remove(route);
            if (tempRoute != null) {
                mMapView.getOverlayManager().remove(tempRoute);
            }
            tempRoute = tempRide.getRoute();
            tempRoute.setWidth(8.0f);
            // tempRoute.getPaint().setStrokeJoin(Paint.Join.ROUND);
            tempRoute.getPaint().setStrokeCap(Paint.Cap.ROUND);
            Log.d(TAG, "temp route size: " + tempRoute.getPoints().size());
            mMapView.getOverlayManager().add(editableRoute);
            mMapView.getOverlayManager().add(tempRoute);


        } else {
            route = ride.getRoute();
            Log.d(TAG, "route size: " + route.getPoints().size());
            if (editableRoute == null) {
                Log.d(TAG, "adding editableRoute");
                editableRoute = new Polyline();
                editableRoute.setPoints(route.getPoints());
                editableRoute.setWidth(40.0f);

                editableRoute.getPaint().setColor(getColor(R.color.colorAccent));
                editableRoute.getPaint().setStrokeCap(Paint.Cap.ROUND);

                mMapView.getOverlayManager().add(editableRoute);
            }
            mMapView.getOverlayManager().add(route);

        }


        // Get a bounding box of the route so the view can be moved to it and the zoom can be
        // set accordingly

        if(!temp) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    bBox = getBoundingBox(route);
                    zoomToBBox(bBox);
                    mMapView.invalidate();

                }
            });
        }

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // (3): CenterMap
        ImageButton centerMap = findViewById(R.id.bounding_box_center_button);

        centerMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "boundingBoxCenterMap clicked ");
                zoomToBBox(bBox);
            }
        });

        // Set the icons for event markers
        // (1) Automatically recognized, not yet annotated

        editMarkerDefault = getResources().getDrawable(R.drawable.edit_event_blue, null);

        // (2) Custom, not yet annotated

        editCustMarker = getResources().getDrawable(R.drawable.edit_event_green, null);

        // (3) Automatically recognized, annotated

        editDoneDefault = getResources().getDrawable(R.drawable.edited_event_blue, null);

        // (4) Custom, not yet annotated

        editDoneCust = getResources().getDrawable(R.drawable.edited_event_green, null);

        // Call function for drawing markers for all AccEvents in ride, now encapsulated in
        // MarkerFunct class for better readability

        Log.d(TAG, "setting up Executors");
        pool = Executors.newFixedThreadPool(4);

        // Create an instance of MarkerFunct-class which provides all functionality related to
        // incident markers
        Log.d(TAG, "creating MarkerFunct object");
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
        if (temp) {
            if (myMarkerFunct != null) {
                myMarkerFunct.deleteAllMarkers();
            }
            myMarkerFunct = new MarkerFunct(ShowRouteActivity.this, true);
        } else {
            myMarkerFunct = new MarkerFunct(ShowRouteActivity.this, false);
        }

        // Show all the incidents present in our ride object
        Log.d(TAG, "showing all incidents");

                myMarkerFunct.showIncidents();
            }
        });

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




        routeSize = route.getPoints().size();
        if (routeSize < 2) {
            routeSize = 2;
        }
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                privacySlider.setRange(0, routeSize);
                if (!temp) {
                    privacySlider.setValue(0, routeSize);
                }
            }
        });

        Log.d(TAG, "route.size(): " + routeSize);

        lastLeft = 0;
        lastRight = routeSize;

        privacySlider.setOnRangeChangedListener(new OnRangeChangedListener() {

            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                left[0] = (int) leftValue;
                right[0] = (int) rightValue;
                int routeSize = route.getPoints().size();
                if(rightValue > routeSize) {
                    rightValue = routeSize;
                }
                editableRoute.setPoints(route.getPoints().subList((int)leftValue,(int)rightValue));
                mMapView.invalidate();
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
                //start tracking touch
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
                showWarning = lookUpBooleanSharedPrefs("ShowRoute-Warning", true, "simraPrefs", ShowRouteActivity.this);
                if (showWarning) {
                    firePrivacySliderWarningDialog(left[0], right[0]);
                } else {
                    //stop tracking touch
                    Log.d(TAG, "left: " + left[0] + " right: " + right[0]);

                    new RideUpdateTask(true).execute();

                    lastLeft = left[0];
                    lastRight = right[0];
                }

            }
        });


        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                privacySlider.setRange(0, routeSize);

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

            StringBuffer content = new StringBuffer();
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
                    content.append(metaDataRide).append(System.lineSeparator());
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
                String path = ShowRouteActivity.this.getFilesDir().getPath();
                boolean success = tempGpsFile.renameTo(new File(path + File.separator + pathToAccGpsFile));
                Log.d(TAG, "tempGpsFile successfully renamed: " + success);
            }
            String pathToAccEventsFile = "accEvents" + ride.getId() + ".csv";
            if (tempAccEventsPath != null) {
                deleteFile(pathToAccEventsFile);
                String path = ShowRouteActivity.this.getFilesDir().getPath();
                File tempAccEventsFile = new File(path + File.separator + tempAccEventsPath);
                Log.d(TAG, "path of tempAccEventsFile: " + tempAccEventsFile.getPath());
                boolean success = tempAccEventsFile.renameTo(new File(path + File.separator + pathToAccEventsFile));
                Log.d(TAG, "tempAccEventsFile successfully renamed: " + success);
            }

            Toast.makeText(this, getString(R.string.savedRide), Toast.LENGTH_SHORT).show();
            finish();
        });

        overlayEvents = new MapEventsOverlay(getBaseContext(), mReceive);
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mMapView.getOverlays().add(overlayEvents);
                mMapView.invalidate();
            }
        });

    }

    private File updateRoute(int left, int right, String pathToAccGpsFile) {

        File inputFile = getFileStreamPath(pathToAccGpsFile);
        StringBuilder content = new StringBuilder();
        //String content = "";
        try(BufferedWriter writer = new BufferedWriter((new FileWriter(inputFile,false)))) {

            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(ride.accGpsFile)))) {
                String line;
                content.append(br.readLine()).append(System.lineSeparator()); // fileInfo
                content.append(br.readLine()).append(System.lineSeparator()); // csv header
                int partOfRideNumber = 0;
                while ((line = br.readLine()) != null) {
                    if (!line.startsWith(",,")) {
                        partOfRideNumber++;
                    }
                    if ((partOfRideNumber >= left) && (partOfRideNumber <= right)) {
                        content.append(line + System.lineSeparator());
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            writer.append(content);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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


        try {
            pool.shutdown();
            pool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        } catch (NullPointerException npe) {
            npe.printStackTrace();
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

    public void fireRideSettingsDialog() {
        Log.d(TAG, "fireRideSettingsDialog()");
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

        if (trailer == 1) {
            trailerCheckBoxButton.setChecked(true);
        }

        // doneButton click listener.
        MaterialButton doneButton = (MaterialButton) settingsView.findViewById(R.id.done_button);

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    bike = bikeTypeSpinner.getSelectedItemPosition();
                    pLoc = phoneLocationSpinner.getSelectedItemPosition();
                    if (childCheckBoxButton.isChecked()) {
                        child = 1;
                    } else {
                        child = 0;
                    }
                    if (trailerCheckBoxButton.isChecked()) {
                        trailer = 1;
                    } else {
                        trailer = 0;
                    }
                    if (rememberMyChoiceCheckBox.isChecked()) {
                        writeIntToSharedPrefs("Settings-BikeType", bike, "simraPrefs", ShowRouteActivity.this);
                        writeIntToSharedPrefs("Settings-PhoneLocation", pLoc, "simraPrefs", ShowRouteActivity.this);
                        writeIntToSharedPrefs("Settings-Child", child, "simraPrefs", ShowRouteActivity.this);
                        writeIntToSharedPrefs("Settings-Trailer", trailer, "simraPrefs", ShowRouteActivity.this);
                    }
                    // Close Alert Dialog.
                    alertDialog.cancel();
                    progressBarRelativeLayout.setVisibility(View.VISIBLE);
                    new RideUpdateTask(false).execute();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        builder.setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();
    }


    public boolean firePrivacySliderWarningDialog(int left, int right) {

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
        alert.setTitle(getString(R.string.warning));
        alert.setMessage(getString(R.string.warningRefresMessage));
        alert.setView(checkBoxView);
        alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                continueWithRefresh = true;
                new RideUpdateTask(true).execute();
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
