package com.activitytracker.app.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * A recurring sadhana plan / target.
 * This is a TEMPLATE — not a calendar entry. Each day, planned vs actual is computed at runtime.
 */
data class SadhanaPlan(
    val id: String,
    val name: String,
    val description: String? = null,
    val activityTypeId: String,
    val targetDurationMinutes: Int? = null,
    val targetSessionsPerDay: Int = 1,
    val timeSlot: TimeSlot = TimeSlot.ANYTIME,
    val preferredStartHour: Int? = null,
    val preferredStartMinute: Int? = null,
    /** If set, this plan should happen ~[gapMinutes] after the activity type with this ID */
    val gapAfterActivityTypeId: String? = null,
    val gapAfterActivityMinutes: Int? = null,
    val isActive: Boolean = true,
    val isDaily: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class TimeSlot {
    MORNING,         // Before noon
    AFTER_BREAKFAST, // After breakfast activity
    AFTERNOON,       // 12:00–16:00
    EVENING,         // 16:00–20:00
    NIGHT,           // 20:00+
    ANYTIME
}

/**
 * A daily note — optional free text for the day.
 */
data class DailyNote(
    val id: String,
    val localDate: LocalDate,
    val note: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * A gap between two consecutive activities.
 */
data class ActivityGap(
    val fromRecord: ActivityRecord,
    val toRecord: ActivityRecord,
    val gapMinutes: Int
) {
    val display: String get() = formatDurationMinutes(gapMinutes)
}

/**
 * A sadhana plan paired with its matching actual record (if any) for a given day.
 */
data class PlannedVsActual(
    val plan: SadhanaPlan,
    val actualRecord: ActivityRecord? = null
) {
    val isCompleted: Boolean get() = actualRecord?.isCompleted == true
    val completionPercent: Int? get() {
        val target = plan.targetDurationMinutes ?: return null
        val actual = actualRecord?.durationMinutes ?: return 0
        return ((actual.toFloat() / target) * 100).toInt().coerceIn(0, 999)
    }
}

/**
 * Sleep summary for a given day (may include multiple segments: overnight + naps).
 */
data class SleepSummary(
    val date: LocalDate,
    val segments: List<ActivityRecord>,  // all SLEEP records for this date
    val totalMinutes: Int,
    val lastNightRecord: ActivityRecord? // the SLEEP record with earliest startTime
) {
    val hasSleep: Boolean get() = segments.isNotEmpty()
    val totalDisplay: String get() = if (totalMinutes > 0) formatDurationMinutes(totalMinutes) else "--"
}

/**
 * Computed daily statistics for a given date.
 */
data class DailyStats(
    val date: LocalDate,
    // Sleep
    val sleepSummary: SleepSummary,
    // Sadhana
    val totalSadhanaDurationMinutes: Int = 0,
    val sadhanaSessions: Int = 0,
    val plannedSessions: List<PlannedVsActual> = emptyList(),
    val completedPlannedCount: Int = 0,
    val missedPlannedCount: Int = 0,
    // Meals
    val breakfastTime: Instant? = null,
    val lunchTime: Instant? = null,
    val dinnerTime: Instant? = null,
    // Work
    val totalWorkDurationMinutes: Int = 0,
    // Misc
    val wakeTime: Instant? = null,
    val activityBreakdown: Map<ActivityCategory, Int> = emptyMap(),
    val longestSadhanaMinutes: Int = 0,
    val gaps: List<ActivityGap> = emptyList(),
    val mobileScreenTimeMinutes: Int? = null
) {
    val sadhanaProgressPercent: Int get() {
        val target = plannedSessions.sumOf { it.plan.targetDurationMinutes ?: 0 }
        if (target == 0) return 0
        return ((totalSadhanaDurationMinutes.toFloat() / target) * 100).toInt().coerceIn(0, 100)
    }
}
