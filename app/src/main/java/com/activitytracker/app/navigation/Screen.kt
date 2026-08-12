package com.activitytracker.app.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable data object Home : Screen
    @Serializable data object Timeline : Screen
    @Serializable data object History : Screen
    @Serializable data class DayDetail(val dateString: String) : Screen
    @Serializable data object Statistics : Screen
    @Serializable data object Settings : Screen
    @Serializable data object ActivityTypes : Screen
    @Serializable data object SadhanaPlans : Screen
    @Serializable data object GeneralSettings : Screen
    @Serializable data class AddEditActivity(
        val recordId: String? = null,
        val dateString: String? = null,
        val activityTypeId: String? = null
    ) : Screen
    @Serializable data class AddEditSadhanaPlan(val planId: String? = null) : Screen
}
