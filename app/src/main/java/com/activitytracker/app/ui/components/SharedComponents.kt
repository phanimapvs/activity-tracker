package com.activitytracker.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.activitytracker.app.core.util.format24h
import com.activitytracker.app.domain.model.ActivityRecord
import com.activitytracker.app.domain.model.PlannedVsActual
import com.activitytracker.app.domain.model.formatDurationMinutes

/**
 * A compact sadhana card showing name, toggle (complete/pending), duration, time, and inline note.
 *
 * Layout: [Icon | Name  Duration  [Toggle]] + optional time row + optional note row
 */
@Composable
fun SadhanaCard(
    plannedVsActual: PlannedVsActual,
    accentColor: Color,
    use24h: Boolean = true,
    onToggleComplete: (PlannedVsActual) -> Unit,
    onEditNote: (ActivityRecord) -> Unit,
    onCardClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val plan = plannedVsActual.plan
    val record = plannedVsActual.actualRecord
    val isCompleted = plannedVsActual.isCompleted

    val toggleColor by animateColorAsState(
        targetValue = if (isCompleted) accentColor else MaterialTheme.colorScheme.outline,
        animationSpec = tween(300),
        label = "toggle_color"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onCardClick != null) Modifier.clickable(onClick = onCardClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // --- Row 1: Icon + Name + Duration + Toggle ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Colored dot indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )

                // Plan name
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Duration display (actual or target)
                val durationText = when {
                    record?.durationMinutes != null -> formatDurationMinutes(record.durationMinutes)
                    plan.targetDurationMinutes != null -> "Target: ${formatDurationMinutes(plan.targetDurationMinutes)}"
                    else -> ""
                }
                if (durationText.isNotEmpty()) {
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Completion toggle
                Switch(
                    checked = isCompleted,
                    onCheckedChange = { onToggleComplete(plannedVsActual) },
                    modifier = Modifier.height(24.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accentColor,
                        checkedTrackColor = accentColor.copy(alpha = 0.3f),
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            // --- Row 2: Time range + progress (if completed) ---
            if (record != null && (record.startTime != null || record.endTime != null)) {
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val startStr = record.startTime?.format24h() ?: "..."
                    val endStr = record.endTime?.format24h() ?: "..."
                    Text(
                        text = "$startStr → $endStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Progress % vs target
                    plannedVsActual.completionPercent?.let { pct ->
                        if (plan.targetDurationMinutes != null) {
                            val color = if (pct >= 100) accentColor else MaterialTheme.colorScheme.outline
                            Text(
                                text = "($pct%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = color
                            )
                        }
                    }
                }
            }

            // --- Row 3: Inline note (tap to edit) ---
            val noteText = record?.notes
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { record?.let { onEditNote(it) } }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Note",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = noteText?.ifBlank { null } ?: "Add note...",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (noteText.isNullOrBlank())
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * A stat card used on the dashboard for Sleep, Sadhana summary, Work, Meals.
 */
@Composable
fun StatCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onCardClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onCardClick != null) Modifier.clickable(onClick = onCardClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )
                }
                if (onCardClick != null) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit section",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            content()
        }
    }
}

/**
 * Quick action button — large tap target, labeled icon button with optional active status indicator.
 */
@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    isActive: Boolean = false,
    activeText: String? = null,
    modifier: Modifier = Modifier
) {
    val bgAlpha = if (isActive) 0.35f else 0.15f
    val containerShape = RoundedCornerShape(12.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(containerShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = bgAlpha),
            border = if (isActive) androidx.compose.foundation.BorderStroke(2.dp, tint) else null,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
            }
        }
        Text(
            text = activeText ?: label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) tint else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * A single row in the timeline — shows colored dot, time, activity name.
 */
@Composable
fun TimelineRow(
    record: ActivityRecord,
    accentColor: Color,
    use24h: Boolean = true,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "bg_color"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time column
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(52.dp)
        ) {
            val startStr = record.startTime?.format24h() ?: ""
            val endStr = record.endTime?.format24h() ?: if (record.status.name == "ONGOING") "now" else ""
            if (startStr.isNotEmpty()) {
                Text(startStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (endStr.isNotEmpty()) {
                Text(endStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }

        // Colored line + dot
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else accentColor)
            )
        }

        // Activity name + duration
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.activityType?.name ?: "Unknown",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            if (record.durationMinutes != null && record.durationMinutes > 0) {
                Text(
                    text = formatDurationMinutes(record.durationMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Completed checkmark or selection indicator
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        } else if (record.isCompleted) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Completed",
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
