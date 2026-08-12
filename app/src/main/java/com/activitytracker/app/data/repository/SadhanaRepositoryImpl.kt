package com.activitytracker.app.data.repository

import com.activitytracker.app.data.local.AppDatabase
import com.activitytracker.app.data.mapper.toDomain
import com.activitytracker.app.data.mapper.toEntity
import com.activitytracker.app.domain.model.DailyNote
import com.activitytracker.app.domain.model.SadhanaPlan
import com.activitytracker.app.domain.repository.NoteRepository
import com.activitytracker.app.domain.repository.SadhanaPlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SadhanaPlanRepositoryImpl @Inject constructor(
    private val db: AppDatabase
) : SadhanaPlanRepository {

    override fun getAllActivePlans(): Flow<List<SadhanaPlan>> {
        return db.sadhanaPlanDao().getAllActive().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getPlanById(id: String): SadhanaPlan? {
        return db.sadhanaPlanDao().getById(id)?.toDomain()
    }

    override suspend fun savePlan(plan: SadhanaPlan) {
        db.sadhanaPlanDao().upsert(plan.toEntity())
    }

    override suspend fun deletePlan(id: String) {
        db.sadhanaPlanDao().softDelete(id, Clock.System.now().toEpochMilliseconds())
    }
}

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val db: AppDatabase
) : NoteRepository {

    override suspend fun getNoteForDate(date: LocalDate): DailyNote? {
        return db.dailyNoteDao().getForDate(date.toString())?.toDomain()
    }

    override fun getNoteForDateFlow(date: LocalDate): Flow<DailyNote?> {
        return db.dailyNoteDao().getForDateFlow(date.toString()).map { it?.toDomain() }
    }

    override suspend fun saveNote(note: DailyNote) {
        db.dailyNoteDao().upsert(note.toEntity())
    }

    override suspend fun deleteNote(date: LocalDate) {
        db.dailyNoteDao().deleteForDate(date.toString())
    }
}
