package com.activitytracker.app.domain.usecase.emptystomach

import com.activitytracker.app.domain.model.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class EmptyStomachCalculatorTest {

    private val calculator = EmptyStomachCalculator()
    private val defaultConfig = EmptyStomachConfig(
        waterGapMinutes = 20,
        lightFoodGapMinutes = 150,     // 2h 30m
        fullMealGapMinutes = 240       // 4h
    )

    private val baseDate = LocalDate(2026, 8, 12)
    // 2026-08-12 12:00:00 UTC = 1786536000000 ms
    private val now = Instant.fromEpochMilliseconds(1786536000000)

    private fun createRecord(
        category: ActivityCategory,
        subCategory: ActivitySubCategory,
        name: String,
        startTime: Instant
    ) = ActivityRecord(
        id = UUID.randomUUID().toString(),
        localDate = baseDate,
        activityTypeId = "type_id",
        activityType = ActivityType(
            id = "type_id",
            name = name,
            category = category,
            subCategory = subCategory,
            createdAt = startTime,
            updatedAt = startTime
        ),
        startTime = startTime,
        endTime = null,
        durationMinutes = null,
        isCompleted = true,
        status = ActivityStatus.COMPLETED,
        createdAt = startTime,
        updatedAt = startTime
    )

    // Case 1: Breakfast at 7:00 AM, Sadhana at 11:30 AM (4h 30m elapsed -> Ready)
    @Test
    fun `test Case 1 - Breakfast 4h30m elapsed is READY`() {
        val breakfastTime = now - 4.hours - 30.minutes
        val record = createRecord(ActivityCategory.MEAL, ActivitySubCategory.BREAKFAST, "Breakfast", breakfastTime)

        val result = calculator.calculateReadiness(now, listOf(record), defaultConfig)

        assertEquals(EmptyStomachStatus.READY, result.status)
        assertTrue(result.isReady)
        assertEquals(270, result.elapsedMinutes)
        assertEquals(0, result.remainingMinutes)
    }

    // Case 2: Lunch at 1:00 PM, Sadhana at 4:00 PM (3h elapsed -> Not Ready)
    @Test
    fun `test Case 2 - Lunch 3h elapsed is NOT_READY`() {
        val lunchTime = now - 3.hours
        val record = createRecord(ActivityCategory.MEAL, ActivitySubCategory.LUNCH, "Lunch", lunchTime)

        val result = calculator.calculateReadiness(now, listOf(record), defaultConfig)

        assertEquals(EmptyStomachStatus.NOT_READY, result.status)
        assertFalse(result.isReady)
        assertEquals(180, result.elapsedMinutes)
        assertEquals(60, result.remainingMinutes)
    }

    // Case 3: Snack at 3:00 PM, Sadhana at 6:00 PM (3h elapsed vs 2h30m req -> Ready)
    @Test
    fun `test Case 3 - Snack 3h elapsed is READY`() {
        val snackTime = now - 3.hours
        val record = createRecord(ActivityCategory.MEAL, ActivitySubCategory.SNACK, "Snack", snackTime)

        val result = calculator.calculateReadiness(now, listOf(record), defaultConfig)

        assertEquals(EmptyStomachStatus.READY, result.status)
        assertTrue(result.isReady)
        assertEquals(180, result.elapsedMinutes)
        assertEquals(0, result.remainingMinutes)
    }

    // Case 4: Water at 6:00 AM, Sadhana at 6:10 AM (10m elapsed vs 20m req -> Not Ready)
    @Test
    fun `test Case 4 - Water 10m elapsed is NOT_READY`() {
        val waterTime = now - 10.minutes
        val record = createRecord(ActivityCategory.MEAL, ActivitySubCategory.WATER, "Water Intake", waterTime)

        val result = calculator.calculateReadiness(now, listOf(record), defaultConfig)

        assertEquals(EmptyStomachStatus.NOT_READY, result.status)
        assertEquals(10, result.elapsedMinutes)
        assertEquals(10, result.remainingMinutes)
    }

    // Case 5: Water at 6:00 AM, Sadhana at 6:30 AM (30m elapsed vs 20m req -> Ready)
    @Test
    fun `test Case 5 - Water 30m elapsed is READY`() {
        val waterTime = now - 30.minutes
        val record = createRecord(ActivityCategory.MEAL, ActivitySubCategory.WATER, "Water Intake", waterTime)

        val result = calculator.calculateReadiness(now, listOf(record), defaultConfig)

        assertEquals(EmptyStomachStatus.READY, result.status)
        assertEquals(30, result.elapsedMinutes)
        assertEquals(0, result.remainingMinutes)
    }

    // Case 6: Dinner at 8:00 PM yesterday, 10h elapsed (4h req met -> READY with 10h elapsed count)
    @Test
    fun `test Case 6 - Dinner 10h elapsed is READY`() {
        val dinnerTime = now - 10.hours
        val record = createRecord(ActivityCategory.MEAL, ActivitySubCategory.DINNER, "Dinner", dinnerTime)

        val result = calculator.calculateReadiness(now, listOf(record), defaultConfig)

        assertEquals(EmptyStomachStatus.READY, result.status)
        assertTrue(result.isReady)
        assertEquals(600, result.elapsedMinutes)
    }

    // Case 7: Dinner at 10:00 PM yesterday, 2h 30m elapsed (4h req -> Not Ready)
    @Test
    fun `test Case 7 - Dinner 2h30m elapsed is NOT_READY`() {
        val dinnerTime = now - 2.hours - 30.minutes
        val record = createRecord(ActivityCategory.MEAL, ActivitySubCategory.DINNER, "Dinner", dinnerTime)

        val result = calculator.calculateReadiness(now, listOf(record), defaultConfig)

        assertEquals(EmptyStomachStatus.NOT_READY, result.status)
        assertFalse(result.isReady)
        assertEquals(150, result.elapsedMinutes)
        assertEquals(90, result.remainingMinutes)
    }

    // Case 8: No meals recorded -> UNKNOWN
    @Test
    fun `test Case 8 - No intake records returns UNKNOWN`() {
        val result = calculator.calculateReadiness(now, emptyList(), defaultConfig)

        assertEquals(EmptyStomachStatus.UNKNOWN, result.status)
        assertFalse(result.isReady)
        assertNull(result.elapsedMinutes)
    }

    // Case 9: Multi-intake controlling constraint (Meal 2h ago + Water 30m ago -> NOT_READY due to Meal)
    @Test
    fun `test Case 9 - Meal 2h ago and Water 30m ago flags NOT_READY for meal`() {
        val mealTime = now - 2.hours
        val waterTime = now - 30.minutes

        val mealRecord = createRecord(ActivityCategory.MEAL, ActivitySubCategory.LUNCH, "Lunch", mealTime)
        val waterRecord = createRecord(ActivityCategory.MEAL, ActivitySubCategory.WATER, "Water Intake", waterTime)

        val result = calculator.calculateReadiness(now, listOf(mealRecord, waterRecord), defaultConfig)

        assertEquals(EmptyStomachStatus.NOT_READY, result.status)
        assertEquals(IntakeType.FULL_MEAL, result.controllingIntakeType)
        assertEquals(120, result.remainingMinutes) // 4h - 2h = 2h (120m) remaining
    }

    // Case 10: Per-Sadhana custom requirement (e.g. 5h gap after full meal)
    @Test
    fun `test Case 10 - Custom per-sadhana requirement`() {
        val mealTime = now - 4.hours - 30.minutes // 4h30m elapsed
        val mealRecord = createRecord(ActivityCategory.MEAL, ActivitySubCategory.LUNCH, "Lunch", mealTime)

        // Custom 5h (300m) requirement
        val result = calculator.calculateReadiness(
            now = now,
            intakeRecords = listOf(mealRecord),
            config = defaultConfig,
            sadhanaRequirementMinutes = 300,
            sadhanaName = "Intensive Kriya"
        )

        assertEquals(EmptyStomachStatus.NOT_READY, result.status)
        assertEquals(30, result.remainingMinutes) // 300m - 270m = 30m remaining
    }
}
