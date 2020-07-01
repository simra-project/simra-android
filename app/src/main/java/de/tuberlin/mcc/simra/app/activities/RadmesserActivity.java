package de.tuberlin.mcc.simra.app.activities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.services.RadmesserService;
import pl.droidsonroids.gif.GifImageView;


public class RadmesserActivity extends AppCompatActivity {
    boolean deviceConnected = false;
    LinearLayout connectDevicesLayout;
    LinearLayout devicesList;
    LinearLayout deviceLayout;
    TextView deviceInfoTextView;
    BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radmesser);
        initializeToolBar();
        Log.i("start","RadmesserActivity");
        connectDevicesLayout = findViewById(R.id.connectDevicesLayout);
        devicesList = findViewById(R.id.devicesList);
        deviceLayout = findViewById(R.id.deviceLayout);
        deviceInfoTextView = findViewById(R.id.deviceInfoTextView);

        /*Intent intent = new Intent(RadmesserActivity.this, RadmesserService.class);
        startService(intent);*/

        Button disconnectBTN = findViewById(R.id.btnDisconnect);
        disconnectBTN.setOnClickListener(view -> RadmesserService.disconnectAndUnpairDevice(this));


    }

    private void registerReceiver(){
        receiver = RadmesserService.registerCallbacks(this, new RadmesserService.RadmesserServiceCallbacks() {
            @Override
            public void onDeviceFound(String deviceName, String deviceId) {
                Button button = new Button(RadmesserActivity.this);
                button.setText("Connect with " + deviceName);
                button.setOnClickListener(v -> connectToDevice(deviceId));
                devicesList.addView(button);
            }
            @Override
            public void onConnectionStateChanged(RadmesserService.ConnectionState newState) {
                Log.i("connState",newState.toString());
                switch (newState) {
                    case PAIRING:
                        showTutorialDialog();
                        break;
                    case CONNECTED:
                        deviceConnected = true;
                        closeTutorialDialog();
                        break;
                    case CONNECTION_REFUSED:
                    case DISCONNECTED:
                        deviceConnected = false;
                        break;
                }
                // ?????
                connectDevicesLayout.setVisibility(deviceConnected ? View.GONE : View.VISIBLE);
                deviceLayout.setVisibility(deviceConnected ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onDistanceValue(String value) {
                int distance = -1;
                String[] splitted = value.split(",");
                if (splitted.length == 2) distance = Integer.parseInt(splitted[0]);

                deviceInfoTextView.setText("Connected with " + "\n" + "Last distance: " + distance + " cm");
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
    private AlertDialog alertDL;
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
        alertDL = alert.show();

    }
    private void closeTutorialDialog() {
        if(alertDL!=null)
            alertDL.dismiss();
    }

    @Override
    protected void onPause() {
        RadmesserService.unRegisterCallbacks(receiver,this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver();
        startScanningDevices();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

