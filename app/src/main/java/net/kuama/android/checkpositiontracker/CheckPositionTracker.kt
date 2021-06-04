package net.kuama.android.checkpositiontracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZonedDateTime
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
    var receiveAt: LocalDateTime
}

// implementation
class OutRouteTrackerEvent(
    override val travel: Travel,
    override var currentPosition: Position,
    override var receiveAt: LocalDateTime
) : TrackerEvent {

}

class MissedEtaTrackerEvent(
    override val travel: Travel,
    override var currentPosition: Position,
    override var receiveAt: LocalDateTime
) : TrackerEvent {

}

class StationaryTrackerEvent(
    override val travel: Travel,
    override var currentPosition: Position,
    override var receiveAt: LocalDateTime
) : TrackerEvent {

}

class LowBatteryTrackerEvent(
    override val travel: Travel,
    override var currentPosition: Position,
    override var receiveAt: LocalDateTime
) : TrackerEvent {

}

class CompletedTravelTrackerEvent(
    override val travel: Travel,
    override var currentPosition: Position,
    override var receiveAt: LocalDateTime
) : TrackerEvent {

}


// travel has a status, a destination, a pause duration (optional), and a end date
interface Travel {
    var status: TravelStatus
    val destination: Position
    var pauseDuration: Duration?
    var endAt: LocalDateTime
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



// the checkPositionTracker can throw 5 types of event
class CheckPositionTracker(private val travel: Travel, private val route: RouteInterface) :
    BroadcastReceiver() {

    private var subject: BehaviorSubject<TrackerEvent> = BehaviorSubject.create()

    var actualPosition: Position? = null
        @RequiresApi(Build.VERSION_CODES.O)
        set(newPosition) {
            if (newPosition == null)
                return
            if (!isOnTrack(newPosition))
                subject.onNext(OutRouteTrackerEvent(travel, newPosition, LocalDateTime.now()))
            else if (isLate())
                subject.onNext(MissedEtaTrackerEvent(travel, newPosition, LocalDateTime.now()))
            else if (hasCompletedTrack(newPosition))
                subject.onNext(CompletedTravelTrackerEvent(travel, newPosition, LocalDateTime.now()))
            else if (newPosition == actualPosition)
                subject.onNext(StationaryTrackerEvent(travel, newPosition, LocalDateTime.now()))
            field = newPosition
        }


    fun isOnTrack(actualPosition: Position): Boolean {
        return route.path.contains(actualPosition)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun isLate(): Boolean {
        return LocalDateTime.now().isAfter(travel.endAt)
    }

    private fun hasCompletedTrack(actualPosition: Position): Boolean {
        return route.path.last() == actualPosition
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        actualPosition = intent?.position
    }

    fun run(): Flowable<TrackerEvent> = subject
        .toFlowable(BackpressureStrategy.LATEST)
}