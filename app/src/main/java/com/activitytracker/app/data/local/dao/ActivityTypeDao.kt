package com.activitytracker.app.data.local.dao

import androidx.room.*
import com.activitytracker.app.data.local.entity.ActivityTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityTypeDao {
    @Query("SELECT * FROM activity_types WHERE isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun getAllActive(): Flow<List<ActivityTypeEntity>>

    @Query("SELECT * FROM activity_types WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ActivityTypeEntity?

    @Query("SELECT * FROM activity_types WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ActivityTypeEntity>

    @Query("SELECT * FROM activity_types WHERE category = :category AND isActive = 1 ORDER BY sortOrder ASC")
    suspend fun getByCategory(category: String): List<ActivityTypeEntity>

    @Upsert
    suspend fun upsert(entity: ActivityTypeEntity)

    @Query("UPDATE activity_types SET isActive = 0, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}
