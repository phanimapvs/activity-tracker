package com.activitytracker.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.activitytracker.app.core.util.DateTimeUtils
import com.activitytracker.app.feature.addedit.AddEditActivityScreen
import com.activitytracker.app.feature.history.HistoryScreen
import com.activitytracker.app.feature.home.HomeScreen
import com.activitytracker.app.feature.settings.SettingsScreen
import com.activitytracker.app.feature.settings.activitytypes.ActivityTypesScreen
import com.activitytracker.app.feature.settings.sadhanaplans.SadhanaPlansScreen
import com.activitytracker.app.feature.stats.StatsScreen
import com.activitytracker.app.feature.timeline.TimelineScreen

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any
)

val bottomNavItems = listOf(
    BottomNavItem("Home",     Icons.Default.Home,        Screen.Home),
    BottomNavItem("Timeline", Icons.Default.Timeline,    Screen.Timeline),
    BottomNavItem("History",  Icons.Default.CalendarMonth, Screen.History),
    BottomNavItem("Stats",    Icons.Default.BarChart,    Screen.Statistics),
    BottomNavItem("Settings", Icons.Default.Settings,    Screen.Settings),
)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val showBottomBar = bottomNavItems.any { item ->
        currentDest?.hierarchy?.any { it.hasRoute(item.route::class) } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDest?.hierarchy?.any {
                            it.hasRoute(item.route::class)
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, item.label) },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home,
            modifier = Modifier.padding(innerPadding)
        ) {
            homeGraph(navController)
            timelineGraph(navController)
            historyGraph(navController)
            statisticsGraph(navController)
            settingsGraph(navController)
            addEditActivityGraph(navController)
            
            // ── Day detail (from history — future) ────────────────────────
            composable<Screen.DayDetail> {
                val route = it.toRoute<Screen.DayDetail>()
                PlaceholderScreen("Day Detail: ${route.dateString}")
            }
        }
    }
}

private fun NavGraphBuilder.homeGraph(navController: NavHostController) {
    composable<Screen.Home> {
        HomeScreen(
            onNavigateToTimeline = { navController.navigate(Screen.Timeline) },
            onNavigateToAddActivity = { typeId ->
                navController.navigate(
                    Screen.AddEditActivity(
                        dateString = DateTimeUtils.nowLocalDate().toString(),
                        activityTypeId = typeId
                    )
                )
            },
            onNavigateToActivityDetail = { id ->
                navController.navigate(Screen.AddEditActivity(recordId = id))
            }
        )
    }
}

private fun NavGraphBuilder.timelineGraph(navController: NavHostController) {
    composable<Screen.Timeline> {
        TimelineScreen(
            onNavigateUp = { navController.navigateUp() },
            onAddActivity = { _ ->
                navController.navigate(
                    Screen.AddEditActivity(
                        dateString = DateTimeUtils.nowLocalDate().toString()
                    )
                )
            },
            onActivityClick = { id ->
                navController.navigate(Screen.AddEditActivity(recordId = id))
            }
        )
    }
}

private fun NavGraphBuilder.historyGraph(navController: NavHostController) {
    composable<Screen.History> {
        HistoryScreen(
            onNavigateToAddEdit = { dateStr, recordId ->
                navController.navigate(
                    Screen.AddEditActivity(recordId = recordId, dateString = dateStr)
                )
            }
        )
    }
}

private fun NavGraphBuilder.statisticsGraph(navController: NavHostController) {
    composable<Screen.Statistics> {
        StatsScreen()
    }
}

private fun NavGraphBuilder.settingsGraph(navController: NavHostController) {
    composable<Screen.Settings> {
        SettingsScreen(
            onNavigateToActivityTypes = { navController.navigate(Screen.ActivityTypes) },
            onNavigateToSadhanaPlans  = { navController.navigate(Screen.SadhanaPlans) }
        )
    }

    composable<Screen.ActivityTypes> {
        ActivityTypesScreen(
            onNavigateUp = { navController.navigateUp() }
        )
    }

    composable<Screen.SadhanaPlans> {
        SadhanaPlansScreen(
            onNavigateUp = { navController.navigateUp() }
        )
    }
}

private fun NavGraphBuilder.addEditActivityGraph(navController: NavHostController) {
    composable<Screen.AddEditActivity> {
        AddEditActivityScreen(
            onNavigateUp = { navController.navigateUp() }
        )
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(name, style = MaterialTheme.typography.bodyLarge)
    }
}
