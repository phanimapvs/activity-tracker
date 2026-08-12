package com.activitytracker.app.data.mapper

import com.activitytracker.app.data.local.entity.ActivityRecordEntity
import com.activitytracker.app.data.local.entity.ActivityTypeEntity
import com.activitytracker.app.data.local.entity.DailyNoteEntity
import com.activitytracker.app.data.local.entity.SadhanaPlanEntity
import com.activitytracker.app.domain.model.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ---------- ActivityType ----------

fun ActivityTypeEntity.toDomain() = ActivityType(
    id = id,
    name = name,
    category = ActivityCategory.valueOf(category),
    subCategory = try { ActivitySubCategory.valueOf(subCategory) } catch (e: Exception) { ActivitySubCategory.NONE },
    iconKey = iconKey,
    colorArgb = colorArgb,
    isActive = isActive,
    sortOrder = sortOrder,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt)
)

fun ActivityType.toEntity() = ActivityTypeEntity(
    id = id,
    name = name,
    category = category.name,
    subCategory = subCategory.name,
    iconKey = iconKey,
    colorArgb = colorArgb,
    isActive = isActive,
    sortOrder = sortOrder,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds()
)

// ---------- ActivityRecord ----------

fun ActivityRecordEntity.toDomain(type: ActivityType? = null) = ActivityRecord(
    id = id,
    localDate = LocalDate.parse(localDate),
    activityTypeId = activityTypeId,
    activityType = type,
    startTime = startTime?.let { Instant.fromEpochMilliseconds(it) },
    endTime = endTime?.let { Instant.fromEpochMilliseconds(it) },
    durationMinutes = durationMinutes,
    notes = notes,
    isCompleted = isCompleted,
    isManuallyEdited = isManuallyEdited,
    sadhanaPlanId = sadhanaPlanId,
    status = try { ActivityStatus.valueOf(status) } catch (e: Exception) { ActivityStatus.COMPLETED },
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt)
)

fun ActivityRecord.toEntity() = ActivityRecordEntity(
    id = id,
    localDate = localDate.toString(),
    activityTypeId = activityTypeId,
    startTime = startTime?.toEpochMilliseconds(),
    endTime = endTime?.toEpochMilliseconds(),
    durationMinutes = durationMinutes,
    notes = notes,
    isCompleted = isCompleted,
    isManuallyEdited = isManuallyEdited,
    sadhanaPlanId = sadhanaPlanId,
    status = status.name,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds()
)

// ---------- SadhanaPlan ----------

fun SadhanaPlanEntity.toDomain() = SadhanaPlan(
    id = id,
    name = name,
    description = description,
    activityTypeId = activityTypeId,
    targetDurationMinutes = targetDurationMinutes,
    targetSessionsPerDay = targetSessionsPerDay,
    timeSlot = try { TimeSlot.valueOf(timeSlot) } catch (e: Exception) { TimeSlot.ANYTIME },
    preferredStartHour = preferredStartHour,
    preferredStartMinute = preferredStartMinute,
    gapAfterActivityTypeId = gapAfterActivityTypeId,
    gapAfterActivityMinutes = gapAfterActivityMinutes,
    isActive = isActive,
    isDaily = isDaily,
    sortOrder = sortOrder,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt)
)

fun SadhanaPlan.toEntity() = SadhanaPlanEntity(
    id = id,
    name = name,
    description = description,
    activityTypeId = activityTypeId,
    targetDurationMinutes = targetDurationMinutes,
    targetSessionsPerDay = targetSessionsPerDay,
    timeSlot = timeSlot.name,
    preferredStartHour = preferredStartHour,
    preferredStartMinute = preferredStartMinute,
    gapAfterActivityTypeId = gapAfterActivityTypeId,
    gapAfterActivityMinutes = gapAfterActivityMinutes,
    isActive = isActive,
    isDaily = isDaily,
    sortOrder = sortOrder,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds()
)

// ---------- DailyNote ----------

fun DailyNoteEntity.toDomain() = DailyNote(
    id = id,
    localDate = LocalDate.parse(localDate),
    note = note,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt)
)

fun DailyNote.toEntity() = DailyNoteEntity(
    id = id,
    localDate = localDate.toString(),
    note = note,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds()
)
