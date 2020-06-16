package de.tuberlin.mcc.simra.app.subactivites;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.HashMap;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.services.RadmesserService;
import de.tuberlin.mcc.simra.app.services.radmesser.RadmesserDevice;
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
            Log.d("receiver", "Got message: " + message);
            deviceInfoTextView.setText("Verbunden mit Gerät " + mBoundRadmesserService.connectedDevice.getID() + "\n" + "Letzte Distanz: " + message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_connection);
        initializeToolBar();

        connectDevicesLayout = findViewById(R.id.connectDevicesLayout);
        devicesList = findViewById(R.id.devicesList);

        deviceLayout = findViewById(R.id.deviceLayout);
        deviceInfoTextView = findViewById(R.id.deviceInfoTextView);

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
            button.setText("Verbinden mit " + deviceId);
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

