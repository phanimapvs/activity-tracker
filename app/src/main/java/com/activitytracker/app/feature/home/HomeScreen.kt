package com.activitytracker.app.feature.home

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.activitytracker.app.core.util.format24h
import com.activitytracker.app.core.util.DateTimeUtils.toDisplayString
import com.activitytracker.app.core.util.categoryColor
import com.activitytracker.app.domain.model.*
import com.activitytracker.app.ui.components.*
import com.activitytracker.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class QuickActionPending(
    val title: String,
    val category: ActivityCategory,
    val subCategory: ActivitySubCategory? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTimeline: () -> Unit,
    onNavigateToAddActivity: (String?) -> Unit,
    onNavigateToActivityDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editingMealSubCategory by remember { mutableStateOf<ActivitySubCategory?>(null) }
    var pendingQuickAction by remember { mutableStateOf<QuickActionPending?>(null) }

    // Dialog states for Work & Sleep
    var showSleepTrackingDialog by remember { mutableStateOf(false) }
    var showWorkStartDialog by remember { mutableStateOf(false) }
    var showWorkStopDialog by remember { mutableStateOf(false) }

    val ongoingWorkRecord = remember(state.ongoingActivities) {
        state.ongoingActivities.firstOrNull { it.activityType?.category == ActivityCategory.WORK }
    }

    // Live clock state updated per 10s to avoid per-frame recompositions
    var currentTimeString by remember {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        mutableStateOf("%02d:%02d".format(now.hour, now.minute))
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            currentTimeString = "%02d:%02d".format(now.hour, now.minute)
        }
    }

    // 1. Sleep Tracking Dialog (Date & Time Overnight Transition)
    if (showSleepTrackingDialog) {
        SleepTrackingDialog(
            onSaveSleep = { bedDate, bedTime, wakeDate, wakeTime, note ->
                viewModel.saveSleepRecord(bedDate, bedTime, wakeDate, wakeTime, note)
                showSleepTrackingDialog = false
            },
            onDismiss = { showSleepTrackingDialog = false }
        )
    }

    // 2. Work Start Timer Dialog
    if (showWorkStartDialog) {
        WorkStartDialog(
            onStartWork = { startHHmm, note ->
                viewModel.startWorkSession(startHHmm, note)
                showWorkStartDialog = false
            },
            onDismiss = { showWorkStartDialog = false }
        )
    }

    // 3. Work Stop Timer Dialog
    if (showWorkStopDialog && ongoingWorkRecord != null) {
        WorkStopDialog(
            ongoingRecord = ongoingWorkRecord,
            onStopWork = { stopHHmm, note ->
                viewModel.stopWorkSession(ongoingWorkRecord, stopHHmm, note)
                showWorkStopDialog = false
            },
            onDismiss = { showWorkStopDialog = false }
        )
    }

    // Quick Action Confirmation & Time/Note Entry Dialog
    pendingQuickAction?.let { action ->
        QuickActionConfirmDialog(
            actionTitle = action.title,
            onConfirm = { timeHHmm, note ->
                viewModel.saveConfirmedQuickAction(
                    category = action.category,
                    subCategory = action.subCategory,
                    timeHHmm = timeHHmm,
                    notes = note
                )
                pendingQuickAction = null
            },
            onDismiss = { pendingQuickAction = null }
        )
    }

    // Sadhana completion dialog
    state.pendingSadhanaCompletion?.let { pva ->
        SadhanaCompletionDialog(
            sadhanaName = pva.plan.name,
            targetMinutes = pva.plan.targetDurationMinutes,
            onConfirm = { mins, notes -> viewModel.onSadhanaCompletionConfirmed(mins, notes) },
            onDismiss = { viewModel.onSadhanaCompletionDismissed() }
        )
    }

    // Meal Time Manual Entry Dialog
    editingMealSubCategory?.let { sub ->
        val existingTime = when (sub) {
            ActivitySubCategory.BREAKFAST -> state.stats?.breakfastTime?.format24h()
            ActivitySubCategory.LUNCH -> state.stats?.lunchTime?.format24h()
            ActivitySubCategory.DINNER -> state.stats?.dinnerTime?.format24h()
            else -> null
        }
        MealTimeEditDialog(
            subCategory = sub,
            existingTime = existingTime,
            onSave = { timeStr: String ->
                viewModel.saveCustomMealTime(sub, timeStr)
                editingMealSubCategory = null
            },
            onDismiss = { editingMealSubCategory = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.today.toDisplayString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = currentTimeString,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToAddActivity(null) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add activity")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 80.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- One-Time Action Quick Icons ---
            item(key = "quick_actions") {
                QuickActionsSection(
                    isWorking = ongoingWorkRecord != null,
                    onWakeUp = {
                        pendingQuickAction = QuickActionPending("Wake Up", ActivityCategory.WAKE)
                    },
                    onSleep = {
                        showSleepTrackingDialog = true
                    },
                    onBreakfast = {
                        pendingQuickAction = QuickActionPending("Breakfast", ActivityCategory.MEAL, ActivitySubCategory.BREAKFAST)
                    },
                    onLunch = {
                        pendingQuickAction = QuickActionPending("Lunch", ActivityCategory.MEAL, ActivitySubCategory.LUNCH)
                    },
                    onDinner = {
                        pendingQuickAction = QuickActionPending("Dinner", ActivityCategory.MEAL, ActivitySubCategory.DINNER)
                    },
                    onWorkToggle = {
                        if (ongoingWorkRecord != null) {
                            showWorkStopDialog = true
                        } else {
                            showWorkStartDialog = true
                        }
                    }
                )
            }

            // --- EMPTY STOMACH CONDITION CARD ---
            state.emptyStomachReadiness?.let { readiness ->
                item(key = "empty_stomach_card") {
                    EmptyStomachCard(readiness = readiness)
                }
            }

            // --- Sleep Card (Clickable to open Sleep Tracking Dialog) ---
            state.stats?.sleepSummary?.let { sleep ->
                item(key = "sleep_summary") {
                    SleepCard(
                        sleep = sleep,
                        use24h = state.use24h,
                        onCardClick = { showSleepTrackingDialog = true }
                    )
                }
            }

            // --- Mobile Screen Time Card ---
            item(key = "mobile_screen_time_card") {
                MobileScreenTimeCard(
                    screenTimeMinutes = state.stats?.mobileScreenTimeMinutes,
                    hasPermission = state.hasUsagePermission
                )
            }

            // --- Sadhana Section ---
            state.stats?.plannedSessions?.let { sessions ->
                if (sessions.isNotEmpty()) {
                    item(key = "sadhana_header") {
                        SadhanaSectionHeader(stats = state.stats!!)
                    }
                    items(sessions, key = { it.plan.id }) { pva ->
                        SadhanaCard(
                            plannedVsActual = pva,
                            accentColor = ColorSadhana,
                            use24h = state.use24h,
                            onToggleComplete = { viewModel.onSadhanaToggleTapped(it) },
                            onEditNote = { record ->
                                onNavigateToActivityDetail(record.id)
                            },
                            onCardClick = {
                                pva.actualRecord?.let { record ->
                                    onNavigateToActivityDetail(record.id)
                                } ?: onNavigateToAddActivity(pva.plan.activityTypeId)
                            }
                        )
                    }
                }
            }

            // --- Meals Card (Interactive Manual Time Edit on Tap) ---
            state.stats?.let { stats ->
                item(key = "meals_summary") {
                    MealsCard(
                        stats = stats,
                        use24h = state.use24h,
                        onMealClick = { sub -> editingMealSubCategory = sub }
                    )
                }
            }

            // --- Timeline (Today, Compact View) ---
            state.stats?.let { _ ->
                item(key = "timeline_header") {
                    TimelineSectionHeader(onViewAll = onNavigateToTimeline)
                }
                item(key = "timeline_button") {
                    OutlinedButton(
                        onClick = onNavigateToTimeline,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open Full Interactive Timeline →", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}


