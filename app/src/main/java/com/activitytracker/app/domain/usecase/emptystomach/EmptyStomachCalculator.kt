package com.activitytracker.app.domain.usecase.emptystomach

import com.activitytracker.app.domain.model.*
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/**
 * Domain service engine for evaluating Empty Stomach Condition & Practice Readiness.
 *
 * All calculations are DST-safe pure functional operations based strictly on configured
 * food/water intake intervals.
 */
class EmptyStomachCalculator @Inject constructor() {

    fun calculateReadiness(
        now: Instant,
        intakeRecords: List<ActivityRecord>,
        config: EmptyStomachConfig,
        sadhanaRequirementMinutes: Int? = null,
        sadhanaName: String? = null
    ): PracticeReadinessResult {
        // Filter relevant intake records
        val validIntakes = intakeRecords.filter { record ->
            val cat = record.activityType?.category
            val sub = record.activityType?.subCategory
            val name = record.activityType?.name?.lowercase() ?: ""
            cat == ActivityCategory.MEAL ||
                    sub == ActivitySubCategory.WATER ||
                    name.contains("water") ||
                    name.contains("drink") ||
                    name.contains("tea") ||
                    name.contains("coffee") ||
                    record.startTime != null
        }.filter { record ->
            record.startTime != null && record.startTime <= now
        }.sortedByDescending { it.startTime }

        if (validIntakes.isEmpty()) {
            return PracticeReadinessResult(
                status = EmptyStomachStatus.UNKNOWN,
                controllingIntakeRecord = null,
                controllingIntakeType = null,
                lastIntakeTime = null,
                elapsedMinutes = null,
                requiredGapMinutes = null,
                remainingMinutes = null,
                readyAt = null,
                sadhanaName = sadhanaName,
                isSadhanaSpecific = sadhanaRequirementMinutes != null
            )
        }

        // Rule evaluation: Find the intake constraint requiring the most remaining time
        var highestRemainingMins = -1
        var controllingRecord: ActivityRecord = validIntakes.first()
        var controllingType: IntakeType = classifyIntake(controllingRecord)
        var controllingRequiredGap = 0
        var controllingElapsedMins = 0
        var controllingReadyAt: Instant = now

        for (record in validIntakes) {
            val startTime = record.startTime ?: continue
            val intakeType = classifyIntake(record)
            val requiredGap = when {
                sadhanaRequirementMinutes != null && intakeType == IntakeType.FULL_MEAL -> sadhanaRequirementMinutes
                intakeType == IntakeType.WATER -> config.waterGapMinutes
                intakeType == IntakeType.LIGHT_FOOD -> config.lightFoodGapMinutes
                else -> config.fullMealGapMinutes
            }

            val elapsedMins = ((now - startTime).inWholeMinutes).toInt()
            val remainingMins = (requiredGap - elapsedMins).coerceAtLeast(0)
            val readyAtInstant = startTime + requiredGap.minutes

            if (remainingMins > highestRemainingMins) {
                highestRemainingMins = remainingMins
                controllingRecord = record
                controllingType = intakeType
                controllingRequiredGap = requiredGap
                controllingElapsedMins = elapsedMins
                controllingReadyAt = readyAtInstant
            }
        }

        val status = if (highestRemainingMins > 0) EmptyStomachStatus.NOT_READY else EmptyStomachStatus.READY

        return PracticeReadinessResult(
            status = status,
            controllingIntakeRecord = controllingRecord,
            controllingIntakeType = controllingType,
            lastIntakeTime = controllingRecord.startTime,
            elapsedMinutes = controllingElapsedMins,
            requiredGapMinutes = controllingRequiredGap,
            remainingMinutes = highestRemainingMins,
            readyAt = controllingReadyAt,
            sadhanaName = sadhanaName,
            isSadhanaSpecific = sadhanaRequirementMinutes != null
        )
    }

    /**
     * Classifies an ActivityRecord into an IntakeType (WATER, LIGHT_FOOD, FULL_MEAL).
     */
    fun classifyIntake(record: ActivityRecord): IntakeType {
        val sub = record.activityType?.subCategory
        val name = record.activityType?.name?.lowercase() ?: ""

        if (sub == ActivitySubCategory.WATER || name.contains("water") || name.contains("drink")) {
            return IntakeType.WATER
        }
        if (sub == ActivitySubCategory.SNACK || name.contains("snack") || name.contains("fruit") || name.contains("tea") || name.contains("coffee")) {
            return IntakeType.LIGHT_FOOD
        }
        return IntakeType.FULL_MEAL
    }
}
