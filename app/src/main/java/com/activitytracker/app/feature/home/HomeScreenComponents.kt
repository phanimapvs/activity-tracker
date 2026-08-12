package com.activitytracker.app.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.activitytracker.app.core.util.format24h
import com.activitytracker.app.domain.model.ActivityCategory
import com.activitytracker.app.domain.model.ActivitySubCategory
import com.activitytracker.app.domain.model.DailyStats
import com.activitytracker.app.domain.model.SleepSummary
import com.activitytracker.app.ui.components.QuickActionButton
import com.activitytracker.app.ui.theme.ColorMeal
import com.activitytracker.app.ui.theme.ColorSadhana
import com.activitytracker.app.ui.theme.ColorSleep
import com.activitytracker.app.ui.theme.ColorWake
import com.activitytracker.app.ui.theme.ColorWork
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun QuickActionsSection(
    isWorking: Boolean,
    onWakeUp: () -> Unit,
    onSleep: () -> Unit,
    onBreakfast: () -> Unit,
    onLunch: () -> Unit,
    onDinner: () -> Unit,
    onWorkToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
            Text(
                text = "Instant Quick Actions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickActionButton(
                    icon = Icons.Default.WbSunny,
                    label = "Wake",
                    tint = ColorWake,
                    onClick = onWakeUp
                )
                QuickActionButton(
                    icon = Icons.Default.Bedtime,
                    label = "Sleep",
                    tint = ColorSleep,
                    onClick = onSleep
                )
                QuickActionButton(
                    icon = Icons.Default.FreeBreakfast,
                    label = "B'fast",
                    tint = ColorMeal,
                    onClick = onBreakfast
                )
                QuickActionButton(
                    icon = Icons.Default.LunchDining,
                    label = "Lunch",
                    tint = ColorMeal,
                    onClick = onLunch
                )
                QuickActionButton(
                    icon = Icons.Default.DinnerDining,
                    label = "Dinner",
                    tint = ColorMeal,
                    onClick = onDinner
                )
                QuickActionButton(
                    icon = Icons.Default.Work,
                    label = if (isWorking) "Working" else "Work",
                    tint = ColorWork,
                    isActive = isWorking,
                    onClick = onWorkToggle
                )
            }
        }
    }
}

@Composable
fun SleepCard(
    sleep: SleepSummary,
    use24h: Boolean,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSleep.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.NightsStay,
                contentDescription = null,
                tint = ColorSleep,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Yesterday's Sleep (Tap to Track/Edit)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = sleep.totalDisplay,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorSleep
                )
            }
        }
    }
}

@Composable
fun SadhanaSectionHeader(stats: DailyStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Daily Sadhana",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${stats.completedPlannedCount}/${stats.plannedSessions.size} Completed",
            style = MaterialTheme.typography.bodySmall,
            color = if (stats.completedPlannedCount == stats.plannedSessions.size) ColorSadhana else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun MealsCard(
    stats: DailyStats,
    use24h: Boolean,
    onMealClick: (ActivitySubCategory) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorMeal.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Meals Today (Tap time to edit)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MealTimeItem(
                    label = "Breakfast",
                    time = stats.breakfastTime?.format24h() ?: "Tap to log",
                    icon = Icons.Default.FreeBreakfast,
                    onClick = { onMealClick(ActivitySubCategory.BREAKFAST) }
                )
                MealTimeItem(
                    label = "Lunch",
                    time = stats.lunchTime?.format24h() ?: "Tap to log",
                    icon = Icons.Default.LunchDining,
                    onClick = { onMealClick(ActivitySubCategory.LUNCH) }
                )
                MealTimeItem(
                    label = "Dinner",
                    time = stats.dinnerTime?.format24h() ?: "Tap to log",
                    icon = Icons.Default.DinnerDining,
                    onClick = { onMealClick(ActivitySubCategory.DINNER) }
                )
            }
        }
    }
}

@Composable
fun MealTimeItem(
    label: String,
    time: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = ColorMeal, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(time, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ColorMeal)
    }
}

@Composable
fun TimelineSectionHeader(onViewAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Today's Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = onViewAll) {
            Text("Full View →")
        }
    }
}

@Composable
fun MealTimeEditDialog(
    subCategory: ActivitySubCategory,
    existingTime: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    var timeInput by remember { mutableStateOf(existingTime ?: "%02d:%02d".format(now.hour, now.minute)) }
    var isError by remember { mutableStateOf(false) }

    fun validate(input: String): Boolean {
        val parts = input.trim().split(":")
        if (parts.size != 2) return false
        val h = parts[0].toIntOrNull() ?: return false
        val m = parts[1].toIntOrNull() ?: return false
        return h in 0..23 && m in 0..59
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Meal Time - ${subCategory.name.lowercase().replaceFirstChar { it.uppercase() }}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = timeInput,
                    onValueChange = {
                        timeInput = it
                        isError = !validate(it)
                    },
                    label = { Text("Time (HH:mm)") },
                    isError = isError,
                    supportingText = { if (isError) Text("Enter time as 08:30 or 13:15") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validate(timeInput)) onSave(timeInput) else isError = true
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
