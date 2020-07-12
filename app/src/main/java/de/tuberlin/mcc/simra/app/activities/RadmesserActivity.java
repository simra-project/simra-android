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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.HashSet;
import java.util.Set;

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
    ProgressBar closePassBar;
    ProgressBar searchingCircle;
    TextView deviceInfoTextView; // (Innerhalb deviceLayout)
    BroadcastReceiver receiver;
    Switch takePicturesButton;
    Button retryButton;
    Button connectButton;
    private AlertDialog alertDialog;
    Set<BluetoothDevice> foundDevices;
    BluetoothDevice selectedDevice;
    RadioGroup devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        foundDevices = new HashSet<>();
        setContentView(R.layout.activity_radmesser);
        initializeToolBar();
        Log.i("start", "RadmesserActivity");
        connectDevicesLayout = findViewById(R.id.connectDevicesLayout);
        devicesList = findViewById(R.id.devicesList);
        searchingCircle = findViewById(R.id.searching);
        retryButton = findViewById(R.id.retry);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScanningDevices();
                foundDevices = new HashSet<>();
            }
        });
        devices  = new RadioGroup(this);
        devices.setOrientation(RadioGroup.VERTICAL);
        devices.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {
                for(BluetoothDevice device : foundDevices){
                    if(device.hashCode() == id){
                        selectedDevice = device;
                        connectButton.setText("Connect to " + selectedDevice.deviceName);
                        connectButton.setVisibility(View.VISIBLE);
                    }
                }

            }
        });
        deviceLayout = findViewById(R.id.deviceLayout);
        pairingLayout = findViewById(R.id.pairing);
        closePassBar = findViewById(R.id.progressBarClosePass);
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
        connectButton = new Button(this);
        connectButton.setVisibility(View.GONE);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(selectedDevice != null){
                    RadmesserService.connectDevice(RadmesserActivity.this, selectedDevice.deviceId);
                }
            }
        });
        connectDevicesLayout.addView(devices);
        connectDevicesLayout.addView(connectButton);
    }

    private void setClosePassBarColor(int distanceInCm){
        int maxColorValue = Math.min(distanceInCm, 200); // 200 cm ist maximum, das grün
        // Algoritmus found https://stackoverflow.com/questions/340209/generate-colors-between-red-and-green-for-a-power-meter
        // Da n zwischen 0 -100 liegen soll und das maximum 200 ist, dann halbieren immer den Wert.
        int normalizedValue = maxColorValue / 2;
        int red = (255 * normalizedValue) / 100;
        int green = (255 * (100 - normalizedValue)) / 100;
        int blue = 0;
        // Color und Progress sind abhängig
        closePassBar.setProgressTintList(ColorStateList.valueOf(Color.rgb(red, green, blue)));
        closePassBar.setProgress(normalizedValue);
    }

    private void createRadioButton(BluetoothDevice device) {
        RadioButton radioButton =  new RadioButton(this);
        radioButton.setText(device.deviceName);
        radioButton.setId(device.hashCode());
        devices.addView(radioButton);
    }

    private void showRetryButton(){
        searchingCircle.setVisibility(View.GONE);
        retryButton.setVisibility(View.VISIBLE);
    }

    private void hideRetryButton(){
        searchingCircle.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
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
                showRetryButton();
            case DISCONNECTED:
                showRetryButton();
            case SEARCHING:
                deviceLayout.setVisibility(View.GONE);
                connectDevicesLayout.setVisibility(View.VISIBLE);
                pairingLayout.setVisibility(View.GONE);
                hideRetryButton();
            default:
                break;
        }
    }

    private void registerReceiver() {
        receiver = RadmesserService.registerCallbacks(this, new RadmesserService.RadmesserServiceCallbacks() {
            @Override
            public void onDeviceFound(String deviceName, String deviceId) {
                BluetoothDevice foundDevice = new BluetoothDevice(deviceName, deviceId);
                if(!foundDevices.contains(foundDevice)){
                    foundDevices.add(foundDevice);
                    createRadioButton(foundDevice);

                }
            }

            @Override
            public void onConnectionStateChanged(RadmesserService.ConnectionState newState) {
                updateUI(newState);
            }

            @Override
            public void onDistanceValue(RadmesserService.Measurement value) {
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

    private class BluetoothDevice{
        private String deviceName;
        private String deviceId;

        public BluetoothDevice(String deviceName, String deviceId){
            this.deviceName = deviceName;
            this.deviceId = deviceId;
        }

        @Override
        public int hashCode() {
            return deviceName.hashCode() * deviceId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            BluetoothDevice other = (BluetoothDevice) obj;
            if (!deviceName.equals(other.deviceName))
                return false;
            if (!deviceId.equals(other.deviceId))
                return false;
            return true;
        }
    }
}

