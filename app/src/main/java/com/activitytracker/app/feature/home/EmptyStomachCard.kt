package com.activitytracker.app.feature.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.activitytracker.app.core.util.format24h
import com.activitytracker.app.domain.model.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun EmptyStomachCard(
    readiness: PracticeReadinessResult,
    modifier: Modifier = Modifier
) {
    val status = readiness.status

    val cardBg = when (status) {
        EmptyStomachStatus.READY -> Color(0xFF1B5E20).copy(alpha = 0.12f)
        EmptyStomachStatus.NOT_READY -> Color(0xFFE65100).copy(alpha = 0.12f)
        EmptyStomachStatus.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val badgeColor = when (status) {
        EmptyStomachStatus.READY -> Color(0xFF2E7D32)
        EmptyStomachStatus.NOT_READY -> Color(0xFFEF6C00)
        EmptyStomachStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row: Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Spa, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
                Text(
                    text = "EMPTY STOMACH CONDITION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            // Status Main Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (status) {
                            EmptyStomachStatus.READY -> Icons.Default.CheckCircle
                            EmptyStomachStatus.NOT_READY -> Icons.Default.Timer
                            EmptyStomachStatus.UNKNOWN -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    val mainText = when (status) {
                        EmptyStomachStatus.READY -> "Ready for Practice"
                        EmptyStomachStatus.NOT_READY -> {
                            val remMins = readiness.remainingMinutes ?: 0
                            val h = remMins / 60
                            val m = remMins % 60
                            val remStr = if (h > 0) "${h}h ${m}m" else "${m}m"
                            "Not Ready Yet (in $remStr)"
                        }
                        EmptyStomachStatus.UNKNOWN -> "Intake Not Recorded"
                    }

                    Text(
                        text = mainText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )

                    // Sub-caption details
                    val detailsText = when (status) {
                        EmptyStomachStatus.UNKNOWN -> "No food or water records logged today"
                        else -> {
                            val name = readiness.controllingIntakeRecord?.activityType?.name ?: readiness.controllingIntakeType?.name ?: "Intake"
                            val timeStr = readiness.lastIntakeTime?.format24h() ?: ""
                            val elapsedStr = readiness.elapsedMinutes?.let { formatDurationMinutes(it) } ?: ""
                            "Last intake: $name at $timeStr ($elapsedStr ago)"
                        }
                    }

                    Text(
                        text = detailsText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Milestone Progression Bar (20m -> 2.5h -> 4h)
            if (status != EmptyStomachStatus.UNKNOWN && readiness.elapsedMinutes != null) {
                MilestoneProgressBar(
                    elapsedMinutes = readiness.elapsedMinutes,
                    requiredGapMinutes = readiness.requiredGapMinutes ?: 240,
                    readyAt = readiness.readyAt,
                    badgeColor = badgeColor
                )
            }
        }
    }
}

@Composable
private fun MilestoneProgressBar(
    elapsedMinutes: Int,
    requiredGapMinutes: Int,
    readyAt: kotlinx.datetime.Instant?,
    badgeColor: Color
) {
    // 3 Key Milestones in minutes: Water (20m), Light Food (150m / 2.5h), Full Meal (240m / 4h)
    val maxBarMinutes = 240.0f
    val progressFraction = (elapsedMinutes.toFloat() / maxBarMinutes).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Continuous Progress Bar with Milestone Markings
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(badgeColor.copy(alpha = 0.15f))
        ) {
            // Filled progress
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressFraction)
                    .clip(RoundedCornerShape(5.dp))
                    .background(badgeColor)
            )
        }

        // Milestone Labels & Markers Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MilestoneNode(
                label = "20m",
                subLabel = "Water",
                isPassed = elapsedMinutes >= 20,
                activeColor = badgeColor
            )
            MilestoneNode(
                label = "2.5h",
                subLabel = "Light Food",
                isPassed = elapsedMinutes >= 150,
                activeColor = badgeColor
            )
            MilestoneNode(
                label = "4.0h",
                subLabel = "Full Meal",
                isPassed = elapsedMinutes >= 240,
                activeColor = badgeColor
            )
        }

        // Expected Ready Clock (if still in progress)
        if (elapsedMinutes < requiredGapMinutes && readyAt != null) {
            val rLdt = readyAt.toLocalDateTime(TimeZone.currentSystemDefault())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Expected ready at %02d:%02d".format(rLdt.hour, rLdt.minute),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor
                )
            }
        }
    }
}

@Composable
private fun MilestoneNode(
    label: String,
    subLabel: String,
    isPassed: Boolean,
    activeColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(if (isPassed) activeColor else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (isPassed) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isPassed) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isPassed) FontWeight.Bold else FontWeight.Normal,
                color = if (isPassed) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
