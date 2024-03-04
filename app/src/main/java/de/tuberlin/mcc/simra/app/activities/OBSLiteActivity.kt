package de.tuberlin.mcc.simra.app.activities

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import com.google.protobuf.InvalidProtocolBufferException
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import de.tuberlin.mcc.simra.app.Event
import de.tuberlin.mcc.simra.app.R
import de.tuberlin.mcc.simra.app.databinding.ActivityObsliteBinding
import de.tuberlin.mcc.simra.app.services.RecorderService
import de.tuberlin.mcc.simra.app.util.BaseActivity
import de.tuberlin.mcc.simra.app.util.CobsUtils
import de.tuberlin.mcc.simra.app.util.SharedPref
import de.tuberlin.mcc.simra.app.util.Utils
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.util.LinkedList
import java.util.concurrent.ConcurrentLinkedDeque


class OBSLiteActivity : BaseActivity(), SerialInputOutputManager.Listener {
    private lateinit var binding: ActivityObsliteBinding
    private val TAG = "OBSLiteActivity_LOG"

    private var deviceName = "---"
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    var obsLiteEnabled = false
    private lateinit var port: UsbSerialPort
    private var obsLiteConnected = false
    private var usbManager: UsbManager? = null
    var byteListQueue = ConcurrentLinkedDeque<LinkedList<Byte>>()
    var lastByteRead: Byte? = null
    private var usbDevice: UsbDevice? = null
    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    private var permissionIntent: PendingIntent? = null
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "action: ${intent.action}")
            if (ACTION_USB_PERMISSION == intent.action) {

                // synchronized(this) {
                    val device: UsbDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                UsbManager.EXTRA_DEVICE,
                                UsbDevice::class.java
                            )
                        } else {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }

                    Log.d(TAG, "device: $device")
                    Log.d(
                        TAG,
                        "UsbManager.EXTRA_PERMISSION_GRANTED: ${
                            intent.getBooleanExtra(
                                UsbManager.EXTRA_PERMISSION_GRANTED,
                                false
                            )
                        }"
                    )
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {
                            usbDevice = device
                            binding.obsLiteMainView.visibility = View.VISIBLE
                            binding.loadingAnimationLayout.visibility = View.GONE
                            obsLiteConnected = true
                            updateButton()

                            val availableDrivers =
                                UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
                            if (availableDrivers.isNotEmpty()) {
                                val driver = availableDrivers[0]
                                val connection = usbManager!!.openDevice(driver.device)
                                port = driver.ports[0] // Most devices have just one port
                                try {
                                    port.open(connection)
                                    port.setParameters(
                                        115200,
                                        8,
                                        UsbSerialPort.STOPBITS_1,
                                        UsbSerialPort.PARITY_NONE
                                    )
                                    Log.d(RecorderService.TAG, "usb serial port opened")
                                    val usbIoManager =
                                        SerialInputOutputManager(port, this@OBSLiteActivity)
                                    // usbIoManager!!.run()
                                    usbIoManager.start()
                                } catch (e: IOException) {
                                    throw RuntimeException(e)
                                }


                            }

                        }
                    } else {
                        Log.d(TAG, "permission denied for device $device")
                    }
                // }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityResultLauncher = Utils.activityResultLauncher(this@OBSLiteActivity)
        binding = ActivityObsliteBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        initializeToolBar()

        // OBS-Lite
        obsLiteEnabled = SharedPref.Settings.OBSLite.isEnabled(this)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val explicitIntent = Intent(ACTION_USB_PERMISSION)
        explicitIntent.setPackage(this.packageName);


        permissionIntent = PendingIntent.getBroadcast(this, 0, explicitIntent,
            PendingIntent.FLAG_MUTABLE)
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.registerReceiver(usbReceiver, filter, RECEIVER_EXPORTED)
        } else {
            this.registerReceiver(usbReceiver, filter)
        }


        updateButton()

        // handlebar width
        binding.handleBarWidth.maxValue = 60
        binding.handleBarWidth.minValue = 0
        binding.handleBarWidth.value =
            SharedPref.Settings.Ride.OvertakeWidth.getHandlebarWidth(this)
        binding.handleBarWidth.setOnValueChangedListener { picker, oldVal, newVal ->
            SharedPref.Settings.Ride.OvertakeWidth.setTotalWidthThroughHandlebarWidth(newVal, this)
        }
    }

    private fun getOBSLitePermission() {


        val deviceList = usbManager?.getDeviceList()
        deviceList?.values?.forEach { device ->
            Log.d(TAG, "deviceList device $device")
            usbDevice = device
        }


        usbManager?.requestPermission(usbDevice, permissionIntent)

        /*usbManager = getSystemService(USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

        if (availableDrivers.isNotEmpty()) {
            val manager = getSystemService(Context.USB_SERVICE) as UsbManager
            permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)

            *//*obsLiteConnection = usbManager!!.openDevice(driver.device)
            if (obsLiteConnection == null) {
                val filter = IntentFilter(ACTION_USB_PERMISSION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    this.registerReceiver(usbReceiver, filter, RECEIVER_EXPORTED)
                } else {
                    this.registerReceiver(usbReceiver, filter)
                }
                val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)
                usbManager!!.requestPermission(driver.device, permissionIntent)
            }*//*
        }*/
    }

    private fun disconnectOBSLite() {
        try {
            usbDevice = null
            if (port.isOpen) {
                port.close()
            }
            obsLiteConnected = false
            updateButton()
        } catch (e: InvocationTargetException) {
            e.cause?.printStackTrace()
        }
    }


    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectOBSLite()
    }

    private fun setColePassBarColor(distance: Int, progressBar: ProgressBar) {
        val maxColorValue = distance.coerceAtMost(200) // maximum: 200cm (green)
        val normalizedValue = maxColorValue / 2
        val red = (255 * (100 - normalizedValue)) / 100
        val green = (255 * normalizedValue) / 100
        val blue = 0
        // Color and Progress are dependant of distance
        progressBar.progressTintList =
            ColorStateList.valueOf(Color.rgb(red, green, blue))
        progressBar.progress = normalizedValue
    }

    private fun initializeToolBar() {
        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        binding.toolbar.toolbar.title = ""
        binding.toolbar.toolbar.subtitle = ""
        binding.toolbar.toolbarTitle.text = getString(R.string.obs)
        binding.toolbar.backButton.setOnClickListener { v -> finish() }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun onNewData(data: ByteArray?) {

        runOnUiThread {

            // Log.d(TAG, "data.toHexString: ${data?.toHexString()}")
            for (datum in data!!) {
                if (lastByteRead?.toInt() == 0x00){
                    val newByteList = LinkedList<Byte>()
                    newByteList.add(datum)
                    byteListQueue.add(newByteList)
                } else {
                    if (byteListQueue.isNotEmpty()) {
                        byteListQueue.last.add(datum)
                    } else {
                        val newByteList = LinkedList<Byte>()
                        newByteList.add(datum)
                        byteListQueue.add(newByteList)
                    }
                }
                lastByteRead = datum
            }
            // Log.d(TAG, "byteListQueue: $byteListQueue")

            val byteListQueueSB = StringBuilder()
            byteListQueueSB.append("[")
            for (byteList in byteListQueue) {
                byteListQueueSB.append("[")
                val sb = StringBuilder()
                for (byte in byteList) {
                    sb.append(String.format("%02x", byte))
                }
                byteListQueueSB.append(sb.toString()).append("], ")
            }
            byteListQueueSB.append("]")
            Log.d(TAG, "byteListQueueSB: $byteListQueueSB")
            var foundZero = false

            val result = StringBuilder()
            val normalizedByteStringBuilder = StringBuilder()

            val byteLinkedList = byteListQueue.peekFirst()
            for (aByte in byteLinkedList!!) {

                result.append("\\x").append(String.format("%02x", aByte))
                normalizedByteStringBuilder.append(String.format("%02x", aByte))
                // Log.d(TAG,String.format("%02x", aByte))
                if (aByte.toInt() == 0x00) {
                    // Log.d(TAG, "found!")
                    foundZero = true
                }
            }
            // Log.d(TAG, "onNewData result: $result")
            // Log.d(TAG, "onNewData normalString: $normalizedByteStringBuilder")
            if (foundZero) {
                    // Log.d(TAG, "chunk: $chunk")
                    // val decodedData = CobsUtils.decode(hexStringToByteArray(chunk))
                    val decodedData = CobsUtils.decode(byteLinkedList.toByteArray())
                    val decodedResult = StringBuilder()
                    for (dByte in decodedData) {
                        decodedResult.append("\\x").append(String.format("%02x", dByte))
                    }
                    // Log.d(TAG, "decoded: $decodedResult")
                    try {
                        val event: Event = Event.parseFrom(decodedData)
                        if (event.hasDistanceMeasurement() && event.distanceMeasurement.distance < 5) {
                            val distance = ((event.distanceMeasurement.distance * 100) + SharedPref.Settings.Ride.OvertakeWidth.getHandlebarWidth(this)).toInt()
                            if (event.distanceMeasurement.sourceId == 1) {
                                Log.d(
                                    TAG,
                                    "left distance: $distance"
                                )
                                binding.leftSensorTextView.text = this@OBSLiteActivity.getString(R.string.obs_lite_text_last_distance_left,distance)
                                setColePassBarColor(distance.toInt(),binding.leftSensorProgressBar)
                            } else {
                                Log.d(
                                    TAG,
                                    "right distance: $distance"
                                )
                                binding.rightSensorTextView.text = this@OBSLiteActivity.getString(R.string.obs_lite_text_last_distance_right,distance)
                                setColePassBarColor(distance.toInt(),binding.rightSensorProgressBar)
                            }

                        }
                    } catch (e: InvalidProtocolBufferException) {
                        // throw java.lang.RuntimeException(e)
                    }
                // }
                byteListQueue.removeFirst()
            }

        }

    }

    override fun onRunError(e: Exception?) {
        e?.printStackTrace()
    }

    private fun updateButton() {
        runOnUiThread {
            if (obsLiteConnected) {
                binding.usbButton.text = getString(R.string.obs_activity_button_disconnect_device)
                binding.usbButton.setOnClickListener {
                    disconnectOBSLite()
                }
            } else {
                binding.usbButton.text = getString(R.string.obs_activity_button_connect)
                binding.usbButton.setOnClickListener {
                    getOBSLitePermission()
                }
            }
        }

    }
}