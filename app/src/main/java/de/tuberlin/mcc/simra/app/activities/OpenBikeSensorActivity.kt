package de.tuberlin.mcc.simra.app.activities

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import de.tuberlin.mcc.simra.app.R
import de.tuberlin.mcc.simra.app.databinding.ActivityOpenbikesensorBinding
import de.tuberlin.mcc.simra.app.util.BaseActivity
import de.tuberlin.mcc.simra.app.util.ConnectionManager
import de.tuberlin.mcc.simra.app.util.ConnectionManager.BLESTATE
import de.tuberlin.mcc.simra.app.util.ConnectionManager.CLOSE_PASS_CHARACTERISTIC_UUID
import de.tuberlin.mcc.simra.app.util.ConnectionManager.SENSOR_DISTANCE_CHARACTERISTIC_UUID
import de.tuberlin.mcc.simra.app.util.PermissionHelper.REQUEST_ENABLE_BT
import de.tuberlin.mcc.simra.app.util.PermissionHelper.hasBLEPermissions
import de.tuberlin.mcc.simra.app.util.PermissionHelper.requestBlePermissions
import de.tuberlin.mcc.simra.app.util.SharedPref
import de.tuberlin.mcc.simra.app.util.Utils.activityResultLauncher
import de.tuberlin.mcc.simra.app.util.ble.ConnectionEventListener
import java.util.TreeSet

private const val TAG = "OpenBikeSensorActivityCM_LOG"

class OpenBikeSensorActivity : BaseActivity() {
    private lateinit var binding: ActivityOpenbikesensorBinding

    private val notifyingCharacteristicsToSubscribeTo =
        listOf(SENSOR_DISTANCE_CHARACTERISTIC_UUID, CLOSE_PASS_CHARACTERISTIC_UUID)
    private var deviceName = "---"
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    // Gleitender Median über die gemessenen Distanzen (nur Anzeige)
    private val movingMedian = MovingMedian()

