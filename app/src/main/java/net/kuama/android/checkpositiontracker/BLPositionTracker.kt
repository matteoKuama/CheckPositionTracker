package net.kuama.android.checkpositiontracker

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.preference.PreferenceManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import net.kuama.android.backgroundLocation.service.BackgroundService
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.time.LocalDateTime

// Position source
//      -> created by service --> launches broadcast
//          --> started by activity
// BLService
// --> to work it needs
//      --> user's journey
//      --> arrival time
// --> started by activity

// TODO: aggiungere supporto alla batteria
// TODO: riorganizzare il codice
// TODO: provare il codice


const val LATITUDE_EXTRA = "latitude"
const val LONGITUDE_EXTRA = "longitude"
const val DATE_TIME = "received at"
const val CURRENT_STATUS = "current status"


// Single location of a journey
@JsonClass(generateAdapter = true)
data class Journey(val path: List<Position>)


// When the user is supposed to arrive
typealias ETA = DateTime

enum class PathStatus {
    Safe,
    OutRoute,
    MissedEta,
    Stationary,
    LowBattery,
    Completed
}

class BLService : Service() {
    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        val formatter: DateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss")
    }

    class Builder() {
        private var eta: ETA? = null
        fun eta(eta: ETA) = apply {
            this.eta = eta
        }

        private var journey: Journey? = null
        fun journey(journey: Journey) = apply {
            this.journey = journey
        }

        fun build(context: Context): Intent {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = preferences.edit()
            editor.putString("eta", this.eta!!.toString(formatter))
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val adapter: JsonAdapter<Journey> = moshi.adapter(Journey::class.java)
            val stringJson = adapter.toJson(journey)



            editor.putString("journey", stringJson)
            editor.apply()
            return Intent(context, BLService::class.java)
        }
    }

    class LocationReceiver : BroadcastReceiver() {

        private var eta: ETA? = null
        private var route: List<Position>? = null
        private var oldPosition: Position? = null
        private var sendAlert: Boolean = false
        private var status: PathStatus = PathStatus.Safe

        override fun onReceive(context: Context?, intent: Intent?) {
            val newPosition: Position = intent?.position ?: return

            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val etaString = preferences.getString("eta", null)
//            val batteryLow = preferences.getBoolean("battery_low", false)
            eta = formatter.parseDateTime(etaString)
            val jsonJourney: String? = preferences.getString("journey", null)

            // TODO
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val adapter: JsonAdapter<Journey> = moshi.adapter(Journey::class.java)
            route = adapter.fromJson(jsonJourney)?.path

            // position to status....
            if (!isOnTrack(newPosition)!!) {
                Log.d("Location", "is not on track")
                sendAlert = true
                status = PathStatus.OutRoute
            }
            else if (isLate()!!) {
                Log.d("Location", "is late")
                sendAlert = true
                status = PathStatus.MissedEta
            }
            if (hasCompletedTrack(newPosition)) {
                Log.d("Location", "has completed track")
                sendAlert = true
                status = PathStatus.Completed
            }
            if (newPosition == oldPosition) {
                Log.d("Location", "is standing")
                sendAlert = true
                status = PathStatus.Stationary
            }
//                checkPositionService.sendTrackerEvent(TrackerEventType.stationaryTrackerEvent)
//            else if (isBatteryTenOrFivePercent())
//                Log.d("Location", "battery low")
            oldPosition = newPosition
            if (sendAlert) {
                val intent = Intent()
                    .also {
                        it.action = BLService::class.java.name
                        it.flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
                        it.putExtra(CURRENT_STATUS, status.name)
                        it.putExtra(LATITUDE_EXTRA, oldPosition!!.latitude)
                        it.putExtra(LONGITUDE_EXTRA, oldPosition!!.longitude)
                        it.putExtra(DATE_TIME, DateTime.now())
                    }
                context?.sendBroadcast(intent)
            }
//            val status: PathStatus = TODO()
//            Intent()
//                .also {
//                    it.action = BLService::class.java.name
//                    it.flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
//                    it.putExtra("current_status", status.name)
//                }
//            context?.sendBroadcast(intent)
        }


        private fun isOnTrack(actualPosition: Position): Boolean? {
            return route?.contains(actualPosition)
        }

        private fun isLate(): Boolean? {
            return eta?.isBeforeNow

        }

        private fun hasCompletedTrack(actualPosition: Position): Boolean {
            Log.d("Location", "arrival: ${route?.last()}")
            return route?.last() == actualPosition
        }

//        private fun isBatteryTenOrFivePercent(): Boolean {
//            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
//            return batteryLevel == 10 || batteryLevel == 5
//        }
    }

    class LowBatteryReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = preferences.edit()
            editor.putBoolean("battery_low", true)
            editor.apply()
        }
    }

    class OkBatteryReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = preferences.edit()
            editor.putBoolean("battery_low", false)
            editor.apply()
        }
    }

    override fun onCreate() {
        super.onCreate()
        // register to location updates
//        val myBroadcastReceiver = LocationReceiver()
//        val intentFilter = IntentFilter(BackgroundService::class.java.name)
//        registerReceiver(myBroadcastReceiver, intentFilter)
        val lowBatteryReceiver = LowBatteryReceiver()
        val okBatteryReceiver = OkBatteryReceiver()
        val lowBatteryFilter = IntentFilter(Intent.ACTION_BATTERY_LOW)
        val okBatteryFilter = IntentFilter(Intent.ACTION_BATTERY_OKAY)
        registerReceiver(lowBatteryReceiver, lowBatteryFilter)
        registerReceiver(okBatteryReceiver, okBatteryFilter)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }
}
