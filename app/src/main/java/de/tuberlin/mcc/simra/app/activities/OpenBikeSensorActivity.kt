package de.tuberlin.mcc.simra.app.activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
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
import de.tuberlin.mcc.simra.app.util.Utils
import de.tuberlin.mcc.simra.app.util.Utils.activityResultLauncher
import de.tuberlin.mcc.simra.app.util.ble.ConnectionEventListener


private const val TAG = "OpenBikeSensorActivityCM_LOG"
private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1

class OpenBikeSensorActivity : BaseActivity() {
    private lateinit var binding: ActivityOpenbikesensorBinding


    private val notifyingCharacteristicsToSubscribeTo = listOf(SENSOR_DISTANCE_CHARACTERISTIC_UUID, CLOSE_PASS_CHARACTERISTIC_UUID)
    private var blestate = BLESTATE.DISCONNECTED
    private var deviceName = "---"
    private lateinit var activityResultLauncher:ActivityResultLauncher<Intent>


    private fun updateUI() {
        runOnUiThread {
            Log.d(TAG, "updateUI() - blestate: $blestate")
            when (blestate) {
                BLESTATE.DISCONNECTED -> {
                    binding.bluetoothButton.text = getString(R.string.obs_activity_button_start_scan)
                    binding.statusText.text = getString(R.string.obs_activity_text_start)
                }
                BLESTATE.FOUND -> {
                    binding.bluetoothButton.text = getString(R.string.obs_activity_button_connect_device)
                    binding.statusText.text = getString(R.string.obs_activity_text_connect,deviceName)
                }
                BLESTATE.CONNECTED -> {
                    binding.bluetoothButton.text = getString(R.string.obs_activity_button_disconnect_device)
                    binding.statusText.text = getString(R.string.obs_activity_text_disconnect,deviceName)
                }
                else -> {
                    binding.bluetoothButton.text = getString(R.string.obs_activity_button_stop_scan)
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
            blestate = BLESTATE.CONNECTED
            deviceName = ConnectionManager.scanResult.device.name
            updateUI()
        }

        binding.bluetoothButton.setOnClickListener {

            Log.d(TAG, "pressed button. blestate: $blestate")
            when (blestate) {
                BLESTATE.DISCONNECTED -> {
                    if (!hasBLEPermissions(this)) {
                        requestBlePermissions(this@OpenBikeSensorActivity, REQUEST_ENABLE_BT)
                    } else {
                        ConnectionManager.startScan(this)
                    }
                }
                BLESTATE.FOUND -> ConnectionManager.connect(notifyingCharacteristicsToSubscribeTo,this)
                BLESTATE.CONNECTED -> ConnectionManager.disconnect(ConnectionManager.scanResult.device)
                else -> ConnectionManager.stopScan()
            }
        }

        // handlebar width
        binding.handleBarWidth.maxValue = 60
        binding.handleBarWidth.minValue = 0
        binding.handleBarWidth.value = SharedPref.Settings.Ride.OvertakeWidth.getHandlebarWidth(this)
        binding.handleBarWidth.setOnValueChangedListener { picker, oldVal, newVal ->
            SharedPref.Settings.Ride.OvertakeWidth.setTotalWidthThroughHandlebarWidth(newVal, this)
        }
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
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
                blestate = BLESTATE.SEARCHING
                updateUI()
            }
            onDeviceFound = {
                Log.d(TAG, "connectionEventListener: onDeviceFound")
                deviceName = it.name
                blestate = BLESTATE.FOUND
                updateUI()
            }
            onScanStop = {
                Log.d(TAG, "connectionEventListener: onScanStop")
                if (!it) {
                    blestate = BLESTATE.DISCONNECTED
                    updateUI()
                }
            }
            onConnectionSetupComplete = {
                Log.d(TAG, "connectionEventListener: onConnectionSetupComplete")
                deviceName = it.device.name
                SharedPref.App.OpenBikeSensor.setObsDeviceName(deviceName, this@OpenBikeSensorActivity)
                blestate = BLESTATE.CONNECTED
                updateUI()
            }
            onDisconnect = {
                Log.d(TAG, "connectionEventListener: onDisconnect")
                deviceName = "---"
                SharedPref.App.OpenBikeSensor.deleteObsDeviceName(this@OpenBikeSensorActivity)
                blestate = BLESTATE.DISCONNECTED
                updateUI()
            }
            onSensorDistanceNotification = {
                binding.deviceInfoTextView.text = this@OpenBikeSensorActivity.getString(R.string.obs_activity_text_last_distance,it.leftDistance)
                setColePassBarColor(it.leftDistance.toInt())
            }
            onClosePassNotification = {
                Log.d(TAG, "ClosePass - time: ${it.obsTime} left distance: ${it.leftDistance} right distance: ${it.rightDistance}")
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
        binding.progressBarClosePass.progressTintList = ColorStateList.valueOf(Color.rgb(red,green,blue))
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

}