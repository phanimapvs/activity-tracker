package com.activitytracker.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.activitytracker.app.domain.model.EmptyStomachConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val timeFormat24h: Boolean = true,
    val weekStartMonday: Boolean = true,
    val themeMode: String = "SYSTEM",   // SYSTEM | LIGHT | DARK
    val isSeeded: Boolean = false,
    val emptyStomachConfig: EmptyStomachConfig = EmptyStomachConfig()
)

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val TIME_FORMAT_24H = booleanPreferencesKey("time_format_24h")
        val WEEK_START_MONDAY = booleanPreferencesKey("week_start_monday")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_SEEDED = booleanPreferencesKey("is_seeded")

        // Empty Stomach Config Keys
        val WATER_GAP_MINS = intPreferencesKey("water_gap_mins")
        val LIGHT_FOOD_GAP_MINS = intPreferencesKey("light_food_gap_mins")
        val FULL_MEAL_GAP_MINS = intPreferencesKey("full_meal_gap_mins")
        val SHOW_EMPTY_STOMACH_CARD = booleanPreferencesKey("show_empty_stomach_card")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            UserPreferences(
                timeFormat24h = prefs[Keys.TIME_FORMAT_24H] ?: true,
                weekStartMonday = prefs[Keys.WEEK_START_MONDAY] ?: true,
                themeMode = prefs[Keys.THEME_MODE] ?: "SYSTEM",
                isSeeded = prefs[Keys.IS_SEEDED] ?: false,
                emptyStomachConfig = EmptyStomachConfig(
                    waterGapMinutes = prefs[Keys.WATER_GAP_MINS] ?: 20,
                    lightFoodGapMinutes = prefs[Keys.LIGHT_FOOD_GAP_MINS] ?: 150,
                    fullMealGapMinutes = prefs[Keys.FULL_MEAL_GAP_MINS] ?: 240,
                    isEnabled = prefs[Keys.SHOW_EMPTY_STOMACH_CARD] ?: true
                )
            )
        }

    suspend fun setTimeFormat24h(value: Boolean) {
        context.dataStore.edit { it[Keys.TIME_FORMAT_24H] = value }
    }

    suspend fun setWeekStartMonday(value: Boolean) {
        context.dataStore.edit { it[Keys.WEEK_START_MONDAY] = value }
    }

    suspend fun setThemeMode(value: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = value }
    }

    suspend fun markSeeded() {
        context.dataStore.edit { it[Keys.IS_SEEDED] = true }
    }

    suspend fun updateEmptyStomachConfig(config: EmptyStomachConfig) {
        context.dataStore.edit {
            it[Keys.WATER_GAP_MINS] = config.waterGapMinutes
            it[Keys.LIGHT_FOOD_GAP_MINS] = config.lightFoodGapMinutes
            it[Keys.FULL_MEAL_GAP_MINS] = config.fullMealGapMinutes
            it[Keys.SHOW_EMPTY_STOMACH_CARD] = config.isEnabled
        }
    }
}
