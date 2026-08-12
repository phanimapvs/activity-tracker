package com.activitytracker.app.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.activitytracker.app.core.util.DateTimeUtils
import com.activitytracker.app.domain.model.formatDurationMinutes
import com.activitytracker.app.ui.theme.ColorSleep
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTrackingDialog(
    initialBedtimeHHmm: String? = "23:00",
    initialWakeTimeHHmm: String? = null,
    onSaveSleep: (bedDate: LocalDate, bedTimeHHmm: String, wakeDate: LocalDate, wakeTimeHHmm: String, note: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val today = remember { DateTimeUtils.nowLocalDate() }
    val yesterday = remember { today.minus(1, DateTimeUnit.DAY) }
    val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }

    var bedDateIsYesterday by remember { mutableStateOf(true) } // true = Yesterday, false = Today
    var bedTimeInput by remember { mutableStateOf(initialBedtimeHHmm ?: "23:00") }

    var wakeDateIsToday by remember { mutableStateOf(true) } // true = Today, false = Yesterday
    var wakeTimeInput by remember {
        mutableStateOf(initialWakeTimeHHmm ?: "%02d:%02d".format(now.hour, now.minute))
    }

    var noteInput by remember { mutableStateOf("") }
    var isTimeError by remember { mutableStateOf(false) }

    val actualBedDate = if (bedDateIsYesterday) yesterday else today
    val actualWakeDate = if (wakeDateIsToday) today else yesterday

    fun parseLdt(date: LocalDate, timeStr: String): LocalDateTime? {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return LocalDateTime(date.year, date.month, date.dayOfMonth, h, m, 0)
    }

    val bedLdt = parseLdt(actualBedDate, bedTimeInput)
    val wakeLdt = parseLdt(actualWakeDate, wakeTimeInput)

    val calculatedMins = remember(bedLdt, wakeLdt) {
        if (bedLdt != null && wakeLdt != null) {
            val bedInst = bedLdt.toInstant(TimeZone.currentSystemDefault())
            val wakeInst = wakeLdt.toInstant(TimeZone.currentSystemDefault())
            val diff = (wakeInst - bedInst).inWholeMinutes
            if (diff > 0) diff.toInt() else null
        } else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bedtime, contentDescription = null, tint = ColorSleep, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text("Sleep Tracker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Track overnight sleep transition across dates:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Bedtime Section (Went to sleep)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("1. Went to Sleep (Bedtime)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = bedDateIsYesterday,
                                onClick = { bedDateIsYesterday = true },
                                label = { Text("Yesterday (${yesterday.dayOfMonth}/${yesterday.monthNumber})") }
                            )
                            FilterChip(
                                selected = !bedDateIsYesterday,
                                onClick = { bedDateIsYesterday = false },
                                label = { Text("Today (${today.dayOfMonth}/${today.monthNumber})") }
                            )
                        }

                        OutlinedTextField(
                            value = bedTimeInput,
                            onValueChange = { bedTimeInput = it },
                            label = { Text("Bedtime (HH:mm)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 2. Wake-up Section (Woke up)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("2. Woke Up (Wake Time)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = wakeDateIsToday,
                                onClick = { wakeDateIsToday = true },
                                label = { Text("Today (${today.dayOfMonth}/${today.monthNumber})") }
                            )
                            FilterChip(
                                selected = !wakeDateIsToday,
                                onClick = { wakeDateIsToday = false },
                                label = { Text("Yesterday (${yesterday.dayOfMonth}/${yesterday.monthNumber})") }
                            )
                        }

                        OutlinedTextField(
                            value = wakeTimeInput,
                            onValueChange = { wakeTimeInput = it },
                            label = { Text("Wake Time (HH:mm)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Calculated Duration Banner
                if (calculatedMins != null) {
                    Surface(
                        color = ColorSleep.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Duration:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                formatDurationMinutes(calculatedMins),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorSleep
                            )
                        }
                    }
                } else {
                    Text(
                        "⚠️ Wake time must be after bedtime date & time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Notes
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Note (Optional)") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                    placeholder = { Text("e.g. Woke up refreshed") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (calculatedMins != null) {
                        onSaveSleep(
                            actualBedDate,
                            bedTimeInput,
                            actualWakeDate,
                            wakeTimeInput,
                            noteInput.ifBlank { null }
                        )
                    }
                },
                enabled = calculatedMins != null
            ) {
                Text("Save Sleep Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
