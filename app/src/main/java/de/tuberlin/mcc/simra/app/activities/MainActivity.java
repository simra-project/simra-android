package de.tuberlin.mcc.simra.app.activities;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.HttpsURLConnection;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.util.Consumer;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.databinding.ActivityMainBinding;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.entities.MetaData;
import de.tuberlin.mcc.simra.app.entities.Profile;
import de.tuberlin.mcc.simra.app.util.ConnectionManager.BLESTATE;
import de.tuberlin.mcc.simra.app.services.RecorderService;
import de.tuberlin.mcc.simra.app.util.ConnectionManager;
import de.tuberlin.mcc.simra.app.util.IOUtils;
import de.tuberlin.mcc.simra.app.util.IncidentBroadcaster;
import de.tuberlin.mcc.simra.app.util.PermissionHelper;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import de.tuberlin.mcc.simra.app.util.ble.ConnectionEventListener;

import static de.tuberlin.mcc.simra.app.entities.Profile.profileIsInUnknownRegion;
import static de.tuberlin.mcc.simra.app.update.VersionUpdater.Legacy.Utils.getAppVersionNumber;
import static de.tuberlin.mcc.simra.app.util.Constants.ZOOM_LEVEL;
import static de.tuberlin.mcc.simra.app.util.PermissionHelper.hasBLEPermissions;
import static de.tuberlin.mcc.simra.app.util.PermissionHelper.requestBlePermissions;
import static de.tuberlin.mcc.simra.app.util.SimRAuthenticator.getClientHash;
import static de.tuberlin.mcc.simra.app.util.Utils.activityResultLauncher;
import static de.tuberlin.mcc.simra.app.util.Utils.fireProfileRegionPrompt;
import static de.tuberlin.mcc.simra.app.util.Utils.getNews;
import static de.tuberlin.mcc.simra.app.util.Utils.isLocationServiceOff;
import static de.tuberlin.mcc.simra.app.util.Utils.nearestRegionsToThisLocation;
import static de.tuberlin.mcc.simra.app.util.Utils.overwriteFile;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, LocationListener, ActivityCompat.OnRequestPermissionsResultCallback{

    private static final String TAG = "MainActivity_LOG";
    private final static int REQUEST_ENABLE_BT = 1;

    public static ExecutorService myEx;
    ActivityMainBinding binding;
    Intent recService;
    RecorderService mBoundRecorderService;
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
    boolean obsEnabled = false;
    private MapView mMapView;
    private MapController mMapController;
    private MyLocationNewOverlay mLocationOverlay;
    private LocationManager locationManager;
    private Boolean recording = false;
    private ConnectionEventListener connectionEventListener = null;
    private int nRetries = 0; // number of OBS connection retries
    private boolean showingOBSWarning = false;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    /**
     * Prompts user to start recording or open the OBS settings.
     * Gets called, when OBS is enabled in the settings and user tries to star recording but there
     * is not a connection to an OBS yet.
     */
    private void showOBSNotConnectedRecordingWarning() {
        android.app.AlertDialog.Builder alert = new android.app.AlertDialog.Builder(this);
        alert.setTitle(R.string.not_connected_warning_title);
        alert.setMessage(R.string.not_connected_recording_warning_message);
        alert.setPositiveButton(R.string.yes, (dialog, whichButton) -> {
            startRecording();
        });
        alert.setNegativeButton(R.string.no_open_settings, (dialog, whichButton) -> {
            startActivity(new Intent(this, OpenBikeSensorActivity.class));
        });
        alert.show();
    }

    /**
     * Prompts user to open OBS settings or deactivate OBS.
     * Gets called, when a connection to an OBS fails three times in a row.
     */
    private void showOBSNotConnectedWarning() {
        if (showingOBSWarning) {
            return;
        }
        showingOBSWarning = true;
        android.app.AlertDialog.Builder alert = new android.app.AlertDialog.Builder(this);
        alert.setTitle(R.string.could_not_connect_warning_title);
        alert.setMessage(R.string.could_not_connect_warning_message);
        alert.setPositiveButton(R.string.open_settings, (dialog, whichButton) -> {
            showingOBSWarning = false;
            startActivity(new Intent(this, OpenBikeSensorActivity.class));
        });
        alert.setNegativeButton(R.string.disable_obs, (dialog, whichButton) -> {
            showingOBSWarning = false;
            deactivateOBS();
        });
        alert.setOnDismissListener(dialog -> showingOBSWarning = false);
        alert.show();
    }

    /**
     * Prompts user to enable Bluetooth or deactivate OBS.
     * Gets called, if OBS is enabled in the settings but Bluetooth is disabled, so SimRa cannot
     * connect to OBS.
     */
    private void showBluetoothNotEnableWarning() {
        android.app.AlertDialog.Builder alert = new android.app.AlertDialog.Builder(this);
        alert.setTitle(R.string.bluetooth_not_enable_title);
        alert.setMessage(R.string.bluetooth_not_enable_message);
        alert.setPositiveButton(R.string.yes, (dialog, whichButton) -> {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activityResultLauncher.launch(enableBtIntent);
        });
        alert.setNegativeButton(R.string.no, (dialog, whichButton) -> {
            deactivateOBS();
        });
        alert.show();
    }


    /**
     * Gets called if user responds to the Bluetooth alert. Starts obs connection or disables obs.
     */
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth was enabled
                if (obsEnabled) {
                    new Thread(this::tryConnectToOBS).start();
                }
            } else if (resultCode == RESULT_CANCELED) {
                // Bluetooth was not enabled
                deactivateOBS();
            }
        }
    }

    // OBS-Lite
    UsbManager usbManager;
    SerialInputOutputManager usbIoManager;
    LinkedList<Byte> byteLinkedList = new LinkedList<>();
    LinkedList<LinkedList<Byte>> splittedByteLinkedList = new LinkedList<>();

    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "intent: " + intent);
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            Log.d(TAG, "device: " + device);
                            UsbInterface intf = device.getInterface(0);
                            Log.d(TAG, "intf: " + intf);
                            UsbEndpoint endpoint = intf.getEndpoint(0);
                            Log.d(TAG, "endpoint: " + endpoint);
                            UsbDeviceConnection connection = usbManager.openDevice(device);
                            Log.d(TAG, "connection: " + connection);
                            connection.claimInterface(intf, true);
                            byte[] bytes = new byte[0];
                            connection.bulkTransfer(endpoint,bytes,119500,6000);
                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
            mLocationOverlay.enableFollowLocation();
            mMapController.setZoom(ZOOM_LEVEL);
        });

        binding.appBarMain.buttonStartRecording.setOnClickListener(v -> {
            if (obsEnabled) {
                if (ConnectionManager.INSTANCE.getBleState() == BLESTATE.DISCONNECTED) { // if not connected to OBS, try to connect
                    showOBSNotConnectedRecordingWarning(); // try one reconnect and show warning if that one fails too
                    return;
                }
            }
            startRecording();
        });

        Consumer<Integer> recordIncident = incidentType -> {
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
                if (mBoundRecorderService.hasRecordedEnough()) {
                    ShowRouteActivity.startShowRouteActivity(mBoundRecorderService.getCurrentRideKey(),
                            MetaData.STATE.JUST_RECORDED, true, this);
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
                Log.e(TAG, "Exception: " + e.getMessage());
                Log.e(TAG, Arrays.toString(e.getStackTrace()));            }
        });

        new CheckVersionTask().execute();


        // OpenBikeSensor
        activityResultLauncher = activityResultLauncher(MainActivity.this);
        binding.appBarMain.buttonRideSettingsObs.setOnClickListener(view -> startActivity(new Intent(this, OpenBikeSensorActivity.class)));


        // OBS Lite
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());
        if (connection == null) {
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.registerReceiver(usbReceiver,filter,RECEIVER_EXPORTED);
            } else {
                this.registerReceiver(usbReceiver,filter);
            }
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
            usbManager.requestPermission(driver.getDevice(),permissionIntent);
        }

    }

    /**
     * Runnable to connect to OBS.
     */
    FutureTask<Boolean> obsFT;


    public class OBSTryConnectRunnable implements Runnable {
        public void run() {
            if (connectionEventListener == null) {
                // Log.d(TAG, "creating connectionEventListener");
                connectionEventListener = new ConnectionEventListener();
                connectionEventListener.setOnScanStart(isSearching -> {
                    Log.d(TAG, "isSearching: " + isSearching);
                    // updateOBSButtonStatus(BLESTATE.SEARCHING);
                    updateOBSButtonStatus();
                    return null;
                });
                connectionEventListener.setOnScanStop(foundOBS -> {
                    // Log.d(TAG, "foundOBS: " + foundOBS);
                    // updateOBSButtonStatus(BLESTATE.DISCONNECTED);
                    updateOBSButtonStatus();
                    return null;
                });
                connectionEventListener.setOnConnectionFailed(bluetoothDevice -> {
                    Log.e(TAG,"Connecting to " + bluetoothDevice.getName() + " failed!");
                    obsFT.run();
                    return null;
                });
                connectionEventListener.setOnDeviceFound(bluetoothDevice -> {
                    // Log.d(TAG, "Device found: " + bluetoothDevice.getName());
                    // updateOBSButtonStatus(BLESTATE.FOUND);
                    updateOBSButtonStatus();
                    List<UUID> characteristicsToSubscribeTo = List.of(ConnectionManager.INSTANCE.getCLOSE_PASS_CHARACTERISTIC_UUID(),ConnectionManager.INSTANCE.getSENSOR_DISTANCE_CHARACTERISTIC_UUID());
                    ConnectionManager.INSTANCE.connect(characteristicsToSubscribeTo,MainActivity.this);
                    return null;
                });
                connectionEventListener.setOnConnectionSetupComplete(bluetoothGatt -> {
                    // Log.d(TAG, "Connection setup complete.");
                    // updateOBSButtonStatus(BLESTATE.CONNECTED);
                    updateOBSButtonStatus();
                    obsFT.run();
                    return null;
                });
                connectionEventListener.setOnClosePassNotification(measurement -> {
                    // Log.d(TAG, "Close Pass! Time: " + measurement.getObsTime() + " left: " + measurement.getLeftDistance() + " right: " + measurement.getRightDistance());
                    return null;
                });
                connectionEventListener.setOnTimeRead(time -> {
                    // Log.d(TAG, "Time: " + time);
                    return null;
                });
                connectionEventListener.setOnDisconnect(bluetoothDevice -> {
                    // Log.d(TAG, "Disconnected from " + bluetoothDevice.getName());
                    // updateOBSButtonStatus(BLESTATE.DISCONNECTED);
                    updateOBSButtonStatus();
                    return null;
                });
            }

            ConnectionManager.INSTANCE.registerListener(connectionEventListener);
            if (ConnectionManager.INSTANCE.getBleState() == BLESTATE.SEARCHING) {
                ConnectionManager.INSTANCE.stopScan();
                /*if (ConnectionManager.INSTANCE.isConnected()) {
                    // updateOBSButtonStatus(BLESTATE.CONNECTED);
                }*/
            }
            if (ConnectionManager.INSTANCE.getBleState() == BLESTATE.DISCONNECTED) {
                ConnectionManager.INSTANCE.startScan(MainActivity.this);
                // updateOBSButtonStatus(BLESTATE.SEARCHING);
            }
            updateOBSButtonStatus();
        }
    }



    private void tryConnectToOBS() {
        if(hasBLEPermissions(MainActivity.this)) {
            Log.d(TAG, "has BLE Permissions");
            if (!obsEnabled) {
                Log.e(TAG, "OBS is not enabled (anymore)");
                return;
            }
            if (ConnectionManager.INSTANCE.getBleState() == BLESTATE.CONNECTED && connectionEventListener != null) {
                Log.e(TAG, "Already connected to OBS");
                return;
            }
            obsFT = new FutureTask<>(() -> {}, null);
            OBSTryConnectRunnable runnable = new OBSTryConnectRunnable();
            // Log.d(TAG, "running tryConnectToOBS");
            runOnUiThread(runnable);
            int MAX_NUMBER_OF_OBS_CONNECTION_RETRIES = 3;
            try {
                obsFT.get(10, TimeUnit.SECONDS); // this will block 10 seconds until Runnable completes
                if (ConnectionManager.INSTANCE.getBleState() == BLESTATE.DISCONNECTED) {
                    if (nRetries <= MAX_NUMBER_OF_OBS_CONNECTION_RETRIES) {
                        nRetries++;
                        tryConnectToOBS();
                    } else {
                        runOnUiThread(this::showOBSNotConnectedWarning);
                    }
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                Log.e(TAG, "Exception: " + e.getMessage());
                Log.e(TAG, Arrays.toString(e.getStackTrace()));
                e.printStackTrace();
                // Log.d(TAG, "exception while connecting to OBS, trying again");
                if (nRetries <= MAX_NUMBER_OF_OBS_CONNECTION_RETRIES) {
                    nRetries++;
                    tryConnectToOBS();
                } else {
                    runOnUiThread(this::showOBSNotConnectedWarning);
                }
            }
        } else {
            Log.d(TAG, "has not BLE permissions. Requesting...");
            requestBlePermissions(MainActivity.this, REQUEST_ENABLE_BT);
        }
    }



    /**
     * Deactivates the OBS Settings, so that in future SimRa won't try to connect to OBS automatically
     */
    private void deactivateOBS() {
        obsEnabled = false;
        binding.appBarMain.buttonRideSettingsObs.setVisibility(View.GONE);
        SharedPref.Settings.OpenBikeSensor.setEnabled(false, this);
    }


    public void displayButtonsForMenu() {
        binding.appBarMain.buttonStartRecording.setVisibility(View.VISIBLE);
        binding.appBarMain.buttonStopRecording.setVisibility(View.INVISIBLE);

        binding.appBarMain.toolbar.setVisibility(View.VISIBLE);
        binding.appBarMain.reportIncidentContainer.setVisibility(View.GONE);

        updateOBSButtonStatus();
    }

    public void displayButtonsForDrive() {
        binding.appBarMain.buttonStopRecording.setVisibility(View.VISIBLE);
        binding.appBarMain.buttonStartRecording.setVisibility(View.INVISIBLE);

        binding.appBarMain.toolbar.setVisibility(View.GONE);
        if (SharedPref.Settings.IncidentsButtonsDuringRide.getIncidentButtonsEnabled(this)) {
            binding.appBarMain.reportIncidentContainer.setVisibility(View.VISIBLE);
        }
        binding.appBarMain.buttonRideSettingsObs.setVisibility(View.GONE);
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

    private void updateOBSButtonStatus() {
        FloatingActionButton obsButton = binding.appBarMain.buttonRideSettingsObs;
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (obsEnabled) {
            // enable Bluetooth button
            navigationView.getMenu().findItem(R.id.nav_bluetooth_connection).setVisible(true);
            obsButton.setVisibility(View.VISIBLE);
        } else {
            // disable Bluetooth button
            navigationView.getMenu().findItem(R.id.nav_bluetooth_connection).setVisible(false);
            obsButton.setVisibility(View.GONE);
        }
        switch (ConnectionManager.INSTANCE.getBleState()) {
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
            case FOUND:
            case CONNECTING:
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
        super.onResume();

        // OpenBikeSensor
        obsEnabled = SharedPref.Settings.OpenBikeSensor.isEnabled(this);
        updateOBSButtonStatus();
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
            } else if(ConnectionManager.INSTANCE.getBleState() != BLESTATE.CONNECTED) {
                // Bluetooth is enabled
                Log.d(TAG, "tryConnectToOBS from 623");
                new Thread(this::tryConnectToOBS).start();
            }
        }
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
            Log.e(TAG, "onStart() permission not granted yet - Exception: " + se.getMessage());
            Log.e(TAG, Arrays.toString(se.getStackTrace()));
            Log.d(TAG, "onStart() permission not granted yet");
        }

        // Refresh the osmdroid configuration on resuming.
        mMapView.onResume(); // needed for compass and icons
        mLocationOverlay.onResume();
        mLocationOverlay.enableMyLocation();

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
        // Stop listening to OBS callbacks
        if (connectionEventListener != null) {
            ConnectionManager.INSTANCE.unregisterListener(connectionEventListener);
        }
    }

    @SuppressLint("MissingPermission")
    public void onStop() {
        super.onStop();
        try {
            final Location myLocation = mLocationOverlay.getLastFix();
            if (myLocation != null) {
                SharedPreferences.Editor editor = getSharedPreferences("simraPrefs", Context.MODE_PRIVATE).edit();
                editor.putString("lastLoc_latitude", String.valueOf(myLocation.getLatitude()));
                editor.putString("lastLoc_longitude", String.valueOf(myLocation.getLongitude()));
                editor.apply();
            }

        } catch (Exception se) {
            Log.e(TAG, "onStop() permission not granted yet - Exception: " + se.getMessage());
            Log.e(TAG, Arrays.toString(se.getStackTrace()));
            se.printStackTrace();
        }
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
        } else if (id == R.id.nav_dashboard) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(getString(R.string.link_simra_Dashboard)));
            startActivity(intent);
        } else if (id == R.id.nav_imprint) {
            Intent intent = new Intent(MainActivity.this, WebActivity.class);
            intent.putExtra("URL", getString(R.string.tuberlin_impressum));
            startActivity(intent);
        } else if (id == R.id.nav_contact) {
            Intent i = new Intent(MainActivity.this, ContactActivity.class);
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

    private void fireNewAppVersionPrompt(int installedAppVersion, int newestAppVersion, String urlToNewestAPK, Boolean critical) {
        Log.d(TAG, "fireNewAppVersionPrompt()");
        // Store the created AlertDialog instance.
        // Because only AlertDialog has cancel method.
        AlertDialog alertDialog = null;
        // Create a alert dialog builder.
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        // Get custom login form view.
        View settingsView = getLayoutInflater().inflate(R.layout.new_update_prompt, null);

        // Set above view in alert dialog.
        builder.setView(settingsView);

        builder.setTitle(getString(R.string.new_app_version_title));

        ((TextView) settingsView.findViewById(R.id.installed_version_textView)).setText(getString(R.string.installed_version) + " " + installedAppVersion);
        ((TextView) settingsView.findViewById(R.id.newest_version_textView)).setText(getString(R.string.newest_version) + " " + newestAppVersion);

        Button googlePlayStoreButton = settingsView.findViewById(R.id.google_play_store_button);

        googlePlayStoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
            }
        });

        Button apkButton = settingsView.findViewById(R.id.apk_button);

        apkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urlToNewestAPK)));
            }
        });

        alertDialog = builder.create();

        if (critical) {
            Button closeSimRaButton = settingsView.findViewById(R.id.close_simra_button);
            closeSimRaButton.setVisibility(View.VISIBLE);
            closeSimRaButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        } else {
            Button laterButton = settingsView.findViewById(R.id.later_button);
            laterButton.setVisibility(View.VISIBLE);
            AlertDialog finalAlertDialog = alertDialog;
            laterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finalAlertDialog.cancel();
                    new NewsTask().execute();
                }
            });
        }

        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(false);
        alertDialog.show();

    }

    private void fireNewsPrompt() {

        // get the news from the downloaded config
        String[] simRa_news_config = getNews(MainActivity.this);
        if (simRa_news_config.length <= 1) {
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
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(10, 10, 10, 10);
        for (int i = 1; i < simRa_news_config.length; i++) {
            if (simRa_news_config[i].startsWith("i")){
                continue;
            }
            TextView tv = new TextView(MainActivity.this);
            int textColor = getResources().getColor(R.color.colorPrimary, null);
            if (simRa_news_config[i].startsWith("*")) {
                textColor = getResources().getColor(R.color.colorAccent, null);
            }
            tv.setTextColor(textColor);
            // set text of TextView to text of news element
            tv.setText(simRa_news_config[i].substring(1));
            tv.setWidth(linearLayout.getWidth());
            linearLayout.addView(tv, i, layoutParams);
        }

        // Set above view in alert dialog.
        builder.setView(newsView);

        builder.setTitle(getString(R.string.news));

        alertDialog = builder.create();

        Button okButton = newsView.findViewById(R.id.ok_button);

        int newsID = Integer.parseInt(simRa_news_config[0].substring(1));

        AlertDialog finalAlertDialog = alertDialog;
        okButton.setOnClickListener(v -> {
            SharedPref.App.News.setLastSeenNewsID(newsID, MainActivity.this);
            finalAlertDialog.cancel();
            // download the newest region list from the backend and prompt user to go to "Profile" and set region, if a new region has been added and the region is set as UNKNOWN or other.
            new RegionTask().execute();
        });

        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(false);
        alertDialog.show();

    }

    private boolean actualSelectedRegionNotInTopThreeNearestRegion() {
        if (PermissionHelper.hasBasePermissions(MainActivity.this)) {
            @SuppressLint("MissingPermission")
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                return false;
            }
            int selectedRegion = Profile.loadProfile(null, MainActivity.this).region;
            int[] nearestRegions = nearestRegionsToThisLocation(location.getLatitude(), location.getLongitude(), MainActivity.this);
            for (int nearestRegion : nearestRegions) {
                if (nearestRegion == selectedRegion) {
                    return false;
                }
            }
        }
        return true;
    }

    private class CheckVersionTask extends AsyncTask<String, String, String> {
        int installedAppVersion = -1;
        int newestAppVersion = 0;
        String urlToNewestAPK = null;
        Boolean critical = null;

        private CheckVersionTask() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.findViewById(R.id.checkingAppVersionProgressBarRelativeLayout).setVisibility(View.VISIBLE);
                }
            });
        }

        @Override
        protected String doInBackground(String... strings) {
            installedAppVersion = getAppVersionNumber(MainActivity.this);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            StringBuilder response = new StringBuilder();
            try {
                URL url = new URL(BuildConfig.API_ENDPOINT + BuildConfig.API_VERSION + "check-version?clientHash=" + getClientHash(MainActivity.this));
                Log.d(TAG, "URL: " + url.toString());
                HttpsURLConnection urlConnection =
                        (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream()));
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                int status = urlConnection.getResponseCode();
                Log.d(TAG, "Server status: " + status);
            } catch (IOException e) {
                Log.e(TAG, "checkVersion Exception: " + e.getMessage());
                Log.e(TAG, Arrays.toString(e.getStackTrace()));
                e.printStackTrace();
            }
            Log.d(TAG, "GET version response: " + response.toString());
            String[] responseArray = response.toString().split("splitter");
            if (responseArray.length > 2) {
                critical = Boolean.valueOf(responseArray[0]);
                newestAppVersion = Integer.valueOf(responseArray[1]);
                urlToNewestAPK = responseArray[2];
                return response.toString();
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.findViewById(R.id.checkingAppVersionProgressBarRelativeLayout).setVisibility(View.GONE);
                }
            });
            if ((newestAppVersion > 0 && urlToNewestAPK != null && critical != null) && installedAppVersion < newestAppVersion) {
                MainActivity.this.fireNewAppVersionPrompt(installedAppVersion, newestAppVersion, urlToNewestAPK, critical);
            } else {
                new NewsTask().execute();
            }
        }
    }

    private class RegionTask extends AsyncTask<String, String, String> {
        int regionsID = -1;
        int lastSeenRegionsID = 0;

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
            lastSeenRegionsID = SharedPref.App.Regions.getLastSeenRegionsID(MainActivity.this);
            try {

                URL url = new URL(
                        BuildConfig.API_ENDPOINT + BuildConfig.API_VERSION + "check-regions?clientHash=" + getClientHash(MainActivity.this) + "&lastSeenRegionsID=" + lastSeenRegionsID);
                Log.d(TAG, "URL: " + url.toString());
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (regionsID == -1 && inputLine.startsWith("#")) {
                        regionsID = Integer.parseInt(inputLine.replace("#", ""));
                    } else {
                        checkRegionsResponse.append(inputLine).append(System.lineSeparator());
                    }
                }
                in.close();
                status = urlConnection.getResponseCode();
                Log.d(TAG, "Server status: " + status);
            } catch (IOException e) {
                Log.e(TAG, "RegionTask Exception: " + e.getMessage());
                Log.e(TAG, Arrays.toString(e.getStackTrace()));
                e.printStackTrace();
            }
            Log.d(TAG, "GET regions response: " + checkRegionsResponse.toString());
            if (status == 200 && checkRegionsResponse.length() > 0) {
                File regionsFile = IOUtils.Files.getRegionsFile(MainActivity.this);
                overwriteFile(checkRegionsResponse.toString(), regionsFile);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            // prompt user to go to "Profile" and set region, if regions have been updated and the region is set as UNKNOWN or other.
            Log.d(TAG, lastSeenRegionsID + " < " + regionsID + ": " + (lastSeenRegionsID < regionsID));
            Log.d(TAG, "actualSelectedRegionNotInTopThreeNearestRegion(): " + actualSelectedRegionNotInTopThreeNearestRegion());
            if (!SharedPref.App.RegionsPrompt.getRegionPromptShownAfterV81(MainActivity.this) || (!SharedPref.App.RegionsPrompt.getDoNotShowRegionPrompt(MainActivity.this) &&
                    (((lastSeenRegionsID < regionsID) && profileIsInUnknownRegion(MainActivity.this)) ||
                            actualSelectedRegionNotInTopThreeNearestRegion()))) {
                fireProfileRegionPrompt(regionsID, MainActivity.this);
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
                        BuildConfig.API_ENDPOINT + BuildConfig.API_VERSION + "check-news?clientHash=" + getClientHash(MainActivity.this) + "&lastSeenNewsID=" + lastSeenNewsID + "&newsLanguage=de");
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
                    if (newsID == -1 && inputLine_de.startsWith("#")) {
                        newsID = Integer.parseInt(inputLine_de.replace("#", ""));
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
                Log.e(TAG, "NewsTask Exception: " + e.getMessage());
                Log.e(TAG, Arrays.toString(e.getStackTrace()));
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
            if (SharedPref.App.News.getLastSeenNewsID(MainActivity.this) < newsID) {
                fireNewsPrompt();
            } else {
                // download the newest region list from the backend and prompt user to go to "Profile" and set region, if a new region has been added and the region is set as UNKNOWN or other.
                new RegionTask().execute();
            }
        }
    }

}
