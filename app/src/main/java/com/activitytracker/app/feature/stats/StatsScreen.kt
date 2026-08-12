package com.activitytracker.app.feature.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.activitytracker.app.core.util.DateTimeUtils
import com.activitytracker.app.core.util.format24h
import com.activitytracker.app.domain.model.DailyStats
import com.activitytracker.app.domain.model.formatDurationMinutes
import com.activitytracker.app.domain.usecase.stats.GetDailyStatsUseCase
import com.activitytracker.app.ui.components.StatCard
import com.activitytracker.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalDate
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val getDailyStats: GetDailyStatsUseCase
) : ViewModel() {

    private val today = DateTimeUtils.nowLocalDate()
    val stats: StateFlow<DailyStats?> = getDailyStats(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
                start = 16.dp, end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val s = stats
            if (s == null) {
                item { CircularProgressIndicator() }
                return@LazyColumn
            }

            item {
                StatCard("Sleep", Icons.Default.Bedtime, ColorSleep) {
                    if (s.sleepSummary.hasSleep) {
                        Text(s.sleepSummary.totalDisplay, style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold)
                        s.sleepSummary.lastNightRecord?.let { r ->
                            Text(
                                "${r.startTime?.format24h() ?: "?"} → ${r.endTime?.format24h() ?: "?"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text("Not recorded", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                StatCard("Sadhana", Icons.Default.SelfImprovement, ColorSadhana) {
                    Text(
                        formatDurationMinutes(s.totalSadhanaDurationMinutes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${s.completedPlannedCount} / ${s.plannedSessions.size} planned sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (s.totalSadhanaDurationMinutes > 0 && s.sadhanaProgressPercent > 0) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { s.sadhanaProgressPercent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = ColorSadhana
                        )
                        Text(
                            "${s.sadhanaProgressPercent}% of target",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                StatCard("Work", Icons.Default.Work, ColorWork) {
                    Text(
                        formatDurationMinutes(s.totalWorkDurationMinutes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                StatCard("Meals", Icons.Default.Restaurant, ColorMeal) {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        Column { Text("Breakfast", style = MaterialTheme.typography.labelSmall); Text(s.breakfastTime?.format24h() ?: "--") }
                        Column { Text("Lunch", style = MaterialTheme.typography.labelSmall); Text(s.lunchTime?.format24h() ?: "--") }
                        Column { Text("Dinner", style = MaterialTheme.typography.labelSmall); Text(s.dinnerTime?.format24h() ?: "--") }
                    }
                }
            }

            s.mobileScreenTimeMinutes?.let { screenTime ->
                item {
                    StatCard("Screen Time", Icons.Default.Smartphone, MaterialTheme.colorScheme.primary) {
                        Text(
                            formatDurationMinutes(screenTime),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
