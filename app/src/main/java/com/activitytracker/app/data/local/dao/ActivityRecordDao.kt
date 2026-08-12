package com.activitytracker.app.data.local.dao

import androidx.room.*
import com.activitytracker.app.data.local.entity.ActivityRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityRecordDao {

    @Query("""
        SELECT ar.* FROM activity_records ar
        WHERE ar.localDate = :localDate
        ORDER BY ar.startTime ASC, ar.createdAt ASC
    """)
    fun getForDate(localDate: String): Flow<List<ActivityRecordEntity>>

    @Query("""
        SELECT ar.* FROM activity_records ar
        WHERE ar.localDate >= :fromDate AND ar.localDate <= :toDate
        ORDER BY ar.localDate ASC, ar.startTime ASC
    """)
    fun getForDateRange(fromDate: String, toDate: String): Flow<List<ActivityRecordEntity>>

    @Query("SELECT * FROM activity_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ActivityRecordEntity?

    @Query("SELECT * FROM activity_records WHERE status = 'ONGOING' ORDER BY startTime DESC")
    fun getOngoing(): Flow<List<ActivityRecordEntity>>

    @Upsert
    suspend fun upsert(entity: ActivityRecordEntity)

    @Query("DELETE FROM activity_records WHERE id = :id")
    suspend fun deleteById(id: String)
}
