package com.activitytracker.app.domain.repository

import com.activitytracker.app.data.local.entity.DailyScreenTimeEntity
import kotlinx.coroutines.flow.Flow

interface ScreenTimeRepository {
    fun getScreenTimeFlow(localDateStr: String): Flow<DailyScreenTimeEntity?>
    suspend fun getScreenTime(localDateStr: String): DailyScreenTimeEntity?
    suspend fun syncPastDays(days: Int)
    suspend fun deleteHistory()
}
