package de.tuberlin.mcc.simra.app.obslite

import android.util.Log
import de.tuberlin.mcc.simra.app.DistanceMeasurement
import de.tuberlin.mcc.simra.app.Event
import de.tuberlin.mcc.simra.app.Geolocation
import de.tuberlin.mcc.simra.app.Time
import de.tuberlin.mcc.simra.app.util.CobsUtils

class OBSLiteSession(private val startEpoch: Long) {

    private var events: ArrayList<Event> = ArrayList()
    val TAG = "OBSLiteSession_LOG"
    private var obsStartTimeSeconds = -1L
    private var lastLat: Double = 0.0
    private var lastLon: Double = 0.0

    private var completeEvents = ArrayList<Byte>()

    fun addEvent(lat: Double, lon: Double, altitude: Double, accuracy: Float, byteArray: ByteArray) {
        val geolocation: Geolocation = Geolocation.newBuilder().setLatitude(lat).setLongitude(lon)
            .setAltitude(altitude).setHdop(accuracy).build()

        var obsEvent: Event = Event.parseFrom(byteArray)

        if (obsStartTimeSeconds == -1L) {
            obsStartTimeSeconds = obsEvent.getTime(0).seconds
        }

        val secondsSinceFirstOBSTime = obsEvent.getTime(0).seconds - obsStartTimeSeconds
        val epochSecondsOfEvent = ((startEpoch/1000) + secondsSinceFirstOBSTime)
        val nanoseconds = obsEvent.getTime(0).nanoseconds

        val time: Time = Time.newBuilder().setSeconds(epochSecondsOfEvent).setNanoseconds(nanoseconds).setSourceId(2).setReference(Time.Reference.UNIX).build()

        if (obsEvent.hasDistanceMeasurement()) {
            val dm = obsEvent.distanceMeasurement
            obsEvent = obsEvent.toBuilder().addTime(time).setDistanceMeasurement(dm).build()
        } else if (obsEvent.hasUserInput()) {
            val ui = obsEvent.userInput
            obsEvent = obsEvent.toBuilder().addTime(time).setUserInput(ui).build()
        } else {
            obsEvent = obsEvent.toBuilder().addTime(time).build()
        }
        if (lat != lastLat || lon != lastLon) {
            val gpsEvent = Event.newBuilder().setGeolocation(geolocation).addTime(time).build()
            events.add(gpsEvent)
            completeEvents.addAll(encodeEvent(gpsEvent))
        }
        Log.d(TAG, obsEvent.toString())
        events.add(obsEvent)
        completeEvents.addAll(encodeEvent(obsEvent))
        lastLat = lat
        lastLon = lon
    }

    private fun encodeEvent(event: Event?): Collection<Byte> {
        Log.d(TAG, event.toString())
        val byteArray = event?.toByteArray()
        return CobsUtils.encode2(byteArray)
        /*Log.d(TAG, "=========================")
        Log.d(TAG, Event.parseFrom(CobsUtils.decode(cobsByteArray)).toString())*/
    }

    fun getCompleteEvents(): ByteArray {
        return completeEvents.toByteArray()
    }


}