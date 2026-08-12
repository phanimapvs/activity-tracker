package com.activitytracker.app.core.tile

import com.activitytracker.app.domain.model.ActivityCategory
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WakeTileService : BaseActivityTileService() {
    override val categoryName: String = "WAKE"
    override val categoryEnum: ActivityCategory = ActivityCategory.WAKE
    override val tileLabel: String = "Wake Up"
}
