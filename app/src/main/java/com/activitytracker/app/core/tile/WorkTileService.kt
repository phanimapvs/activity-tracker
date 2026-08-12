package com.activitytracker.app.core.tile

import com.activitytracker.app.domain.model.ActivityCategory
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WorkTileService : BaseActivityTileService() {
    override val categoryName: String = "WORK"
    override val categoryEnum: ActivityCategory = ActivityCategory.WORK
    override val tileLabel: String = "Work Session"
}
