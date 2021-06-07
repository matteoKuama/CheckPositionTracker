package net.kuama.android.checkpositiontracker

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import net.kuama.android.backgroundLocation.service.BackgroundService
import java.time.Duration
import java.time.LocalDateTime

// finche' non si sistema l'import di Position
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
//

const val TYPE_EXTRA = "type"

// event thrown during a travel
interface TrackerEvent {
    val travel: Travel
    var currentPosition: Position
    var receiveAt: LocalDateTime
    val type: TrackerEventType
}

enum class TrackerEventType{
    outRouteTrackerEvent,
    missedEtaTrackerEvent,
    stationaryTrackerEvent,
    lowBatteryTrackerEvent,
    completedTravelTrackerEvent
}



// travel has a status, a destination, a pause duration (optional), and a end date
interface Travel {
    var status: TravelStatus
    val destination: Position
    var pauseDuration: Duration?
    var endAt: LocalDateTime
}

class MyTravel(
    override var status: TravelStatus,
    override val destination: Position,
    override var pauseDuration: Duration?,
    override var endAt: LocalDateTime
) : Travel{

}

// the user can be traveling, on pause, cancel the travel, be on emergency, has completed the travel
enum class TravelStatus {
    traveling,
    paused,
    canceled,
    emergency,
    completed
}


// a route is a list of positions, has a duration and a mode of traveling
interface RouteInterface {
    val path: List<Position>
    var duration: Duration
    val mode: RouteMode
}

class Route(
    override val path: List<Position>,
    override var duration: Duration,
    override val mode: RouteMode
) : RouteInterface {

}

// the type for traveling
enum class RouteMode {
    walking,
    cycling,
    driving,
    transit
}


/** CheckPositionTracker can throw 5 types of event:
 * OutRouteTrackerEvent, MissedEtaTrackerEvent, CompletedTravelTrackerEvent, StationaryTrackerEvent, LowBatteryTrackerEvent
 *
 */
class CheckPositionTracker(
    private val travel: Travel,
    private val route: RouteInterface,
    private val batteryManager: BatteryManager
) :
    BroadcastReceiver() {

/*
qui ricevo le posizioni e decido cosa fare
 */

    var actualPosition: Position? = null
        @RequiresApi(Build.VERSION_CODES.O)
        set(newPosition) {
            if (newPosition == null)
                return
            if (!isOnTrack(newPosition))
                Log.d("Location", "is not on track")
                //checkPositionService.sendTrackerEvent(TrackerEventType.outRouteTrackerEvent)
            else if (isLate())
                Log.d("Location", "is late")
//                checkPositionService.sendTrackerEvent(TrackerEventType.missedEtaTrackerEvent)
            else if (hasCompletedTrack(newPosition))
                Log.d("Location", "has completed track")
//            checkPositionService.sendTrackerEvent(TrackerEventType.completedTravelTrackerEvent)
            else if (newPosition == actualPosition)
                Log.d("Location", "is standing")
//                checkPositionService.sendTrackerEvent(TrackerEventType.stationaryTrackerEvent)
            else if (isBatteryTenOrFivePercent())
                Log.d("Location", "battery low")
//            checkPositionService.sendTrackerEvent(TrackerEventType.lowBatteryTrackerEvent)
            field = newPosition
        }


    fun isOnTrack(actualPosition: Position): Boolean {
        return route.path.contains(actualPosition)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun isLate(): Boolean {
        return LocalDateTime.now().isAfter(travel.endAt)
    }

    private fun hasCompletedTrack(actualPosition: Position): Boolean {
        return route.path.last() == actualPosition
    }

    private fun isBatteryTenOrFivePercent(): Boolean {
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return batteryLevel == 10 || batteryLevel == 5
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        actualPosition = intent?.position
        Log.d("Location", "received new position ${actualPosition?.latitude} - ${actualPosition?.longitude}")
    }

}

/*
qui invio i broadcast con gli eventi
 */
class CheckPositionService: Service(){


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    fun sendTrackerEvent(trackerEventType: TrackerEventType){
        Log.d("Location", "send tracker event")
        val actionName = javaClass.name
        val intent = Intent()
            .also {
                it.action = actionName
                it.flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
                it.putExtra(TYPE_EXTRA, trackerEventType)
            }
        sendBroadcast(intent)
    }

}