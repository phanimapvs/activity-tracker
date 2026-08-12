package com.activitytracker.app.data.local

import com.activitytracker.app.data.local.entity.ActivityTypeEntity
import com.activitytracker.app.data.local.entity.SadhanaPlanEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the database with default activity types and sadhana plans on first install.
 * Runs once; subsequent launches check if data already exists.
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val db: AppDatabase
) {
    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        // Check if already seeded (any active type exists)
        val existing = db.activityTypeDao().getByCategory("SADHANA")
        if (existing.isNotEmpty()) return@withContext

        val now = Clock.System.now().toEpochMilliseconds()

        // --- Default Activity Types ---
        val types = listOf(
            type(now, "Wake Up",        "WAKE",     "NONE",     "wb_sunny",       0xFF_FDD835.toInt(), 0),
            type(now, "Sleep",          "SLEEP",    "NONE",     "bedtime",        0xFF_5C6BC0.toInt(), 1),
            type(now, "Morning Japa",   "SADHANA",  "NONE",     "self_improvement",0xFF_AB47BC.toInt(), 2),
            type(now, "Meditation",     "SADHANA",  "NONE",     "spa",            0xFF_7E57C2.toInt(), 3),
            type(now, "Reading",        "SADHANA",  "NONE",     "menu_book",      0xFF_5C6BC0.toInt(), 4),
            type(now, "Prayer",         "SADHANA",  "NONE",     "volunteer_activism",0xFF_EC407A.toInt(), 5),
            type(now, "Exercise",       "EXERCISE", "NONE",     "fitness_center", 0xFF_26A69A.toInt(), 6),
            type(now, "Breakfast",      "MEAL",     "BREAKFAST","free_breakfast",  0xFF_FF7043.toInt(), 7),
            type(now, "Lunch",          "MEAL",     "LUNCH",    "lunch_dining",   0xFF_FFA726.toInt(), 8),
            type(now, "Dinner",         "MEAL",     "DINNER",   "dinner_dining",  0xFF_EF5350.toInt(), 9),
            type(now, "Snack",          "MEAL",     "SNACK",    "bakery_dining",  0xFF_FFCA28.toInt(), 10),
            type(now, "Water Intake",   "MEAL",     "WATER",    "water_drop",     0xFF_29B6F6.toInt(), 11),
            type(now, "Office Work",    "WORK",     "OFFICE",   "work",           0xFF_42A5F5.toInt(), 12),
            type(now, "Work from Home", "WORK",     "WORK_FROM_HOME","home_work", 0xFF_26C6DA.toInt(), 13),
            type(now, "Personal",       "PERSONAL", "NONE",     "person",         0xFF_66BB6A.toInt(), 14),
        )
        types.forEach { db.activityTypeDao().upsert(it) }

        // --- Default Sadhana Plans ---
        val sadhanaTypeId = types.first { it.name == "Morning Japa" }.id
        val meditationTypeId = types.first { it.name == "Meditation" }.id

        val plans = listOf(
            plan(now, "Morning Japa", sadhanaTypeId, 45, "MORNING", 5, 45, 0),
            plan(now, "Meditation",   meditationTypeId, 20, "MORNING", 6, 30, 1),
        )
        plans.forEach { db.sadhanaPlanDao().upsert(it) }
    }

    private fun type(
        now: Long, name: String, category: String, sub: String,
        icon: String, color: Int, order: Int
    ) = ActivityTypeEntity(
        id = UUID.randomUUID().toString(),
        name = name,
        category = category,
        subCategory = sub,
        iconKey = icon,
        colorArgb = color,
        isActive = true,
        sortOrder = order,
        createdAt = now,
        updatedAt = now
    )

    private fun plan(
        now: Long, name: String, typeId: String, targetMins: Int,
        slot: String, prefHour: Int, prefMin: Int, order: Int
    ) = SadhanaPlanEntity(
        id = UUID.randomUUID().toString(),
        name = name,
        activityTypeId = typeId,
        targetDurationMinutes = targetMins,
        timeSlot = slot,
        preferredStartHour = prefHour,
        preferredStartMinute = prefMin,
        isActive = true,
        isDaily = true,
        sortOrder = order,
        createdAt = now,
        updatedAt = now
    )
}
