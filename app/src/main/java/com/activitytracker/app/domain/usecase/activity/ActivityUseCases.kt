package com.activitytracker.app.domain.usecase.activity

import com.activitytracker.app.domain.model.*
import com.activitytracker.app.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/**
 * Quick-start an activity: records startTime = now, status = ONGOING.
 * Used by quick-action buttons (Start Sadhana, Start Work, etc.)
 */
class StartActivityUseCase @Inject constructor(
    private val repository: ActivityRepository
) {
    suspend operator fun invoke(
        activityTypeId: String,
        localDate: LocalDate,
        sadhanaPlanId: String? = null
    ): ActivityRecord {
        val now = Clock.System.now()
        val record = ActivityRecord(
            id = UUID.randomUUID().toString(),
            localDate = localDate,
            activityTypeId = activityTypeId,
            startTime = now,
            endTime = null,
            durationMinutes = null,
            isCompleted = false,
            sadhanaPlanId = sadhanaPlanId,
            status = ActivityStatus.ONGOING,
            createdAt = now,
            updatedAt = now
        )
        repository.saveActivity(record)
        return record
    }
}

/**
 * Finish an activity: records endTime = now, calculates duration, marks completed.
 * Called when user toggles sadhana ON or taps "End Activity".
 *
 * If [enteredDurationMinutes] is provided (from the sadhana toggle dialog),
 * we derive startTime = endTime - duration (if startTime was not set).
 */
class FinishActivityUseCase @Inject constructor(
    private val repository: ActivityRepository
) {
    suspend operator fun invoke(
        record: ActivityRecord,
        enteredDurationMinutes: Int? = null,
        notes: String? = null
    ): ActivityRecord {
        val now = Clock.System.now()

        val durationMins: Int?
        val startTime: kotlinx.datetime.Instant?
        val endTime = now

        when {
            // Duration was manually entered (sadhana toggle dialog)
            enteredDurationMinutes != null -> {
                durationMins = enteredDurationMinutes
                startTime = record.startTime ?: (now - enteredDurationMinutes.minutes)
            }
            // Both timestamps exist → calculate
            record.startTime != null -> {
                startTime = record.startTime
                durationMins = ((endTime - startTime).inWholeMinutes).toInt()
                    .takeIf { it > 0 }
            }
            else -> {
                startTime = null
                durationMins = null
            }
        }

        val updated = record.copy(
            startTime = startTime,
            endTime = endTime,
            durationMinutes = durationMins,
            notes = notes ?: record.notes,
            isCompleted = true,
            status = ActivityStatus.COMPLETED,
            updatedAt = now
        )
        repository.saveActivity(updated)
        return updated
    }
}

/**
 * Save a complete activity in one call (used by AddEdit form).
 */
class SaveActivityUseCase @Inject constructor(
    private val repository: ActivityRepository
) {
    suspend operator fun invoke(record: ActivityRecord) {
        // Recalculate duration if both times are present
        val finalRecord = if (record.startTime != null && record.endTime != null) {
            val dur = ((record.endTime - record.startTime).inWholeMinutes).toInt()
            record.copy(
                durationMinutes = if (dur > 0) dur else record.durationMinutes,
                updatedAt = Clock.System.now()
            )
        } else {
            record.copy(updatedAt = Clock.System.now())
        }
        repository.saveActivity(finalRecord)
    }
}

/** Get all activities for a given date as a Flow. */
class GetDailyActivitiesUseCase @Inject constructor(
    private val repository: ActivityRepository
) {
    operator fun invoke(date: LocalDate): Flow<List<ActivityRecord>> =
        repository.getActivitiesForDate(date)
}
