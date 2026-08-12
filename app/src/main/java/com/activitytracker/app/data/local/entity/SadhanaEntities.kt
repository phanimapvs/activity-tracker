package com.activitytracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Recurring sadhana plan / target template.
 * [timeSlot]: one of TimeSlot enum name strings (MORNING, AFTER_BREAKFAST, etc.)
 */
@Entity(tableName = "sadhana_plans")
data class SadhanaPlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val activityTypeId: String,
    val targetDurationMinutes: Int? = null,
    val targetSessionsPerDay: Int = 1,
    val timeSlot: String = "ANYTIME",
    val preferredStartHour: Int? = null,
    val preferredStartMinute: Int? = null,
    val gapAfterActivityTypeId: String? = null,
    val gapAfterActivityMinutes: Int? = null,
    val isActive: Boolean = true,
    val isDaily: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Optional free-text note for a given day.
 * [localDate]: "YYYY-MM-DD"
 */
@Entity(tableName = "daily_notes", indices = [Index("localDate", unique = true)])
data class DailyNoteEntity(
    @PrimaryKey val id: String,
    val localDate: String,   // "YYYY-MM-DD"
    val note: String,
    val createdAt: Long,
    val updatedAt: Long
)
