package de.tuberlin.mcc.simra.app.activities

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import com.google.protobuf.InvalidProtocolBufferException
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import de.tuberlin.mcc.simra.app.DistanceMeasurement
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
import java.util.TreeSet
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue


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
    var distanceQueue: ConcurrentLinkedQueue<Int> = ConcurrentLinkedQueue<Int>()
    var startTime = -1L
    var movingMedian: MovingMedian = MovingMedian()
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
                            updateOBSLiteButton()

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

        // handlebar width
        // left
        binding.handleBarWidthLeft.maxValue = 60
        binding.handleBarWidthLeft.minValue = 0
        binding.handleBarWidthLeft.value =
            SharedPref.Settings.Ride.OvertakeWidth.getHandlebarWidthLeft(this)
        binding.handleBarWidthLeft.setOnValueChangedListener { picker, oldVal, newVal ->
            SharedPref.Settings.Ride.OvertakeWidth.setTotalWidthThroughHandlebarWidthLeft(newVal, this)
        }
        // right
        binding.handleBarWidthRight.maxValue = 60
        binding.handleBarWidthRight.minValue = 0
        binding.handleBarWidthRight.value =
            SharedPref.Settings.Ride.OvertakeWidth.getHandlebarWidthRight(this)
        binding.handleBarWidthRight.setOnValueChangedListener { picker, oldVal, newVal ->
            SharedPref.Settings.Ride.OvertakeWidth.setTotalWidthThroughHandlebarWidthRight(newVal, this)
        }

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
        updateOBSLiteButton()
        val obsLiteUrl = SharedPref.Settings.OBSLite.getObsLiteURL(this)
        binding.obsLiteURL.setText(obsLiteUrl,TextView.BufferType.EDITABLE)

        val obsUsername = SharedPref.Settings.OBSLite.getObsLiteUsername(this)
        binding.obsLiteUsername.setText(obsUsername,TextView.BufferType.EDITABLE)

        val obsLiteAPIKey = SharedPref.Settings.OBSLite.getObsLiteAPIKey(this)
        binding.obsLiteAPIKey.setText(obsLiteAPIKey,TextView.BufferType.EDITABLE)

    }

    private fun getOBSLitePermission() {

        val deviceList = usbManager?.getDeviceList()
        deviceList?.values?.forEach { device ->
            Log.d(TAG, "deviceList device $device")
            usbDevice = device
        }

        usbManager?.requestPermission(usbDevice, permissionIntent)

    }

    private fun disconnectOBSLite() {
        try {
            usbDevice = null
            if (this::port.isInitialized && port.isOpen) {
                port.close()
            }
            obsLiteConnected = false
            updateOBSLiteButton()
        } catch (e: InvocationTargetException) {
            e.cause?.printStackTrace()
        }
    }


    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        val obsLiteUrl = binding.obsLiteURL.getText()
        SharedPref.Settings.OBSLite.setObsLiteURL(obsLiteUrl.toString(),this)

        val obsLiteUsername = binding.obsLiteUsername.getText()
        SharedPref.Settings.OBSLite.setObsLiteUsername(obsLiteUsername.toString(),this)

        val obsLiteAPIKey = binding.obsLiteAPIKey.getText()
        SharedPref.Settings.OBSLite.setObsLiteAPIKey(obsLiteAPIKey.toString(),this)
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

    /** OBS-Lite related **/

    // Called when new data from usb is read
    override fun onNewData(data: ByteArray?) {
        runOnUiThread {

            fillByteList(data)
            // printByteList()

            val foundZero = completeCobsAvailable()

            if (foundZero) {
                // handle Event if, complete COBS package was found (ending with 00)
                handleEvent()
            }
        }

    }

    // handles distance event and user input events of obs lite
    private fun handleEvent() {
        /*if (byteListQueue.first.size > 11) {
            byteListQueue.removeFirst()
            return
        }*/
        // Log.d(TAG,"byteListQueue.first.size: ${byteListQueue.first.size}")
        val decodedData = CobsUtils.decode(byteListQueue.first.toByteArray())
        val decodedData2 = CobsUtils.decode(byteListQueue.first)

        Log.d(TAG, (decodedData.contentEquals(decodedData2)).toString())
        try {
            var event: Event = Event.parseFrom(decodedData)
            // Log.d(TAG, "" + event)
            // event is distance event
            if (event.hasDistanceMeasurement() && event.distanceMeasurement.distance < 5) {
                // convert distance to cm + handlebar width
                val distance = ((event.distanceMeasurement.distance * 100) + SharedPref.Settings.Ride.OvertakeWidth.getHandlebarWidth(this)).toInt()
                // left sensor event
                if (event.distanceMeasurement.sourceId == 1) {
                    binding.leftSensorTextView.text = this@OBSLiteActivity.getString(R.string.obs_lite_text_last_distance_left,distance)
                    setColePassBarColor(distance,binding.leftSensorProgressBar)
                    val eventTime = event.getTime(0).seconds
                    if (startTime == -1L) {
                        startTime = eventTime
                    }
                    // calculate minimal moving median for when the user presses obs lite button
                    movingMedian.newValue(distance)
                    // right sensor event
                } else {
                    binding.rightSensorTextView.text = this@OBSLiteActivity.getString(R.string.obs_lite_text_last_distance_right,distance)
                    setColePassBarColor(distance,binding.rightSensorProgressBar)
                }
                // event is user input event
            } else if (event.hasUserInput()) {
                val dm: DistanceMeasurement = DistanceMeasurement.newBuilder()
                    .setDistance(movingMedian.median.toFloat()).build()

                event = event.toBuilder().setDistanceMeasurement(dm).build()

                binding.userInputProgressbarTextView.text = this@OBSLiteActivity.getString(R.string.overtake_distance_left,movingMedian.median)
                setColePassBarColor(movingMedian.median,binding.leftSensorUserInputProgressBar)
                binding.userInputTextView.text =
                    this@OBSLiteActivity.getString(R.string.overtake_press_button) + event

            }
        } catch (_: InvalidProtocolBufferException) {
        }
        // if first byte list is handled, remove it.
        byteListQueue.removeFirst()
    }

    // checks whether the next byteList in queue is a complete COBS package
    private fun completeCobsAvailable(): Boolean {
        for (aByte in byteListQueue.peekFirst()!!) {
            if (aByte.toInt() == 0x00) {
                return true
            }
        }
        return false
    }

    // pretty print the byteListQueue
    private fun printByteList() {
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
    }

    // handles the byteListQueues, which contain the COBS packages
    private fun fillByteList(data: ByteArray?) {
        for (datum in data!!) {
            if (lastByteRead?.toInt() == 0x00){ // start new COBS package when last byte was 00
                val newByteList = LinkedList<Byte>()
                newByteList.add(datum)
                byteListQueue.add(newByteList)

            } else { // COBS package is not completed yet, continue the same package
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
    }

    override fun onRunError(e: Exception?) {
        e?.printStackTrace()
    }

    private fun updateOBSLiteButton() {
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

    /**
     * From https://www.geeksforgeeks.org/median-of-sliding-window-in-an-array/
     * Slightly edited
     */
    class MovingMedian {
        private val TAG = "MovingMedian_LOG"
        var distanceArray: ArrayList<Int> = ArrayList()
        var windowSize = 3
        var median = 0
        // Pair class for the value and its index
        class Pair // Constructor
            (private var value: Int, private var index: Int) : Comparable<Pair?> {
            // This method will be used by the treeset to search a value by index and setting the tree nodes (left or right)
            override fun compareTo(other: Pair?): Int {

                // Two nodes are equal only when
                // their indices are same
                return if (index == other?.index) {
                    0
                } else if (value == other?.value) {
                    index.compareTo(other.index)
                } else {
                    value.compareTo(other!!.value)
                }
            }

            // Function to return the value of the current object
            fun value(): Int {
                return value
            }

            // Update the value and the position for the same object to save space
            fun renew(v: Int, p: Int) {
                value = v
                index = p
            }

            override fun toString(): String {
                return String.format("(%d, %d)", value, index)
            }
        }


        // Function to print the median for the current window
        fun printMedian(minSet: TreeSet<Pair?>, maxSet: TreeSet<Pair?>, window: Int): Int {

            // If the window size is even then the median will be the average of the two middle elements
            return if (window % 2 == 0) {
                (((minSet.last()!!.value() + maxSet.first()!!.value()) / 2.0).toInt())
            } else {
                (if (minSet.size > maxSet.size) minSet.last()!!.value() else maxSet.first()!!.value())
            }
        }

        // Function to find the median of every window of size k
        fun findMedian(arr: ArrayList<Int>, k: Int): ArrayList<Int> {
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

                // Below logic is to maintain the maxSet and the minSet criteria
                if (arr[i] < maxSet.first()!!.value()) {
                    minSet.add(windowPairs[i])
                } else {
                    minSet.add(maxSet.pollFirst())
                    maxSet.add(windowPairs[i])
                }
            }
            result.add(printMedian(minSet, maxSet, k))
            for (i in k until arr.size) {

                // Get the pair at the start of the window, this will reset to 0 at every k, 2k, 3k, ...
                val temp = windowPairs[i % k]
                if (temp!!.value() <= minSet.last()!!.value()) {

                    // Remove the starting pair of the window
                    minSet.remove(temp)

                    // Renew window start to new window end
                    temp.renew(arr[i], i)

                    // Below logic is to maintain the maxSet and the minSet criteria
                    if (temp.value() < maxSet.first()!!.value()) {
                        minSet.add(temp)
                    } else {
                        minSet.add(maxSet.pollFirst())
                        maxSet.add(temp)
                    }
                } else {
                    maxSet.remove(temp)
                    temp.renew(arr[i], i)

                    // Below logic is to maintain the maxSet and the minSet criteria
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
            // max array size is 122 (~ 5 seconds), delete oldest value, if exceeded.
            if (distanceArray.size >= 122) {
                distanceArray = distanceArray.drop(1) as ArrayList<Int>
            }
            distanceArray.add(distance)
            // calculate median only if if distanceArray is big enough.
            if (distanceArray.size >= windowSize) {
                median = findMedian(distanceArray, windowSize).minOrNull()!!
            }
        }
    }
}