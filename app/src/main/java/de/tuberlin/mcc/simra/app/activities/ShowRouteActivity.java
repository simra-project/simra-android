package de.tuberlin.mcc.simra.app.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.RangeSlider;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.IconOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.annotation.IncidentPopUpActivity;
import de.tuberlin.mcc.simra.app.annotation.MarkerFunct;
import de.tuberlin.mcc.simra.app.databinding.ActivityShowRouteBinding;
import de.tuberlin.mcc.simra.app.entities.DataLog;
import de.tuberlin.mcc.simra.app.entities.IncidentLog;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.entities.MetaData;
import de.tuberlin.mcc.simra.app.entities.MetaDataEntry;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import de.tuberlin.mcc.simra.app.util.IOUtils;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import de.tuberlin.mcc.simra.app.util.Utils;

import static de.tuberlin.mcc.simra.app.util.Constants.ZOOM_LEVEL;
import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpIntSharedPrefs;

public class ShowRouteActivity extends BaseActivity {

    private static final String TAG = "ShowRouteActivity_LOG";
    private static final String EXTRA_RIDE_ID = "EXTRA_RIDE_ID";
    private static final String EXTRA_STATE = "EXTRA_STATE";
    public ExecutorService pool = Executors.newFixedThreadPool(6);

    public int state;
    int start = 0;
    int end = 0;
    ActivityShowRouteBinding binding;

    IconOverlay startFlagOverlay;
    IconOverlay finishFlagOverlay;
    MapEventsOverlay overlayEvents;
    boolean addCustomMarkerMode;
    MarkerFunct myMarkerFunct;

    int bike;
    int child;
    int trailer;
    int pLoc;
    int rideId;
    File gpsFile;
    Polyline route;
    Polyline editableRoute;
    int routeSize = 3;
    BoundingBox bBox;

    private IncidentLog incidentLog;
    private DataLog dataLog;
    private DataLog originalDataLog;

    /**
     * Returns the longitudes of the southern- and northernmost points
     * as well as the latitudes of the western- and easternmost points
     *
     * @param pl
     * @return double Array {South, North, West, East}
     */
    private static BoundingBox getBoundingBox(Polyline pl) {

        // {North, East, South, West}
        List<GeoPoint> geoPoints = pl.getPoints();

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

    public static void startShowRouteActivity(int rideId, Integer state, Context context) {
        Intent intent = new Intent(context, ShowRouteActivity.class);
        intent.putExtra(EXTRA_RIDE_ID, rideId);
        intent.putExtra(EXTRA_STATE, state);
        context.startActivity(intent);

    }

    public MapView getmMapView() {
        return binding.showRouteMap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityShowRouteBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Toolbar
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        Toolbar toolbar = binding.toolbar.toolbar;
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        TextView toolbarTxt = binding.toolbar.toolbarTitle;
        toolbarTxt.setText(R.string.title_activity_showRoute);
        binding.toolbar.backButton.setOnClickListener(v -> finish());

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Map configuration
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        MapView mMapView = binding.showRouteMap;
        mMapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mMapView.setMultiTouchControls(true); // gesture zooming
        mMapView.setFlingEnabled(true);
        MapController mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(ZOOM_LEVEL);
        binding.copyrightText.setMovementMethod(LinkMovementMethod.getInstance());

        if (!getIntent().hasExtra(EXTRA_RIDE_ID)) {
            throw new RuntimeException("Extra: " + EXTRA_RIDE_ID + " not defined.");
        }
        rideId = getIntent().getIntExtra(EXTRA_RIDE_ID, 0);
        state = getIntent().getIntExtra(EXTRA_STATE, MetaData.STATE.JUST_RECORDED);
        Log.d(TAG, "before load");

        new LoadOriginalDataLogTask().execute();
        Log.d(TAG, "after load");

        binding.exitAddIncidentModeButton.setVisibility(View.GONE);

        // scales tiles to dpi of current display
        mMapView.setTilesScaledToDpi(true);

        gpsFile = IOUtils.Files.getGPSLogFile(rideId, false, this);

        bike = SharedPref.Settings.Ride.BikeType.getBikeType(this);
        child = SharedPref.Settings.Ride.ChildOnBoard.getValue(this);
        trailer = SharedPref.Settings.Ride.BikeWithTrailer.getValue(this);
        pLoc = SharedPref.Settings.Ride.PhoneLocation.getPhoneLocation(this);

        binding.routePrivacySlider.addOnSliderTouchListener(new RangeSlider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull RangeSlider slider) {
                // Remove Icons for better visibility of the track
                mMapView.getOverlays().remove(startFlagOverlay);
                mMapView.getOverlays().remove(finishFlagOverlay);
            }

            @Override
            public void onStopTrackingTouch(@NonNull RangeSlider slider) {
                start = Math.round(slider.getValues().get(0));
                end = Math.round(slider.getValues().get(1));
                if (end > originalDataLog.rideAnalysisData.route.getPoints().size()) {
                    end = routeSize;
                }
                new RideUpdateTask(true, true).execute();
            }
        });
        binding.routePrivacySlider.addOnChangeListener((slider, changeListener, touchChangeListener) -> {
            if (editableRoute != null) {
                editableRoute.setPoints(originalDataLog.rideAnalysisData.route.getPoints().subList(Math.round(slider.getValues().get(0)), Math.round(slider.getValues().get(1))));
            }
            mMapView.invalidate();
        });

