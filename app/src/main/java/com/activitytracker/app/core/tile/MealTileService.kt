package com.activitytracker.app.core.tile

import com.activitytracker.app.domain.model.ActivityCategory
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MealTileService : BaseActivityTileService() {
    override val categoryName: String = "MEAL"
    override val categoryEnum: ActivityCategory = ActivityCategory.MEAL
    override val tileLabel: String = "Meal Log"
}
