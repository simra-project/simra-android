package de.tuberlin.mcc.simra.app.obslite

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import de.tuberlin.mcc.simra.app.BuildConfig
import de.tuberlin.mcc.simra.app.DistanceMeasurement
import de.tuberlin.mcc.simra.app.Event
import de.tuberlin.mcc.simra.app.Geolocation
import de.tuberlin.mcc.simra.app.Metadata
import de.tuberlin.mcc.simra.app.Time
import de.tuberlin.mcc.simra.app.activities.OBSLiteActivity
import de.tuberlin.mcc.simra.app.util.CobsUtils
import de.tuberlin.mcc.simra.app.util.SharedPref
import java.util.LinkedList
import java.util.concurrent.ConcurrentLinkedDeque

class OBSLiteSession(val context: Context) {
    var obsLiteStartTime: Long = 0L
    val TAG = "OBSLiteSession2_LOG"
    private var events: ArrayList<Event> = ArrayList()
    var byteListQueue = ConcurrentLinkedDeque<LinkedList<Byte>>()
    var lastByteRead: Byte? = null
    var movingMedian: OBSLiteActivity.MovingMedian = OBSLiteActivity.MovingMedian()
    var startTime = -1L
    private var lastLat: Double = 0.0
    private var lastLon: Double = 0.0
    private var completeEvents = ArrayList<Byte>()
    init {

        val handlebarOffsetLeft: ByteString = ByteString.copyFromUtf8(SharedPref.Settings.Ride.OvertakeWidth.getHandlebarWidthLeft(context)
            .toString())
        val handlebarOffsetRight: ByteString = ByteString.copyFromUtf8(SharedPref.Settings.Ride.OvertakeWidth.getHandlebarWidthRight(context)
            .toString())
        val metaData: Metadata = Metadata.newBuilder()
            .putData("HandlebarOffsetLeft",handlebarOffsetLeft)
            .putData("HandlebarOffsetRight",handlebarOffsetRight)
            .putData("SimRaVersion", ByteString.copyFromUtf8(BuildConfig.VERSION_NAME))
            .build()
        events.add(Event.newBuilder().setMetadata(metaData).build())
    }



    // handles distance event and user input events of obs lite
    fun handleEvent(lat: Double, lon: Double, altitude: Double, accuracy: Float) : Event? {
        val decodedData = CobsUtils.decode(byteListQueue.first)

        try {
            var obsEvent: Event = Event.parseFrom(decodedData)

            if (startTime == -1L) {
                startTime = obsEvent.getTime(0).seconds
            }

            val obsTime: Time = Time.newBuilder().setNanoseconds(obsEvent.getTime(0).nanoseconds)
                .setSourceId(2).setReference(Time.Reference.UNIX).build()

            val smartphoneTime: Time = Time.newBuilder().setNanoseconds(System.currentTimeMillis().toInt())
                .setSourceId(3).setReference(Time.Reference.UNIX).build()

            /*val secondsSinceFirstOBSTime = obsEvent.getTime(0).seconds - startTime
            val epochSecondsOfEvent = ((obsLiteStartTime/1000) + secondsSinceFirstOBSTime)
            val nanoseconds = obsEvent.getTime(0).nanoseconds

            val time: Time = Time.newBuilder().setSeconds(epochSecondsOfEvent).setNanoseconds(nanoseconds).setSourceId(2).setReference(
                Time.Reference.UNIX).build()*/

            if (lat != lastLat || lon != lastLon) {
                val geolocation: Geolocation = Geolocation.newBuilder()
                    .setLatitude(lat).setLongitude(lon)
                    .setAltitude(altitude).setHdop(accuracy).build()

                val gpsEvent = Event.newBuilder().setGeolocation(geolocation).addTime(obsTime).addTime(smartphoneTime).build()
                events.add(gpsEvent)
                completeEvents.addAll(encodeEvent(gpsEvent))
                lastLat = lat
                lastLon = lon
            }

            if (obsEvent.hasDistanceMeasurement()) {
                val dm = obsEvent.distanceMeasurement
                obsEvent = obsEvent.toBuilder().addTime(obsTime).addTime(smartphoneTime).setDistanceMeasurement(dm).build()
                // left sensor event
                if (obsEvent.distanceMeasurement.sourceId == 1) {
                    val distance = ((obsEvent.distanceMeasurement.distance * 100) + SharedPref.Settings.Ride.OvertakeWidth.getHandlebarWidthLeft(context)).toInt()
                    // calculate minimal moving median for when the user presses obs lite button
                    movingMedian.newValue(distance)
                    // Log.d(TAG, "distance event: $event")
                }
            } else if (obsEvent.hasUserInput()) {
                val ui = obsEvent.userInput
                obsEvent = obsEvent.toBuilder().addTime(obsTime).addTime(smartphoneTime).setUserInput(ui).build()
                events.add(obsEvent)
                completeEvents.addAll(encodeEvent(obsEvent))
                val dm: DistanceMeasurement = DistanceMeasurement.newBuilder()
                    .setDistance(movingMedian.median.toFloat()).build()
                obsEvent = obsEvent.toBuilder().addTime(obsTime).addTime(smartphoneTime).setDistanceMeasurement(dm).build()
                // Log.d(TAG, "user input event: $obsEvent")
                byteListQueue.removeFirst()
                return obsEvent

            } else {
                obsEvent = obsEvent.toBuilder().addTime(obsTime).addTime(smartphoneTime).build()
                Log.d(TAG, obsEvent.toString())
            }

            // Log.d(TAG, obsEvent.toString())
            events.add(obsEvent)
            completeEvents.addAll(encodeEvent(obsEvent))


        } catch (_: InvalidProtocolBufferException) {
        }
        // if first byte list is handled, remove it.
        byteListQueue.removeFirst()
        return null
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
            // start new COBS package when last byte was 00 or it is the first data
            if (lastByteRead?.toInt() == 0x00 || byteListQueue.isEmpty()){
                val newByteList = LinkedList<Byte>()
                newByteList.add(datum)
                byteListQueue.add(newByteList)
            } else { // COBS package is not completed yet, continue the same package
                byteListQueue.last.add(datum)
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

    private fun encodeEvent(event: Event?): Collection<Byte> {
        // Log.d(TAG, event.toString())
        val byteArray = event?.toByteArray()
        return CobsUtils.encode2(byteArray)
        /*Log.d(TAG, "=========================")
        Log.d(TAG, Event.parseFrom(CobsUtils.decode(cobsByteArray)).toString())*/
    }

    fun getCompleteEvents(): ByteArray {
        return completeEvents.toByteArray()
    }

    // Adds a GPS event with the new Lat,Lon and the time of the new GPS
    // (used by RecorderService's onLocationChanged)
    fun addGPSEvent(location: Location) {
        val geolocation: Geolocation = Geolocation.newBuilder()
            .setLatitude(location.latitude).setLongitude(location.longitude)
            .setAltitude(location.altitude).setHdop(location.accuracy).build()

        val time: Time = Time.newBuilder().setNanoseconds((location.time*1000000).toInt()).build()

        val gpsEvent = Event.newBuilder().setGeolocation(geolocation).addTime(time).build()
        events.add(gpsEvent)
        completeEvents.addAll(encodeEvent(gpsEvent))

    }
}