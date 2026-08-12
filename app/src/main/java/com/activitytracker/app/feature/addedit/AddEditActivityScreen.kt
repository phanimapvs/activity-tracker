package com.activitytracker.app.feature.addedit

import androidx.compose.ui.res.stringResource
import com.activitytracker.app.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.activitytracker.app.core.util.DateTimeUtils
import com.activitytracker.app.core.util.format24h
import com.activitytracker.app.domain.model.*
import com.activitytracker.app.domain.repository.ActivityRepository
import com.activitytracker.app.domain.repository.ActivityTypeRepository
import com.activitytracker.app.domain.usecase.activity.SaveActivityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import java.util.UUID
import javax.inject.Inject

data class AddEditUiState(
    val isLoading: Boolean = true,
    val activityTypes: List<ActivityType> = emptyList(),
    val selectedTypeId: String? = null,
    val startTimeText: String = "",   // "HH:mm"
    val endTimeText: String = "",
    val durationText: String = "",    // manual duration input
    val notesText: String = "",
    val isCompleted: Boolean = true,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false
)

@HiltViewModel
class AddEditActivityViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val typeRepository: ActivityTypeRepository,
    private val saveActivity: SaveActivityUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val recordId: String? = savedStateHandle["recordId"]
    val isEditMode: Boolean = recordId != null
    private val dateArg: String? = savedStateHandle["dateString"]
    private val preselectedTypeId: String? = savedStateHandle["activityTypeId"]

    val date: LocalDate = dateArg?.let { LocalDate.parse(it) } ?: DateTimeUtils.nowLocalDate()

    private val _state = MutableStateFlow(AddEditUiState())
    val state: StateFlow<AddEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val types = typeRepository.getAllActiveTypes().first()
            val existing = recordId?.let { activityRepository.getActivityById(it) }
            val now = Clock.System.now()

            _state.update {
                it.copy(
                    isLoading = false,
                    activityTypes = types,
                    selectedTypeId = existing?.activityTypeId ?: preselectedTypeId ?: types.firstOrNull()?.id,
                    startTimeText = existing?.startTime?.format24h() ?: now.format24h(),
                    endTimeText = existing?.endTime?.format24h() ?: "",
                    durationText = existing?.durationMinutes?.toString() ?: "",
                    notesText = existing?.notes ?: "",
                    isCompleted = existing?.isCompleted ?: true
                )
            }
        }
    }

    fun onTypeSelected(id: String) = _state.update { it.copy(selectedTypeId = id) }
    fun onStartTimeChanged(v: String) = _state.update { it.copy(startTimeText = v) }
    fun onEndTimeChanged(v: String) = _state.update { it.copy(endTimeText = v) }
    fun onDurationChanged(v: String) = _state.update { it.copy(durationText = v) }
    fun onNotesChanged(v: String) = _state.update { it.copy(notesText = v) }
    fun onCompletedToggle(v: Boolean) = _state.update { it.copy(isCompleted = v) }

    fun save() {
        val s = _state.value
        val typeId = s.selectedTypeId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            val tz = TimeZone.currentSystemDefault()
            val now = Clock.System.now()

            fun parseTime(text: String): Instant? {
                if (text.isBlank()) return null
                return try {
                    val parts = text.split(":")
                    val h = parts[0].toInt(); val m = parts[1].toInt()
                    val ldt = LocalDateTime(date, LocalTime(h, m))
                    ldt.toInstant(tz)
                } catch (e: Exception) { null }
            }

            val start = parseTime(s.startTimeText)
            val end = parseTime(s.endTimeText)
            val enteredDur = parseDurationInput(s.durationText)
            val calcDur = if (start != null && end != null && end > start)
                ((end - start).inWholeMinutes).toInt() else null
            val finalDur = calcDur ?: enteredDur

            val record = ActivityRecord(
                id = recordId ?: UUID.randomUUID().toString(),
                localDate = date,
                activityTypeId = typeId,
                startTime = start,
                endTime = end,
                durationMinutes = finalDur,
                notes = s.notesText.ifBlank { null },
                isCompleted = s.isCompleted,
                status = if (s.isCompleted) ActivityStatus.COMPLETED else ActivityStatus.ONGOING,
                createdAt = now,
                updatedAt = now
            )
            saveActivity(record)
            _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
        }
    }

    fun delete() {
        val id = recordId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            activityRepository.deleteActivity(id)
            _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditActivityScreen(
    onNavigateUp: () -> Unit,
    viewModel: AddEditActivityViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onNavigateUp()
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_activity_title)) },
            text = { Text(stringResource(R.string.delete_activity_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.delete()
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditMode) stringResource(R.string.edit_activity) else stringResource(R.string.new_activity)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.Close, stringResource(R.string.cancel))
                    }
                },
                actions = {
                    if (viewModel.isEditMode) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete activity", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = !state.isSaving && state.selectedTypeId != null
                    ) {
                        Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Activity type selector
            Text(stringResource(R.string.activity_type), style = MaterialTheme.typography.labelLarge)
            state.activityTypes.forEach { type ->
                val selected = type.id == state.selectedTypeId
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.onTypeSelected(type.id) },
                    label = { Text(type.name) }
                )
            }

            HorizontalDivider()

            // Time fields
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.startTimeText,
                    onValueChange = viewModel::onStartTimeChanged,
                    label = { Text(stringResource(R.string.start_time_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.endTimeText,
                    onValueChange = viewModel::onEndTimeChanged,
                    label = { Text(stringResource(R.string.end_time_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = state.durationText,
                onValueChange = viewModel::onDurationChanged,
                label = { Text(stringResource(R.string.duration_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = {
                    val parsed = parseDurationInput(state.durationText)
                    if (parsed != null) Text(formatDurationMinutes(parsed))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Completed toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.completed), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = state.isCompleted, onCheckedChange = viewModel::onCompletedToggle)
            }

            // Notes
            OutlinedTextField(
                value = state.notesText,
                onValueChange = viewModel::onNotesChanged,
                label = { Text(stringResource(R.string.notes_optional)) },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            if (viewModel.isEditMode) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.delete_activity))
                }
            }
        }
    }
}
