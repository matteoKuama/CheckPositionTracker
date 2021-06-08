package net.kuama.android.checkpositiontracker

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter


/**
 * LATITUDE_EXTRA, LONGITUDE_EXTRA, DATE_TIME, CURRENT_STATUS
 * use this values as keys to retrieve the date from the broadcast
 */

const val LATITUDE_EXTRA = "latitude"
const val LONGITUDE_EXTRA = "longitude"
const val DATE_TIME = "received at"
const val CURRENT_STATUS = "current status"
const val SETTINGS = "settings"

data class Position(val latitude: Double, val longitude: Double)

val Intent.position: Position?
    get() {
        val latitude: Double? = extras?.getDouble("latitude")
        val longitude: Double? = extras?.getDouble("longitude")

        return if (latitude !== null && longitude !== null) {
            Position(latitude, longitude)
        } else null
    }
val Location.position: Position
    get() = Position(latitude = this.latitude, longitude = this.longitude)


val Intent.status: String
    get() = extras?.get(CURRENT_STATUS).toString()

val Intent.date: DateTime
    get() = extras?.get(DATE_TIME) as DateTime


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

/**
 * This service checks constantly the position of the user and it sends a broadcast if the user in danger
 *
 * To instantiate this class it is necessary to use the Builder. The Builder needs a Journey that
 * the user will do and a ETA for completing the Journey.
 * This class receives he current position of the user and analyzes if the user is currently in danger.
 * The user is in danger if one of this condition is satisfied:
 * +The user is going outside the provided Journey
 * +The user hasn't reached the destination in time
 * +The user isn't moving
 * +The battery is at 5% or 10%
 * If one of these conditions are satisfied this class launches a broadcast with some information regarding
 * the type of the alert sent, the current position of the user and the date and the time in which the even occurred.
 * A broadcast is sent also if the destination is reached by the user.
 */
class BLService : Service() {
    /**
     * This is a companion object that formats the DateTime
     */
    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        val formatter: DateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss")
    }

    class Builder {
        private var eta: ETA? = null
        fun eta(eta: ETA) = apply {
            this.eta = eta
        }

        private var journey: Journey? = null
        fun journey(journey: Journey) = apply {
            this.journey = journey
        }

        fun build(context: Context): Intent {
            val preferences = context.getSharedPreferences(SETTINGS, MODE_PRIVATE)
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

    /**
     * This class receives the position from the plugin and analyzes it.
     *
     * This class extends BroadcastReceiver and it receives the position from the plugin.
     * It determines if the user is in danger and if it has completed his journey.
     * If so, it sends a broadcast with information regarding the user status, position and date/time of the event.
     */
    class LocationReceiver : BroadcastReceiver() {

        private var eta: ETA? = null
        private var route: List<Position>? = null
        private var oldPosition: Position? = null
        private var batteryManager: BatteryManager? = null
        private var pathStatus: PathStatus = PathStatus.Safe

        /**
         * This method receives location updates
         *
         * This method receives location updates and it analyzes them.
         * The user is in danger if one of this condition is satisfied:
         * +The user is going outside the provided Journey
         * +The user hasn't reached the destination in time
         * +The user isn't moving
         * +The battery is at 5% or 10%
         * If one of these conditions are satisfied this class launches a broadcast with some information regarding
         * the type of the alert sent, the current position of the user and the date and the time in which the even occurred.
         * A broadcast is sent also if the destination is reached by the user.
         */
        override fun onReceive(context: Context?, intent: Intent?) {
            val newPosition: Position = intent?.position ?: return

            batteryManager =
                context?.applicationContext?.getSystemService(BATTERY_SERVICE) as BatteryManager
            val preferences = context.getSharedPreferences(SETTINGS, MODE_PRIVATE)
            val etaString = preferences?.getString("eta", null)
            eta = formatter.parseDateTime(etaString)
            val jsonJourney: String? = preferences?.getString("journey", null)

            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val adapter: JsonAdapter<Journey> = moshi.adapter(Journey::class.java)
            route = adapter.fromJson(jsonJourney)?.path

            if (inDangerOrCompleted(newPosition)) {
                sendBroadcastAlert(context)
            }
        }

        /**
         * This method checks if the user has completed the journey or if he's in danger.
         *
         * @param newPosition: the new position of the user
         * @return true if the user has completed the journey, the battery is at 10% or 5%,
         * the user is off-track, the user missed the ETA or if the user is stationary
         *
         * This method checks if the user has completed the journey or if he's in danger.
         * If the user is one of these conditions, it changes also the PathStatus to the corresponding event.
         */
        private fun inDangerOrCompleted(newPosition: Position): Boolean {
            if (hasCompletedTrack(newPosition))
                pathStatus = PathStatus.Completed
            else if (isBatteryTenOrFivePercent())
                pathStatus = PathStatus.LowBattery
            else if (!isOnTrack(newPosition)!!)
                pathStatus = PathStatus.OutRoute
            else if (isLate()!!)
                pathStatus = PathStatus.MissedEta
            else if (newPosition == oldPosition)
                pathStatus = PathStatus.Stationary
            oldPosition = newPosition
            return pathStatus != PathStatus.Safe
        }

        /**
         * This method sends a broadcast to alert that the user is in danger or has completed the journey.
         *
         * @param context: the context from which it will send the broadcast alert.
         *
         */
        private fun sendBroadcastAlert(context: Context?) {
            val intent = Intent().apply {
                action = BLService::class.java.name
                flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
                putExtra(CURRENT_STATUS, pathStatus.name)
                putExtra(LATITUDE_EXTRA, oldPosition!!.latitude)
                putExtra(LONGITUDE_EXTRA, oldPosition!!.longitude)
                putExtra(DATE_TIME, DateTime.now())
            }
            context?.sendBroadcast(intent)
        }


        /**
         * This method checks if the user is on the expected journey.
         *
         * @param actualPosition: the current position of the user
         * @return: true if the user is on the expected journey, false otherwise
         */
        private fun isOnTrack(actualPosition: Position): Boolean? {
            return route?.contains(actualPosition)
        }


        /**
         * This method checks if the user is late on the journey.
         *
         * @return: true if the user is late.
         */
        private fun isLate(): Boolean? {
            return eta?.isBeforeNow

        }

        /**
         * This method checks if the user has successfully completed the journey.
         *
         * @param actualPosition: the current position of the user
         * @return true if the user has completed the journey, false otherwise.
         */
        private fun hasCompletedTrack(actualPosition: Position): Boolean {
            return route?.last() == actualPosition
        }


        /**
         * This method checks if the battery level is 10% or 5%.
         *
         * @return true if the battery level is 10% or 5%.
         */
        private fun isBatteryTenOrFivePercent(): Boolean {
            val batteryLevel =
                batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

            return batteryLevel == 10 || batteryLevel == 5
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }
}