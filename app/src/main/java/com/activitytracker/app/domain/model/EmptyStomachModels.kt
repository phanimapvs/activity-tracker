package com.activitytracker.app.domain.model

import kotlinx.datetime.Instant

/**
 * Intake classification for Empty Stomach condition calculation.
 */
enum class IntakeType {
    WATER,       // Default recommended gap: 20 min
    LIGHT_FOOD,  // Default recommended gap: 2.5 hours (150 min)
    FULL_MEAL    // Default recommended gap: 4 hours (240 min)
}

/**
 * Visual readiness status states.
 */
enum class EmptyStomachStatus {
    READY,      // Minimum required gap has passed
    NOT_READY,  // Minimum required gap has NOT passed
    UNKNOWN     // No intake records recorded today or previous evening
}

/**
 * Configurable thresholds for empty-stomach readiness calculation.
 */
data class EmptyStomachConfig(
    val waterGapMinutes: Int = 20,
    val lightFoodGapMinutes: Int = 150,     // 2h 30m
    val fullMealGapMinutes: Int = 240,       // 4h
    val isEnabled: Boolean = true
)

/**
 * Calculated result from EmptyStomachCalculator.
 */
data class PracticeReadinessResult(
    val status: EmptyStomachStatus,
    val controllingIntakeRecord: ActivityRecord?,
    val controllingIntakeType: IntakeType?,
    val lastIntakeTime: Instant?,
    val elapsedMinutes: Int?,
    val requiredGapMinutes: Int?,
    val remainingMinutes: Int?,
    val readyAt: Instant?,
    val sadhanaName: String? = null,
    val isSadhanaSpecific: Boolean = false
) {
    val isReady: Boolean get() = status == EmptyStomachStatus.READY
}
