package com.activitytracker.app.feature.settings

import androidx.compose.ui.res.stringResource
import com.activitytracker.app.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.activitytracker.app.data.preferences.UserPreferences
import com.activitytracker.app.data.preferences.UserPreferencesDataStore
import com.activitytracker.app.domain.model.EmptyStomachConfig
import com.activitytracker.app.domain.repository.ScreenTimeRepository
import com.activitytracker.app.domain.usecase.screentime.ScreenTimeCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
    private val screenTimeCalculator: ScreenTimeCalculator,
    private val screenTimeRepository: ScreenTimeRepository
) : ViewModel() {
    val preferences: StateFlow<UserPreferences> = prefs.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val hasUsagePermission = MutableStateFlow(false)
    val isSyncing = MutableStateFlow(false)
    val syncMessage = MutableStateFlow<String?>(null)

    fun checkUsagePermission() {
        hasUsagePermission.value = screenTimeCalculator.hasUsageAccessPermission()
    }

    fun syncScreenTime() {
        if (isSyncing.value) return
        viewModelScope.launch {
            isSyncing.value = true
            try {
                screenTimeRepository.syncPastDays(7)
                syncMessage.value = "Screen Time sync complete"
            } catch (e: Exception) {
                syncMessage.value = "Screen Time sync failed"
            } finally {
                isSyncing.value = false
            }
        }
    }

    fun deleteScreenTimeHistory() = viewModelScope.launch {
        screenTimeRepository.deleteHistory()
        syncMessage.value = "Screen Time data cleared"
    }

    fun dismissSyncMessage() {
        syncMessage.value = null
    }

    fun setTimeFormat24h(v: Boolean) = viewModelScope.launch { prefs.setTimeFormat24h(v) }
    fun setThemeMode(v: String) = viewModelScope.launch { prefs.setThemeMode(v) }
    fun setWeekStartMonday(v: Boolean) = viewModelScope.launch { prefs.setWeekStartMonday(v) }
    fun updateEmptyStomachConfig(config: EmptyStomachConfig) = viewModelScope.launch { prefs.updateEmptyStomachConfig(config) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToActivityTypes: () -> Unit,
    onNavigateToSadhanaPlans: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val hasUsagePermission by viewModel.hasUsagePermission.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    val esConfig = prefs.emptyStomachConfig
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSyncMessage()
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Screen Time Data?") },
            text = { Text("This will permanently delete the recorded screen-time history from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteScreenTimeHistory()
                    showClearConfirm = false
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkUsagePermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 80.dp
        )) {
            item {
                SettingsSectionHeader(stringResource(R.string.customization))
                SettingsNavItem(
                    icon = Icons.Default.FitnessCenter,
                    label = stringResource(R.string.sadhana_plans),
                    subtitle = stringResource(R.string.sadhana_plans_desc),
                    onClick = onNavigateToSadhanaPlans
                )
                SettingsNavItem(
                    icon = Icons.Default.Category,
                    label = stringResource(R.string.activity_types),
                    subtitle = stringResource(R.string.activity_types_desc),
                    onClick = onNavigateToActivityTypes
                )
            }

            item {
                SettingsSectionHeader(stringResource(R.string.mobile_screen_time))
                if (hasUsagePermission) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.screen_time_tracking_enabled)) },
                        leadingContent = { Icon(Icons.Default.Smartphone, null) }
                    )
                    SettingsNavItem(
                        icon = Icons.Default.Sync,
                        label = if (isSyncing) "Syncing..." else stringResource(R.string.sync_screen_time),
                        subtitle = stringResource(R.string.sync_screen_time_desc),
                        onClick = { viewModel.syncScreenTime() }
                    )
                    SettingsNavItem(
                        icon = Icons.Default.DeleteForever,
                        label = stringResource(R.string.delete_history),
                        subtitle = stringResource(R.string.delete_history_desc),
                        onClick = { showClearConfirm = true }
                    )
                } else {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.access_not_granted)) },
                        supportingContent = { Text(stringResource(R.string.requires_usage_access)) },
                        leadingContent = { Icon(Icons.Default.Smartphone, null) },
                        trailingContent = {
                            Button(onClick = {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            }) {
                                Text(stringResource(R.string.grant_access))
                            }
                        }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            item {
                SettingsSectionHeader(stringResource(R.string.empty_stomach_readiness))
                SettingsToggleItem(
                    icon = Icons.Default.Spa,
                    label = stringResource(R.string.enable_empty_stomach),
                    checked = esConfig.isEnabled,
                    onCheckedChange = {
                        viewModel.updateEmptyStomachConfig(esConfig.copy(isEnabled = it))
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            item {
                SettingsSectionHeader(stringResource(R.string.display))
                SettingsToggleItem(
                    icon = Icons.Default.Schedule,
                    label = stringResource(R.string.time_format_24h),
                    checked = prefs.timeFormat24h,
                    onCheckedChange = viewModel::setTimeFormat24h
                )
                SettingsToggleItem(
                    icon = Icons.Default.CalendarToday,
                    label = stringResource(R.string.week_starts_monday),
                    checked = prefs.weekStartMonday,
                    onCheckedChange = viewModel::setWeekStartMonday
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}
