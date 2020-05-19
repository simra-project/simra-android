package de.tuberlin.mcc.simra.app.main;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class RadmesserService extends Service {
    private IBinder mBinder = new MyBinder();
    private int connectionStatus = 0;




    // das soll mit einem Listener passieren
    public int getConnectionStatus(){
        return connectionStatus;
    }

    public void setConnectionStatus(int newStatus){
        this.connectionStatus = newStatus;
    }

    @Override
    public IBinder onBind(Intent intent) {
       return mBinder;
    }

    public class MyBinder extends Binder {
        public RadmesserService getService() {
            return RadmesserService.this;
        }
    }
}


/*

    LINUS FALLS DU ETWAS VON DIESEM CODE BRAUCHST , VERWENDE DIE GERNE, ANSONSTEN KANN VON MIR AUS WEG


      private final static int REQUEST_ENABLE_BT = 1;

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
                if (device != null && device.getName() != null) {
                    LinearLayout deviceLayout = createLinearLayout();
                    String deviceFound = device.getName() + "\n" + device.getAddress();
                    // Das hier soll gewrapped werden in ein anderes Layout mit einem Button, der das Verbinden erlaubt
                    TextView deviceInfo = createTextView();
                    deviceInfo.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    String toSet = deviceFound + "\n";
                    deviceInfo.setText(toSet);
                    deviceInfo.setPadding(20, 20, 20, 20);// in pixels (left, top, right, bottom)
                    Button connectButton = createButton();
                    connectButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            // Here later connect to this device addres
                            System.out.println("Device address : " + device.getAddress());
                        }
                    });
                    connectButton.setText("Connect");
                    deviceLayout.addView(deviceInfo);
                    deviceLayout.addView(connectButton);
                    discoveredDevicesLayout.addView(deviceLayout);
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



    LinearLayout createLinearLayout(){
        return new LinearLayout(this);
    }

    Button createButton(){
        return new Button(this);
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
        discoveredDevicesLayout.setOrientation(LinearLayout.VERTICAL);
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


*/
