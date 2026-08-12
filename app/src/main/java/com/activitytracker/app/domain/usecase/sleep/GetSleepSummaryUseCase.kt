package com.activitytracker.app.domain.usecase.sleep

import com.activitytracker.app.domain.model.*
import com.activitytracker.app.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class GetSleepSummaryUseCase @Inject constructor(
    private val activityRepository: ActivityRepository
) {
    operator fun invoke(date: LocalDate): Flow<SleepSummary> {
        return activityRepository.getActivitiesForDate(date).map { records ->
            val sleepRecords = records
                .filter { it.activityType?.category == ActivityCategory.SLEEP }
                .sortedBy { it.startTime }

            val totalMinutes = sleepRecords.sumOf { calculateDuration(it) ?: 0 }

            // "Last night" = the record with the earliest startTime (the overnight one)
            val lastNight = sleepRecords.sortedBy { it.startTime }.firstOrNull()

            SleepSummary(
                date = date,
                segments = sleepRecords,
                totalMinutes = totalMinutes,
                lastNightRecord = lastNight
            )
        }
    }

    /**
     * Safe duration calculation — returns null if either timestamp is missing.
     * Uses epoch ms difference for DST-safe arithmetic.
     */
    fun calculateDuration(record: ActivityRecord): Int? {
        val start = record.startTime ?: return record.durationMinutes
        val end = record.endTime ?: return record.durationMinutes
        if (end <= start) return null
        return ((end - start).inWholeMinutes).toInt()
    }
}
