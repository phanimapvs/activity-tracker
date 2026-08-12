package com.activitytracker.app.domain.repository

import com.activitytracker.app.domain.model.DailyNote
import com.activitytracker.app.domain.model.SadhanaPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface SadhanaPlanRepository {
    fun getAllActivePlans(): Flow<List<SadhanaPlan>>
    suspend fun getPlanById(id: String): SadhanaPlan?
    suspend fun savePlan(plan: SadhanaPlan)
    suspend fun deletePlan(id: String)
}

interface NoteRepository {
    suspend fun getNoteForDate(date: LocalDate): DailyNote?
    fun getNoteForDateFlow(date: LocalDate): Flow<DailyNote?>
    suspend fun saveNote(note: DailyNote)
    suspend fun deleteNote(date: LocalDate)
}
