package de.tuberlin.mcc.simra.app.activities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.services.RadmesserService;
import de.tuberlin.mcc.simra.app.util.PermissionHelper;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import pl.droidsonroids.gif.GifImageView;


public class RadmesserActivity extends AppCompatActivity {
    LinearLayout connectDevicesLayout; // Verfügbare Geräte
    LinearLayout deviceLayout; // Connected Device
    LinearLayout pairingLayout; // Connected Device
    LinearLayout devicesList; // Button list (Innerhalb ConnectDevicesLayout)
    ProgressBar progressBar;
    TextView deviceInfoTextView; // (Innerhalb deviceLayout)
    BroadcastReceiver receiver;
    Switch takePicturesButton;
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radmesser);
        initializeToolBar();
        Log.i("start", "RadmesserActivity");
        connectDevicesLayout = findViewById(R.id.connectDevicesLayout);
        devicesList = findViewById(R.id.devicesList);
        deviceLayout = findViewById(R.id.deviceLayout);
        pairingLayout = findViewById(R.id.pairing);
        progressBar = findViewById(R.id.progressBarClosePass);
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

        Button disconnectBTN = findViewById(R.id.btnDisconnect);
        disconnectBTN.setOnClickListener(view -> RadmesserService.disconnectAndUnpairDevice(this));
        RadmesserService.ConnectionState currentState = RadmesserService.getConnectionState();
        updateUI(currentState);
        if(!currentState.equals(RadmesserService.ConnectionState.CONNECTED)){
            startScanningDevices();
        }
    }

    private void setClosePassBarColor(int distanceInCm){
        int maxColorValue = Math.min(distanceInCm, 200); // 200 cm ist maximum, das grün
        // Algoritmus found https://stackoverflow.com/questions/340209/generate-colors-between-red-and-green-for-a-power-meter
        // Da n zwischen 0 -100 liegen soll und das maximum 200 ist, dann halbieren immer den Wert.
        int normalizedValue = distanceInCm / 2;
        int red = (255 * normalizedValue) / 100;
        int green = (255 * (100 - normalizedValue)) / 100;
        int blue = 0;
        // Color und Progress sind abhängig
        progressBar.setProgressTintList(ColorStateList.valueOf(Color.rgb(red, green, blue)));
        progressBar.setProgress(normalizedValue);
    }

    private void updateUI(RadmesserService.ConnectionState state){
        switch (state) {
            case PAIRING:
                deviceLayout.setVisibility(View.GONE);
                connectDevicesLayout.setVisibility(View.GONE);
                pairingLayout.setVisibility(View.VISIBLE);
                showTutorialDialog();
                break;
            case CONNECTED:
                deviceLayout.setVisibility(View.VISIBLE);
                connectDevicesLayout.setVisibility(View.GONE);
                pairingLayout.setVisibility(View.GONE);
                closeTutorialDialog();
                break;
            case CONNECTION_REFUSED:
            case DISCONNECTED:
            default:
                deviceLayout.setVisibility(View.GONE);
                connectDevicesLayout.setVisibility(View.VISIBLE);
                pairingLayout.setVisibility(View.GONE);
                closeTutorialDialog();
                break;
        }

    }

    private void registerReceiver() {
        receiver = RadmesserService.registerCallbacks(this, new RadmesserService.RadmesserServiceCallbacks() {
            @Override
            public void onDeviceFound(String deviceName, String deviceId) {
                Button button = new Button(RadmesserActivity.this);
                button.setText("Connect with " + deviceName);
                button.setOnClickListener(v -> connectToDevice(deviceId));
                devicesList.addView(button);
                Log.i("RadmesserService", "DeviceFound in radmesser activity");
            }

            @Override
            public void onConnectionStateChanged(RadmesserService.ConnectionState newState) {
                Log.i("connState", newState.toString());
                updateUI(newState);
            }

            @Override
            public void onDistanceValue(RadmesserService.Measurement value) {
                Log.i("RadmesserService", "Value found : " + value);
                int distance = -1;
                if (value!= null && value.leftSensorValues.size() > 0){
                    distance = value.leftSensorValues.get(0);
                    deviceInfoTextView.setText("Connected with " + "\n" + "Last distance: " + distance + " cm");
                    setClosePassBarColor(distance);
                    Log.i("RadmesserService", "Distance found : " + distance);
                }
            }
        });

    }

    private void startScanningDevices() {
        devicesList.removeAllViews();
        RadmesserService.startScanning(this);
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

    private void connectToDevice(String deviceId) {
        RadmesserService.connectDevice(this, deviceId);
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
        alertDialog = alert.show();

    }

    private void closeTutorialDialog() {
        if (alertDialog != null)
            alertDialog.dismiss();
    }

    @Override
    protected void onPause() {
        RadmesserService.unRegisterCallbacks(receiver, this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver();
        RadmesserService.ConnectionState currentState = RadmesserService.getConnectionState();
        if(!currentState.equals(RadmesserService.ConnectionState.CONNECTED)){
            startScanningDevices();
        }
        Toast.makeText(this, currentState.toString(), Toast.LENGTH_SHORT).show();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

