package de.tuberlin.mcc.simra.app.obslite

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.widget.ProgressBar
import com.google.protobuf.InvalidProtocolBufferException
import de.tuberlin.mcc.simra.app.DistanceMeasurement
import de.tuberlin.mcc.simra.app.Event
import de.tuberlin.mcc.simra.app.R
import de.tuberlin.mcc.simra.app.activities.OBSLiteActivity
import de.tuberlin.mcc.simra.app.databinding.ActivityObsliteBinding
import de.tuberlin.mcc.simra.app.util.CobsUtils
import de.tuberlin.mcc.simra.app.util.SharedPref
import java.util.LinkedList
import java.util.concurrent.ConcurrentLinkedDeque

class OBSLiteSession2() {
    val TAG = "OBSLiteSession2_LOG"

    var byteListQueue = ConcurrentLinkedDeque<LinkedList<Byte>>()
    var lastByteRead: Byte? = null
    var movingMedian: OBSLiteActivity.MovingMedian = OBSLiteActivity.MovingMedian()
    var startTime = -1L





    // handles distance event and user input events of obs lite
    fun handleEvent(binding: ActivityObsliteBinding, context: Context) {
        val decodedData = CobsUtils.decode(byteListQueue.first)

        try {
            var event: Event = Event.parseFrom(decodedData)
            // Log.d(TAG, "" + event)
            // event is distance event
            if (event.hasDistanceMeasurement() && event.distanceMeasurement.distance < 5) {
                // convert distance to cm + handlebar width
                val distance = ((event.distanceMeasurement.distance * 100) + SharedPref.Settings.Ride.OvertakeWidth.getHandlebarWidth(context)).toInt()
                // left sensor event
                if (event.distanceMeasurement.sourceId == 1) {
                    binding.leftSensorTextView.text = context.getString(R.string.obs_lite_text_last_distance_left,distance)
                    setColePassBarColor(distance,binding.leftSensorProgressBar)
                    val eventTime = event.getTime(0).seconds
                    if (startTime == -1L) {
                        startTime = eventTime
                    }
                    // calculate minimal moving median for when the user presses obs lite button
                    movingMedian.newValue(distance)
                    // right sensor event
                } else {
                    binding.rightSensorTextView.text = context.getString(R.string.obs_lite_text_last_distance_right,distance)
                    setColePassBarColor(distance,binding.rightSensorProgressBar)
                }
                // event is user input event
            } else if (event.hasUserInput()) {
                val dm: DistanceMeasurement = DistanceMeasurement.newBuilder()
                    .setDistance(movingMedian.median.toFloat()).build()

                event = event.toBuilder().setDistanceMeasurement(dm).build()

                binding.userInputProgressbarTextView.text = context.getString(R.string.overtake_distance_left,movingMedian.median)
                setColePassBarColor(movingMedian.median,binding.leftSensorUserInputProgressBar)
                binding.userInputTextView.text =
                    context.getString(R.string.overtake_press_button) + event

            }
        } catch (_: InvalidProtocolBufferException) {
        }
        // if first byte list is handled, remove it.
        byteListQueue.removeFirst()
    }

    fun handleEvent(context: Context) {
        val decodedData = CobsUtils.decode(byteListQueue.first)

        try {
            var event: Event = Event.parseFrom(decodedData)
            // Log.d(TAG, "" + event)
            // event is distance event
            if (event.hasDistanceMeasurement() && event.distanceMeasurement.distance < 5) {
                // convert distance to cm + handlebar width
                val distance = ((event.distanceMeasurement.distance * 100) + SharedPref.Settings.Ride.OvertakeWidth.getHandlebarWidth(context)).toInt()
                // left sensor event
                if (event.distanceMeasurement.sourceId == 1) {
                    val eventTime = event.getTime(0).seconds
                    if (startTime == -1L) {
                        startTime = eventTime
                    }
                    // calculate minimal moving median for when the user presses obs lite button
                    movingMedian.newValue(distance)
                    Log.d(TAG, "distance event: $event")
                }
                // event is user input event
            } else if (event.hasUserInput()) {
                val dm: DistanceMeasurement = DistanceMeasurement.newBuilder()
                    .setDistance(movingMedian.median.toFloat()).build()
                event = event.toBuilder().setDistanceMeasurement(dm).build()
                Log.d(TAG, "user input event: $event")

            }
        } catch (_: InvalidProtocolBufferException) {
        }
        // if first byte list is handled, remove it.
        byteListQueue.removeFirst()
    }

    // checks whether the next byteList in queue is a complete COBS package
    fun completeCobsAvailable(): Boolean {
        for (aByte in byteListQueue.peekFirst()!!) {
            if (aByte.toInt() == 0x00) {
                return true
            }
        }
        return false
    }

    // handles the byteListQueues, which contain the COBS packages
    fun fillByteList(data: ByteArray?) {
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

    // pretty print the byteListQueue
    fun printByteList() {
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
}