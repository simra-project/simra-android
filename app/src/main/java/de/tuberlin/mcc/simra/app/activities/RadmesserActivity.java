package de.tuberlin.mcc.simra.app.activities;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.HashMap;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.services.RadmesserService;
import de.tuberlin.mcc.simra.app.services.radmesser.RadmesserDevice;
import de.tuberlin.mcc.simra.app.util.PermissionHelper;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import pl.droidsonroids.gif.GifImageView;

import static de.tuberlin.mcc.simra.app.services.RadmesserService.ConnectionStatus;

public class RadmesserActivity extends AppCompatActivity {

    RadmesserService mBoundRadmesserService;
    boolean deviceConnected = false;

    LinearLayout connectDevicesLayout;
    LinearLayout devicesList;

    LinearLayout deviceLayout;
    TextView deviceInfoTextView;

    Handler pollDevicesHandler;
    Switch takePicturesButton;

    private ServiceConnection mRadmesserServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RadmesserService.LocalBinder myBinder = (RadmesserService.LocalBinder) service;
            mBoundRadmesserService = myBinder.getService();
            updateViewMode();
        }
    };

    private BroadcastReceiver distanceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("distance");
            if (message == null) return;
            Log.d("receiver", "Got message: " + message);

            int distance = -1;
            String[] splitted = message.split(",");
            if (splitted.length == 2) distance = Integer.parseInt(splitted[0]);

            deviceInfoTextView.setText("Connected with " + mBoundRadmesserService.connectedDevice.getID() + "\n" + "Last distance: " + distance + " cm");
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionHelper.Camera.PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                SharedPref.Settings.Ride.PicturesDuringRide.setMakePictureDuringRide(true, this);
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                takePicturesButton.setChecked(false);
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radmesser);
        initializeToolBar();

        connectDevicesLayout = findViewById(R.id.connectDevicesLayout);
        devicesList = findViewById(R.id.devicesList);

        deviceLayout = findViewById(R.id.deviceLayout);
        deviceInfoTextView = findViewById(R.id.deviceInfoTextView);

        NumberPicker handleBarWidth = findViewById(R.id.handleBarWidth);
        handleBarWidth.setMaxValue(40);
        handleBarWidth.setMinValue(0);
        handleBarWidth.setValue(SharedPref.Settings.Ride.OvertakeWidth.getHandlebarWidth(this));
        handleBarWidth.setOnValueChangedListener((numberPicker, oldVal, newVal) -> {
            SharedPref.Settings.Ride.OvertakeWidth.setTotalWidthThroughHandlebarWidth(newVal, this);
        });

        NumberPicker takePictureInterval = findViewById(R.id.takePictureDuringRideInterval);
        takePictureInterval.setMaxValue(20);
        takePictureInterval.setMinValue(0);
        takePictureInterval.setValue(SharedPref.Settings.Ride.PicturesDuringRideInterval.getInterval(this));
        takePictureInterval.setOnValueChangedListener((numberPicker, oldVal, newVal) -> {
            SharedPref.Settings.Ride.PicturesDuringRideInterval.setInterval(newVal, this);
        });

        takePicturesButton = findViewById(R.id.takePictureDuringRideButton);
        takePicturesButton.setChecked(SharedPref.Settings.Ride.PicturesDuringRide.isActivated(this));
        takePicturesButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (PermissionHelper.Camera.hasPermission(this)) {
                    // Wants to activate this Functionality and already has Camera Permission
                    SharedPref.Settings.Ride.PicturesDuringRide.setMakePictureDuringRide(true, this);
                } else {
                    // Wants to activate this Functionality and already has does not have Camera Permission
                    PermissionHelper.Camera.requestPermissions(RadmesserActivity.this);
                }
            } else {
                // Deactivate Functionality
                SharedPref.Settings.Ride.PicturesDuringRide.setMakePictureDuringRide(false, this);
            }
        });

        Intent intent = new Intent(RadmesserActivity.this, RadmesserService.class);
        startService(intent);
        bindService(intent, mRadmesserServiceConnection, Context.BIND_IMPORTANT);
    }

    private void initializeToolBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        TextView toolbarTxt = findViewById(R.id.toolbar_title);
        toolbarTxt.setText("Radmesser");

        ImageButton backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(v -> finish());
    }

    private void updateViewMode() {
        // TODO: implement all connection statuses
        deviceConnected = mBoundRadmesserService != null && (mBoundRadmesserService.getCurrentConnectionStatus() == ConnectionStatus.CONNECTED || mBoundRadmesserService.getCurrentConnectionStatus() == ConnectionStatus.PAIRING);

        connectDevicesLayout.setVisibility(deviceConnected ? View.GONE : View.VISIBLE);
        deviceLayout.setVisibility(deviceConnected ? View.VISIBLE : View.GONE);

        if (deviceConnected) {
            if (pollDevicesHandler != null) {
                pollDevicesHandler.removeCallbacksAndMessages(null);
                pollDevicesHandler = null;
            }

            deviceInfoTextView.setText("Verbunden mit Gerät " + mBoundRadmesserService.connectedDevice.getID());
        } else {
            if (pollDevicesHandler == null) {
                pollDevicesHandler = new Handler();
                int delayMs = 1000;

                pollDevicesHandler.postDelayed(new Runnable() {
                    public void run() {
                        pollAvailableDevices();
                        pollDevicesHandler.postDelayed(this, delayMs);
                    }
                }, delayMs);
            }
        }
    }

    private void pollAvailableDevices() {
        HashMap<String, BluetoothDevice> devices = mBoundRadmesserService.scanner.getFoundDevices();

        // TODO: mit einer "richtigen" ListView (o.ä.) implementieren, da hier die Buttons jedes Mal neu erstellt werden
        devicesList.removeAllViews();

        for (String deviceId : devices.keySet()) {
            Button button = new Button(RadmesserActivity.this);
            button.setText("Connect with " + deviceId);
            button.setOnClickListener(v -> connectToDevice(deviceId));
            devicesList.addView(button);
        }
    }

    private void connectToDevice(String deviceId) {
        mBoundRadmesserService.connectToDevice(deviceId, device -> {
            updateViewMode();
            showTutorialDialog();
        });
    }

    private void showTutorialDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Verbindung mit Radmesser");
        alert.setMessage("\nBitte halten Sie Ihr Hand nah an den Abstandsensor für 3 Sekunden");

        LinearLayout gifLayout = new LinearLayout(this);
        LinearLayout.LayoutParams gifMargins = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        GifImageView gif = new GifImageView(this);
        gif.setImageResource(R.drawable.tutorial);
        gif.setVisibility(View.VISIBLE);
        gifMargins.setMargins(50, 0, 50, 0);
        gif.setLayoutParams(gifMargins);
        gifLayout.addView(gif);
        alert.setView(gifLayout);
        alert.setPositiveButton("Ok", (dialog, whichButton) -> {
        });
        alert.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(distanceReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(distanceReceiver, new IntentFilter(RadmesserDevice.UUID_SERVICE_DISTANCE));
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}

