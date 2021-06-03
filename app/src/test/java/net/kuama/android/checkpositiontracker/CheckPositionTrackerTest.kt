package net.kuama.android.checkpositiontracker

import junit.framework.TestCase
import io.mockk.*


class CheckPositionTrackerTest : TestCase() {

    private val travel = mockk<Travel>()
    private val route = mockk<Route>()
    private val checkPositionTracker = CheckPositionTracker(travel, route)

    fun testOutRouteTrackerEvent() {
        // Arrange

        // Act

        // Assert
    }

    fun testMissedEtaTrackerEvent() {}

    fun testStationaryTrackerEvent() {}

    fun testLowBatteryTrackerEvent() {}

    fun testCompletedTravelTrackerEvent() {}
}