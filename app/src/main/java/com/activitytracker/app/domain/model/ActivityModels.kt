package com.activitytracker.app.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Categories for activities. Extensible via ActivityType configuration.
 */
enum class ActivityCategory {
    WAKE, SLEEP, SADHANA, MEAL, WORK, EXERCISE, PERSONAL, OTHER
}

/**
 * Sub-categories — used for MEAL and WORK types.
 */
enum class ActivitySubCategory {
    // Meal
    BREAKFAST, LUNCH, DINNER, SNACK, WATER,
    // Work
    OFFICE, WORK_FROM_HOME, OTHER_WORK,
    // General
    NONE
}

/**
 * Status of a recorded activity.
 */
enum class ActivityStatus {
    ONGOING,    // started but not finished
    COMPLETED,  // fully recorded with end time
    MISSED,     // planned but not done
    SKIPPED     // user marked as intentionally skipped
}

/**
 * A user-configurable activity type (e.g. "Morning Japa", "Office Work", "Breakfast").
 * This is the template; ActivityRecord is the instance.
 */
data class ActivityType(
    val id: String,
    val name: String,
    val category: ActivityCategory,
    val subCategory: ActivitySubCategory = ActivitySubCategory.NONE,
    val iconKey: String? = null,
    val colorArgb: Int? = null,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * A single recorded activity event.
 *
 * Key design decisions:
 * - [localDate] is the date this record "belongs to" for dashboard purposes.
 *   For SLEEP records specifically, this is the WAKE-UP date (so Day 2's dashboard
 *   shows last night's sleep).
 * - [startTime] and [endTime] are epoch-ms UTC — DST-safe and sortable.
 * - [durationMinutes] is stored redundantly for fast aggregation; always recalculated on edit.
 * - Overlapping activities are explicitly allowed.
 */
data class ActivityRecord(
    val id: String,
    val localDate: LocalDate,
    val activityTypeId: String,
    val activityType: ActivityType? = null, // joined when needed
    val startTime: Instant?,
    val endTime: Instant?,
    val durationMinutes: Int?,
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val isManuallyEdited: Boolean = false,
    val sadhanaPlanId: String? = null,
    val status: ActivityStatus = ActivityStatus.COMPLETED,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    /** Convenience: is this a sleep record? */
    val isSleep: Boolean get() = activityType?.category == ActivityCategory.SLEEP

    /** Convenience: is this a sadhana record? */
    val isSadhana: Boolean get() = activityType?.category == ActivityCategory.SADHANA

    /** Human-readable duration string: "45 min", "1h 30m", "2h 00m" */
    val durationDisplay: String get() {
        val mins = durationMinutes ?: return "--"
        return formatDurationMinutes(mins)
    }
}

fun formatDurationMinutes(totalMinutes: Int): String {
    if (totalMinutes <= 0) return "0 min"
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return when {
        hours == 0 -> "$mins min"
        mins == 0 -> "${hours}h"
        else -> "${hours}h ${mins}m"
    }
}

/**
 * Parses a user-entered duration string into total minutes.
 * Accepts: "45" → 45min, "1.5" → 90min, "90" → 90min, "1h30" → 90min, "1:30" → 90min
 */
fun parseDurationInput(input: String): Int? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null

    // "1h30m", "1h30", "1h 30m" patterns
    val hPattern = Regex("""(\d+)\s*h\s*(\d*)\s*m?""", RegexOption.IGNORE_CASE)
    hPattern.matchEntire(trimmed)?.let { m ->
        val h = m.groupValues[1].toIntOrNull() ?: 0
        val min = m.groupValues[2].toIntOrNull() ?: 0
        return h * 60 + min
    }

    // "1:30" pattern
    val colonPattern = Regex("""(\d+):(\d{2})""")
    colonPattern.matchEntire(trimmed)?.let { m ->
        val h = m.groupValues[1].toIntOrNull() ?: 0
        val min = m.groupValues[2].toIntOrNull() ?: 0
        return h * 60 + min
    }

    // Plain number: integer (45 → 45 min) vs decimal (1.5 → 90 min)
    if (trimmed.contains('.')) {
        trimmed.toDoubleOrNull()?.let { d ->
            return (d * 60).toInt().coerceAtLeast(1)
        }
    } else {
        trimmed.toIntOrNull()?.let { i ->
            return i.coerceAtLeast(1)
        }
    }

    return null
}