        if (state < MetaData.STATE.SYNCED) {
            binding.addIncidentButton.setVisibility(View.VISIBLE);
            binding.exitShowRouteButton.setVisibility(View.INVISIBLE);
            if (!IOUtils.isDirectoryEmpty(IOUtils.Directories.getPictureCacheDirectoryPath())) {
                EvaluateClosePassActivity.startEvaluateClosePassActivity(rideId, this);
            }
        } else {
            binding.addIncidentButton.setVisibility(View.GONE);
            binding.privacySliderLinearLayout.setVisibility(View.INVISIBLE);
            binding.privacySliderDescription.setVisibility(View.INVISIBLE);
            binding.saveRouteButton.setVisibility(View.INVISIBLE);
            binding.exitShowRouteButton.setOnClickListener(v -> finish());
        }


        // Functionality for 'edit mode', i.e. the mode in which users can put their own incidents
        // onto the map
        binding.addIncidentButton.setOnClickListener((View v) -> {
            binding.addIncidentButton.setVisibility(View.INVISIBLE);
            binding.exitAddIncidentModeButton.setVisibility(View.VISIBLE);
            addCustomMarkerMode = true;
        });

        binding.exitAddIncidentModeButton.setOnClickListener((View v) -> {
            binding.addIncidentButton.setVisibility(View.VISIBLE);
            binding.exitAddIncidentModeButton.setVisibility(View.INVISIBLE);
            addCustomMarkerMode = false;
        });

