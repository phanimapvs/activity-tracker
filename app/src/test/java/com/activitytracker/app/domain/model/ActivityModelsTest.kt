package com.activitytracker.app.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ActivityModelsTest {

    @Test
    fun `formatDurationMinutes - zero returns 0 min`() {
        assertEquals("0 min", formatDurationMinutes(0))
    }

    @Test
    fun `formatDurationMinutes - minutes only`() {
        assertEquals("45 min", formatDurationMinutes(45))
    }

    @Test
    fun `formatDurationMinutes - exact hours`() {
        assertEquals("2h", formatDurationMinutes(120))
    }

    @Test
    fun `formatDurationMinutes - hours and minutes`() {
        assertEquals("1h 30m", formatDurationMinutes(90))
        assertEquals("6h 42m", formatDurationMinutes(402))
    }

    @Test
    fun `parseDurationInput - plain integer minutes`() {
        assertEquals(45, parseDurationInput("45"))
        assertEquals(90, parseDurationInput("90"))
    }

    @Test
    fun `parseDurationInput - decimal hours`() {
        assertEquals(90, parseDurationInput("1.5"))
        assertEquals(30, parseDurationInput("0.5"))
        assertEquals(150, parseDurationInput("2.5"))
    }

    @Test
    fun `parseDurationInput - hour+minute pattern`() {
        assertEquals(90, parseDurationInput("1h30m"))
        assertEquals(90, parseDurationInput("1h30"))
        assertEquals(90, parseDurationInput("1h 30m"))
    }

    @Test
    fun `parseDurationInput - colon pattern`() {
        assertEquals(90, parseDurationInput("1:30"))
        assertEquals(405, parseDurationInput("6:45"))
    }

    @Test
    fun `parseDurationInput - blank returns null`() {
        assertNull(parseDurationInput(""))
        assertNull(parseDurationInput("   "))
    }

    @Test
    fun `parseDurationInput - invalid returns null`() {
        assertNull(parseDurationInput("abc"))
    }

    @Test
    fun `parseDurationInput - whitespace trimmed`() {
        assertEquals(45, parseDurationInput("  45  "))
    }
}
