/*
 * Copyright 2019 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tuberlin.mcc.simra.app.util

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import de.tuberlin.mcc.simra.app.util.ble.*
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

object ConnectionManager {
    private const val TAG = "ConnectionManager_LOG"
    val OBS_SERVICE_UUID = UUID.fromString("1FE7FAF9-CE63-4236-0004-000000000000")
    val SENSOR_DISTANCE_CHARACTERISTIC_UUID = UUID.fromString("1FE7FAF9-CE63-4236-0004-000000000002")
    val TIME_CHARACTERISTIC_UUID = UUID.fromString("1FE7FAF9-CE63-4236-0004-000000000001")
    val CLOSE_PASS_CHARACTERISTIC_UUID = UUID.fromString("1FE7FAF9-CE63-4236-0004-000000000003")
    val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var listeners: MutableSet<WeakReference<ConnectionEventListener>> = mutableSetOf()

    private val operationQueue = ConcurrentLinkedQueue<BleOperationType>()
    private var pendingOperation: BleOperationType? = null
    private val deviceGattMap = ConcurrentHashMap<BluetoothDevice, BluetoothGatt>()
    private lateinit var notifyingCharacteristicsToSubscribeTo: List<UUID>
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bleScanner: BluetoothLeScanner
    lateinit var scanResult: ScanResult
    var bleState = BLESTATE.DISCONNECTED
    private var isConnected = false
    private var isSearching = false
    private var foundOBS = false
    var startTime = 0L

    fun registerListener(listener: ConnectionEventListener) {
        if (listeners.map { it.get() }.contains(listener)) { return }
        listeners.add(WeakReference(listener))
        listeners = listeners.filter { it.get() != null }.toMutableSet()
        // Log.d(TAG, "Added listener $listener, ${listeners.size} listeners total")
    }

    fun unregisterListener(listener: ConnectionEventListener) {
        // Removing elements while in a loop results in a java.util.ConcurrentModificationException
        var toRemove: WeakReference<ConnectionEventListener>? = null
        listeners.forEach {
            if (it.get() == listener) {
                toRemove = it
            }
        }
        toRemove?.let {
            listeners.remove(it)
            // Log.d(TAG, "Removed listener ${it.get()}, ${listeners.size} listeners total")
        }
    }

    fun startScan(context: Context) {
        if (isConnected) {
            Log.e(TAG, "Already connected to an OpenBikeSensor")
        } else if (isSearching) {
            Log.e(TAG, "Already searching for an OpenBikeSensor")
        } else {
            foundOBS = false
            enqueueOperation(StartScan(null,context))
        }
    }

    fun stopScan() {
        if (!isSearching) {
            Log.e(TAG, "There is no search to be stopped")
        } else {
            bleState = if (isConnected) BLESTATE.CONNECTED else BLESTATE.DISCONNECTED
            isSearching = false
            if (pendingOperation is StartScan) {
                pendingOperation = null
                if (operationQueue.isNotEmpty()) {
                    doNextOperation()
                }
            }
            bleScanner.stopScan(scanCallback)
            listeners.forEach { it.get()?.onScanStop?.invoke(foundOBS) }
        }
    }

    fun connect(notifyingCharacteristicsToSubscribeTo: List<UUID>, context: Context) {
        if (scanResult.device.isConnected()) {
            Log.e(TAG,"Already connected to ${scanResult.device.address}!")
        } else {
            this@ConnectionManager.notifyingCharacteristicsToSubscribeTo = notifyingCharacteristicsToSubscribeTo
            enqueueOperation(Connect(scanResult.device, context.applicationContext))
        }
    }

    fun disconnect(device: BluetoothDevice) {
        if (device.isConnected()) {
            enqueueOperation(Disconnect(device))
        } else {
            Log.e(TAG,"Not connected to ${device.address}, cannot teardown connection!")
        }
    }

    fun subscribeToSensorDistance() {
        if (scanResult.device.isConnected()) {
            enqueueOperation(EnableNotifications(scanResult.device, SENSOR_DISTANCE_CHARACTERISTIC_UUID))
        } else {
            Log.e(TAG,"Not connected to ${scanResult.device.address}, cannot enable notifications")
        }
    }

    fun subscribeToClosePass() {
        if (scanResult.device.isConnected()) {
            enqueueOperation(EnableNotifications(scanResult.device, CLOSE_PASS_CHARACTERISTIC_UUID))
        } else {
            Log.e(TAG,"Not connected to ${scanResult.device.address}, cannot enable notifications")
        }
    }

    fun readTime() {
        if (scanResult.device.isConnected()) {
            enqueueOperation(CharacteristicRead(scanResult.device, TIME_CHARACTERISTIC_UUID))
        } else {
            Log.e(TAG,"Not connected to ${scanResult.device.address}, cannot perform characteristic read")
        }
    }



    fun readCharacteristic(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic) {
        if (device.isConnected() && characteristic.isReadable()) {
            enqueueOperation(CharacteristicRead(device, characteristic.uuid))
        } else if (!characteristic.isReadable()) {
            Log.e(TAG,"Attempting to read ${characteristic.uuid} that isn't readable!")
        } else if (!device.isConnected()) {
            Log.e(TAG,"Not connected to ${device.address}, cannot perform characteristic read")
        }
    }

    fun readDescriptor(device: BluetoothDevice, descriptor: BluetoothGattDescriptor) {
        if (device.isConnected() && descriptor.isReadable()) {
            enqueueOperation(DescriptorRead(device, descriptor.uuid))
        } else if (!descriptor.isReadable()) {
            Log.e(TAG, "Attempting to read ${descriptor.uuid} that isn't readable!")
        } else if (!device.isConnected()) {
            Log.e(TAG,"Not connected to ${device.address}, cannot perform descriptor read")
        }
    }

    fun writeDescriptor(device: BluetoothDevice, descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        if (device.isConnected() && (descriptor.isWritable() || descriptor.isCccd())) {
            enqueueOperation(DescriptorWrite(device, descriptor.uuid, descriptor, payload))
        } else if (!device.isConnected()) {
            Log.e(TAG,"Not connected to ${device.address}, cannot perform descriptor write")
        } else if (!descriptor.isWritable() && !descriptor.isCccd()) {
            Log.e(TAG,"Descriptor ${descriptor.uuid} cannot be written to")
        }
    }

    fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        val device = scanResult.device
        if (device.isConnected() && (characteristic.isNotifiable())) {
            enqueueOperation(EnableNotifications(device, characteristic.uuid))
        } else if (!device.isConnected()) {
            Log.e(TAG,"Not connected to ${device.address}, cannot enable notifications")
        } else if (!characteristic.isNotifiable()) {
            Log.e(TAG,"Characteristic ${characteristic.uuid} doesn't support notifications")
        }
    }

    fun disableNotifications(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic) {
        if (device.isConnected() && (characteristic.isNotifiable())) {
            enqueueOperation(DisableNotifications(device, characteristic.uuid))
        } else if (!device.isConnected()) {
            Log.e(TAG,"Not connected to ${device.address}, cannot disable notifications")
        } else if (!characteristic.isNotifiable()) {
            Log.e(TAG,"Characteristic ${characteristic.uuid} doesn't support notifications")
        }
    }
    @Synchronized
    private fun enqueueOperation(operation: BleOperationType) {
        operationQueue.add(operation)
        if (pendingOperation == null) {
            doNextOperation()
        }
    }

    /**
     * Perform a given [BleOperationType]. All permission checks are performed before an operation
     * can be enqueued by [enqueueOperation].
     */
    @Synchronized
    private fun doNextOperation() {
        if (pendingOperation != null) {
            Log.e(TAG,"doNextOperation() called when an operation is pending! Aborting.")
            return
        }

        val operation = operationQueue.poll() ?: run {
            Log.d(TAG,"Operation queue empty, returning")
            return
        }
        pendingOperation = operation

        if (operation is StartScan) {
            with(operation) {
                Log.d(TAG, "Starting scan")
                isSearching = true
                bleState = BLESTATE.SEARCHING
                listeners.forEach { it.get()?.onScanStart?.invoke(isSearching) }
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                bluetoothAdapter = bluetoothManager.adapter
                bleScanner = bluetoothAdapter.bluetoothLeScanner
                val filter = listOf(
                    ScanFilter.Builder().setServiceUuid(
                        ParcelUuid.fromString(OBS_SERVICE_UUID.toString())
                    ).build()
                )
                val settings = ScanSettings.Builder().setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH).setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
                bleScanner.startScan(filter,settings,scanCallback)
            }
            return
        }

        // Handle Connect separately from other operations that require device to be connected
        if (operation is Connect) {
            with(operation) {
                Log.d(TAG,"Connecting to ${device.address}")
                device.connectGatt(context, false, callback)
            }
            return
        }

        // Check BluetoothGatt availability for other operations
        val gatt = deviceGattMap[operation.device]
            ?: this@ConnectionManager.run {
                Log.e(TAG,"Not connected to ${operation.device?.address}! Aborting $operation operation.")
                signalEndOfOperation()
                return
            }

        // TODO: Make sure each operation ultimately leads to signalEndOfOperation()
        // TODO: Refactor this into an BleOperationType abstract or extension function
        when (operation) {
            is Disconnect -> with(operation) {
                Log.d(TAG,"Disconnecting from ${device.address}")
                isConnected = false
                gatt.close()
                deviceGattMap.remove(device)
                listeners.forEach { it.get()?.onDisconnect?.invoke(device) }
                signalEndOfOperation()
            }
            is CharacteristicRead -> with(operation) {
                gatt.findCharacteristic(characteristicUuid)?.let { characteristic ->
                    gatt.readCharacteristic(characteristic)
                } ?: this@ConnectionManager.run {
                    Log.e(TAG,"Cannot find $characteristicUuid to read from")
                    signalEndOfOperation()
                }
            }
            is DescriptorWrite -> with(operation) {
                gatt.findDescriptor(descriptorUuid)?.let { descriptor ->
                    descriptor.value = payload
                    gatt.writeDescriptor(descriptor)
                } ?: this@ConnectionManager.run {
                    Log.e(TAG,"Cannot find $descriptorUuid to write to")
                    signalEndOfOperation()
                }
            }
            is DescriptorRead -> with(operation) {
                gatt.findDescriptor(descriptorUuid)?.let { descriptor ->
                    gatt.readDescriptor(descriptor)
                } ?: this@ConnectionManager.run {
                    Log.e(TAG,"Cannot find $descriptorUuid to read from")
                    signalEndOfOperation()
                }
            }
            is EnableNotifications -> with(operation) {
                gatt.findCharacteristic(characteristicUuid)?.let { characteristic ->
                    val payload = when {
                        characteristic.isNotifiable() ->
                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        else ->
                            error("${characteristic.uuid} doesn't support notifications")
                    }

                    characteristic.getDescriptor(CCCD_UUID)?.let { cccDescriptor ->
                        if (!gatt.setCharacteristicNotification(characteristic, true)) {
                            Log.e(TAG,"setCharacteristicNotification failed for ${characteristic.uuid}")
                            signalEndOfOperation()
                            return
                        }

                        cccDescriptor.value = payload
                        gatt.writeDescriptor(cccDescriptor)
                    } ?: this@ConnectionManager.run {
                        Log.e(TAG,"${characteristic.uuid} doesn't contain the CCC descriptor!")
                        signalEndOfOperation()
                    }
                } ?: this@ConnectionManager.run {
                    Log.e(TAG,"Cannot find $characteristicUuid! Failed to enable notifications.")
                    signalEndOfOperation()
                }
            }
            is DisableNotifications -> with(operation) {
                gatt.findCharacteristic(characteristicUuid)?.let { characteristic ->
                    characteristic.getDescriptor(CCCD_UUID)?.let { cccDescriptor ->
                        if (!gatt.setCharacteristicNotification(characteristic, false)) {
                            Log.e(TAG,"setCharacteristicNotification failed for ${characteristic.uuid}")
                            signalEndOfOperation()
                            return
                        }

                        cccDescriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(cccDescriptor)
                    } ?: this@ConnectionManager.run {
                        Log.e(TAG,"${characteristic.uuid} doesn't contain the CCC descriptor!")
                        signalEndOfOperation()
                    }
                } ?: this@ConnectionManager.run {
                    Log.e(TAG,"Cannot find $characteristicUuid! Failed to disable notifications.")
                    signalEndOfOperation()
                }
            }
            else -> {
                Log.e(TAG, "$operation unknown")
                signalEndOfOperation()
            }
        }
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // Log.d(TAG,"onConnectionStateChange: connected to $deviceAddress")
                    deviceGattMap[gatt.device] = gatt
                    isConnected = true
                    bleState = BLESTATE.FOUND
                    Handler(Looper.getMainLooper()).post {
                        gatt.discoverServices() // callback -> onServicesDiscovered()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // Log.e(TAG,"onConnectionStateChange: disconnected from $deviceAddress")
                    disconnect(gatt.device)
                    isConnected = false
                    bleState = BLESTATE.DISCONNECTED
                }
            } else {
                isConnected = false
                bleState = BLESTATE.DISCONNECTED
                // Log.e(TAG,"onConnectionStateChange: status $status encountered for $deviceAddress!")
                if (pendingOperation is Connect) {
                    signalEndOfOperation()
                }
                disconnect(gatt.device)
                listeners.forEach { it.get()?.onConnectionFailed?.invoke(gatt.device)}
            }
        }

        /**
         * Callback of gatt.discoverServices(). Subscribes to requested notifying characteristics
         * and notifies onConnectionSetupComplete listeners
         */
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    bleState = BLESTATE.CONNECTING
                    // Log.d(TAG,"Discovered ${services.size} services for ${device.address}.")
                    subscribeToRequestedNotifyingCharacteristics(gatt)
                    // listeners.forEach { it.get()?.onConnectionSetupComplete?.invoke(this) }
                    readTime()
                } else {
                    Log.e(TAG,"Service discovery failed due to status $status")
                    disconnect(gatt.device)
                }
            }

            if (pendingOperation is Connect) {
                signalEndOfOperation()
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        // Log.i(TAG,"Read characteristic $uuid | value: $value")
                        listeners.forEach { it.get()?.onCharacteristicRead?.invoke(gatt.device, this)}
                        if (uuid == TIME_CHARACTERISTIC_UUID) {
                            val measurement = Measurement(value)
                            listeners.forEach { it.get()?.onTimeRead?.invoke(measurement.obsTime) }
                            if(startTime == 0L) {
                                startTime = System.currentTimeMillis() - measurement.obsTime
                                // Log.d(TAG, "Calculated startTime: $startTime")
                            } else {
                                // Log.d(TAG, "startTime already set to $startTime")
                            }
                        } else {
                            Log.e(TAG, "Unknown UUID")
                        }
                        bleState = BLESTATE.CONNECTED
                        listeners.forEach { it.get()?.onConnectionSetupComplete?.invoke(gatt) }
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.e(TAG,"Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.e(TAG,"Characteristic read failed for $uuid, error: $status")
                    }
                }
            }

            if (pendingOperation is CharacteristicRead) {
                signalEndOfOperation()
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            with(characteristic) {
                // Log.i(TAG,"Characteristic $uuid changed | value: ${value.toHexString()}")
                if (uuid == SENSOR_DISTANCE_CHARACTERISTIC_UUID) {
                    val measurement = Measurement(value)
                    listeners.forEach { it.get()?.onSensorDistanceNotification?.invoke(measurement) }
                } else if (uuid == CLOSE_PASS_CHARACTERISTIC_UUID) {
                    val measurement = Measurement(value)
                    listeners.forEach { it.get()?.onClosePassNotification?.invoke(measurement) }
                }
            }
        }

        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            with(descriptor) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        // Log.i(TAG,"Read descriptor $uuid | value: $value")
                        listeners.forEach { it.get()?.onDescriptorRead?.invoke(gatt.device, this) }
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.e(TAG,"Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.e(TAG,"Descriptor read failed for $uuid, error: $status")
                    }
                }
            }

            if (pendingOperation is DescriptorRead) {
                signalEndOfOperation()
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            with(descriptor) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        // Log.d(TAG,"Wrote to descriptor $uuid | value: $value")

                        if (isCccd()) {
                            onCccdWrite(gatt, value, characteristic)
                        } else {
                            listeners.forEach { it.get()?.onDescriptorWrite?.invoke(gatt.device, this) }
                        }
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.e(TAG,"Write not permitted for $uuid!")
                    }
                    else -> {
                        Log.e(TAG,"Descriptor write failed for $uuid, error: $status")
                    }
                }
            }

            if (descriptor.isCccd() &&
                (pendingOperation is EnableNotifications || pendingOperation is DisableNotifications)
            ) {
                signalEndOfOperation()
            } else if (!descriptor.isCccd() && pendingOperation is DescriptorWrite) {
                signalEndOfOperation()
            }
        }

        private fun onCccdWrite(gatt: BluetoothGatt, value: ByteArray, characteristic: BluetoothGattCharacteristic) {
            val charUuid = characteristic.uuid
            val notificationsEnabled =
                value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ||
                        value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
            val notificationsDisabled =
                value.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)

            when {
                notificationsEnabled -> {
                    // Log.d(TAG,"Notifications or indications ENABLED on $charUuid")
                    listeners.forEach {
                        it.get()?.onNotificationsEnabled?.invoke(
                            gatt.device,
                            characteristic
                        )
                    }
                }
                notificationsDisabled -> {
                    // Log.d(TAG,"Notifications or indications DISABLED on $charUuid")
                    listeners.forEach {
                        it.get()?.onNotificationsDisabled?.invoke(
                            gatt.device,
                            characteristic
                        )
                    }
                }
                else -> {
                    Log.e(TAG,"Unexpected value $value on CCCD of $charUuid")
                }
            }
        }
    }

    private fun subscribeToRequestedNotifyingCharacteristics(gatt:BluetoothGatt) {
        if (gatt.services.isEmpty()) {
            Log.e(TAG,"No service and characteristic available, call discoverServices() first?")
            return
        }
        gatt.services.forEach { service ->
            service.characteristics.forEach { characteristic ->
                notifyingCharacteristicsToSubscribeTo.forEach {
                    if (characteristic.uuid == it) {
                        Log.d(TAG, "Enabling notifications of Characteristic with UUID: $it")
                        enableNotifications(characteristic)
                    }
                }
            }
        }


    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            with(result.device) {
                if (name != null && name.contains("OpenBikeSensor")) {
                    stopScan()
                    signalEndOfOperation()
                    foundOBS = true
                    bleState = BLESTATE.FOUND
                    // Log.d(TAG, "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address, bondState: $bondState, type: $type")
                    scanResult = result
                    // Log.d(TAG, "listeners.size: ${listeners.size}")
                    listeners.forEach {
                        it.get()?.onDeviceFound?.invoke(this)
                    }

                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG,"onScanFailed: code $errorCode")
            signalEndOfOperation()
        }
    }

    @Synchronized
    private fun signalEndOfOperation() {
        // Log.d(TAG,"End of $pendingOperation")
        pendingOperation = null
        if (operationQueue.isNotEmpty()) {
            doNextOperation()
        }
    }

    sealed class BleOperationType {
        abstract val device: BluetoothDevice?
    }

    data class StartScan(override val device: BluetoothDevice?, val context: Context) : BleOperationType()
    data class Connect(override val device: BluetoothDevice, val context: Context) : BleOperationType()
    data class Disconnect(override val device: BluetoothDevice) : BleOperationType()
    data class CharacteristicRead(override val device: BluetoothDevice, val characteristicUuid: UUID) : BleOperationType()
    data class DescriptorWrite(override val device: BluetoothDevice, val descriptorUuid: UUID, val descriptor: BluetoothGattDescriptor, val payload: ByteArray) : BleOperationType()
    /** Read the value of a descriptor represented by [descriptorUuid] */
    data class DescriptorRead(override val device: BluetoothDevice, val descriptorUuid: UUID) : BleOperationType()
    /** Enable notifications/indications on a characteristic represented by [characteristicUuid] */
    data class EnableNotifications(override val device: BluetoothDevice, val characteristicUuid: UUID) : BleOperationType()
    /** Disable notifications/indications on a characteristic represented by [characteristicUuid] */
    data class DisableNotifications(override val device: BluetoothDevice, val characteristicUuid: UUID) : BleOperationType()

    class Measurement(value: ByteArray) {
        var obsTime: Int = 0
        var realTime: Long = 0
        var leftDistance: Short = 0
        var rightDistance: Short = 0

        init {
            val byteBuffer = ByteBuffer.wrap(value)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
            obsTime = byteBuffer.int
            if (byteBuffer.remaining() > 0) {
                leftDistance = byteBuffer.short
                rightDistance = byteBuffer.short
            }
            realTime = startTime + obsTime

        }

        fun getIncidentDescription(context: Context): String {
            val headerLine = context.getString(de.tuberlin.mcc.simra.app.R.string.obsIncidentDescriptionButtonHeaderLine)
            val leftValue: String = if (leftDistance <= 1500) {
                "$leftDistance cm"
            } else {
                "---"
            }
            val rightValue: String = if (rightDistance <= 1500) {
                "$rightDistance cm"
            } else {
                "---"
            }
            val leftLine = context.getString(de.tuberlin.mcc.simra.app.R.string.left) + leftValue
            val rightLine = context.getString(de.tuberlin.mcc.simra.app.R.string.right) + rightValue
            return headerLine + System.lineSeparator() + leftLine + System.lineSeparator() + rightLine
        }
    }

    private fun BluetoothDevice.isConnected() = deviceGattMap.containsKey(this)

    enum class BLESTATE {
        DISCONNECTED, SEARCHING, FOUND, CONNECTING, CONNECTED
    }

}