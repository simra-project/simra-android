package de.tuberlin.mcc.simra.app.activities;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.databinding.ActivityMainBinding;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.entities.MetaData;
import de.tuberlin.mcc.simra.app.entities.Profile;
import de.tuberlin.mcc.simra.app.services.OBSService;
import de.tuberlin.mcc.simra.app.services.RecorderService;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import de.tuberlin.mcc.simra.app.util.IOUtils;
import de.tuberlin.mcc.simra.app.util.IncidentBroadcaster;
import de.tuberlin.mcc.simra.app.util.PermissionHelper;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import de.tuberlin.mcc.simra.app.util.UpdateHelper;
import de.tuberlin.mcc.simra.app.util.Utils;

import static de.tuberlin.mcc.simra.app.activities.ProfileActivity.startProfileActivityForChooseRegion;
import static de.tuberlin.mcc.simra.app.entities.Profile.profileIsInUnknownRegion;
import static de.tuberlin.mcc.simra.app.util.Constants.ZOOM_LEVEL;
import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpIntSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SimRAuthenticator.getClientHash;
import static de.tuberlin.mcc.simra.app.util.Utils.getCorrectRegionName;
import static de.tuberlin.mcc.simra.app.util.Utils.getNews;
import static de.tuberlin.mcc.simra.app.util.Utils.getRegions;
import static de.tuberlin.mcc.simra.app.util.Utils.isLocationServiceOff;
import static de.tuberlin.mcc.simra.app.util.Utils.overwriteFile;
import static de.tuberlin.mcc.simra.app.util.Utils.regionEncoder;
import static de.tuberlin.mcc.simra.app.util.Utils.regionsDecoder;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, LocationListener {

    private static final String TAG = "MainActivity_LOG";
    private final static int REQUEST_ENABLE_BT = 1;

    public static ExecutorService myEx;
    ActivityMainBinding binding;
    Intent recService;
    RecorderService mBoundRecorderService;
    boolean obsEnabled = false;
    BroadcastReceiver receiver;
    private MapView mMapView;
    private MapController mMapController;
    private MyLocationNewOverlay mLocationOverlay;
    private LocationManager locationManager;
    private Boolean recording = false;

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // ServiceConnection for communicating with RecorderService
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private final ServiceConnection mRecorderServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RecorderService.MyBinder myBinder = (RecorderService.MyBinder) service;
            mBoundRecorderService = myBinder.getService();
        }
    };


    private void showOBSNotConnectedWarning() {
        android.app.AlertDialog.Builder alert = new android.app.AlertDialog.Builder(this);
        alert.setTitle(R.string.not_connected_warnung_title);
        alert.setMessage(R.string.not_connected_warnung_message);
        alert.setPositiveButton(R.string.yes, (dialog, whichButton) -> {
            startRecording();
        });
        alert.setNegativeButton(R.string.cancel_button, (dialog, whichButton) -> {
            startActivity(new Intent(this, OpenBikeSensorActivity.class));
        });
        alert.show();
    }

    private void showBluetoothNotEnableWarning() {
        android.app.AlertDialog.Builder alert = new android.app.AlertDialog.Builder(this);
        alert.setTitle(R.string.bluetooth_not_enable_title);
        alert.setMessage(R.string.bluetooth_not_enable_message);
        alert.setPositiveButton(R.string.yes, (dialog, whichButton) -> {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        });
        alert.setNegativeButton(R.string.no, (dialog, whichButton) -> {
            deactivateOBS();
        });
        alert.show();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth was enabled
                startOBSService();
            } else if (resultCode == RESULT_CANCELED) {
                // Bluetooth was not enabled
                deactivateOBS();
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UpdateHelper.checkForUpdates(this);

        binding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        myEx = Executors.newFixedThreadPool(4);

        // Context of application environment
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(getPackageName());


        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Prepare RecorderService for accelerometer and location data recording
        recService = new Intent(this, RecorderService.class);
        // set up location manager to get location updates
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Map configuration
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        mMapView = binding.appBarMain.mainContent.map;
        mMapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mMapView.setMultiTouchControls(true); // gesture zooming
        mMapView.setFlingEnabled(true);
        mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(ZOOM_LEVEL);
        binding.appBarMain.copyrightText.setMovementMethod(LinkMovementMethod.getInstance());

        // Set compass (from OSMdroid sample project:
        // https://github.com/osmdroid/osmdroid/blob/master/OpenStreetMapViewer/src/main/
        // java/org/osmdroid/samplefragments/location/SampleFollowMe.java)
        CompassOverlay mCompassOverlay = new CompassOverlay(ctx, new InternalCompassOrientationProvider(ctx), mMapView);

        // Sets the icon to device location.
        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mMapView);

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // MyLocationONewOverlayParameters.
        // --> enableMyLocation: Enable receiving location updates from the provided
        // IMyLocationProvider and show your location on the maps.
        // --> enableFollowLocation: Enables "follow" functionality.
        // --> setEnableAutoStop: if true, when the user pans the map, follow my
        // location will
        // automatically disable if false, when the user pans the map,
        // the map will continue to follow current location
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        mLocationOverlay.enableMyLocation();

        // If app has been used before and therefore a last known location is available
        // in sharedPrefs,
        // animate the map to that location.
        // Move map to last location known by locationManager if app is started for the
        // first time.
        SharedPreferences sharedPrefs = getSharedPreferences("simraPrefs", Context.MODE_PRIVATE);
        if (sharedPrefs.contains("lastLoc_latitude") & sharedPrefs.contains("lastLoc_longitude")) {
            GeoPoint lastLoc = new GeoPoint(Double.parseDouble(sharedPrefs.getString("lastLoc_latitude", "")),
                    Double.parseDouble(sharedPrefs.getString("lastLoc_longitude", "")));
            mMapController.animateTo(lastLoc);
        } else {
            try {
                mMapController.animateTo(new GeoPoint(mLocationOverlay.getLastFix().getLatitude(),
                        mLocationOverlay.getLastFix().getLongitude()));
            } catch (RuntimeException re) {
                Log.d(TAG, re.getMessage());
            }
        }

        mMapController.animateTo(mLocationOverlay.getMyLocation());

        // the map will follow the user until the user scrolls in the UI
        mLocationOverlay.enableFollowLocation();

        // scales tiles to dpi of current display
        mMapView.setTilesScaledToDpi(true);

        // Add overlays
        mMapView.getOverlays().add(this.mLocationOverlay);
        mMapView.getOverlays().add(mCompassOverlay);
        // mMapView.getOverlays().add(this.mRotationGestureOverlay);

        mLocationOverlay.setOptionsMenuEnabled(true);
        mCompassOverlay.enableCompass();

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // (1): Toolbar
        setSupportActionBar(binding.appBarMain.toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, binding.appBarMain.toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // CenterMap
        ImageButton centerMap = findViewById(R.id.center_button);
        centerMap.setOnClickListener(v -> {
            Log.d(TAG, "centerMap clicked ");
            mLocationOverlay.enableFollowLocation();
            mMapController.setZoom(ZOOM_LEVEL);
        });

        binding.appBarMain.buttonStartRecording.setOnClickListener(v -> {
            if (obsEnabled) {
                OBSService.ConnectionState currentState = OBSService.getConnectionState();
                if (!currentState.equals(OBSService.ConnectionState.CONNECTED)) {
                    boolean reconnect = OBSService.tryConnectPairedDevice(this);
                    if (!reconnect) {
                        showOBSNotConnectedWarning();
                        return;
                    }
                }
            }
            startRecording();
        });

        Consumer<Integer> recordIncident = (incidentType) -> {
            Toast t = Toast.makeText(MainActivity.this, R.string.recorded_incident, Toast.LENGTH_SHORT);
            t.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 230);
            t.show();

            IncidentBroadcaster.broadcastIncident(MainActivity.this, incidentType);
        };

        this.<MaterialButton>findViewById(R.id.report_closepass_incident).setOnClickListener(v -> {
            recordIncident.accept(IncidentLogEntry.INCIDENT_TYPE.CLOSE_PASS);
        });

        this.<MaterialButton>findViewById(R.id.report_obstacle_incident).setOnClickListener(v -> {
            recordIncident.accept(IncidentLogEntry.INCIDENT_TYPE.OBSTACLE);
        });

        binding.appBarMain.buttonStopRecording.setOnClickListener(v -> {
            try {
                displayButtonsForMenu();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                // Stop RecorderService which is recording accelerometer data
                unbindService(mRecorderServiceConnection);
                stopService(recService);
                recording = false;
                if (mBoundRecorderService.getRecordingAllowed()) {
                    ShowRouteActivity.startShowRouteActivity(mBoundRecorderService.getCurrentRideKey(),
                            MetaData.STATE.JUST_RECORDED, this);
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage(getString(R.string.errorRideNotRecorded))
                            .setCancelable(false)
                            .setPositiveButton("OK", (dialog, which) -> {
                            })
                            .create()
                            .show();
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception: " + e.getLocalizedMessage() + e.getMessage() + e.toString());
            }
        });
        // download the newest region list from the backend and prompt user to go to "Profile" and set region, if a new region has been added and the region is set as UNKNOWN or other.
        new RegionTask().execute();
        new NewsTask().execute();

        // OpenBikeSensor
        binding.appBarMain.buttonRideSettingsObs.setOnClickListener(view -> startActivity(new Intent(this, OpenBikeSensorActivity.class)));

        obsEnabled = SharedPref.Settings.OpenBikeSensor.isEnabled(this);
        updateOBSButtonStatus(OBSService.ConnectionState.DISCONNECTED);
        if (obsEnabled) {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                // Device does not support Bluetooth
                deactivateOBS();
                Toast.makeText(MainActivity.this, R.string.openbikesensor_bluetooth_incompatible, Toast.LENGTH_LONG)
                        .show();
            } else if (!mBluetoothAdapter.isEnabled() && obsEnabled) {
                // Bluetooth is disabled
                showBluetoothNotEnableWarning();
            } else {
                // Bluetooth is enabled
                startOBSService();
            }
        }
    }

    private boolean numberOfRegionsHasIncreased() {
        int lastNumberOfRegions = getRegions(MainActivity.this).length;
        int actualNumberOfRegions = SharedPref.App.Regions.getLastRegionNumberKnown(MainActivity.this);
        SharedPref.App.Regions.setLastRegionNumberKnown(lastNumberOfRegions,MainActivity.this);
        return actualNumberOfRegions < lastNumberOfRegions;
    }

    private void deactivateOBS() {
        obsEnabled = false;
        updateOBSButtonStatus(OBSService.ConnectionState.DISCONNECTED);
        SharedPref.Settings.OpenBikeSensor.setEnabled(false, this);
    }

    private void startOBSService() {
        OBSService.ConnectionState currentState = OBSService.getConnectionState();
        if (obsEnabled && currentState.equals(OBSService.ConnectionState.DISCONNECTED)) {
            OBSService.startScanning(this);
        }
        registerOBSService();
    }

    public void displayButtonsForMenu() {
        binding.appBarMain.buttonStartRecording.setVisibility(View.VISIBLE);
        binding.appBarMain.buttonStopRecording.setVisibility(View.INVISIBLE);

        binding.appBarMain.toolbar.setVisibility(View.VISIBLE);
        binding.appBarMain.reportIncidentContainer.setVisibility(View.GONE);

        //binding.appBarMain.buttonRideSettingsGeneral.setVisibility(View.VISIBLE);
        updateOBSButtonStatus(OBSService.getConnectionState());
    }

    public void displayButtonsForDrive() {
        binding.appBarMain.buttonStopRecording.setVisibility(View.VISIBLE);
        binding.appBarMain.buttonStartRecording.setVisibility(View.INVISIBLE);

        binding.appBarMain.toolbar.setVisibility(View.GONE);
        if (SharedPref.Settings.IncidentsButtonsDuringRide.getIncidentButtonsEnabled(this)) {
            binding.appBarMain.reportIncidentContainer.setVisibility(View.VISIBLE);
        }

        //binding.appBarMain.buttonRideSettingsGeneral.setVisibility(View.GONE);
        updateOBSButtonStatus(OBSService.getConnectionState());

    }

    private void registerOBSService() {
        receiver = OBSService.registerCallbacks(this, new OBSService.OBSServiceCallbacks() {
            public void onConnectionStateChanged(OBSService.ConnectionState newState) {
                updateOBSButtonStatus(newState);
            }

            public void onDeviceFound(String deviceName, String deviceId) {
                if (!OBSService.getConnectionState().equals(OBSService.ConnectionState.CONNECTED)) {
                    Toast.makeText(MainActivity.this, R.string.openbikesensor_toast_devicefound, Toast.LENGTH_LONG)
                            .show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        OBSService.terminateService(this);
        super.onDestroy();
    }

    private void unregisterOBSService() {
        OBSService.unRegisterCallbacks(receiver, this);
        receiver = null;
    }

    private void startRecording() {
        if (!PermissionHelper.hasBasePermissions(this)) {
            PermissionHelper.requestFirstBasePermissionsNotGranted(MainActivity.this);
            Toast.makeText(MainActivity.this, R.string.recording_not_started, Toast.LENGTH_LONG).show();
        } else {
            if (isLocationServiceOff(locationManager)) {
                // notify user
                new AlertDialog.Builder(MainActivity.this).setMessage((R.string.locationServiceisOff + " " + R.string.enableToRecord))
                        .setPositiveButton(android.R.string.ok,
                                (paramDialogInterface, paramInt) -> MainActivity.this
                                        .startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                        .setNegativeButton(R.string.cancel, null).show();
                Toast.makeText(MainActivity.this, R.string.recording_not_started, Toast.LENGTH_LONG).show();

            } else {
                // show stop button, hide start button
                displayButtonsForDrive();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                // start RecorderService for accelerometer data recording
                Intent intent = new Intent(MainActivity.this, RecorderService.class);
                startService(intent);
                bindService(intent, mRecorderServiceConnection, Context.BIND_IMPORTANT);
                recording = true;
                Toast.makeText(MainActivity.this, R.string.recording_started, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateOBSButtonStatus(OBSService.ConnectionState status) {
        FloatingActionButton obsButton = binding.appBarMain.buttonRideSettingsObs;
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (obsEnabled) {
            // einblenden
            navigationView.getMenu().findItem(R.id.nav_bluetooth_connection).setVisible(true);
            obsButton.setVisibility(View.VISIBLE);
        } else {
            // ausblenden
            navigationView.getMenu().findItem(R.id.nav_bluetooth_connection).setVisible(false);
            obsButton.setVisibility(View.GONE);
        }
        switch (status) {
            case DISCONNECTED:
                obsButton.setImageResource(R.drawable.ic_bluetooth_disabled);
                obsButton.setContentDescription(getString(R.string.obsNotConnected));
                obsButton.setColorFilter(Color.RED);
                break;
            case SEARCHING:
                obsButton.setImageResource(R.drawable.ic_bluetooth_searching);
                obsButton.setContentDescription(getString(R.string.obsSearching));
                obsButton.setColorFilter(Color.WHITE);
                break;
            case PAIRING:
                obsButton.setImageResource(R.drawable.ic_bluetooth_searching);
                obsButton.setContentDescription(getString(R.string.connecting));
                obsButton.setColorFilter(Color.WHITE);
                break;
            case CONNECTED:
                obsButton.setImageResource(R.drawable.ic_bluetooth_connected);
                obsButton.setContentDescription(getString(R.string.obsConnected));
                obsButton.setColorFilter(Color.GREEN);
                break;
            default:
                break;
        }
    }

    public void onResume() {
        UpdateHelper.checkForUpdates(this);
        obsEnabled = SharedPref.Settings.OpenBikeSensor.isEnabled(this);
        if (obsEnabled) {
            OBSService.tryConnectPairedDevice(this);
        }

        if (receiver == null && obsEnabled) {
            registerOBSService();
        }
        super.onResume();

        // Ensure the button that matches current state is presented.
        if (recording) {
            displayButtonsForDrive();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            displayButtonsForMenu();
        }

        // Load Configuration with changes from onCreate
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Configuration.getInstance().load(this, prefs);

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } catch (SecurityException se) {
            Log.d(TAG, "onStart() permission not granted yet");
        }

        // Refresh the osmdroid configuration on resuming.
        mMapView.onResume(); // needed for compass and icons
        mLocationOverlay.onResume();
        mLocationOverlay.enableMyLocation();
        updateOBSButtonStatus(OBSService.getConnectionState());

    }

    public void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Load Configuration with changes from onCreate
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Configuration.getInstance().save(this, prefs);

        // Refresh the osmdroid configuration on pausing.
        mMapView.onPause(); // needed for compass and icons
        locationManager.removeUpdates(MainActivity.this);
        mLocationOverlay.onPause();
        mLocationOverlay.disableMyLocation();
        Log.d(TAG, "OnPause finished");
        unregisterOBSService();
    }

    @SuppressLint("MissingPermission")
    public void onStop() {
        super.onStop();
        Log.d(TAG, "OnStop called");
        try {
            final Location myLocation = mLocationOverlay.getLastFix();
            if (myLocation != null) {
                SharedPreferences.Editor editor = getSharedPreferences("simraPrefs", Context.MODE_PRIVATE).edit();
                editor.putString("lastLoc_latitude", String.valueOf(myLocation.getLatitude()));
                editor.putString("lastLoc_longitude", String.valueOf(myLocation.getLongitude()));
                editor.apply();
            }

        } catch (Exception se) {
            se.printStackTrace();
        }
        Log.d(TAG, "OnStop finished");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Integer.parseInt(android.os.Build.VERSION.SDK) > 5 && keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Navigation Drawer
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (recording) {
                Intent setIntent = new Intent(Intent.ACTION_MAIN);
                setIntent.addCategory(Intent.CATEGORY_HOME);
                setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(setIntent);
            } else {
                super.onBackPressed();
            }
        }
    }

    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_history) {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_demographic_data) {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_statistics) {
            Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_aboutSimRa) {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_setting) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_tutorial) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(getString(R.string.link_to_tutorial)));
            startActivity(i);
        } else if (id == R.id.nav_feedback) {
            // src:
            // https://stackoverflow.com/questions/2197741/how-can-i-send-emails-from-my-android-application
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.feedbackReceiver)});
            i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedbackHeader));
            i.putExtra(Intent.EXTRA_TEXT, (getString(R.string.feedbackReceiver)) + System.lineSeparator()
                    + "App Version: " + BuildConfig.VERSION_CODE + System.lineSeparator() + "Android Version: ");
            try {
                startActivity(Intent.createChooser(i, "Send mail..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
            }

        } else if (id == R.id.nav_imprint) {
            Intent intent = new Intent(MainActivity.this, WebActivity.class);
            intent.putExtra("URL", getString(R.string.tuberlin_impressum));

            startActivity(intent);
        } else if (id == R.id.nav_twitter) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(getString(R.string.link_to_twitter)));
            startActivity(i);
        } else if (id == R.id.nav_bluetooth_connection) {
            Intent intent = new Intent(MainActivity.this, OpenBikeSensorActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    private void fireNewsPrompt() {
        Log.d(TAG, "fireWhatIsNewPrompt()");

        // get the news from the downloaded config
        String[] simRa_news_config = getNews(MainActivity.this);
        if(simRa_news_config.length <=1) {
            Log.e(TAG, "Empty simRa_new_config!");
            return;
        }

        // Store the created AlertDialog instance.
        // Because only AlertDialog has cancel method.
        AlertDialog alertDialog;
        // Create a alert dialog builder.
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        // Get news popup view.
        View newsView = getLayoutInflater().inflate(R.layout.news_popup, null);
        LinearLayout linearLayout = newsView.findViewById(R.id.news_blocks);
        for (int i = 1; i < simRa_news_config.length; i++) {
            // get TextView to be filled with news element
            TextView thisTextView = (TextView) linearLayout.getChildAt(i-1);
            // make TextView visible
            thisTextView.setVisibility(View.VISIBLE);
            // set text color to colorAccent, if the news element starts with a * instead of a -
            int textColor = getResources().getColor(R.color.colorPrimary,null);
            if(simRa_news_config[i].startsWith("*")) {
                textColor = getResources().getColor(R.color.colorAccent,null);
            }
            thisTextView.setTextColor(textColor);
            // set text of TextView to text of news element
            thisTextView.setText(simRa_news_config[i].substring(1));
        }

        // Set above view in alert dialog.
        builder.setView(newsView);

        builder.setTitle(getString(R.string.news));

        alertDialog = builder.create();

        Button okButton = newsView.findViewById(R.id.ok_button);

        int newsID = Integer.parseInt(simRa_news_config[0].substring(1));

        AlertDialog finalAlertDialog = alertDialog;
        okButton.setOnClickListener(v -> {
            SharedPref.App.News.setLastSeenNewsID(newsID,MainActivity.this);
            finalAlertDialog.cancel();
        });

        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(false);
        alertDialog.show();

    }

    public void fireProfileRegionPrompt() {
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setTitle(getString(R.string.chooseRegion));
        alert.setMessage(R.string.pleaseChooseRegion);
        alert.setNeutralButton(R.string.yes, (dialogInterface, j) -> {
            startProfileActivityForChooseRegion(MainActivity.this);
        });
        alert.setNegativeButton(R.string.later,null);
        alert.show();
    }

    private class RegionTask extends AsyncTask<String, String, String> {

        private RegionTask() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {

            StringBuilder checkRegionsResponse = new StringBuilder();
            int status = 0;
            try {

                URL url = new URL(
                        BuildConfig.API_ENDPOINT + "check/regions-coords?clientHash=" + getClientHash(MainActivity.this));
                Log.d(TAG, "URL: " + url.toString());
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    checkRegionsResponse.append(inputLine).append(System.lineSeparator());
                }
                in.close();
                status = urlConnection.getResponseCode();
                Log.d(TAG, "Server status: " + status);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "GET regions response: " + checkRegionsResponse.toString());
            if (status == 200) {
                File regionsFile = IOUtils.Files.getRegionsFile(MainActivity.this);
                overwriteFile(checkRegionsResponse.toString(), regionsFile);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            // prompt user to go to "Profile" and set region, if a new region has been added to SimRa and the region is set as UNKNOWN or other.
            if (numberOfRegionsHasIncreased() && profileIsInUnknownRegion(MainActivity.this)) {
                fireProfileRegionPrompt();
            }
        }
    }
    private class NewsTask extends AsyncTask<String, String, String> {
        int newsID = -1;
        private NewsTask() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {

            StringBuilder checkNewsResponseDE = new StringBuilder();
            StringBuilder checkNewsResponseEN = new StringBuilder();
            int statusDE = 0;
            int statusEN = 0;
            int lastSeenNewsID = SharedPref.App.News.getLastSeenNewsID(MainActivity.this);
            try {

                URL url_de = new URL(
                        BuildConfig.API_ENDPOINT + "check/news?clientHash=" + getClientHash(MainActivity.this) + "&lastSeenNewsID=" + lastSeenNewsID + "&newsLanguage=de");
                Log.d(TAG, "URL_DE: " + url_de.toString());
                URL url_en = new URL(
                        BuildConfig.API_ENDPOINT + "check/news?clientHash=" + getClientHash(MainActivity.this) + "&lastSeenNewsID=" + lastSeenNewsID + "&newsLanguage=en");
                Log.d(TAG, "URL_EN: " + url_en.toString());
                HttpsURLConnection url_de_Connection = (HttpsURLConnection) url_de.openConnection();
                HttpsURLConnection url_en_Connection = (HttpsURLConnection) url_en.openConnection();
                url_de_Connection.setRequestMethod("GET");
                url_en_Connection.setRequestMethod("GET");
                url_de_Connection.setReadTimeout(10000);
                url_en_Connection.setReadTimeout(10000);
                url_de_Connection.setConnectTimeout(15000);
                url_en_Connection.setConnectTimeout(15000);
                BufferedReader in_de = new BufferedReader(new InputStreamReader(url_de_Connection.getInputStream()));
                BufferedReader in_en = new BufferedReader(new InputStreamReader(url_en_Connection.getInputStream()));

                String inputLine_de;
                String inputLine_en;

                while ((inputLine_de = in_de.readLine()) != null) {
                    if (newsID == -1 && inputLine_de.startsWith("#")){
                        newsID = Integer.parseInt(inputLine_de.replace("#",""));
                    }
                    checkNewsResponseDE.append(inputLine_de).append(System.lineSeparator());
                }
                in_de.close();
                while ((inputLine_en = in_en.readLine()) != null) {
                    checkNewsResponseEN.append(inputLine_en).append(System.lineSeparator());
                }
                in_en.close();
                statusDE = url_de_Connection.getResponseCode();
                statusEN = url_en_Connection.getResponseCode();

                Log.d(TAG, "Server statusDE: " + statusDE + " statusEN: " + statusEN);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "GET news DE response: " + checkNewsResponseDE.toString());
            Log.d(TAG, "GET news EN response: " + checkNewsResponseEN.toString());
            if (statusDE == 200 && checkNewsResponseDE.length() > 0) {
                File newsFile = IOUtils.Files.getDENewsFile(MainActivity.this);
                overwriteFile(checkNewsResponseDE.toString(), newsFile);
            }
            if (statusEN == 200 && checkNewsResponseDE.length() > 0) {
                File newsFile = IOUtils.Files.getENNewsFile(MainActivity.this);
                overwriteFile(checkNewsResponseEN.toString(), newsFile);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(SharedPref.App.News.getLastSeenNewsID(MainActivity.this) < newsID) {
                fireNewsPrompt();
            }
        }
    }

}
