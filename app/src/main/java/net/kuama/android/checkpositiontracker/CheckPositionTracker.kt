package net.kuama.android.checkpositiontracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import java.time.Duration
import java.util.*

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


// event thrown during a travel
interface TrackerEvent {
    val travel: Travel
    var currentPosition: Position
    var receiveAt: Date
}

// implementation
class OutRouteTrackerEvent(
    override val travel: Travel,
    override var currentPosition: Position,
    override var receiveAt: Date
) : TrackerEvent {

}

class MissedEtaTrackerEvent(
    override val travel: Travel,
    override var currentPosition: Position,
    override var receiveAt: Date
) : TrackerEvent {

}

class StationaryTrackerEvent(
    override val travel: Travel,
    override var currentPosition: Position,
    override var receiveAt: Date
) : TrackerEvent {

}

class LowBatteryTrackerEvent(
    override val travel: Travel,
    override var currentPosition: Position,
    override var receiveAt: Date
) : TrackerEvent {

}

class CompletedTravelTrackerEvent(
    override val travel: Travel,
    override var currentPosition: Position,
    override var receiveAt: Date
) : TrackerEvent {

}


// travel has a status, a destination, a pause duration (optional), and a end date
interface Travel {
    var status: TravelStatus
    val destination: Position
    var pauseDuration: Duration?
    var endAt: Date
}

// the user can be traveling, on pause, cancel the travel, be on emergency, has completed the travel
enum class TravelStatus {
    traveling,
    paused,
    canceled,
    emergency,
    completed
}

// the checkPositionTracker can throw 5 types of event
interface CheckPositionTrackerInterface {
    fun outRouteTrackerEvent(): TrackerEvent
    fun missedEtaTrackerEvent(): TrackerEvent
    fun stationaryTrackerEvent(): TrackerEvent
    fun lowBatteryTrackerEvent(): TrackerEvent
    fun completedTravelTrackerEvent(): TrackerEvent
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
) : RouteInterface{

}

// the type for traveling
enum class RouteMode {
    walking,
    cycling,
    driving,
    transit
}


/*
checkPositionTracker
    run(){
        check...

    }
 */

class CheckPositionTracker(val travel: Travel, private val route: RouteInterface) :
    CheckPositionTrackerInterface {


    var actualPosition: Position = TODO()
        set(newPosition) {
            if (!isOnTrack(newPosition))
                outRouteTrackerEvent()
            else if (isLate())
                missedEtaTrackerEvent()
            else if (hasCompletedTrack(newPosition))
                completedTravelTrackerEvent()
            else
                travel.status = TravelStatus.traveling
            field = newPosition
        }


    private fun isOnTrack(actualPosition: Position): Boolean {
        return route.path.contains(actualPosition)
    }

    private fun isLate(): Boolean {
        return Date().after(travel.endAt)
    }

    private fun hasCompletedTrack(actualPosition: Position): Boolean {
        return route.path.last() == actualPosition
    }

    override fun outRouteTrackerEvent(): TrackerEvent {
        travel.status = TravelStatus.emergency
        return OutRouteTrackerEvent(travel, actualPosition, Date())
    }

    override fun missedEtaTrackerEvent(): TrackerEvent {
        travel.status = TravelStatus.emergency
        return MissedEtaTrackerEvent(travel, actualPosition, Date())
    }

    override fun stationaryTrackerEvent(): TrackerEvent {
        travel.status = TravelStatus.emergency
        return StationaryTrackerEvent(travel, actualPosition, Date())
    }

    override fun lowBatteryTrackerEvent(): TrackerEvent {
        travel.status = TravelStatus.emergency
        return LowBatteryTrackerEvent(travel, actualPosition, Date())
    }

    override fun completedTravelTrackerEvent(): TrackerEvent {
        travel.status = TravelStatus.completed
        return CompletedTravelTrackerEvent(travel, actualPosition, Date())
    }

}


// the broadcast receiver
class MyLocationReceiver : BroadcastReceiver() {


    override fun onReceive(context: Context?, intent: Intent?) {
        val position: Position? = intent?.position

    }
}