package com.activitytracker.app.domain.usecase.screentime

import android.app.usage.UsageEvents
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ActivityInfo
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ScreenTimeCalculatorTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var calculator: ScreenTimeCalculator

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        
        context = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)
        every { context.packageManager } returns packageManager
        
        // Mock Launcher to return null to avoid stub exceptions on Info classes
        every { packageManager.resolveActivity(any(), any<PackageManager.ResolveInfoFlags>()) } returns null
        every { packageManager.resolveActivity(any(), any<Int>()) } returns null

        calculator = ScreenTimeCalculator(context)
    }

    private fun testIntervals(eventsList: List<Triple<Long, Int, String?>>, targetDate: LocalDate): Int {
        val midnight = targetDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        
        // Use standard testing bounds
        val targetBeginTime = midnight
        val capTime = midnight + (24L * 60 * 60 * 1000L) // End of day
        val searchStartTime = midnight - (24L * 60 * 60 * 1000L) // 24 hours prior
        
        val eventDataIterator = eventsList.map { 
            UsageEventData(it.first, it.second, it.third) 
        }.iterator()
        
        return calculator.calculateForegroundTimeMinutesInternal(
            targetBeginTime = targetBeginTime,
            capTime = capTime,
            searchStartTime = searchStartTime,
            events = eventDataIterator
        )
    }

    @Test
    fun `test exact boundaries - perfect single interval`() {
        val targetDate = LocalDate(2026, 8, 12)
        val midnight = targetDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        
        val startTs = midnight + (10 * 60 * 60 * 1000L)
        val endTs = midnight + (11 * 60 * 60 * 1000L)

        val eventsList = listOf(
            Triple(startTs, UsageEvents.Event.SCREEN_INTERACTIVE, null),
            Triple(startTs, UsageEvents.Event.KEYGUARD_HIDDEN, null),
            Triple(startTs, UsageEvents.Event.ACTIVITY_RESUMED, "com.example.app"),
            Triple(endTs, UsageEvents.Event.ACTIVITY_PAUSED, "com.example.app"),
            Triple(endTs, UsageEvents.Event.SCREEN_NON_INTERACTIVE, null)
        )

        val minutes = testIntervals(eventsList, targetDate)
        assertEquals("Should be exactly 60 minutes", 60, minutes)
    }

    @Test
    fun `test midnight boundary - usage starts before midnight and continues`() {
        val targetDate = LocalDate(2026, 8, 12)
        val midnight = targetDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        
        val startTs = midnight - (10 * 60 * 1000L) // 11:50 PM yesterday
        val endTs = midnight + (20 * 60 * 1000L)  // 12:20 AM today

        val eventsList = listOf(
            Triple(startTs, UsageEvents.Event.SCREEN_INTERACTIVE, null),
            Triple(startTs, UsageEvents.Event.KEYGUARD_HIDDEN, null),
            Triple(startTs, UsageEvents.Event.ACTIVITY_RESUMED, "com.example.app"),
            Triple(endTs, UsageEvents.Event.ACTIVITY_PAUSED, "com.example.app"),
            Triple(endTs, UsageEvents.Event.SCREEN_NON_INTERACTIVE, null)
        )

        val minutes = testIntervals(eventsList, targetDate)
        assertEquals("Should only count the 20 minutes that occurred on August 12", 20, minutes)
    }

    @Test
    fun `test split screen overlap - no double counting`() {
        val targetDate = LocalDate(2026, 8, 12)
        val midnight = targetDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        
        val ts1 = midnight + (10 * 60 * 1000L)
        val ts2 = midnight + (15 * 60 * 1000L)
        val ts3 = midnight + (20 * 60 * 1000L)
        val ts4 = midnight + (30 * 60 * 1000L)

        val eventsList = listOf(
            Triple(ts1, UsageEvents.Event.SCREEN_INTERACTIVE, null),
            Triple(ts1, UsageEvents.Event.KEYGUARD_HIDDEN, null),
            Triple(ts1, UsageEvents.Event.ACTIVITY_RESUMED, "com.app.A"), 
            Triple(ts2, UsageEvents.Event.ACTIVITY_RESUMED, "com.app.B"), 
            Triple(ts3, UsageEvents.Event.ACTIVITY_PAUSED, "com.app.A"),  
            Triple(ts4, UsageEvents.Event.ACTIVITY_PAUSED, "com.app.B"),  
            Triple(ts4, UsageEvents.Event.SCREEN_NON_INTERACTIVE, null)
        )

        val minutes = testIntervals(eventsList, targetDate)
        assertEquals("Overlap should not be double counted", 20, minutes)
    }
}

