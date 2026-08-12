package com.activitytracker.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activitytracker.app.core.util.DateTimeUtils
import com.activitytracker.app.data.local.DatabaseSeeder
import com.activitytracker.app.data.preferences.UserPreferencesDataStore
import com.activitytracker.app.domain.model.*
import com.activitytracker.app.domain.repository.ActivityRepository
import com.activitytracker.app.domain.repository.ActivityTypeRepository
import com.activitytracker.app.domain.usecase.activity.FinishActivityUseCase
import com.activitytracker.app.domain.usecase.activity.SaveActivityUseCase
import com.activitytracker.app.domain.usecase.activity.StartActivityUseCase
import com.activitytracker.app.domain.usecase.emptystomach.EmptyStomachCalculator
import com.activitytracker.app.domain.usecase.screentime.ScreenTimeCalculator
import com.activitytracker.app.domain.repository.ScreenTimeRepository
import com.activitytracker.app.domain.usecase.stats.GetDailyStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

data class HomeUiState(
    val isLoading: Boolean = true,
    val today: LocalDate = DateTimeUtils.nowLocalDate(),
    val stats: DailyStats? = null,
    val ongoingActivities: List<ActivityRecord> = emptyList(),
    val use24h: Boolean = true,
    val emptyStomachReadiness: PracticeReadinessResult? = null,
    val pendingSadhanaCompletion: PlannedVsActual? = null, // drives the duration dialog
    val hasUsagePermission: Boolean = false,
    val snackbarMessage: String? = null
) {
    val isWorking: Boolean get() = ongoingActivities.any { it.activityType?.category == ActivityCategory.WORK }
    val isSleeping: Boolean get() = ongoingActivities.any { it.activityType?.category == ActivityCategory.SLEEP }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDailyStats: GetDailyStatsUseCase,
    private val startActivity: StartActivityUseCase,
    private val finishActivity: FinishActivityUseCase,
    private val saveActivity: SaveActivityUseCase,
    private val activityRepository: ActivityRepository,
    private val typeRepository: ActivityTypeRepository,
    private val emptyStomachCalculator: EmptyStomachCalculator,
    private val screenTimeCalculator: ScreenTimeCalculator,
    private val screenTimeRepository: ScreenTimeRepository,
    private val prefs: UserPreferencesDataStore,
    private val seeder: DatabaseSeeder
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed on first launch
            seeder.seedIfEmpty()
        }

        val today = DateTimeUtils.nowLocalDate()
        val yesterday = today.minus(1, DateTimeUnit.DAY)

        viewModelScope.launch {
            if (screenTimeCalculator.hasUsageAccessPermission()) {
                screenTimeRepository.syncPastDays(7)
            }
        }

        viewModelScope.launch {
            val ticker = flow {
                while (true) {
                    emit(Unit)
                    kotlinx.coroutines.delay(30_000L) // tick every 30 seconds
                }
            }

            combine(
                getDailyStats(today),
                activityRepository.getActivitiesForDateRange(yesterday, today),
                activityRepository.getOngoingActivities(),
                prefs.preferences,
                ticker
            ) { stats, rangeRecords, ongoing, userPrefs, _ ->
                val now = Clock.System.now()
                val readiness = emptyStomachCalculator.calculateReadiness(
                    now = now,
                    intakeRecords = rangeRecords,
                    config = userPrefs.emptyStomachConfig
                )

                val hasUsagePermission = screenTimeCalculator.hasUsageAccessPermission()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        stats = stats,
                        ongoingActivities = ongoing,
                        use24h = userPrefs.timeFormat24h,
                        emptyStomachReadiness = readiness,
                        hasUsagePermission = hasUsagePermission
                    )
                }
            }.collect()
        }
    }

    /** User tapped a sadhana toggle → show duration dialog */
    fun onSadhanaToggleTapped(pva: PlannedVsActual) {
        if (pva.isCompleted) {
            // Toggle OFF — mark as not completed
            viewModelScope.launch {
                pva.actualRecord?.let {
                    saveActivity(it.copy(isCompleted = false, status = ActivityStatus.COMPLETED, updatedAt = Clock.System.now()))
                }
            }
        } else {
            // Toggle ON → show dialog asking for duration
            _uiState.update { it.copy(pendingSadhanaCompletion = pva) }
        }
    }

    /** User submitted duration from the completion dialog */
    fun onSadhanaCompletionConfirmed(durationMinutes: Int, notes: String?) {
        val pva = _uiState.value.pendingSadhanaCompletion ?: return
        _uiState.update { it.copy(pendingSadhanaCompletion = null) }

        viewModelScope.launch {
            val existing = pva.actualRecord
            val today = DateTimeUtils.nowLocalDate()

            if (existing != null) {
                finishActivity(existing, enteredDurationMinutes = durationMinutes, notes = notes)
            } else {
                // No record yet — create one linked to the plan
                val typeId = pva.plan.activityTypeId
                val now = Clock.System.now()
                val startTime = now - durationMinutes.minutes
                val record = ActivityRecord(
                    id = UUID.randomUUID().toString(),
                    localDate = today,
                    activityTypeId = typeId,
                    startTime = startTime,
                    endTime = now,
                    durationMinutes = durationMinutes,
                    notes = notes,
                    isCompleted = true,
                    sadhanaPlanId = pva.plan.id,
                    status = ActivityStatus.COMPLETED,
                    createdAt = now,
                    updatedAt = now
                )
                activityRepository.saveActivity(record)
            }
        }
    }

    fun onSadhanaCompletionDismissed() {
        _uiState.update { it.copy(pendingSadhanaCompletion = null) }
    }

    /** Save sleep record with transition dates and timestamps */
    fun saveSleepRecord(
        bedDate: LocalDate,
        bedTimeHHmm: String,
        wakeDate: LocalDate,
        wakeTimeHHmm: String,
        notes: String?
    ) {
        viewModelScope.launch {
            val bedParts = bedTimeHHmm.split(":")
            val wakeParts = wakeTimeHHmm.split(":")
            if (bedParts.size != 2 || wakeParts.size != 2) return@launch

            val bedH = bedParts[0].toIntOrNull() ?: return@launch
            val bedM = bedParts[1].toIntOrNull() ?: return@launch
            val wakeH = wakeParts[0].toIntOrNull() ?: return@launch
            val wakeM = wakeParts[1].toIntOrNull() ?: return@launch

            val bedLdt = kotlinx.datetime.LocalDateTime(bedDate.year, bedDate.month, bedDate.dayOfMonth, bedH, bedM, 0)
            val wakeLdt = kotlinx.datetime.LocalDateTime(wakeDate.year, wakeDate.month, wakeDate.dayOfMonth, wakeH, wakeM, 0)

            val bedInst = bedLdt.toInstant(TimeZone.currentSystemDefault())
            val wakeInst = wakeLdt.toInstant(TimeZone.currentSystemDefault())
            val diffMins = ((wakeInst - bedInst).inWholeMinutes).toInt()
            if (diffMins <= 0) return@launch

            val types = typeRepository.getTypesByCategory("SLEEP")
            val type = types.firstOrNull() ?: return@launch

            val now = Clock.System.now()
            val record = ActivityRecord(
                id = UUID.randomUUID().toString(),
                localDate = wakeDate,
                activityTypeId = type.id,
                startTime = bedInst,
                endTime = wakeInst,
                durationMinutes = diffMins,
                notes = notes,
                isCompleted = true,
                status = ActivityStatus.COMPLETED,
                createdAt = now,
                updatedAt = now
            )
            activityRepository.saveActivity(record)
        }
    }

    /** Start ONGOING work timer snapshot */
    fun startWorkSession(timeHHmm: String, notes: String?) {
        viewModelScope.launch {
            val startInst = DateTimeUtils.parseTimeRelativeToday(timeHHmm) ?: return@launch
            val today = startInst.toLocalDateTime(DateTimeUtils.appTimeZone).date

            val types = typeRepository.getTypesByCategory("WORK")
            val type = types.firstOrNull() ?: return@launch
            val now = Clock.System.now()

            val record = ActivityRecord(
                id = UUID.randomUUID().toString(),
                localDate = today,
                activityTypeId = type.id,
                startTime = startInst,
                endTime = null,
                durationMinutes = null,
                notes = notes,
                isCompleted = false,
                status = ActivityStatus.ONGOING,
                createdAt = now,
                updatedAt = now
            )
            activityRepository.saveActivity(record)
        }
    }

    /** Stop ONGOING work timer & calculate total work hours */
    fun stopWorkSession(ongoingRecord: ActivityRecord, stopTimeHHmm: String, notes: String?) {
        viewModelScope.launch {
            val stopInst = DateTimeUtils.parseTimeRelativeToday(stopTimeHHmm) ?: return@launch

            val startInst = ongoingRecord.startTime ?: stopInst
            val durMins = ((stopInst - startInst).inWholeMinutes).toInt()
            val now = Clock.System.now()

            val updatedRecord = ongoingRecord.copy(
                endTime = stopInst,
                durationMinutes = if (durMins > 0) durMins else 0,
                notes = notes ?: ongoingRecord.notes,
                isCompleted = true,
                status = ActivityStatus.COMPLETED,
                updatedAt = now
            )
            activityRepository.saveActivity(updatedRecord)
        }
    }

    /** Save a quick action with custom confirmed time & optional note */
    fun saveConfirmedQuickAction(
        category: ActivityCategory,
        subCategory: ActivitySubCategory? = null,
        timeHHmm: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            val types = typeRepository.getTypesByCategory(category.name)
            val type = if (subCategory != null) {
                types.firstOrNull { it.subCategory == subCategory } ?: types.firstOrNull()
            } else {
                types.firstOrNull()
            } ?: return@launch

            val today = DateTimeUtils.nowLocalDate()
            val now = Clock.System.now()

            val startTime = if (!timeHHmm.isNullOrBlank()) {
                DateTimeUtils.parseTimeRelativeToday(timeHHmm) ?: now
            } else now

            val recordDate = startTime.toLocalDateTime(DateTimeUtils.appTimeZone).date

            val record = ActivityRecord(
                id = UUID.randomUUID().toString(),
                localDate = recordDate,
                activityTypeId = type.id,
                startTime = startTime,
                endTime = null,
                durationMinutes = null,
                notes = notes,
                isCompleted = true,
                status = ActivityStatus.COMPLETED,
                createdAt = now,
                updatedAt = now
            )
            activityRepository.saveActivity(record)
        }
    }

    /** Manually set or edit meal time from UI dialog */
    fun saveCustomMealTime(subCategory: ActivitySubCategory, timeHHmm: String) {
        viewModelScope.launch {
            val instant = DateTimeUtils.parseTimeRelativeToday(timeHHmm) ?: return@launch
            val today = instant.toLocalDateTime(DateTimeUtils.appTimeZone).date
            
            val typeList = typeRepository.getTypesByCategory("MEAL")
            val type = typeList.firstOrNull { it.subCategory == subCategory }
                ?: typeList.firstOrNull() ?: return@launch

            val now = Clock.System.now()

            val todayActivities = activityRepository.getActivitiesForDate(today).first()
            val existing = todayActivities.firstOrNull { it.activityTypeId == type.id }

            val record = existing?.copy(
                startTime = instant,
                isCompleted = true,
                status = ActivityStatus.COMPLETED,
                updatedAt = now
            ) ?: ActivityRecord(
                id = UUID.randomUUID().toString(),
                localDate = today,
                activityTypeId = type.id,
                startTime = instant,
                endTime = null,
                durationMinutes = null,
                isCompleted = true,
                status = ActivityStatus.COMPLETED,
                createdAt = now,
                updatedAt = now
            )
            activityRepository.saveActivity(record)
        }
    }

    fun onNoteEditRequested(record: ActivityRecord, newNote: String) {
        viewModelScope.launch {
            saveActivity(record.copy(notes = newNote))
        }
    }

    fun dismissSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }
}
