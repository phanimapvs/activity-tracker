package com.activitytracker.app.feature.settings.sadhanaplans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.activitytracker.app.domain.model.*
import com.activitytracker.app.domain.repository.ActivityTypeRepository
import com.activitytracker.app.domain.repository.SadhanaPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SadhanaPlansViewModel @Inject constructor(
    private val planRepository: SadhanaPlanRepository,
    private val typeRepository: ActivityTypeRepository
) : ViewModel() {

    val plans: StateFlow<List<SadhanaPlan>> = planRepository.getAllActivePlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityTypes: StateFlow<List<ActivityType>> = typeRepository.getAllActiveTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _editTarget = MutableStateFlow<SadhanaPlan?>(null)
    val editTarget: StateFlow<SadhanaPlan?> = _editTarget.asStateFlow()

    fun startEdit(plan: SadhanaPlan?) = _editTarget.update { plan }
    fun dismissEdit() = _editTarget.update { null }

    fun save(
        name: String,
        activityTypeId: String,
        targetMins: Int?,
        slot: TimeSlot,
        existing: SadhanaPlan?
    ) {
        viewModelScope.launch {
            val now = Clock.System.now()
            val plan = existing?.copy(
                name = name, activityTypeId = activityTypeId,
                targetDurationMinutes = targetMins, timeSlot = slot, updatedAt = now
            ) ?: SadhanaPlan(
                id = UUID.randomUUID().toString(),
                name = name,
                activityTypeId = activityTypeId,
                targetDurationMinutes = targetMins,
                timeSlot = slot,
                isActive = true,
                isDaily = true,
                sortOrder = 99,
                createdAt = now,
                updatedAt = now
            )
            planRepository.savePlan(plan)
            _editTarget.update { null }
        }
    }

    fun delete(id: String) = viewModelScope.launch { planRepository.deletePlan(id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SadhanaPlansScreen(
    onNavigateUp: () -> Unit,
    viewModel: SadhanaPlansViewModel = hiltViewModel()
) {
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val types by viewModel.activityTypes.collectAsStateWithLifecycle()
    val editTarget by viewModel.editTarget.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    if (showAddDialog || editTarget != null) {
        SadhanaPlanEditDialog(
            existing = editTarget,
            activityTypes = types,
            onSave = { name, typeId, targetMins, slot ->
                viewModel.save(name, typeId, targetMins, slot, editTarget)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false; viewModel.dismissEdit() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sadhana Plans") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add plan")
            }
        }
    ) { padding ->
        if (plans.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.SelfImprovement, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                    Text("No sadhana plans yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Tap + to add your first plan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 80.dp,
                start = 16.dp, end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(plans, key = { it.id }) { plan ->
                SadhanaPlanCard(
                    plan = plan,
                    activityTypes = types,
                    onEdit = { viewModel.startEdit(plan) },
                    onDelete = { viewModel.delete(plan.id) }
                )
            }
        }
    }
}

@Composable
private fun SadhanaPlanCard(
    plan: SadhanaPlan,
    activityTypes: List<ActivityType>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val typeName = activityTypes.firstOrNull { it.id == plan.activityTypeId }?.name ?: plan.name

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove \"${plan.name}\"?") },
            text = { Text("This plan will no longer appear in your daily sadhana list.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(plan.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    plan.targetDurationMinutes?.let { mins ->
                        Text(
                            "Target: ${formatDur(mins)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        plan.timeSlot.name.lowercase().replace("_", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (typeName != plan.name) {
                    Text("Activity: $typeName", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(18.dp)) }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SadhanaPlanEditDialog(
    existing: SadhanaPlan?,
    activityTypes: List<ActivityType>,
    onSave: (name: String, typeId: String, targetMins: Int?, slot: TimeSlot) -> Unit,
    onDismiss: () -> Unit
) {
    val sadhanaTypes = activityTypes.filter { it.category == ActivityCategory.SADHANA }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var selectedTypeId by remember { mutableStateOf(existing?.activityTypeId ?: sadhanaTypes.firstOrNull()?.id ?: "") }
    var targetInput by remember { mutableStateOf(existing?.targetDurationMinutes?.toString() ?: "") }
    var selectedSlot by remember { mutableStateOf(existing?.timeSlot ?: TimeSlot.ANYTIME) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Sadhana Plan" else "New Sadhana Plan") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Plan name") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (sadhanaTypes.isNotEmpty()) {
                    Text("Activity type", style = MaterialTheme.typography.labelMedium)
                    sadhanaTypes.forEach { type ->
                        FilterChip(
                            selected = type.id == selectedTypeId,
                            onClick = { selectedTypeId = type.id },
                            label = { Text(type.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = { Text("Target duration (min, e.g. 45)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    supportingText = {
                        targetInput.toIntOrNull()?.let { Text(formatDur(it)) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Time slot", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TimeSlot.entries.chunked(3).forEach { row ->
                        // Just show all chips wrapping
                    }
                }
                // Time slot chips
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TimeSlot.entries.forEach { slot ->
                        FilterChip(
                            selected = slot == selectedSlot,
                            onClick = { selectedSlot = slot },
                            label = {
                                Text(
                                    slot.name.lowercase().replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) { nameError = true; return@Button }
                val typeId = selectedTypeId.ifBlank { sadhanaTypes.firstOrNull()?.id ?: return@Button }
                onSave(name.trim(), typeId, targetInput.toIntOrNull(), selectedSlot)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatDur(mins: Int): String {
    if (mins <= 0) return "0 min"
    val h = mins / 60; val m = mins % 60
    return if (h == 0) "$m min" else if (m == 0) "${h}h" else "${h}h ${m}m"
}
