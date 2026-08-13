package com.activitytracker.app.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.activitytracker.app.core.util.DateTimeUtils
import com.activitytracker.app.core.util.format24h
import com.activitytracker.app.core.util.DateTimeUtils.toDisplayString
import com.activitytracker.app.core.util.DateTimeUtils.toShortDisplay
import com.activitytracker.app.core.util.categoryColor
import com.activitytracker.app.domain.model.*
import com.activitytracker.app.domain.repository.ActivityRepository
import com.activitytracker.app.domain.usecase.stats.GetDailyStatsUseCase
import com.activitytracker.app.ui.components.TimelineRow
import com.activitytracker.app.ui.components.SadhanaCard
import com.activitytracker.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// HistoryViewModel
// ──────────────────────────────────────────────────────────────────────────────

data class HistoryUiState(
    val selectedDate: LocalDate = DateTimeUtils.nowLocalDate(),
    val displayedMonth: LocalDate = DateTimeUtils.nowLocalDate(),
    val activities: List<ActivityRecord> = emptyList(),
    val stats: DailyStats? = null,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val getDailyStats: GetDailyStatsUseCase
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(DateTimeUtils.nowLocalDate())
    private val _displayedMonth = MutableStateFlow(
        DateTimeUtils.nowLocalDate().let { LocalDate(it.year, it.month, 1) }
    )

    val state: StateFlow<HistoryUiState> = combine(_selectedDate, _displayedMonth) { date, month ->
        date to month
    }.flatMapLatest { (date, month) ->
        combine(
            activityRepository.getActivitiesForDate(date),
            getDailyStats(date)
        ) { acts, stats ->
            HistoryUiState(
                selectedDate = date,
                displayedMonth = month,
                activities = acts,
                stats = stats,
                isLoading = false
            )
        }
    }
    .distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    fun selectDate(date: LocalDate) {
        _selectedDate.update { date }
        _displayedMonth.update { LocalDate(date.year, date.month, 1) }
    }

    fun prevMonth() {
        _displayedMonth.update {
            val prev = it.minus(1, DateTimeUnit.MONTH)
            LocalDate(prev.year, prev.month, 1)
        }
    }

    fun nextMonth() {
        _displayedMonth.update {
            val next = it.plus(1, DateTimeUnit.MONTH)
            LocalDate(next.year, next.month, 1)
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// HistoryScreen — calendar + day detail
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToAddEdit: (String, String?) -> Unit,  // (dateString, recordId?)
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddEdit(state.selectedDate.toString(), null) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add activity")
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 80.dp
            )
        ) {
            // Calendar
            item { CalendarSection(state = state, viewModel = viewModel) }

            // Selected day header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = state.selectedDate.toDisplayString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (state.stats != null) {
                        Text(
                            text = "${state.activities.size} activities",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            // Sleep summary for selected day
            state.stats?.sleepSummary?.let { sleep ->
                if (sleep.hasSleep) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Bedtime, null, tint = ColorSleep, modifier = Modifier.size(16.dp))
                            Text(
                                "Sleep: ${sleep.totalDisplay}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            sleep.lastNightRecord?.let { r ->
                                Text(
                                    "(${r.startTime?.format24h() ?: "?"} → ${r.endTime?.format24h() ?: "?"})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // Activity timeline for selected day
            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.activities.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No activities recorded for this day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(
                    state.activities.sortedWith(compareBy(nullsLast()) { it.startTime }),
                    key = { it.id }
                ) { record ->
                    val colorLong = record.activityType?.colorArgb?.toLong()
                        ?: categoryColor(record.activityType?.category ?: ActivityCategory.OTHER)
                    TimelineRow(
                        record = record,
                        accentColor = Color(colorLong),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable { onNavigateToAddEdit(state.selectedDate.toString(), record.id) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        thickness = 0.5.dp
                    )
                }
            }

            // Sadhana summary
            state.stats?.plannedSessions?.let { sessions ->
                if (sessions.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SelfImprovement, null, tint = ColorSadhana, modifier = Modifier.size(16.dp))
                            Text(
                                "Sadhana: ${state.stats!!.completedPlannedCount}/${sessions.size} · ${formatDur(state.stats!!.totalSadhanaDurationMinutes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Mobile Screen Time
            state.stats?.mobileScreenTimeMinutes?.let { screenTime ->
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Smartphone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            "Mobile Screen Time: ${formatDur(screenTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarSection(state: HistoryUiState, viewModel: HistoryViewModel) {
    val month = state.displayedMonth
    val today = DateTimeUtils.nowLocalDate()
    val selected = state.selectedDate

    // Days in month
    val firstDay = LocalDate(month.year, month.month, 1)
    val daysInMonth = month.month.length(isLeapYear(month.year))
    // Day of week of first day (Mon=1..Sun=7, shift to Mon=0)
    val startDow = (firstDay.dayOfWeek.ordinal) // 0=Mon

    Column(modifier = Modifier.padding(16.dp)) {
        // Month navigation header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { viewModel.prevMonth() }) {
                Icon(Icons.Default.ChevronLeft, "Previous month")
            }
            val monthName = month.month.name.lowercase().replaceFirstChar { it.uppercase() }
            Text(
                "$monthName ${month.year}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { viewModel.nextMonth() }) {
                Icon(Icons.Default.ChevronRight, "Next month")
            }
        }

        // Weekday headers
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                Text(
                    d, modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Day grid — 7 columns
        val totalCells = startDow + daysInMonth
        val rows = (totalCells + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - startDow + 1
                    if (dayNum < 1 || dayNum > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val dayDate = LocalDate(month.year, month.month, dayNum)
                        val isSelected = dayDate == selected
                        val isToday = dayDate == today
                        val isFuture = dayDate > today

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(enabled = !isFuture) { viewModel.selectDate(dayDate) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$dayNum",
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isLeapYear(year: Int) = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

private fun formatDur(mins: Int): String {
    if (mins <= 0) return "0 min"
    val h = mins / 60; val m = mins % 60
    return if (h == 0) "$m min" else if (m == 0) "${h}h" else "${h}h ${m}m"
}
