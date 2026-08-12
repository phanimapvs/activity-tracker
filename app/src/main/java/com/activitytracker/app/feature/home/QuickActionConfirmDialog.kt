package com.activitytracker.app.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.activitytracker.app.core.util.DateTimeUtils
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun QuickActionConfirmDialog(
    actionTitle: String,
    onConfirm: (timeHHmm: String, note: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    var timeInput by remember { mutableStateOf("%02d:%02d".format(now.hour, now.minute)) }
    var noteInput by remember { mutableStateOf("") }
    var isTimeError by remember { mutableStateOf(false) }

    fun parseAndValidateTime(input: String): Boolean {
        val parts = input.trim().split(":")
        if (parts.size != 2) return false
        val h = parts[0].toIntOrNull() ?: return false
        val m = parts[1].toIntOrNull() ?: return false
        return h in 0..23 && m in 0..59
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record $actionTitle",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Confirm the details for this record below:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Time Input
                OutlinedTextField(
                    value = timeInput,
                    onValueChange = {
                        timeInput = it
                        isTimeError = !parseAndValidateTime(it)
                    },
                    label = { Text("Time (HH:mm)") },
                    leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                    isError = isTimeError,
                    supportingText = {
                        if (isTimeError) {
                            Text("Use 24-hour format (e.g. 08:30 or 21:15)")
                        } else {
                            Text("Time when this action occurred")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Time Adjust Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            val cur = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                            timeInput = "%02d:%02d".format(cur.hour, cur.minute)
                            isTimeError = false
                        },
                        label = { Text("Now") }
                    )
                    AssistChip(
                        onClick = {
                            val cur = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                            val totalMins = (cur.hour * 60 + cur.minute - 15 + 1440) % 1440
                            timeInput = "%02d:%02d".format(totalMins / 60, totalMins % 60)
                            isTimeError = false
                        },
                        label = { Text("-15m") }
                    )
                    AssistChip(
                        onClick = {
                            val cur = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                            val totalMins = (cur.hour * 60 + cur.minute - 30 + 1440) % 1440
                            timeInput = "%02d:%02d".format(totalMins / 60, totalMins % 60)
                            isTimeError = false
                        },
                        label = { Text("-30m") }
                    )
                }

                // Note Input
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Note (Optional)") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                    placeholder = { Text("e.g. Had light breakfast / Slept well") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parseAndValidateTime(timeInput)) {
                        onConfirm(timeInput, noteInput.ifBlank { null })
                    } else {
                        isTimeError = true
                    }
                },
                enabled = !isTimeError && timeInput.isNotBlank()
            ) {
                Text("Confirm & Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
