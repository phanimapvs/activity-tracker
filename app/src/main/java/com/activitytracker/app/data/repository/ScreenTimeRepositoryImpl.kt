package com.activitytracker.app.data.repository

import com.activitytracker.app.data.local.dao.DailyScreenTimeDao
import com.activitytracker.app.data.local.entity.DailyScreenTimeEntity
import com.activitytracker.app.domain.repository.ScreenTimeRepository
import com.activitytracker.app.domain.usecase.screentime.ScreenTimeCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

class ScreenTimeRepositoryImpl @Inject constructor(
    private val dao: DailyScreenTimeDao,
    private val calculator: ScreenTimeCalculator
) : ScreenTimeRepository {

    override fun getScreenTimeFlow(localDateStr: String): Flow<DailyScreenTimeEntity?> {
        return dao.getByDateFlow(localDateStr)
    }

    override suspend fun getScreenTime(localDateStr: String): DailyScreenTimeEntity? {
        return dao.getByDate(localDateStr)
    }

    override suspend fun deleteHistory() {
        withContext(Dispatchers.IO) {
            dao.deleteAll()
        }
    }

    override suspend fun syncPastDays(days: Int) {
        if (!calculator.hasUsageAccessPermission()) return

        withContext(Dispatchers.IO) {
            val now = Clock.System.now()
            val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
            
            // Sync from today backward
            for (i in 0 until days) {
                val targetDate = today.minus(i, DateTimeUnit.DAY)
                val durationMinutes = calculator.calculateForegroundTimeMinutes(targetDate)
                val entity = DailyScreenTimeEntity(
                    localDate = targetDate.toString(),
                    totalDurationMinutes = durationMinutes,
                    lastSyncedAt = now.toEpochMilliseconds()
                )
                dao.insertOrReplace(entity)
            }
        }
    }
}
