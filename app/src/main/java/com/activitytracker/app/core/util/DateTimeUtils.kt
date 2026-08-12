package com.activitytracker.app.core.util

import com.activitytracker.app.domain.model.ActivityCategory
import kotlinx.datetime.*

/**
 * Formatting helpers — all pure functions, no Android deps.
 */
object DateTimeUtils {

    val appTimeZone: TimeZone get() = TimeZone.currentSystemDefault()

    fun nowLocalDate(): LocalDate = Clock.System.now().toLocalDateTime(appTimeZone).date

    fun Instant.toLocalDateTime(): LocalDateTime = this.toLocalDateTime(appTimeZone)

    /**
     * Format a time range: "05:45 → 06:30"
     * Shows "..." for null endpoint
     */
    fun formatRange(start: Instant?, end: Instant?, use24h: Boolean = true): String {
        val s = start?.let { if (use24h) it.format24h() else it.format12h() } ?: "..."
        val e = end?.let { if (use24h) it.format24h() else it.format12h() } ?: "..."
        return "$s → $e"
    }

    /** Format LocalDate as "Wednesday, Aug 12" */
    fun LocalDate.toDisplayString(): String {
        val dayName = dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        val monthName = month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        return "$dayName, $monthName $dayOfMonth"
    }

    /** Format LocalDate as "Aug 12" */
    fun LocalDate.toShortDisplay(): String {
        val monthName = month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        return "$monthName $dayOfMonth"
    }

    /** Calculate duration in minutes between two Instants. Returns null if invalid. */
    fun durationMinutes(start: Instant, end: Instant): Int? {
        val mins = (end - start).inWholeMinutes
        return if (mins > 0) mins.toInt() else null
    }

    /**
     * Determine localDate for a SLEEP record.
     * Rule: localDate = wake-up date (endTime's local date).
     * If endTime is null, falls back to startTime's local date, then today.
     */
    fun sleepLocalDate(startTime: Instant?, endTime: Instant?): LocalDate {
        val reference = endTime ?: startTime ?: Clock.System.now()
        return reference.toLocalDateTime(appTimeZone).date
    }

    /**
     * Parses a "HH:mm" time string into an Instant.
     * If the parsed time is strictly greater than the current time, it assumes
     * the time belongs to the *previous* day (e.g., logging a 23:30 event at 00:15 AM).
     */
    fun parseTimeRelativeToday(timeHHmm: String): Instant? {
        val parts = timeHHmm.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null

        val nowLdt = Clock.System.now().toLocalDateTime(appTimeZone)
        val today = nowLdt.date
        var targetDate = today

        // If time is in the future, it likely refers to yesterday
        if (h > nowLdt.hour || (h == nowLdt.hour && m > nowLdt.minute)) {
            targetDate = today.minus(1, DateTimeUnit.DAY)
        }

        val ldt = LocalDateTime(targetDate.year, targetDate.month, targetDate.dayOfMonth, h, m, 0)
        return ldt.toInstant(appTimeZone)
    }
}

/** Format Instant as "HH:mm" (24-hour) */
fun Instant.format24h(): String {
    val ldt = this.toLocalDateTime(DateTimeUtils.appTimeZone)
    return "%02d:%02d".format(ldt.hour, ldt.minute)
}

/** Format Instant as "h:mm AM/PM" (12-hour) */
fun Instant.format12h(): String {
    val ldt = this.toLocalDateTime(DateTimeUtils.appTimeZone)
    val h = if (ldt.hour % 12 == 0) 12 else ldt.hour % 12
    val suffix = if (ldt.hour < 12) "AM" else "PM"
    return "%d:%02d %s".format(h, ldt.minute, suffix)
}

/** Map ActivityCategory to a display color (ARGB Int from theme) */
fun categoryColor(category: ActivityCategory): Long = when (category) {
    ActivityCategory.SADHANA  -> 0xFF9C6DFF
    ActivityCategory.SLEEP    -> 0xFF5C7CFF
    ActivityCategory.MEAL     -> 0xFFFF7A4A
    ActivityCategory.WORK     -> 0xFF4AABFF
    ActivityCategory.EXERCISE -> 0xFF4AE3B5
    ActivityCategory.PERSONAL -> 0xFF6BC950
    ActivityCategory.WAKE     -> 0xFFFFD04A
    ActivityCategory.OTHER    -> 0xFF9E9E9E
}

fun categoryLabel(category: ActivityCategory): String = when (category) {
    ActivityCategory.SADHANA  -> "Sadhana"
    ActivityCategory.SLEEP    -> "Sleep"
    ActivityCategory.MEAL     -> "Meal"
    ActivityCategory.WORK     -> "Work"
    ActivityCategory.EXERCISE -> "Exercise"
    ActivityCategory.PERSONAL -> "Personal"
    ActivityCategory.WAKE     -> "Wake"
    ActivityCategory.OTHER    -> "Other"
}
