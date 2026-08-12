package com.activitytracker.app.feature.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.activitytracker.app.core.util.DateTimeUtils
import com.activitytracker.app.core.util.DateTimeUtils.toDisplayString
import com.activitytracker.app.core.util.categoryColor
import com.activitytracker.app.domain.model.ActivityCategory
import com.activitytracker.app.domain.model.ActivityRecord
import com.activitytracker.app.domain.repository.ActivityRepository
import com.activitytracker.app.ui.components.TimelineRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalDate
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repository: ActivityRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val dateArg: String? = savedStateHandle["dateString"]
    val date: LocalDate = dateArg?.let { LocalDate.parse(it) } ?: DateTimeUtils.nowLocalDate()

    val activities: StateFlow<List<ActivityRecord>> = repository
        .getActivitiesForDate(date)
        .map { list -> list.sortedWith(compareBy(nullsLast()) { r -> r.startTime }) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    date: LocalDate? = null,
    onNavigateUp: () -> Unit,
    onAddActivity: (String?) -> Unit,
    onActivityClick: (String) -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val activities by viewModel.activities.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = viewModel.date.toDisplayString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddActivity(null) }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, "Add activity")
            }
        }
    ) { padding ->
        if (activities.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "No activities recorded",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Tap + to add one",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
            items(activities, key = { it.id }) { record ->
                TimelineRow(
                    record = record,
                    accentColor = Color(categoryColor(record.activityType?.category ?: ActivityCategory.OTHER)),
                    use24h = true,
                    onClick = { onActivityClick(record.id) }
                )
            }
        }
    }
}
