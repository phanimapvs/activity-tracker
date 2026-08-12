package com.activitytracker.app.domain.repository

import com.activitytracker.app.domain.model.ActivityRecord
import com.activitytracker.app.domain.model.ActivityType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface ActivityRepository {
    fun getActivitiesForDate(date: LocalDate): Flow<List<ActivityRecord>>
    fun getActivitiesForDateRange(from: LocalDate, to: LocalDate): Flow<List<ActivityRecord>>
    suspend fun getActivityById(id: String): ActivityRecord?
    suspend fun saveActivity(record: ActivityRecord)
    suspend fun deleteActivity(id: String)
    /** Returns the currently ONGOING activity (no endTime), if any */
    fun getOngoingActivities(): Flow<List<ActivityRecord>>
}

interface ActivityTypeRepository {
    fun getAllActiveTypes(): Flow<List<ActivityType>>
    suspend fun getTypeById(id: String): ActivityType?
    suspend fun saveType(type: ActivityType)
    suspend fun deleteType(id: String)
    suspend fun getTypesByCategory(category: String): List<ActivityType>
}
