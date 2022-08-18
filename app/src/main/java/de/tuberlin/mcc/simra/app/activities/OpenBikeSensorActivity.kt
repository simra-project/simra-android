package de.tuberlin.mcc.simra.app.activities

import android.bluetooth.*
import android.content.*
import android.os.*
import android.util.Log
import de.tuberlin.mcc.simra.app.R
import de.tuberlin.mcc.simra.app.databinding.ActivityOpenbikesensorBinding
import de.tuberlin.mcc.simra.app.services.OBSService
import de.tuberlin.mcc.simra.app.util.BaseActivity


private const val TAG = "OpenBikeSensorActivity_LOG"
private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2
/*private val OBS_SERVICE_UUID = UUID.fromString("1FE7FAF9-CE63-4236-0004-000000000000")
private val HEART_RATE_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")
private val SENSOR_DISTANCE_CHARACTERISTIC_UUID = UUID.fromString("1FE7FAF9-CE63-4236-0004-000000000002")
private val CLOSE_PASS_CHARACTERISTIC_UUID = UUID.fromString("1FE7FAF9-CE63-4236-0004-000000000003")
private val HEART_RATE_CHARACTERISTIC_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")*/

class OpenBikeSensorActivity : BaseActivity() {
    private lateinit var binding: ActivityOpenbikesensorBinding
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }
    private var blestate = BLESTATE.START
        set(value) {
            field = value
            runOnUiThread {
                Log.d(TAG, "blestate: $blestate")
                binding.bluetoothButton.text =
                        when (blestate) {
                            BLESTATE.START -> getString(R.string.obs_activity_button_start_scan)
                            BLESTATE.CONNECT -> getString(R.string.obs_activity_button_connect_device)
                            BLESTATE.DISCONNECT -> getString(R.string.obs_activity_button_disconnect_device)
                            else -> getString(R.string.obs_activity_button_stop_scan)
                        }
            }
        }
    /*private var obsFound = false
    private var isScanning = false
        set(value) {
            field = value
            runOnUiThread {
                Log.d(TAG, "changed value of isScanning: $isScanning obsFound: $obsFound")
                binding.bluetoothButton.text =
                    if (!isScanning && !obsFound) getString(R.string.obs_activity_button_start_scan)
                    else if (!isScanning && obsFound) getString(R.string.obs_activity_button_connect_device)
                    else getString(R.string.obs_activity_button_stop_scan) }
        }
    private var isConnected = false
        set(value) {
            field = value
            runOnUiThread {
                binding.bluetoothButton.text =
                    if (value) getString(R.string.obs_activity_button_disconnect_device)
                    else binding.bluetoothButton.text
            }
        }*/
    // private lateinit var result: ScanResult
    // private lateinit var gatt: BluetoothGatt
    // private var

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpenbikesensorBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val gattServiceIntent = Intent(this, OBSService::class.java)
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        val a = 0b10011 // 19
        Log.d(TAG, "a: $a")
        Log.d(TAG, "a shr 8: ${a shr 8}")
        Log.d(TAG, "a ushr 8: ${a ushr 8}")

        binding.bluetoothButton.setOnClickListener {
            /*Log.d(TAG, "pressed button. isScanning: $isScanning obsFound: $obsFound isConnected: $isConnected")
            if (!isScanning && !obsFound && !isConnected) {
                obsService?.startBleScan()
            } else if (!isScanning && obsFound && !isConnected) {
                obsService?.connectDevice(baseContext)
            } else if (isScanning && !isConnected) {
                obsService?.stopBleScan()
            } else {
                obsService?.disconnectDevice()
            }*/
            Log.d(TAG, "pressed button. blestate: $blestate")
            when (blestate) {
                BLESTATE.START ->  obsService?.startBleScan()
                BLESTATE.CONNECT -> obsService?.connectDevice(baseContext)
                BLESTATE.DISCONNECT -> obsService?.disconnectDevice()
                else -> obsService?.stopBleScan()
            }
        }
    }


    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(stateChangedReceiver, IntentFilter("stateChanged"))
        registerReceiver(closePassReceiver, IntentFilter("closePassNotification"))
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(stateChangedReceiver)
        unregisterReceiver(closePassReceiver)
    }

    /*private fun stopBleScan() {
        Log.d(TAG, "stopping scan")
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }*/

    /*private fun startBleScan() {
        Log.d(TAG, "starting scan")
        // results are sent to onScanResult
        bleScanner.startScan(scanCallback)
        isScanning = true
    }*/

    /*private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            with(result.device) {
                if (name != null && name.contains("OpenBikeSensor")) {
                    Log.d(TAG, "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address, bondState: $bondState, type: $type")
                    this@OpenBikeSensorActivity.result = result
                    obsFound = true
                    stopBleScan()
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d(TAG, "onScanFailed: code $errorCode")
        }
    }*/

    /*fun writeDescriptor(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        let {
            Log.d(TAG, " descriptor.value: ${descriptor.value} payload: ${payload.toHexString()} " )
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        }
    }

    fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        val payload = when {
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> {
                Log.e("ConnectionManager", "${characteristic.uuid} doesn't support notifications/indications")
                return
            }
        }

        characteristic.getDescriptor(CCCD_UUID)?.let { cccDescriptor ->
            Log.d(TAG, "CCCD found for ${characteristic.uuid}")
            if (!gatt.setCharacteristicNotification(characteristic, true)) {
                Log.d(TAG, "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, payload)
        } ?: Log.d(TAG, "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    fun disableNotifications(characteristic: BluetoothGattCharacteristic) {
        if (!characteristic.isNotifiable()) {
            Log.d(TAG, "${characteristic.uuid} doesn't support notifications")
            return
        }

        characteristic.getDescriptor(CCCD_UUID)?.let { cccDescriptor ->
            if (!gatt.setCharacteristicNotification(characteristic, false)) {
                Log.d(TAG, "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        } ?: Log.d(TAG, "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }*/

    /*private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Successfully connected to $deviceAddress")
                    // running on main thread because of a bug in older android versions
                    Handler(Looper.getMainLooper()).post {
                        // results are sent to onServicesDiscovered()
                        gatt.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Successfully disconnected from $deviceAddress")
                    gatt.close()
                    isConnected = false
                    isScanning = false
                    obsFound = false
                }
            } else {
                Log.d(TAG, "Error $status encountered for $deviceAddress! Disconnecting...")
                gatt.close()
                isScanning = false
                obsFound = false
                isConnected = false
            }
        }*/




       /* override fun onServicesDiscovered(bluetoothGatt: BluetoothGatt, status: Int) {
            this@OpenBikeSensorActivity.gatt = bluetoothGatt
            // Log.w(TAG, "Discovered ${gatt.services.size} services for ${gatt.device.address}")
            // gatt.printGattTable()
            val heartRateService = gatt.getService(HEART_RATE_SERVICE_UUID)
            val heartRateCharacteristic = heartRateService.getCharacteristic(HEART_RATE_CHARACTERISTIC_UUID)
            enableNotifications(heartRateCharacteristic)
            val obsService = gatt.getService(OBS_SERVICE_UUID)
            val sensorDistanceCharacteristic = obsService.getCharacteristic(SENSOR_DISTANCE_CHARACTERISTIC_UUID)
            val closePassCharacteristic = obsService.getCharacteristic(CLOSE_PASS_CHARACTERISTIC_UUID)

            enableNotifications(sensorDistanceCharacteristic)
            enableNotifications(closePassCharacteristic)
            gatt.services.forEach { service ->
                service.characteristics.forEach{ characteristic ->
                    enableNotifications(characteristic)
                }
            }


            gatt.setCharacteristicNotification(sensorDistanceCharacteristic,true)
            gatt.setCharacteristicNotification(closePassCharacteristic,true)

            val sensorDistanceDescriptor = sensorDistanceCharacteristic.getDescriptor(CCCD_UUID)
            val closePassDescriptor = closePassCharacteristic.getDescriptor(CCCD_UUID)

            sensorDistanceDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            closePassDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(sensorDistanceDescriptor);
            gatt.writeDescriptor(closePassDescriptor);
            isConnected = true
            Log.d(TAG, "connection setup complete.")
        // Consider connection setup as complete here

        }*/

        /*override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic) {
                Log.d(TAG, "Characteristic $uuid changed | value: ${value.toHexString()}")
            }
        }

    }*/

    /*private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.d(TAG, "No service and characteristic available, call discoverServices() first?")
            return
        }
        services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                    separator = "\n|--",
                    prefix = "|--"
            ) { it.uuid.toString() }
            Log.d(TAG, "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )
        }
    }*/

    /*fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

    fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return properties and property != 0
    }*/

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    /*val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }*/

    /*fun ByteArray.toHexString(): String =
            joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }*/

    /*private fun disconnectDevice() {
        gatt.disconnect()
    }*/

    /**
     *
     *
     *
     */

    private var obsService : OBSService? = null
    // Code to manage Service lifecycle.
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
                componentName: ComponentName,
                service: IBinder
        ) {
            obsService = (service as OBSService.LocalBinder).getService()
            obsService.let { bluetooth ->
                // call functions on service to check connection and connect to devices
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            obsService = null
        }
    }

    private val stateChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val thisBleState: BLESTATE = intent?.extras?.get("BLESTATE") as BLESTATE
            Log.d(TAG, "onReceive thisBleState: $thisBleState")
            blestate = thisBleState
        }
    }

    private val closePassReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val distanceLeftCm = intent?.extras?.getShort("leftSensor")
            val distanceRightCm = intent?.extras?.getShort("rightSensor")
            Log.d(TAG, "onReceive closePassReceiver: $distanceLeftCm, $distanceRightCm")
        }
    }

    enum class BLESTATE {
        START, STOP, CONNECT, DISCONNECT
    }

}