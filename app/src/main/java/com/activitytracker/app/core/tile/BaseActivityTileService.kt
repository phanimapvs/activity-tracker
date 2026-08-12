package com.activitytracker.app.core.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.activitytracker.app.core.util.DateTimeUtils
import com.activitytracker.app.domain.model.ActivityCategory
import com.activitytracker.app.domain.model.ActivityRecord
import com.activitytracker.app.domain.model.ActivityStatus
import com.activitytracker.app.domain.repository.ActivityRepository
import com.activitytracker.app.domain.repository.ActivityTypeRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import java.util.UUID
import javax.inject.Inject

/**
 * Base Quick Settings TileService for instant point-in-time activity logging.
 * Manages coroutine scope lifecycle, tile listening events, and state updates safely.
 */
abstract class BaseActivityTileService : TileService() {

    @Inject
    lateinit var activityRepository: ActivityRepository

    @Inject
    lateinit var typeRepository: ActivityTypeRepository

    protected val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    abstract val categoryName: String
    abstract val categoryEnum: ActivityCategory
    abstract val tileLabel: String
    open val defaultDurationMinutes: Int? = null

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            val types = typeRepository.getTypesByCategory(categoryName)
            val type = types.firstOrNull() ?: return@launch
            val now = Clock.System.now()
            val today = DateTimeUtils.nowLocalDate()

            val record = ActivityRecord(
                id = UUID.randomUUID().toString(),
                localDate = today,
                activityTypeId = type.id,
                startTime = now,
                endTime = null,
                durationMinutes = defaultDurationMinutes,
                isCompleted = true,
                status = ActivityStatus.COMPLETED,
                createdAt = now,
                updatedAt = now
            )
            activityRepository.saveActivity(record)
            updateTileState(forceLogged = true)
        }
    }

    protected fun updateTileState(forceLogged: Boolean = false) {
        val tile = qsTile ?: return
        serviceScope.launch {
            val today = DateTimeUtils.nowLocalDate()
            val acts = activityRepository.getActivitiesForDate(today).firstOrNull() ?: emptyList()
            val categoryActs = acts.filter { it.activityType?.category == categoryEnum && it.isCompleted }
            val isLogged = forceLogged || categoryActs.isNotEmpty()

            withContext(Dispatchers.Main) {
                tile.state = if (isLogged) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                tile.label = tileLabel
                tile.subtitle = if (isLogged) {
                    if (categoryActs.size > 1) "${categoryActs.size} Logged Today" else "Logged Today"
                } else {
                    "Tap to Log"
                }
                tile.updateTile()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
