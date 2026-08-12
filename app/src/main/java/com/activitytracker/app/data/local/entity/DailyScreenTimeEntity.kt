package com.activitytracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks the total daily mobile screen time.
 * [localDate]: "YYYY-MM-DD"
 */
@Entity(tableName = "daily_screen_time")
data class DailyScreenTimeEntity(
    @PrimaryKey val localDate: String,
    val totalDurationMinutes: Int,
    val lastSyncedAt: Long
)
