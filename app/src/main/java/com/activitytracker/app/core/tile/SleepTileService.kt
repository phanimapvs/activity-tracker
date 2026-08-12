package com.activitytracker.app.core.tile

import com.activitytracker.app.domain.model.ActivityCategory
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SleepTileService : BaseActivityTileService() {
    override val categoryName: String = "SLEEP"
    override val categoryEnum: ActivityCategory = ActivityCategory.SLEEP
    override val tileLabel: String = "Sleep Tracker"
}
