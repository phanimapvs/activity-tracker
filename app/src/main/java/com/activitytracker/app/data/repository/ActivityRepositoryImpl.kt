package com.activitytracker.app.data.repository

import com.activitytracker.app.data.local.AppDatabase
import com.activitytracker.app.data.mapper.toDomain
import com.activitytracker.app.data.mapper.toEntity
import com.activitytracker.app.domain.model.ActivityRecord
import com.activitytracker.app.domain.model.ActivityType
import com.activitytracker.app.domain.repository.ActivityRepository
import com.activitytracker.app.domain.repository.ActivityTypeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepositoryImpl @Inject constructor(
    private val db: AppDatabase
) : ActivityRepository {

    override fun getActivitiesForDate(date: LocalDate): Flow<List<ActivityRecord>> {
        return db.activityRecordDao().getForDate(date.toString()).map { entities ->
            val typeIds = entities.map { it.activityTypeId }.distinct()
            val types = if (typeIds.isEmpty()) emptyMap() else db.activityTypeDao().getByIds(typeIds)
                .map { it.toDomain() }
                .associateBy { it.id }
            entities.map { it.toDomain(types[it.activityTypeId]) }
        }
    }

    override fun getActivitiesForDateRange(from: LocalDate, to: LocalDate): Flow<List<ActivityRecord>> {
        return db.activityRecordDao().getForDateRange(from.toString(), to.toString()).map { entities ->
            val typeIds = entities.map { it.activityTypeId }.distinct()
            val types = if (typeIds.isEmpty()) emptyMap() else db.activityTypeDao().getByIds(typeIds)
                .map { it.toDomain() }
                .associateBy { it.id }
            entities.map { it.toDomain(types[it.activityTypeId]) }
        }
    }

    override suspend fun getActivityById(id: String): ActivityRecord? {
        val entity = db.activityRecordDao().getById(id) ?: return null
        val type = entity.activityTypeId.let { db.activityTypeDao().getById(it)?.toDomain() }
        return entity.toDomain(type)
    }

    override suspend fun saveActivity(record: ActivityRecord) {
        db.activityRecordDao().upsert(record.toEntity())
    }

    override suspend fun deleteActivity(id: String) {
        db.activityRecordDao().deleteById(id)
    }

    override fun getOngoingActivities(): Flow<List<ActivityRecord>> {
        return db.activityRecordDao().getOngoing().map { entities ->
            val typeIds = entities.map { it.activityTypeId }.distinct()
            val types = if (typeIds.isEmpty()) emptyMap() else db.activityTypeDao().getByIds(typeIds)
                .map { it.toDomain() }
                .associateBy { it.id }
            entities.map { it.toDomain(types[it.activityTypeId]) }
        }
    }
}

@Singleton
class ActivityTypeRepositoryImpl @Inject constructor(
    private val db: AppDatabase
) : ActivityTypeRepository {

    override fun getAllActiveTypes(): Flow<List<ActivityType>> {
        return db.activityTypeDao().getAllActive().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTypeById(id: String): ActivityType? {
        return db.activityTypeDao().getById(id)?.toDomain()
    }

    override suspend fun saveType(type: ActivityType) {
        db.activityTypeDao().upsert(type.toEntity())
    }

    override suspend fun deleteType(id: String) {
        db.activityTypeDao().softDelete(id, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun getTypesByCategory(category: String): List<ActivityType> {
        return db.activityTypeDao().getByCategory(category).map { it.toDomain() }
    }
}
