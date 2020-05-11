package de.tuberlin.mcc.simra.app.subactivites;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.tuberlin.mcc.simra.app.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


public class BluetoothConnection extends AppCompatActivity {
    private final static int REQUEST_ENABLE_BT = 1;
    LinearLayout rootLayout;
    LinearLayout enableBluetoothLayout;
    LinearLayout discoveredDevicesLayout;
    BluetoothAdapter bluetoothAdapter;

    private final BroadcastReceiver bluetoothConnectionReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                if (device != null) {
                    String deviceFound = device.getName() + "\n" + device.getAddress();
                    // Das hier soll gewrapped werden in ein anderes Layout mit einem Button, der das Verbinden erlaubt
                    TextView deviceView = createTextView();
                    deviceView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    String toSet = deviceFound + "\n";
                    deviceView.setText(toSet);
                    deviceView.setPadding(20, 20, 20, 20);// in pixels (left, top, right, bottom)
                    deviceView.setLines(2);
                    discoveredDevicesLayout.addView(deviceView);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_connection);
        initializeToolBar();
        rootLayout = findViewById(R.id.bluetooth_screen);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothConnectionReciever, filter); // unregister onDestroy
        
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
        }
        if (!bluetoothAdapter.isEnabled()) {
            createEnableBluetoothUI();
        } else {
            renderDiscoverButton();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothConnectionReciever);
    }


    TextView createTextView() {
        return new TextView(this);
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == -1) {
            // Bluetooth successfully enabled, i dont know why the F**k is -1 instead a positive number
            rootLayout.removeView(enableBluetoothLayout);
            renderDiscoverButton();
        }
    }

    private void enableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    private void createEnableBluetoothUI() {
        enableBluetoothLayout = new LinearLayout(this);
        TextView textView1 = new TextView(this);
        textView1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        textView1.setText("Blueetooth is Off ");
        textView1.setPadding(20, 20, 20, 20);// in pixels (left, top, right, bottom)
        enableBluetoothLayout.addView(textView1);
        Button enableButton = new Button(this);
        enableButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                enableBluetooth();
            }
        });
        enableButton.setText("Turn on");
        enableBluetoothLayout.addView(enableButton);
        rootLayout.addView(enableBluetoothLayout);
    }

    private void discoverDevices() {
        bluetoothAdapter.startDiscovery();
        discoveredDevicesLayout = new LinearLayout(this);
        rootLayout.addView(discoveredDevicesLayout);
    }

    private void renderDiscoverButton() {
        Button discoverButton = new Button(this);
        discoverButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                discoverDevices();
            }
        });
        discoverButton.setText("Discover");
        rootLayout.addView(discoverButton);
    }

    private void initializeToolBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        TextView toolbarTxt = findViewById(R.id.toolbar_title);
        toolbarTxt.setText("Bluetooth connection");

        ImageButton backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           finish();
                                       }
                                   }
        );
    }
}

