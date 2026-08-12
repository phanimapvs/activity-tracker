package com.activitytracker.app.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.activitytracker.app.domain.model.formatDurationMinutes
import com.activitytracker.app.domain.model.parseDurationInput

/**
 * Dialog shown when the user toggles a sadhana ON.
 * Asks: "How long did [name] take?" with a plain number field.
 * Input: plain number → converted to minutes (45 → 45min, 1.5 → 90min)
 */
@Composable
fun SadhanaCompletionDialog(
    sadhanaName: String,
    targetMinutes: Int?,
    onConfirm: (durationMinutes: Int, notes: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var durationInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Parsed preview
    val parsedMins = parseDurationInput(durationInput)
    val previewText = parsedMins?.let { formatDurationMinutes(it) } ?: ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = sadhanaName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "How long did it take?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (targetMinutes != null) {
                    Text(
                        text = "Target: ${formatDurationMinutes(targetMinutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = durationInput,
                    onValueChange = {
                        durationInput = it
                        errorMessage = null
                    },
                    label = { Text("Minutes (e.g. 45, 1.5, 90)") },
                    suffix = {
                        if (previewText.isNotEmpty()) {
                            Text(previewText, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { err -> { Text(err) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("e.g. Felt focused...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = parseDurationInput(durationInput)
                    if (mins == null || mins <= 0) {
                        errorMessage = "Enter a valid duration (e.g. 45 or 1.5)"
                    } else {
                        onConfirm(mins, notesInput.ifBlank { null })
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
