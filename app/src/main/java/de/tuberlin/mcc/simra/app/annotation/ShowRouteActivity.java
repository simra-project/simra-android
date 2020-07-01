package de.tuberlin.mcc.simra.app.annotation;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.jaygoo.widget.OnRangeChangedListener;
import com.jaygoo.widget.RangeSeekBar;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.IconOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.activities.EvaluateClosePassActivity;
import de.tuberlin.mcc.simra.app.entities.AccEvent;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import de.tuberlin.mcc.simra.app.util.SharedPref;

import static de.tuberlin.mcc.simra.app.util.Constants.METADATA_HEADER;
import static de.tuberlin.mcc.simra.app.util.Constants.ZOOM_LEVEL;
import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpBooleanSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpIntSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SharedPref.writeBooleanToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.checkForAnnotation;
import static de.tuberlin.mcc.simra.app.util.Utils.fileExists;
import static de.tuberlin.mcc.simra.app.util.Utils.getAppVersionNumber;
import static de.tuberlin.mcc.simra.app.util.Utils.overWriteFile;

public class ShowRouteActivity extends BaseActivity {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Log tag
    private static final String TAG = "ShowRouteActivity_LOG";
    final int[] left = {0};
    final int[] right = {0};
    ////~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Our ride
    public Ride ride;
    public ExecutorService pool;
    public Drawable editMarkerDefault;
    public Drawable editCustMarker;
    public Drawable editDoneDefault;
    public Drawable editDoneCust;
    public int state;
    public Ride tempRide;
    ImageButton backBtn;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // GEOCODER --> obtain GeoPoint from address
    TextView toolbarTxt;
    String startTime = "";
    RelativeLayout saveButton;

    // Marker-icons for different types/states of events:
    // Automatically detected/custom; to be annotated/already annotated
    RelativeLayout exitButton;
    IconOverlay startFlagOverlay;
    IconOverlay finishFlagOverlay;
    MapEventsOverlay overlayEvents;
    boolean addCustomMarkerMode;
    MarkerFunct myMarkerFunct;
    AlertDialog alertDialog;

    int bike;
    int child;
    int trailer;
    int pLoc;
    String duration;
    String pathToAccGpsFile;
    File gpsFile;
    Polyline route;
    Polyline editableRoute;
    File tempGpsFile;
    String tempAccGpsPath;
    String tempAccEventsPath;
    Polyline tempRoute;
    Long tempStartTime;
    Long tempEndTime;
    boolean showWarning = true;
    boolean continueWithRefresh = true;
    int lastLeft;
    int lastRight;
    RangeSeekBar privacySlider;
    int routeSize = 3;
    BoundingBox bBox;
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Map stuff, Overlays
    private MapView mMapView;
    private RelativeLayout addIncBttn;
    private RelativeLayout exitAddIncBttn;
    private View progressBarRelativeLayout;

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

    public MapView getmMapView() {
        return mMapView;
    }

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
        backBtn.setOnClickListener(v -> finish());

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Map configuration
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        mMapView = findViewById(R.id.showRouteMap);
        mMapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mMapView.setMultiTouchControls(true); // gesture zooming
        mMapView.setFlingEnabled(true);
        MapController mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(ZOOM_LEVEL);
        TextView copyrightTxt = findViewById(R.id.copyright_text);
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
        bike = SharedPref.Settings.Ride.BikeType.getBikeType(this);
        child = SharedPref.Settings.Ride.ChildOnBoard.getValue(this);
        trailer = SharedPref.Settings.Ride.BikeWithTrailer.getValue(this);
        pLoc = SharedPref.Settings.Ride.PhoneLocation.getPhoneLocation(this);

        privacySlider = findViewById(R.id.privacySlider);
        TextView privacySliderDescription = findViewById(R.id.privacySliderDescription);
        LinearLayout privacySliderLinearLayout = findViewById(R.id.privacySliderLinearLayout);
        saveButton = findViewById(R.id.saveIncident);
        exitButton = findViewById(R.id.exitShowRoute);

