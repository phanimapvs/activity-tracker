package com.activitytracker.app.core.tile

import com.activitytracker.app.domain.model.ActivityCategory
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SadhanaTileService : BaseActivityTileService() {
    override val categoryName: String = "SADHANA"
    override val categoryEnum: ActivityCategory = ActivityCategory.SADHANA
    override val tileLabel: String = "Quick Sadhana"
    override val defaultDurationMinutes: Int = 30
}