        if (state < MetaData.STATE.SYNCED) {
            fireRideSettingsDialog();
        } else {
            new RideUpdateTask(false, false).execute();
        }
        Log.d(TAG, "onCreate() finished");

    }

    private void refreshRoute(int rideId, boolean updateBoundaries, boolean calculateEvents) {

        if (updateBoundaries && dataLog != null) {
            long startTime = this.originalDataLog.onlyGPSDataLogEntries.get(start).timestamp;
            long endTime = this.originalDataLog.onlyGPSDataLogEntries.get(end).timestamp;
            this.dataLog = DataLog.loadDataLog(rideId, startTime, endTime, this);
            this.incidentLog = IncidentLog.filterIncidentLogTime(IncidentLog.mergeIncidentLogs(this.incidentLog, IncidentLog.loadIncidentLog(rideId, startTime, endTime, this)), startTime, endTime);
        } else {
            this.dataLog = DataLog.loadDataLog(rideId, this);
            this.incidentLog = IncidentLog.loadIncidentLog(rideId, this);
        }
        if (!this.incidentLog.hasAutoGeneratedIncidents() && calculateEvents) {
            List<IncidentLogEntry> autoGeneratedIncidents = Utils.findAccEvents(rideId, this);
            for (IncidentLogEntry autoGeneratedIncident : autoGeneratedIncidents) {
                this.incidentLog.updateOrAddIncident(autoGeneratedIncident);
            }
        }

        if (route != null) {
            binding.showRouteMap.getOverlayManager().remove(route);
        }
        route = this.dataLog.rideAnalysisData.route;
        route.setWidth(8f);
        route.getPaint().setStrokeCap(Paint.Cap.ROUND);
        binding.showRouteMap.getOverlayManager().add(route);


        // Get a bounding box of the route so the view can be moved to it and the zoom can be
        // set accordingly
        runOnUiThread(() -> {
            bBox = getBoundingBox(route);
            zoomToBBox(bBox);
            binding.showRouteMap.invalidate();
        });

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // (3): CenterMap
        binding.boundingBoxCenterButton.setOnClickListener(v -> {
            zoomToBBox(bBox);
        });


        // Create an instance of MarkerFunct-class which provides all functionality related to
        // incident markers
        Log.d(TAG, "creating MarkerFunct object");
        runOnUiThread(() -> {
            if (myMarkerFunct != null) {
                myMarkerFunct.deleteAllMarkers();
            }
            myMarkerFunct = new MarkerFunct(ShowRouteActivity.this, dataLog, incidentLog);
            myMarkerFunct.updateIncidentMarkers(this.incidentLog);

            myMarkerFunct.updateOBSMarkers(this.incidentLog, ShowRouteActivity.this);
        });

        addCustomMarkerMode = false;

        Log.d(TAG, "setting up mReceive");

        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {

                if (addCustomMarkerMode) {
                    // Call custom marker adding functionality which enables the user to put
                    // markers on the map

                    myMarkerFunct.addCustomMarker(p);
                    binding.exitAddIncidentModeButton.performClick();
                    return true;
                } else {
                    InfoWindow.closeAllInfoWindowsOn(binding.showRouteMap);
                    return false;
                }

            }

            @Override
            public boolean longPressHelper(GeoPoint p) {

                if (addCustomMarkerMode) {
                    // Call custom marker adding functionality which enables the user to put
                    // markers on the map
                    myMarkerFunct.addCustomMarker(p);
                    binding.exitAddIncidentModeButton.performClick();
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
            Drawable startFlag = ShowRouteActivity.this.getResources().getDrawable(R.drawable.startblack, null);
            Drawable finishFlag = ShowRouteActivity.this.getResources().getDrawable(R.drawable.racingflagblack, null);
            GeoPoint startFlagPoint = dataLog.rideAnalysisData.route.getPoints().get(0);
            GeoPoint finishFlagPoint = dataLog.rideAnalysisData.route.getPoints().get(dataLog.rideAnalysisData.route.getPoints().size() - 1);

            startFlagOverlay = new IconOverlay(startFlagPoint, startFlag);
            finishFlagOverlay = new IconOverlay(finishFlagPoint, finishFlag);
            binding.showRouteMap.getOverlays().add(startFlagOverlay);
            binding.showRouteMap.getOverlays().add(finishFlagOverlay);
        });

        binding.saveRouteButton.setOnClickListener((View v) -> saveChanges());

        overlayEvents = new MapEventsOverlay(getBaseContext(), mReceive);
        runOnUiThread(() -> {
            binding.showRouteMap.getOverlays().add(overlayEvents);
            binding.showRouteMap.invalidate();
        });

    }

    private void saveChanges() {
        // Save incidents
        IncidentLog.saveIncidentLog(incidentLog, this);
        // Save new Route
        DataLog.saveDataLog(dataLog, this);
        // Update MetaData
        MetaDataEntry metaDataEntry = MetaData.getMetaDataEntryForRide(rideId, this);
        metaDataEntry.startTime = dataLog.startTime;
        metaDataEntry.endTime = dataLog.endTime;
        metaDataEntry.distance = dataLog.rideAnalysisData.distance;
        metaDataEntry.waitedTime = dataLog.rideAnalysisData.waitedTime;
        metaDataEntry.numberOfIncidents = incidentLog.getIncidents().size();
        metaDataEntry.numberOfScaryIncidents = IncidentLog.getScaryIncidents(incidentLog).size();
        metaDataEntry.region = lookUpIntSharedPrefs("Region", 0, "Profile", this);
        metaDataEntry.state = MetaData.STATE.ANNOTATED;
        MetaData.updateOrAddMetaDataEntryForRide(metaDataEntry, this);

        Toast.makeText(this, getString(R.string.savedRide), Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == IncidentPopUpActivity.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                IncidentLogEntry incidentLogEntry = (IncidentLogEntry) intent.getSerializableExtra(IncidentPopUpActivity.EXTRA_INCIDENT);
                incidentLog.updateOrAddIncident(incidentLogEntry);
                myMarkerFunct.setMarker(incidentLogEntry);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

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

    /**
     * Zoom automatically to the bounding box.
     *
     * @param bBox
     */
    public void zoomToBBox(BoundingBox bBox) {
        // Usually the command in the if body should suffice
        // but osmdroid is buggy and we need the else part to fix it.
        MapView mMapView = binding.showRouteMap;
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
        AlertDialog alertDialog = builder.create();
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
        alertDialog.show();
    }

    private class RideUpdateTask extends AsyncTask {

        private boolean updateBoundaries;
        private boolean calculateEvents;

        private RideUpdateTask(boolean updateBoundaries, boolean calculateEvents) {
            this.updateBoundaries = updateBoundaries;
            this.calculateEvents = calculateEvents;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            Log.d(TAG, "doInBackground()");
            refreshRoute(rideId, updateBoundaries, calculateEvents);
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            binding.loadingAnimationLayout.setVisibility(View.VISIBLE);

        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            binding.loadingAnimationLayout.setVisibility(View.GONE);
            if (updateBoundaries) {
                InfoWindow.closeAllInfoWindowsOn(binding.showRouteMap);
            }
        }
    }

    private class LoadOriginalDataLogTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            originalDataLog = DataLog.loadDataLog(rideId, ShowRouteActivity.this);
            Polyline originalRoute = originalDataLog.rideAnalysisData.route;
            incidentLog = IncidentLog.loadIncidentLog(rideId, ShowRouteActivity.this);
            if (editableRoute != null) {
                binding.showRouteMap.getOverlayManager().remove(editableRoute);
            }
            editableRoute = new Polyline();
            editableRoute.setPoints(originalRoute.getPoints());
            editableRoute.setWidth(40.0f);
            editableRoute.getPaint().setColor(getColor(R.color.colorPrimaryDark));
            editableRoute.getPaint().setStrokeCap(Paint.Cap.ROUND);
            binding.showRouteMap.getOverlayManager().add(editableRoute);
            runOnUiThread(() -> {
                binding.routePrivacySlider.setValues(0F, (float) originalRoute.getPoints().size());
                binding.routePrivacySlider.setValueTo(originalRoute.getPoints().size());
                binding.routePrivacySlider.setValueFrom(0F);
            });
            return null;
        }
    }
}