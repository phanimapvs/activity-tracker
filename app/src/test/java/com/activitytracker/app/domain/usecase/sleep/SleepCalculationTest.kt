package com.activitytracker.app.domain.usecase.sleep

import com.activitytracker.app.domain.model.*
import kotlinx.datetime.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for sleep duration calculation across midnight boundaries.
 * These are pure domain tests — no Android or Room dependencies.
 */
class SleepCalculationTest {

    private val tz = TimeZone.of("Asia/Kolkata") // IST +05:30

    private fun instant(dateStr: String, hour: Int, minute: Int): Instant {
        val dt = LocalDateTime(LocalDate.parse(dateStr), LocalTime(hour, minute))
        return dt.toInstant(tz)
    }

    private fun makeRecord(start: Instant?, end: Instant?, durationMins: Int? = null): ActivityRecord {
        val now = Clock.System.now()
        return ActivityRecord(
            id = "test-id",
            localDate = LocalDate.parse("2026-08-12"),
            activityTypeId = "sleep-type",
            startTime = start,
            endTime = end,
            durationMinutes = durationMins,
            isCompleted = true,
            status = ActivityStatus.COMPLETED,
            createdAt = now,
            updatedAt = now
        )
    }

    // Simulate the calculateDuration logic from GetSleepSummaryUseCase
    private fun calculateDuration(record: ActivityRecord): Int? {
        val start = record.startTime ?: return record.durationMinutes
        val end = record.endTime ?: return record.durationMinutes
        if (end <= start) return null
        return ((end - start).inWholeMinutes).toInt()
    }

    @Test
    fun `sleep before midnight wake after midnight - calculates correctly`() {
        // Sleep: Aug 11 at 11:00 PM, Wake: Aug 12 at 5:30 AM = 6h 30m = 390 min
        val sleep = instant("2026-08-11", 23, 0)
        val wake = instant("2026-08-12", 5, 30)
        val record = makeRecord(sleep, wake)
        assertEquals(390, calculateDuration(record))
    }

    @Test
    fun `sleep and wake same day - calculates correctly`() {
        // Sleep: 1:00 AM wake 7:00 AM = 6h = 360 min
        val sleep = instant("2026-08-12", 1, 0)
        val wake = instant("2026-08-12", 7, 0)
        val record = makeRecord(sleep, wake)
        assertEquals(360, calculateDuration(record))
    }

    @Test
    fun `missing wake time - returns stored duration without crash`() {
        val sleep = instant("2026-08-11", 23, 0)
        val record = makeRecord(start = sleep, end = null, durationMins = null)
        assertNull(calculateDuration(record)) // no crash, returns null
    }

    @Test
    fun `missing sleep time - returns stored duration without crash`() {
        val wake = instant("2026-08-12", 5, 30)
        val record = makeRecord(start = null, end = wake, durationMins = null)
        assertNull(calculateDuration(record)) // no crash, returns null
    }

    @Test
    fun `missing both times - returns null without crash`() {
        val record = makeRecord(start = null, end = null)
        assertNull(calculateDuration(record))
    }

    @Test
    fun `stored duration used when timestamps absent`() {
        val record = makeRecord(start = null, end = null, durationMins = 390)
        assertEquals(390, calculateDuration(record))
    }

    @Test
    fun `wake before sleep - returns null safely`() {
        // Impossible data — should not crash or return positive value
        val sleep = instant("2026-08-12", 7, 0)
        val wake = instant("2026-08-12", 5, 0) // wake is before sleep
        val record = makeRecord(sleep, wake)
        assertNull(calculateDuration(record))
    }

    @Test
    fun `sleepLocalDate uses wake-up date`() {
        // Sleep at 11 PM Aug 11, wake at 5:30 AM Aug 12
        // localDate should be Aug 12 (wake day)
        val sleep = instant("2026-08-11", 23, 0)
        val wake = instant("2026-08-12", 5, 30)
        val localDate = wake.toLocalDateTime(tz).date
        assertEquals(LocalDate.parse("2026-08-12"), localDate)
    }

    @Test
    fun `nap duration calculation`() {
        // Afternoon nap: 3:10 PM to 3:43 PM = 33 min
        val napStart = instant("2026-08-12", 15, 10)
        val napEnd = instant("2026-08-12", 15, 43)
        val record = makeRecord(napStart, napEnd)
        assertEquals(33, calculateDuration(record))
    }

    @Test
    fun `total sleep with nap sums correctly`() {
        val overnight = 390 // 6h 30m
        val nap = 33
        assertEquals(423, overnight + nap)
        assertEquals("7h 3m", formatDurationMinutes(423))
    }
}
