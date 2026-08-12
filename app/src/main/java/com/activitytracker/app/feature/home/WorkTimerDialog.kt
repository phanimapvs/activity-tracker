package com.activitytracker.app.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.activitytracker.app.core.util.DateTimeUtils
import com.activitytracker.app.core.util.format24h
import com.activitytracker.app.domain.model.ActivityRecord
import com.activitytracker.app.domain.model.formatDurationMinutes
import com.activitytracker.app.ui.theme.ColorWork
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Composable
fun WorkStartDialog(
    onStartWork: (timeHHmm: String, note: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    var timeInput by remember { mutableStateOf("%02d:%02d".format(now.hour, now.minute)) }
    var noteInput by remember { mutableStateOf("") }
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
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Work, contentDescription = null, tint = ColorWork, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text("Start Work Session", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Snapshots your start time to begin tracking work hours:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = timeInput,
                    onValueChange = {
                        timeInput = it
                        isError = !validate(it)
                    },
                    label = { Text("Start Time (HH:mm)") },
                    leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                    isError = isError,
                    supportingText = { if (isError) Text("Use 24h format (e.g. 09:00)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Note (Optional)") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                    placeholder = { Text("e.g. Sprint planning / Office desk") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validate(timeInput)) {
                        onStartWork(timeInput, noteInput.ifBlank { null })
                    } else isError = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = ColorWork)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Start Work Timer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun WorkStopDialog(
    ongoingRecord: ActivityRecord,
    onStopWork: (stopTimeHHmm: String, note: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    val startTimeStr = remember(ongoingRecord) { ongoingRecord.startTime?.format24h() ?: "09:00" }
    var stopTimeInput by remember { mutableStateOf("%02d:%02d".format(now.hour, now.minute)) }
    var noteInput by remember { mutableStateOf(ongoingRecord.notes ?: "") }
    var isError by remember { mutableStateOf(false) }

    fun validate(input: String): Boolean {
        val parts = input.trim().split(":")
        if (parts.size != 2) return false
        val h = parts[0].toIntOrNull() ?: return false
        val m = parts[1].toIntOrNull() ?: return false
        return h in 0..23 && m in 0..59
    }

    val calculatedDurationStr = remember(stopTimeInput, ongoingRecord.startTime) {
        if (!validate(stopTimeInput) || ongoingRecord.startTime == null) null
        else {
            val parts = stopTimeInput.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            val today = DateTimeUtils.nowLocalDate()
            val stopLdt = kotlinx.datetime.LocalDateTime(today.year, today.month, today.dayOfMonth, h, m, 0)
            val stopInst = stopLdt.toInstant(TimeZone.currentSystemDefault())
            val diffMins = ((stopInst - ongoingRecord.startTime).inWholeMinutes).toInt()
            if (diffMins > 0) formatDurationMinutes(diffMins) else null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Work, contentDescription = null, tint = ColorWork, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text("Stop Work Session", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    color = ColorWork.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Work Started At: $startTimeStr", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        calculatedDurationStr?.let { dur ->
                            Text("Total Elapsed Work: $dur", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorWork)
                        }
                    }
                }

                OutlinedTextField(
                    value = stopTimeInput,
                    onValueChange = {
                        stopTimeInput = it
                        isError = !validate(it)
                    },
                    label = { Text("Stop Time (HH:mm)") },
                    leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                    isError = isError,
                    supportingText = { if (isError) Text("Use 24h format (e.g. 17:30)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Note (Optional)") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                    placeholder = { Text("e.g. Wrapped up daily tasks") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validate(stopTimeInput)) {
                        onStopWork(stopTimeInput, noteInput.ifBlank { null })
                    } else isError = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Stop Timer & Count Hours")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
