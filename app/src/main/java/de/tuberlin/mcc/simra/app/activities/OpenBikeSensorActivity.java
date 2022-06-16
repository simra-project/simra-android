package de.tuberlin.mcc.simra.app.activities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashSet;
import java.util.Set;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.databinding.ActivityOpenbikesensorBinding;
import de.tuberlin.mcc.simra.app.services.OBSService;
import de.tuberlin.mcc.simra.app.util.PermissionHelper;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import pl.droidsonroids.gif.GifImageView;


public class OpenBikeSensorActivity extends AppCompatActivity {
    private static final String TAG = "OpenBikeSensorActivity_LOG";
    BroadcastReceiver receiver;
    Set<BluetoothDevice> foundDevices;
    BluetoothDevice selectedDevice;
    ActivityOpenbikesensorBinding binding;
    RadioGroup devices;
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOpenbikesensorBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        foundDevices = new HashSet<>();
        initializeToolBar();
        Log.d(TAG, "onCreate");
        binding.retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScanningDevices();
                foundDevices = new HashSet<>();
            }
        });
        Button connectButton = new Button(this);
        connectButton.setVisibility(View.GONE);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedDevice != null) {
                    OBSService.connectDevice(OpenBikeSensorActivity.this, selectedDevice.deviceId);
                    Log.d(TAG, "connecting to device: " + selectedDevice.deviceId);
                }
            }
        });
        devices = new RadioGroup(this);
        devices.setOrientation(RadioGroup.VERTICAL);
        devices.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {
                for (BluetoothDevice device : foundDevices) {
                    if (device.hashCode() == id) {
                        selectedDevice = device;
                        connectButton.setText("Connect to " + selectedDevice.deviceName);
                        connectButton.setVisibility(View.VISIBLE);
                    }
                }

            }
        });
        binding.handleBarWidth.setMaxValue(60);
        binding.handleBarWidth.setMinValue(0);
        binding.handleBarWidth.setValue(SharedPref.Settings.Ride.OvertakeWidth.getHandlebarWidth(this));
        binding.handleBarWidth.setOnValueChangedListener((numberPicker, oldVal, newVal) -> {
            SharedPref.Settings.Ride.OvertakeWidth.setTotalWidthThroughHandlebarWidth(newVal, this);
        });

        binding.btnDisconnect.setOnClickListener(view -> OBSService.disconnectAndUnpairDevice(this));
        OBSService.ConnectionState currentState = OBSService.getConnectionState();
        updateUI(currentState);
        if (!currentState.equals(OBSService.ConnectionState.CONNECTED)) {
            startScanningDevices();
        }

        binding.connectDevicesLayout.addView(devices);
        binding.connectDevicesLayout.addView(connectButton);
    }

    private void setClosePassBarColor(int distanceInCm) {
        int maxColorValue = Math.min(distanceInCm, 200); // 200 cm ist maximum, das grün
        // Algoritmus found https://stackoverflow.com/questions/340209/generate-colors-between-red-and-green-for-a-power-meter
        // Da n zwischen 0 -100 liegen soll und das maximum 200 ist, dann halbieren immer den Wert.
        int normalizedValue = maxColorValue / 2;
        int red = (255 * (100 - normalizedValue)) / 100;
        int green = (255 * normalizedValue) / 100;
        int blue = 0;
        // Color und Progress sind abhängig
        binding.progressBarClosePass.setProgressTintList(ColorStateList.valueOf(Color.rgb(red, green, blue)));
        binding.progressBarClosePass.setProgress(normalizedValue);
    }

    private void createRadioButton(BluetoothDevice device) {
        RadioButton radioButton = new RadioButton(this);
        radioButton.setText(device.deviceName);
        radioButton.setId(device.hashCode());
        devices.addView(radioButton);
    }

    private void showRetryButton() {
        binding.searchingCircle.setVisibility(View.GONE);
        binding.retryButton.setVisibility(View.VISIBLE);
    }

    private void hideRetryButton() {
        binding.searchingCircle.setVisibility(View.VISIBLE);
        binding.retryButton.setVisibility(View.GONE);
    }

    private void updateUI(OBSService.ConnectionState state) {
        switch (state) {
            case PAIRING:
                binding.deviceLayout.setVisibility(View.GONE);
                binding.connectDevicesLayout.setVisibility(View.GONE);
                binding.pairingLayout.setVisibility(View.VISIBLE);
                showTutorialDialog();
                break;
            case CONNECTED:
                binding.deviceLayout.setVisibility(View.VISIBLE);
                binding.connectDevicesLayout.setVisibility(View.GONE);
                binding.pairingLayout.setVisibility(View.GONE);
                closeTutorialDialog();
                break;
            case CONNECTION_REFUSED:
                showRetryButton();
            case DISCONNECTED:
                showRetryButton();
            case SEARCHING:
                binding.deviceLayout.setVisibility(View.GONE);
                binding.connectDevicesLayout.setVisibility(View.VISIBLE);
                binding.pairingLayout.setVisibility(View.GONE);
                hideRetryButton();
            default:
                break;
        }
    }

    private void registerReceiver() {
        receiver = OBSService.registerCallbacks(this, new OBSService.OBSServiceCallbacks() {
            @Override
            public void onDeviceFound(String deviceName, String deviceId) {
                BluetoothDevice foundDevice = new BluetoothDevice(deviceName, deviceId);
                if (!foundDevices.contains(foundDevice)) {
                    foundDevices.add(foundDevice);
                    createRadioButton(foundDevice);

                }
            }

            @Override
            public void onConnectionStateChanged(OBSService.ConnectionState newState) {
                Log.d(TAG, "connection state: " + newState.name());
                updateUI(newState);
            }

            @Override
            public void onDistanceValue(OBSService.Measurement value) {
                Log.d(TAG, "value: " + value.toString());
                int distance = -1;
                if (value != null && value.leftSensorValues.size() > 0) {
                    distance = value.leftSensorValues.get(0);
                    binding.deviceInfoTextView.setText(getString(R.string.obs_activity_text_last_distance) + " " + distance + " cm");
                    setClosePassBarColor(distance);
                }
            }
        });

    }

    private void startScanningDevices() {
        binding.devicesList.removeAllViews();
        OBSService.startScanning(this);
    }

    private void initializeToolBar() {
        setSupportActionBar(binding.toolbar.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        binding.toolbar.toolbar.setTitle("");
        binding.toolbar.toolbar.setSubtitle("");
        binding.toolbar.toolbarTitle.setText(getString(R.string.obs));

        binding.toolbar.backButton.setOnClickListener(v -> finish());
    }

    private void connectToDevice(String deviceId) {
        OBSService.connectDevice(this, deviceId);
    }

    private void showTutorialDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.obsConnecting));
        alert.setMessage(getString(R.string.obsTutorial));

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
        OBSService.unRegisterCallbacks(receiver, this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver();
        OBSService.ConnectionState currentState = OBSService.getConnectionState();
        if (!currentState.equals(OBSService.ConnectionState.CONNECTED)) {
            startScanningDevices();
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private class BluetoothDevice {
        private String deviceName;
        private String deviceId;

        public BluetoothDevice(String deviceName, String deviceId) {
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

