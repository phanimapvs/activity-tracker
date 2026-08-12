package com.activitytracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity for user-configurable activity types.
 * category: one of ActivityCategory enum name strings.
 * subCategory: one of ActivitySubCategory enum name strings.
 */
@Entity(tableName = "activity_types")
data class ActivityTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val subCategory: String = "NONE",
    val iconKey: String? = null,
    val colorArgb: Int? = null,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