    private fun updateUI() {
        runOnUiThread {
            Log.d(TAG, "updateUI() - blestate: ${ConnectionManager.bleState}")
            when (ConnectionManager.bleState) {
                BLESTATE.DISCONNECTED -> {
                    binding.bluetoothButton.text =
                        getString(R.string.obs_activity_button_start_scan)
                    binding.statusText.text = getString(R.string.obs_activity_text_start)
                }

                BLESTATE.FOUND -> {
                    binding.bluetoothButton.text =
                        getString(R.string.obs_activity_button_connect_device)
                    binding.statusText.text =
                        getString(R.string.obs_activity_text_connect, deviceName)
                }

                BLESTATE.CONNECTED -> {
                    binding.bluetoothButton.text =
                        getString(R.string.obs_activity_button_disconnect_device)
                    binding.statusText.text =
                        getString(R.string.obs_activity_text_disconnect, deviceName)
                }

                else -> {
                    binding.bluetoothButton.text =
                        getString(R.string.obs_activity_button_stop_scan)
                    binding.statusText.text = getString(R.string.obs_activity_text_stop)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityResultLauncher = activityResultLauncher(this@OpenBikeSensorActivity)
        binding = ActivityOpenbikesensorBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        initializeToolBar()
        if (ConnectionManager.bleState == BLESTATE.CONNECTED) {
            deviceName = ConnectionManager.scanResult.device.name
            updateUI()
        }

        binding.bluetoothButton.setOnClickListener {
            Log.d(TAG, "pressed button. blestate: ${ConnectionManager.bleState}")
            when (ConnectionManager.bleState) {
                BLESTATE.DISCONNECTED -> {
                    if (!hasBLEPermissions(this)) {
                        requestBlePermissions(this@OpenBikeSensorActivity, REQUEST_ENABLE_BT)
                    } else {
                        ConnectionManager.startScan(this)
                    }
                }

                BLESTATE.FOUND -> ConnectionManager.connect(
                    notifyingCharacteristicsToSubscribeTo,
                    this
                )

                BLESTATE.CONNECTED -> ConnectionManager.disconnect(ConnectionManager.scanResult.device)
                else -> ConnectionManager.stopScan()
            }
        }

        // handlebar width
        binding.handleBarWidth.maxValue = 60
        binding.handleBarWidth.minValue = 0
        binding.handleBarWidth.value =
            SharedPref.Settings.Ride.OvertakeWidth.getHandlebarWidth(this)
        binding.handleBarWidth.setOnValueChangedListener { _, _, newVal ->
            SharedPref.Settings.Ride.OvertakeWidth.setTotalWidthThroughHandlebarWidth(
                newVal,
                this
            )
        }
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onResume() {
        super.onResume()

        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
        ConnectionManager.registerListener(connectionEventListener)
    }

    override fun onPause() {
        super.onPause()
        ConnectionManager.unregisterListener(connectionEventListener)
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activityResultLauncher.launch(enableBtIntent)
        }
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onScanStart = {
                Log.d(TAG, "connectionEventListener: onScanStart")
                updateUI()
            }
            onDeviceFound = {
                Log.d(TAG, "connectionEventListener: onDeviceFound")
                deviceName = it.name
                updateUI()
            }
            onScanStop = {
                Log.d(TAG, "connectionEventListener: onScanStop")
                if (!it) {
                    updateUI()
                }
            }
            onConnectionSetupComplete = {
                Log.d(TAG, "connectionEventListener: onConnectionSetupComplete")
                deviceName = it.device.name
                SharedPref.App.OpenBikeSensor.setObsDeviceName(
                    deviceName,
                    this@OpenBikeSensorActivity
                )
                updateUI()
            }
            onDisconnect = {
                Log.d(TAG, "connectionEventListener: onDisconnect")
                deviceName = "---"
                SharedPref.App.OpenBikeSensor.deleteObsDeviceName(this@OpenBikeSensorActivity)
                updateUI()
            }
            onSensorDistanceNotification = {
                // it.leftDistance ist (in cm) – wir glätten die Anzeige mit einem gleitenden Median
                val distanceCm = it.leftDistance.toInt()
                movingMedian.newValue(distanceCm)
                val medianDistance = movingMedian.median

                binding.deviceInfoTextView.text =
                    this@OpenBikeSensorActivity.getString(
                        R.string.obs_activity_text_last_distance,
                        medianDistance
                    )
                setColePassBarColor(medianDistance)
            }
            onClosePassNotification = {
                Log.d(
                    TAG,
                    "ClosePass - time: ${it.obsTime} left distance: ${it.leftDistance} right distance: ${it.rightDistance}"
                )
            }
            onTimeRead = {
                Log.d(TAG, "Time - time: $it")
            }
        }
    }

    private fun setColePassBarColor(distance: Int) {
        val maxColorValue = distance.coerceAtMost(200) // maximum: 200cm (green)
        val normalizedValue = maxColorValue / 2
        val red = (255 * (100 - normalizedValue)) / 100
        val green = (255 * normalizedValue) / 100
        val blue = 0
        // Color and Progress are dependant of distance
        binding.progressBarClosePass.progressTintList =
            ColorStateList.valueOf(Color.rgb(red, green, blue))
        binding.progressBarClosePass.progress = normalizedValue
    }

    private fun initializeToolBar() {
        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        binding.toolbar.toolbar.title = ""
        binding.toolbar.toolbar.subtitle = ""
        binding.toolbar.toolbarTitle.text = getString(R.string.obs)
        binding.toolbar.backButton.setOnClickListener { v -> finish() }
    }

    /**
     * Gleitinenden Median über die letzten Messwerte berechnen.
     * Analog zur MovingMedian-Implementierung in OBSLiteActivity, aber nur für die Anzeige hier.
     */
    class MovingMedian {
        private val TAG = "MovingMedian_LOG"
        var distanceArray: ArrayList<Int> = ArrayList()
        var windowSize = 3
        var median: Int = 0

        // Pair class for the value and its index
        class Pair(private var value: Int, private var index: Int) : Comparable<Pair?> {
            override fun compareTo(other: Pair?): Int {
                return if (index == other?.index) {
                    0
                } else if (value == other?.value) {
                    index.compareTo(other.index)
                } else {
                    value.compareTo(other!!.value)
                }
            }

            fun value(): Int = value

            fun renew(v: Int, p: Int) {
                value = v
                index = p
            }

            override fun toString(): String {
                return String.format("(%d, %d)", value, index)
            }
        }

        // Function to print the median for the current window
        private fun printMedian(
            minSet: TreeSet<Pair?>,
            maxSet: TreeSet<Pair?>,
            window: Int
        ): Int {
            return if (window % 2 == 0) {
                (((minSet.last()!!.value() + maxSet.first()!!.value()) / 2.0).toInt())
            } else {
                if (minSet.size > maxSet.size) minSet.last()!!.value() else maxSet.first()!!.value()
            }
        }

        // Function to find the median of every window of size k
        private fun findMedian(arr: ArrayList<Int>, k: Int): ArrayList<Int> {
            val minSet = TreeSet<Pair?>()
            val maxSet = TreeSet<Pair?>()

            val result: ArrayList<Int> = ArrayList()

            // To hold the pairs, we will keep renewing these instead of creating the new pairs
            val windowPairs = arrayOfNulls<Pair>(k)
            for (i in 0 until k) {
                windowPairs[i] = Pair(arr[i], i)
            }

            // Add k/2 items to maxSet
            for (i in 0 until (k / 2)) {
                maxSet.add(windowPairs[i])
            }
            for (i in k / 2 until k) {
                if (arr[i] < maxSet.first()!!.value()) {
                    minSet.add(windowPairs[i])
                } else {
                    minSet.add(maxSet.pollFirst())
                    maxSet.add(windowPairs[i])
                }
            }
            result.add(printMedian(minSet, maxSet, k))
            for (i in k until arr.size) {
                val temp = windowPairs[i % k]
                if (temp!!.value() <= minSet.last()!!.value()) {
                    minSet.remove(temp)
                    temp.renew(arr[i], i)
                    if (temp.value() < maxSet.first()!!.value()) {
                        minSet.add(temp)
                    } else {
                        minSet.add(maxSet.pollFirst())
                        maxSet.add(temp)
                    }
                } else {
                    maxSet.remove(temp)
                    temp.renew(arr[i], i)
                    if (temp.value() > minSet.last()!!.value()) {
                        maxSet.add(temp)
                    } else {
                        maxSet.add(minSet.pollLast())
                        minSet.add(temp)
                    }
                }
                result.add(printMedian(minSet, maxSet, k))
            }
            return result
        }

        fun newValue(distance: Int) {
            // max array size ist 122 (~ 5 Sekunden), älteste Werte löschen, wenn überschritten
            if (distanceArray.size >= 122) {
                distanceArray = distanceArray.drop(1) as ArrayList<Int>
            }
            distanceArray.add(distance)
            // calculate median only if distanceArray is big enough.
            if (distanceArray.size >= windowSize) {
                val medians = findMedian(distanceArray, windowSize)
                // Wichtig: Median des *letzten* Fensters verwenden, nicht das Minimum aller
                median = medians.last()
            }
        }
    }
}
