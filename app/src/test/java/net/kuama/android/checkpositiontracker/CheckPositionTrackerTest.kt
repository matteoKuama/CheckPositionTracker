package net.kuama.android.checkpositiontracker

import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase
import java.time.LocalDateTime


class CheckPositionTrackerTest : TestCase() {


    fun testOffTrackReturnFalse() {
        // Arrange
        val travel = mockk<Travel>()
        val route = Route(listOf(Position(12.0, 12.0)), mockk(), mockk())
        val checkPositionTracker = CheckPositionTracker(travel, route)
        val newPosition = Position(14.0, 14.0)
        // Act
        checkPositionTracker.actualPosition = newPosition
        // Assert
        assert(!checkPositionTracker.isOnTrack(newPosition))
    }

    fun testIsOnTrackReturnTrue() {
        // Arrange
        val travel = mockk<Travel>()
        val route = Route(listOf(Position(12.0, 12.0)), mockk(), mockk())
        val checkPositionTracker = CheckPositionTracker(travel, route)
        val newPosition = Position(12.0, 12.0)
        every {
            travel.endAt
        } returns mockk()
        // Act
        checkPositionTracker.actualPosition = newPosition
        // Assert
        assert(checkPositionTracker.isOnTrack(newPosition))
    }

    fun testLaunchOutRouteTrackerEvent() {
        // Arrange
        val travel = mockk<Travel>()
        val route = Route(listOf(Position(12.0, 12.0)), mockk(), mockk())
        val checkPositionTracker = CheckPositionTracker(travel, route)
        val observer = checkPositionTracker.run()
        // Act
        val newPosition = Position(14.0, 14.0)
        checkPositionTracker.actualPosition = newPosition
        // Assert
        assert(observer.test().values().last() is OutRouteTrackerEvent)
    }

    fun testDoesntLaunchOutRouteTrackerEvent() {
        // Arrange
        val travel = mockk<Travel>()
        val route = Route(listOf(Position(12.0, 12.0)), mockk(), mockk())
        val checkPositionTracker = CheckPositionTracker(travel, route)
        val observer = checkPositionTracker.run()
        every {
            travel.endAt
        } returns mockk()
        // Act
        val newPosition = Position(12.0, 12.0)
        checkPositionTracker.actualPosition = newPosition
        // Assert
        assertFalse(observer.test().values().last() is OutRouteTrackerEvent)
    }

    fun testLaunchMissedEtaTrackerEvent() {
        // Arrange
        val travel = mockk<Travel>()
        val route = Route(listOf(Position(12.0, 12.0)), mockk(), mockk())
        val checkPositionTracker = CheckPositionTracker(travel, route)
        val observer = checkPositionTracker.run()
        every {
            travel.endAt
        } returns LocalDateTime.of(1999, 12, 12, 12, 12)
        // Act
        val newPosition = Position(12.0, 12.0)
        checkPositionTracker.actualPosition = newPosition
        // Assert
        assert(observer.test().values().last() is MissedEtaTrackerEvent)
    }

    fun testDoesntLaunchMissedEtaTrackerEvent() {
        // Arrange
        val travel = mockk<Travel>()
        val route = Route(listOf(Position(12.0, 12.0)), mockk(), mockk())
        val checkPositionTracker = CheckPositionTracker(travel, route)
        val observer = checkPositionTracker.run()
        every {
            travel.endAt
        } returns LocalDateTime.of(2022, 12, 1, 8, 12)
        // Act
        val newPosition = Position(12.0, 12.0)
        checkPositionTracker.actualPosition = newPosition
        // Assert
        assertFalse(observer.test().values().last() is MissedEtaTrackerEvent)
    }


    fun testStationaryTrackerEvent() {
        // Arrange
        val travel = mockk<Travel>()
        val route = Route(
            listOf(Position(12.0, 12.0), Position(13.0, 12.0), Position(14.0, 12.0)),
            mockk(),
            mockk()
        )
        val checkPositionTracker = CheckPositionTracker(travel, route)
        val observer = checkPositionTracker.run()
        every {
            travel.endAt
        } returns LocalDateTime.of(2022, 12, 1, 8, 12)
        // Act
        val newPosition = Position(13.0, 12.0)
        checkPositionTracker.actualPosition = newPosition
        checkPositionTracker.actualPosition = newPosition
        // Assert
        assert(observer.test().values().last() is StationaryTrackerEvent)
    }

    fun testLowBatteryTrackerEvent() {}

    fun testCompletedTravelTrackerEvent() {
        // Arrange
        val travel = mockk<Travel>()
        val route = Route(
            listOf(Position(12.0, 12.0), Position(13.0, 12.0), Position(14.0, 12.0)),
            mockk(),
            mockk()
        )
        val checkPositionTracker = CheckPositionTracker(travel, route)
        val observer = checkPositionTracker.run()
        every {
            travel.endAt
        } returns LocalDateTime.of(2022, 12, 1, 8, 12)
        // Act
        val newPosition = Position(14.0, 12.0)
        checkPositionTracker.actualPosition = newPosition
        // Assert
        assert(observer.test().values().last() is CompletedTravelTrackerEvent)
    }

    fun testNotCompletedTravelTrackerEvent() {
        // Arrange
        val travel = mockk<Travel>()
        val route = Route(
            listOf(Position(12.0, 12.0), Position(13.0, 12.0), Position(14.0, 12.0)),
            mockk(),
            mockk()
        )
        val checkPositionTracker = CheckPositionTracker(travel, route)
        val observer = checkPositionTracker.run()
        every {
            travel.endAt
        } returns LocalDateTime.of(2022, 12, 1, 8, 12)
        // Act
        val newPosition = Position(13.0, 12.0)
        checkPositionTracker.actualPosition = newPosition
        // Assert
        assert(observer.test().values().isEmpty())
    }
}