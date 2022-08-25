package de.tuberlin.mcc.simra.app.services

import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import de.tuberlin.mcc.simra.app.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


private const val TAG = "OBSService_LOG"
private val OBS_SERVICE_UUID = UUID.fromString("1FE7FAF9-CE63-4236-0004-000000000000")
private val SENSOR_DISTANCE_CHARACTERISTIC_UUID = UUID.fromString("1FE7FAF9-CE63-4236-0004-000000000002")
private val TIME_CHARACTERISTIC_UUID = UUID.fromString("1FE7FAF9-CE63-4236-0004-000000000001")
private val CLOSE_PASS_CHARACTERISTIC_UUID = UUID.fromString("1FE7FAF9-CE63-4236-0004-000000000003")
private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
private val CHARACTERISTIC_FORMAT_UUID = UUID.fromString("00002904-0000-1000-8000-00805f9b34fb")
private const val GATT_MAX_MTU_SIZE = 517
class OBSServicee: Service() {

    private var blestate: BLESTATE = BLESTATE.START
        set(value) {
            field = value
            updateState(value)
        }

    private lateinit var gatt: BluetoothGatt


    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private var result: ScanResult? = null


    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            with(result.device) {
                if (name != null && name.contains("OpenBikeSensor")) {
                    Log.d(TAG, "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address, bondState: $bondState, type: $type")
                    this@OBSServicee.result = result
                    stopBleScan()
                    blestate = BLESTATE.CONNECT

                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d(TAG, "onScanFailed: code $errorCode")
        }
    }

    fun startBleScan() {
        Log.d(TAG, "starting scan")
        // results are sent to onScanResult
        bleScanner.startScan(scanCallback)
        blestate = BLESTATE.STOP
    }

    fun stopBleScan() {
        Log.d(TAG, "stopping scan")
        bleScanner.stopScan(scanCallback)
        blestate = BLESTATE.START
    }

    fun writeDescriptor(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        let {
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        }
    }

    fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        val payload = when {
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> {
                Log.e(TAG, "${characteristic.uuid} doesn't support notifications/indications")
                return
            }
        }

        characteristic.getDescriptor(CCCD_UUID)?.let { cccDescriptor ->
            if (!gatt.setCharacteristicNotification(characteristic, true)) {
                Log.e(TAG, "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, payload)
        } ?: Log.e(TAG, "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Successfully connected to $deviceAddress")
                    // running on main thread because of a bug in older android versions
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(applicationContext, "connection successful", Toast.LENGTH_LONG).show()
                        // results are sent to onServicesDiscovered()
                        gatt.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Successfully disconnected from $deviceAddress")
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(applicationContext, "disconnection successful", Toast.LENGTH_LONG).show()
                    }
                    gatt.close()
                    blestate = BLESTATE.START
                }
            } else {
                Log.e(TAG, "Error $status encountered for $deviceAddress! Disconnecting...")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(applicationContext, status, Toast.LENGTH_LONG).show()
                }
                result = null
                stopBleScan()
                gatt.close()
                blestate = BLESTATE.START
            }
        }

        override fun onServicesDiscovered(bluetoothGatt: BluetoothGatt, status: Int) {
            this@OBSServicee.gatt = bluetoothGatt
            // Log.w(TAG, "Discovered ${gatt.services.size} services for ${gatt.device.address}")
            val obsService = gatt.getService(OBS_SERVICE_UUID)
            // val sensorDistanceCharacteristic = obsService.getCharacteristic(SENSOR_DISTANCE_CHARACTERISTIC_UUID)
            val closePassCharacteristic = obsService.getCharacteristic(CLOSE_PASS_CHARACTERISTIC_UUID)

            // enableNotifications(sensorDistanceCharacteristic)
            enableNotifications(closePassCharacteristic)
            blestate = BLESTATE.DISCONNECT
            Log.d(TAG, "connection setup complete.")
            // Consider connection setup as complete here

        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic) {
                var intent: Intent? = null
                /*if (uuid == SENSOR_DISTANCE_CHARACTERISTIC_UUID) {
                    val byteBuffer: ByteBuffer = ByteBuffer.wrap(value)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    val timerTickMs = byteBuffer.int
                    val distanceLeftCm = byteBuffer.short
                    val distanceRightCm = byteBuffer.short
                    Log.d(TAG, "$timerTickMs $distanceLeftCm $distanceRightCm")
                    intent = Intent("sensorDistanceNotification")
                    intent.putExtra("leftSensor", distanceLeftCm)
                    intent.putExtra("rightSensor", distanceRightCm)
                }
                else */if (uuid == CLOSE_PASS_CHARACTERISTIC_UUID) {
                val byteBuffer: ByteBuffer = ByteBuffer.wrap(value)
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                val timerTickMs = byteBuffer.int
                val distanceLeftCm = byteBuffer.short
                val distanceRightCm = byteBuffer.short
                intent = Intent("closePassNotification")
                intent.putExtra("leftSensor", distanceLeftCm)
                intent.putExtra("rightSensor", distanceRightCm)
            }
                if (intent != null) {
                    applicationContext.sendBroadcast(intent)
                    readObsTime()
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        if (uuid == TIME_CHARACTERISTIC_UUID) {
                            val byteBuffer: ByteBuffer = ByteBuffer.wrap(value)
                            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                            val timeMS = byteBuffer.int
                            Log.d(TAG, "Time: $uuid:\n$timeMS")
                            val intent = Intent("timeNotification")
                            intent.putExtra("time", timeMS)
                            applicationContext.sendBroadcast(intent)
                        } else {

                        }

                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.e(TAG, "Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.e(TAG, "Characteristic read failed for $uuid, error: $status")
                    }
                }
            }
        }
    }

    fun BluetoothGattCharacteristic.isReadable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

    fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

    fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return properties and property != 0
    }

    fun updateState(newState: BLESTATE) {
        val intent = Intent("stateChanged")
        intent.putExtra("BLESTATE", newState)
        applicationContext.sendBroadcast(intent)
    }

    fun readObsTime() {
        val timeChar = gatt
            .getService(OBS_SERVICE_UUID)?.getCharacteristic(TIME_CHARACTERISTIC_UUID)
        if (timeChar?.isReadable() == true) {
            gatt.readCharacteristic(timeChar)
        }
    }

    fun connectDevice(baseContext: Context?) {
        Log.d(TAG, "connecting to device")
        result?.device?.connectGatt(baseContext, false, callback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnectDevice() {
        Log.d(TAG, "disconnecting from device")
        gatt.disconnect()
    }

    inner class LocalBinder : Binder() {
        fun getService() : OBSServicee {
            return this@OBSServicee
        }
    }

    enum class BLESTATE {
        START, STOP, CONNECT, DISCONNECT
    }



}