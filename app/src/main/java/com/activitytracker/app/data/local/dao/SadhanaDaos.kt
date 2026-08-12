package com.activitytracker.app.data.local.dao

import androidx.room.*
import com.activitytracker.app.data.local.entity.DailyNoteEntity
import com.activitytracker.app.data.local.entity.SadhanaPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SadhanaPlanDao {
    @Query("SELECT * FROM sadhana_plans WHERE isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun getAllActive(): Flow<List<SadhanaPlanEntity>>

    @Query("SELECT * FROM sadhana_plans WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SadhanaPlanEntity?

    @Upsert
    suspend fun upsert(entity: SadhanaPlanEntity)

    @Query("UPDATE sadhana_plans SET isActive = 0, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

@Dao
interface DailyNoteDao {
    @Query("SELECT * FROM daily_notes WHERE localDate = :localDate LIMIT 1")
    suspend fun getForDate(localDate: String): DailyNoteEntity?

    @Query("SELECT * FROM daily_notes WHERE localDate = :localDate LIMIT 1")
    fun getForDateFlow(localDate: String): Flow<DailyNoteEntity?>

    @Upsert
    suspend fun upsert(entity: DailyNoteEntity)

    @Query("DELETE FROM daily_notes WHERE localDate = :localDate")
    suspend fun deleteForDate(localDate: String)
}
