package com.activitytracker.app.feature.settings.activitytypes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.activitytracker.app.domain.model.ActivityType
import com.activitytracker.app.domain.model.ActivityCategory
import com.activitytracker.app.domain.model.ActivitySubCategory
import com.activitytracker.app.domain.repository.ActivityTypeRepository
import com.activitytracker.app.core.util.categoryColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ActivityTypesViewModel @Inject constructor(
    private val repository: ActivityTypeRepository
) : ViewModel() {

    val types: StateFlow<List<ActivityType>> = repository.getAllActiveTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _editTarget = MutableStateFlow<ActivityType?>(null)
    val editTarget: StateFlow<ActivityType?> = _editTarget.asStateFlow()

    fun startEdit(type: ActivityType?) = _editTarget.update { type }
    fun dismissEdit() = _editTarget.update { null }

    fun save(name: String, category: ActivityCategory, subCategory: ActivitySubCategory, existing: ActivityType?) {
        viewModelScope.launch {
            val now = Clock.System.now()
            val type = existing?.copy(name = name, category = category, subCategory = subCategory, updatedAt = now)
                ?: ActivityType(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    category = category,
                    subCategory = subCategory,
                    isActive = true,
                    sortOrder = 99,
                    createdAt = now,
                    updatedAt = now
                )
            repository.saveType(type)
            _editTarget.update { null }
        }
    }

    fun delete(id: String) = viewModelScope.launch { repository.deleteType(id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTypesScreen(
    onNavigateUp: () -> Unit,
    viewModel: ActivityTypesViewModel = hiltViewModel()
) {
    val types by viewModel.types.collectAsStateWithLifecycle()
    val editTarget by viewModel.editTarget.collectAsStateWithLifecycle()

    // Edit/Add dialog
    editTarget.let { target ->
        if (target != null || editTarget == ActivityType(
                "", "", ActivityCategory.OTHER, ActivitySubCategory.NONE,
                null, null, true, 0, Clock.System.now(), Clock.System.now()
            )) {
            // Show dialog if we explicitly triggered it
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    if (showAddDialog || editTarget != null) {
        ActivityTypeEditDialog(
            existing = editTarget,
            onSave = { name, cat, sub -> viewModel.save(name, cat, sub, editTarget) },
            onDismiss = { showAddDialog = false; viewModel.dismissEdit() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Types") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add type")
            }
        }
    ) { padding ->
        val grouped = types.groupBy { it.category }

        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 80.dp,
                start = 16.dp, end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            grouped.forEach { (category, categoryTypes) ->
                item {
                    Text(
                        category.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(categoryColor(category)),
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(categoryTypes, key = { it.id }) { type ->
                    ActivityTypeRow(
                        type = type,
                        onEdit = { viewModel.startEdit(type) },
                        onDelete = { viewModel.delete(type.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityTypeRow(
    type: ActivityType,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete \"${type.name}\"?") },
            text = { Text("This will hide the type. Existing records are not affected.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    ListItem(
        headlineContent = { Text(type.name, fontWeight = FontWeight.Medium) },
        supportingContent = {
            if (type.subCategory.name != "NONE") {
                Text(type.subCategory.name, style = MaterialTheme.typography.bodySmall)
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .then(Modifier.padding(0.dp))
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = type.colorArgb?.let { Color(it.toLong() or 0xFF000000) }
                        ?: Color(categoryColor(type.category))
                ) { Box(Modifier.size(10.dp)) }
            }
        },
        trailingContent = {
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(18.dp)) }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityTypeEditDialog(
    existing: ActivityType?,
    onSave: (name: String, category: ActivityCategory, subCategory: ActivitySubCategory) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var selectedCategory by remember { mutableStateOf(existing?.category ?: ActivityCategory.PERSONAL) }
    var selectedSub by remember { mutableStateOf(existing?.subCategory ?: ActivitySubCategory.NONE) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Activity Type" else "New Activity Type") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Name") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", style = MaterialTheme.typography.labelMedium)
                ActivityCategory.entries.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { cat ->
                            FilterChip(
                                selected = cat == selectedCategory,
                                onClick = { selectedCategory = cat; selectedSub = ActivitySubCategory.NONE },
                                label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Sub-category for MEAL and WORK
                if (selectedCategory == ActivityCategory.MEAL) {
                    Text("Meal type", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(ActivitySubCategory.BREAKFAST, ActivitySubCategory.LUNCH,
                               ActivitySubCategory.DINNER, ActivitySubCategory.SNACK).forEach { sub ->
                            FilterChip(
                                selected = sub == selectedSub,
                                onClick = { selectedSub = sub },
                                label = { Text(sub.name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
                if (selectedCategory == ActivityCategory.WORK) {
                    Text("Work type", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(ActivitySubCategory.OFFICE, ActivitySubCategory.WORK_FROM_HOME,
                               ActivitySubCategory.OTHER_WORK).forEach { sub ->
                            FilterChip(
                                selected = sub == selectedSub,
                                onClick = { selectedSub = sub },
                                label = { Text(sub.name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) { nameError = true; return@Button }
                onSave(name.trim(), selectedCategory, selectedSub)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