        if (state < 2) {
            addIncBttn.setVisibility(View.VISIBLE);
            exitButton.setVisibility(View.INVISIBLE);
            //if (!IOUtils.isDirectoryEmpty(IOUtils.Directories.getPictureCacheDirectoryPath())) {
            Intent intent = new Intent(ShowRouteActivity.this, EvaluateClosePassActivity.class);
            intent.putExtra("PathToAccGpsFile", pathToAccGpsFile);
            startActivity(intent);
            //}
        } else {
            addIncBttn.setVisibility(View.GONE);
            privacySliderLinearLayout.setVisibility(View.INVISIBLE);
            privacySliderDescription.setVisibility(View.INVISIBLE);
            saveButton.setVisibility(View.INVISIBLE);
            exitButton.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    exitButton.setElevation(0.0f);
                    exitButton.setBackground(getDrawable(R.drawable.button_pressed));
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    exitButton.setElevation(2 * ShowRouteActivity.this.getResources().getDisplayMetrics().density);
                    exitButton.setBackground(getDrawable(R.drawable.button_unpressed));
                }
                return false;
            });
            exitButton.setOnClickListener(v -> finish());
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

        if (state < 2) {
            fireRideSettingsDialog();
        } else {
            new RideUpdateTask(false, false).execute();
        }
        Log.d(TAG, "onCreate() finished");

    }

    private void refreshRoute(boolean temp, boolean calculate) throws IOException {

        // Create a ride object with the accelerometer, gps and time data
        if (temp) {
            tempAccEventsPath = "TempaccEvents" + ride.getKey() + ".csv";
            tempAccGpsPath = "Temp" + gpsFile.getName();
            runOnUiThread(() -> progressBarRelativeLayout.setVisibility(View.VISIBLE));
            tempGpsFile = updateRoute(left[0], right[0], tempAccGpsPath);
            tempRide = new Ride(tempGpsFile, duration, String.valueOf(tempStartTime), state, bike, child, trailer, pLoc, this, calculate, true);
        } else {
            ride = new Ride(gpsFile, duration, startTime, state, bike, child, trailer, pLoc, this, calculate, false);
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

        if (!temp) {
            runOnUiThread(() -> {
                bBox = getBoundingBox(route);
                zoomToBBox(bBox);
                mMapView.invalidate();
            });
        }

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // (3): CenterMap
        ImageButton centerMap = findViewById(R.id.bounding_box_center_button);

        centerMap.setOnClickListener(v -> {
            Log.d(TAG, "boundingBoxCenterMap clicked ");
            zoomToBBox(bBox);
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
        pool = Executors.newFixedThreadPool(6);

        // Create an instance of MarkerFunct-class which provides all functionality related to
        // incident markers
        Log.d(TAG, "creating MarkerFunct object");
        runOnUiThread(() -> {
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
        runOnUiThread(() -> {
            privacySlider.setRange(0, routeSize);
            ArrayList<OverlayItem> items = new ArrayList<>();
            Drawable startFlag = ShowRouteActivity.this.getResources().getDrawable(R.drawable.startblack, null);
            Drawable finishFlag = ShowRouteActivity.this.getResources().getDrawable(R.drawable.racingflagblack, null);
            GeoPoint startFlagPoint;
            GeoPoint finishFlagPoint;
            if (temp) {
                startFlagPoint = tempRoute.getPoints().get(0);
                finishFlagPoint = tempRoute.getPoints().get(tempRoute.getPoints().size() - 1);
            } else {
                startFlagPoint = route.getPoints().get(0);
                finishFlagPoint = route.getPoints().get(route.getPoints().size() - 1);
                privacySlider.setValue(0, routeSize);
            }

            startFlagOverlay = new IconOverlay(startFlagPoint, startFlag);
            finishFlagOverlay = new IconOverlay(finishFlagPoint, finishFlag);
            mMapView.getOverlays().add(startFlagOverlay);
            mMapView.getOverlays().add(finishFlagOverlay);
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
                if (rightValue > routeSize) {
                    rightValue = routeSize;
                }
                editableRoute.setPoints(route.getPoints().subList((int) leftValue, (int) rightValue));
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
                    Log.d(TAG, "lastLeft: " + lastLeft + " lastRight: " + lastRight);
                    mMapView.getOverlays().remove(startFlagOverlay);
                    mMapView.getOverlays().remove(finishFlagOverlay);


                    new RideUpdateTask(true, true).execute();

                    lastLeft = left[0];
                    lastRight = right[0];
                }

            }
        });


        runOnUiThread(() -> privacySlider.setRange(0, routeSize));

        saveButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                saveButton.setElevation(0.0f);
                saveButton.setBackground(getDrawable(R.drawable.button_pressed));
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                saveButton.setElevation(2 * ShowRouteActivity.this.getResources().getDisplayMetrics().density);
                saveButton.setBackground(getDrawable(R.drawable.button_unpressed));
            }
            return false;
        });

        saveButton.setOnClickListener((View v) -> saveChanges(temp));

        overlayEvents = new MapEventsOverlay(getBaseContext(), mReceive);
        runOnUiThread(() -> {
            mMapView.getOverlays().add(overlayEvents);
            mMapView.invalidate();
        });

    }

    private void saveChanges(boolean temp) {
        Log.d(TAG, "saveChanges(): ride.events.size(): " + ride.events.size());
        StringBuilder metaDataContent = new StringBuilder();
        int appVersion = getAppVersionNumber(ShowRouteActivity.this);
        String metaDataFileVersion = "";
        try (BufferedReader br = new BufferedReader(new FileReader(getFileStreamPath("metaData.csv")))) {
            // metaDataFileVersion line: 23#7
            metaDataFileVersion = br.readLine().split("#")[1];
            // skip header
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] metaDataLine = line.split(",", -1);
                String metaDataRide = line;
                Log.d(TAG, "metaDataLine[0]: " + metaDataLine[0] + " ride.getKey(): " + ride.getKey());
                // loop through metaData.csv to find the right ride
                if (metaDataLine[0].equals(ride.getKey())) {
                    long distance = 0;
                    long waitedTime = 0;
                    int numberOfIncidents = 0;
                    int numberOfScary = 0;
                    if (temp) {
                        metaDataLine[1] = String.valueOf(tempStartTime);
                        metaDataLine[2] = String.valueOf(tempEndTime);
                        distance = tempRide.distance;
                        waitedTime = tempRide.waitedTime;
                        ArrayList<AccEvent> accEventArrayList = tempRide.events;
                        Log.d(TAG, "accEventArrayList.size(): " + accEventArrayList.size());
                        for (int i = 0; i < accEventArrayList.size(); i++) {
                            Log.d(TAG, "accEvent " + tempRide.events.get(i).key + ": " + tempRide.events.get(i).annotated + " scary: " + tempRide.events.get(i).scary);
                            if (accEventArrayList.get(i).annotated) {
                                numberOfIncidents++;
                            }
                            if (accEventArrayList.get(i).scary.equals("1")) {
                                numberOfScary++;
                            }
                        }
                    } else {
                        distance = ride.distance;
                        waitedTime = ride.waitedTime;
                        ArrayList<AccEvent> accEventArrayList = ride.events;
                        Log.d(TAG, "accEventArrayList.size(): " + accEventArrayList.size());
                        for (int i = 0; i < accEventArrayList.size(); i++) {
                            Log.d(TAG, "accEvent " + ride.events.get(i).key + ": " + ride.events.get(i).annotated + " scary: " + ride.events.get(i).scary);
                            if (accEventArrayList.get(i).annotated) {
                                numberOfIncidents++;
                            }
                            if (accEventArrayList.get(i).scary.equals("1")) {
                                numberOfScary++;
                            }
                        }
                    }
                    metaDataLine[3] = "1";

                    // key,startTime,endTime,state,numberOfIncidents,waitedTime,distance
                    metaDataRide = (metaDataLine[0] + "," + metaDataLine[1] + "," + metaDataLine[2] + "," + metaDataLine[3] + "," + numberOfIncidents + "," + waitedTime + "," + distance + "," + numberOfScary + "," + lookUpIntSharedPrefs("Region", 0, "Profile", this));
                }
                Log.d(TAG, "metaDataRide: " + metaDataRide);
                Log.d(TAG, "numberOfIncidents: " + metaDataRide.split(",")[4] + " numberOfScary: " + metaDataRide.split(",")[7]);
                metaDataContent.append(metaDataRide).append(System.lineSeparator());
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        String fileInfoLine = appVersion + "#" + metaDataFileVersion + System.lineSeparator();
        overWriteFile((fileInfoLine + METADATA_HEADER + metaDataContent), "metaData.csv", this);
        /*
        StringBuilder accEventsDataContent = new StringBuilder();
        String accEventsFileVersion = "";
        String accEventName = "accEvents" + ride.getKey() + ".csv";
        if(temp) {
            accEventName = "Temp" + accEventName;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(getFileStreamPath(accEventName)))) {
            // accEventsFileVersion line: 23#7
            accEventsFileVersion = br.readLine().split("#")[1];
            // skip header
            String line = br.readLine();

            while ((line = br.readLine()) != null) {
                String[] accEventsLine = line.split(",", -1);
                if (checkForAnnotation(accEventsLine)) {
                    accEventsDataContent.append(line).append(System.lineSeparator());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        fileInfoLine = appVersion + "#" + accEventsFileVersion + System.lineSeparator();
        overWriteFile((fileInfoLine + ACCEVENTS_HEADER + accEventsDataContent), accEventName, this);
        */
        // tempAccEventsPath
        // tempAccGpsPath
        if (tempGpsFile != null && fileExists(tempGpsFile.getName(), this)) {
            Log.d(TAG, "path of tempGpsFile: " + tempGpsFile.getPath());
            deleteFile(pathToAccGpsFile);
            String path = ShowRouteActivity.this.getFilesDir().getPath();
            boolean success = tempGpsFile.renameTo(new File(path + File.separator + pathToAccGpsFile));
            Log.d(TAG, "tempGpsFile successfully renamed: " + success);
        }
        String pathToAccEventsFile = "accEvents" + ride.getKey() + ".csv";
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

    }

    private File updateRoute(int left, int right, String pathToAccGpsFile) {
        tempStartTime = null;
        tempEndTime = null;
        File inputFile = getFileStreamPath(pathToAccGpsFile);
        StringBuilder content = new StringBuilder();
        //String content = "";
        try (BufferedWriter writer = new BufferedWriter((new FileWriter(inputFile, false)))) {

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
                        // Log.d(TAG, "line: " + line);
                        if (tempStartTime == null || Long.parseLong(line.split(",", -1)[5]) < tempStartTime) {
                            tempStartTime = Long.valueOf(line.split(",", -1)[5]);
                        }
                        if (tempEndTime == null || Long.parseLong(line.split(",", -1)[5]) > tempEndTime) {
                            tempEndTime = Long.valueOf(line.split(",", -1)[5]);
                        }
                        content.append(line).append(System.lineSeparator());
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            writer.append(content);
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

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {

            if (resultCode == Activity.RESULT_OK) {

                String result = data.getStringExtra("result");
                String[] incidentProps = result.split(",", -1);
                Log.d(TAG, "onActivityResult() result: " + result);
                boolean temp = data.getBooleanExtra("temp", false);
                Log.d(TAG, "onActivityResult() temp: " + temp);
                boolean annotated = checkForAnnotation(incidentProps);
                if (temp) {
                    Log.d(TAG, "tempRide.events.size(): " + tempRide.events.size());
                    for (int i = 0; i < tempRide.events.size(); i++) {
                        Log.d(TAG, "tempRide.events.get(i).key: " + tempRide.events.get(i).key +
                                " Integer.valueOf(incidentProps[0]): " + Integer.valueOf(incidentProps[0]));
                        if ((tempRide.events.get(i).key) == Integer.parseInt(incidentProps[0])) {
                            if (annotated) {
                                tempRide.events.get(i).annotated = true;
                            }
                            if (incidentProps[8] != null) {
                                tempRide.events.get(i).incidentType = incidentProps[8];
                            }
                            if (incidentProps[18] != null) {
                                tempRide.events.get(i).scary = incidentProps[18];
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "ride.events.size(): " + ride.events.size());
                    for (int i = 0; i < ride.events.size(); i++) {
                        Log.d(TAG, "ride.events.get(i).key: " + ride.events.get(i).key +
                                " Integer.valueOf(incidentProps[0]): " + Integer.valueOf(incidentProps[0]));
                        if ((ride.events.get(i).key) == Integer.parseInt(incidentProps[0])) {
                            if (annotated) {
                                ride.events.get(i).annotated = true;
                            }
                            if (incidentProps[8] != null) {
                                ride.events.get(i).incidentType = incidentProps[8];
                            }
                            if (incidentProps[18] != null) {
                                ride.events.get(i).scary = incidentProps[18];
                            }
                        }
                    }
                }
                myMarkerFunct.setMarker(new AccEvent(Integer.parseInt(incidentProps[0]),
                        Double.parseDouble(incidentProps[1]), Double.parseDouble(incidentProps[2]),
                        Long.parseLong(incidentProps[3]),
                        annotated, incidentProps[8], incidentProps[18]), Integer.parseInt(incidentProps[0]));

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
        } catch (InterruptedException | NullPointerException ie) {
            ie.printStackTrace();
        }
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
        MaterialButton doneButton = settingsView.findViewById(R.id.done_button);

        doneButton.setOnClickListener(view -> {
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
                    SharedPref.Settings.Ride.PhoneLocation.setPhoneLocation(pLoc, ShowRouteActivity.this);
                    SharedPref.Settings.Ride.ChildOnBoard.setChildOnBoardByValue(child, ShowRouteActivity.this);
                    SharedPref.Settings.Ride.BikeWithTrailer.setTrailerByValue(trailer, ShowRouteActivity.this);
                    SharedPref.Settings.Ride.BikeType.setBikeType(bike, ShowRouteActivity.this);
                }
                // Close Alert Dialog.
                alertDialog.cancel();
                progressBarRelativeLayout.setVisibility(View.VISIBLE);
                if (state == 0) {
                    new RideUpdateTask(false, true).execute();
                } else {
                    new RideUpdateTask(false, false).execute();
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        builder.setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();
    }

    public boolean firePrivacySliderWarningDialog(int left, int right) {

        View checkBoxView = View.inflate(this, R.layout.checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.checkbox);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> writeBooleanToSharedPrefs("ShowRoute-Warning", !checkBox.isChecked(), "simraPrefs", ShowRouteActivity.this));
        checkBox.setText(getString(R.string.doNotShowAgain));
        AlertDialog.Builder alert = new AlertDialog.Builder(ShowRouteActivity.this);
        alert.setTitle(getString(R.string.warning));
        alert.setMessage(getString(R.string.warningRefreshMessage));
        alert.setView(checkBoxView);
        alert.setPositiveButton(R.string.yes, (dialog, id) -> {
            continueWithRefresh = true;
            new RideUpdateTask(true, true).execute();
            lastLeft = left;
            lastRight = right;
        });
        alert.setNegativeButton(R.string.no, (dialog, id) -> {
            continueWithRefresh = false;
            privacySlider.setValue(lastLeft, lastRight);
        });
        alert.show();
        return continueWithRefresh;
    }

    private class RideUpdateTask extends AsyncTask<String, String, String> {

        private boolean temp;
        private boolean calculateEvents;

        private RideUpdateTask(boolean temp, boolean calculateEvents) {
            this.temp = temp;
            this.calculateEvents = calculateEvents;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        protected String doInBackground(String... urls) {
            Log.d(TAG, "doInBackground()");
            try {
                refreshRoute(temp, calculateEvents);
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
}