package com.activitytracker.app.domain.usecase.stats

import com.activitytracker.app.domain.model.*
import com.activitytracker.app.domain.repository.ActivityRepository
import com.activitytracker.app.domain.repository.SadhanaPlanRepository
import com.activitytracker.app.domain.repository.ScreenTimeRepository
import com.activitytracker.app.domain.usecase.sleep.GetSleepSummaryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class GetDailyStatsUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val sadhanaPlanRepository: SadhanaPlanRepository,
    private val getSleepSummary: GetSleepSummaryUseCase,
    private val screenTimeRepository: ScreenTimeRepository
) {
    operator fun invoke(date: LocalDate): Flow<DailyStats> {
        return combine(
            activityRepository.getActivitiesForDate(date),
            sadhanaPlanRepository.getAllActivePlans(),
            getSleepSummary(date),
            screenTimeRepository.getScreenTimeFlow(date.toString())
        ) { records, plans, sleep, screenTimeEntity ->
            computeStats(date, records, plans, sleep, screenTimeEntity?.totalDurationMinutes)
        }.distinctUntilChanged()
    }

    private fun computeStats(
        date: LocalDate,
        records: List<ActivityRecord>,
        plans: List<SadhanaPlan>,
        sleep: SleepSummary,
        screenTimeMinutes: Int?
    ): DailyStats {
        val completed = records.filter { it.isCompleted }

        // Sadhana
        val sadhanaRecords = completed.filter { it.activityType?.category == ActivityCategory.SADHANA }
        val totalSadhanaMins = sadhanaRecords.sumOf { it.durationMinutes ?: 0 }

        // Match plans to actual sessions
        val plannedVsActual = plans.map { plan ->
            val matched = sadhanaRecords.firstOrNull { record ->
                record.sadhanaPlanId == plan.id ||
                record.activityType?.name == plan.name
            }
            PlannedVsActual(plan = plan, actualRecord = matched)
        }

        // Meals
        fun mealTime(sub: ActivitySubCategory) = records
            .firstOrNull { it.activityType?.subCategory == sub }
            ?.startTime

        // Work
        val workRecords = records.filter { it.activityType?.category == ActivityCategory.WORK }
        val totalWorkMins = workRecords.sumOf { it.durationMinutes ?: 0 }

        // Wake time
        val wakeTime = records
            .firstOrNull { it.activityType?.category == ActivityCategory.WAKE }
            ?.startTime

        // Activity category breakdown (minutes per category)
        val breakdown = records
            .groupBy { it.activityType?.category ?: ActivityCategory.OTHER }
            .mapValues { (_, recs) -> recs.sumOf { it.durationMinutes ?: 0 } }

        // Gaps between consecutive non-sleep records sorted by startTime
        val timedRecords = records
            .filter { it.startTime != null && it.activityType?.category != ActivityCategory.SLEEP }
            .sortedBy { it.startTime }
        val gaps = timedRecords.zipWithNext { a, b ->
            val from = a.endTime ?: a.startTime ?: return@zipWithNext null
            val to = b.startTime ?: return@zipWithNext null
            val gapMins = ((to - from).inWholeMinutes).toInt()
            if (gapMins > 0) ActivityGap(a, b, gapMins) else null
        }.filterNotNull()

        return DailyStats(
            date = date,
            sleepSummary = sleep,
            totalSadhanaDurationMinutes = totalSadhanaMins,
            sadhanaSessions = sadhanaRecords.size,
            plannedSessions = plannedVsActual,
            completedPlannedCount = plannedVsActual.count { it.isCompleted },
            missedPlannedCount = plannedVsActual.count { !it.isCompleted },
            breakfastTime = mealTime(ActivitySubCategory.BREAKFAST),
            lunchTime = mealTime(ActivitySubCategory.LUNCH),
            dinnerTime = mealTime(ActivitySubCategory.DINNER),
            totalWorkDurationMinutes = totalWorkMins,
            wakeTime = wakeTime,
            activityBreakdown = breakdown,
            longestSadhanaMinutes = sadhanaRecords.maxOfOrNull { it.durationMinutes ?: 0 } ?: 0,
            gaps = gaps,
            mobileScreenTimeMinutes = screenTimeMinutes
        )
    }
}
