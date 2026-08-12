package com.activitytracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Database entity for a recorded activity instance.
 *
 * [localDate]: "YYYY-MM-DD" string — the date this record belongs to for dashboard queries.
 *   For SLEEP records: this is the WAKE-UP date (so Day 2's dashboard shows last night's sleep).
 * [startTime]: epoch milliseconds UTC (nullable — allows end-time-only entry)
 * [endTime]: epoch milliseconds UTC (null if ongoing or only start recorded)
 * [durationMinutes]: stored redundantly for fast aggregation; recalculated on edit
 * [status]: ONGOING | COMPLETED | MISSED | SKIPPED
 */
@Entity(
    tableName = "activity_records",
    foreignKeys = [
        ForeignKey(
            entity = ActivityTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["activityTypeId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("localDate"),
        Index("activityTypeId"),
        Index("status")
    ]
)
data class ActivityRecordEntity(
    @PrimaryKey val id: String,
    val localDate: String,           // "YYYY-MM-DD"
    val activityTypeId: String,
    val startTime: Long?,            // epoch ms UTC
    val endTime: Long?,              // epoch ms UTC
    val durationMinutes: Int?,
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val isManuallyEdited: Boolean = false,
    val sadhanaPlanId: String? = null,
    val status: String = "COMPLETED",
    val createdAt: Long,
    val updatedAt: Long
)
