package com.activitytracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.activitytracker.app.data.local.entity.DailyScreenTimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyScreenTimeDao {

    @Query("SELECT * FROM daily_screen_time WHERE localDate = :date")
    fun getByDateFlow(date: String): Flow<DailyScreenTimeEntity?>

    @Query("SELECT * FROM daily_screen_time WHERE localDate = :date")
    suspend fun getByDate(date: String): DailyScreenTimeEntity?

    @Query("SELECT * FROM daily_screen_time WHERE localDate BETWEEN :startDate AND :endDate ORDER BY localDate ASC")
    fun getRangeFlow(startDate: String, endDate: String): Flow<List<DailyScreenTimeEntity>>

    @Query("SELECT * FROM daily_screen_time WHERE localDate BETWEEN :startDate AND :endDate ORDER BY localDate ASC")
    suspend fun getRange(startDate: String, endDate: String): List<DailyScreenTimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: DailyScreenTimeEntity)

    @Query("DELETE FROM daily_screen_time")
    suspend fun deleteAll()
    
    @Query("DELETE FROM daily_screen_time WHERE localDate = :date")
    suspend fun deleteByDate(date: String)
}
