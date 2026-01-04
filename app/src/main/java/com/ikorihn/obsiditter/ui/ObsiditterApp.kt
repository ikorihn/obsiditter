package com.ikorihn.obsiditter.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ikorihn.obsiditter.data.Prefs
import com.ikorihn.obsiditter.ui.categorytracker.CategoryTrackerScreen
import com.ikorihn.obsiditter.ui.exercisetracker.ExerciseTrackerScreen
import com.ikorihn.obsiditter.ui.home.HomeScreen
import com.ikorihn.obsiditter.ui.mealtracker.MealTrackerScreen
import com.ikorihn.obsiditter.ui.settings.SettingsScreen
import com.ikorihn.obsiditter.ui.sleeptracker.SleepTrackerScreen
import kotlinx.coroutines.launch

@Composable
fun ObsiditterApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val prefs = remember { Prefs(context) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val startDestination = if (prefs.storageUri == null) "settings" else "home"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    label = { Text("Home") },
                    icon = { Icon(Icons.Default.Home, null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Sleep Tracker") },
                    icon = { Icon(Icons.Default.Nightlight, null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("sleep_tracker")
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Meal Tracker") },
                    icon = { Icon(Icons.Default.Restaurant, null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("meal_tracker")
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Exercise Tracker") },
                    icon = { Icon(Icons.Default.FitnessCenter, null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("exercise_tracker")
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Collections") },
                    icon = { Icon(Icons.Default.Book, null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("category_tracker")
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    icon = { Icon(Icons.Default.Settings, null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("settings")
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable("home") {
                HomeScreen(
                    onSettings = { navController.navigate("settings") },
                    onMenu = { scope.launch { drawerState.open() } },
                    onNavigateToMealTracker = { navController.navigate("meal_tracker") },
                    onNavigateToExerciseTracker = { navController.navigate("exercise_tracker") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateBack = {
                        navController.navigate("home") {
                            popUpTo("settings") { inclusive = true }
                        }
                    }
                )
            }
            composable("sleep_tracker") {
                SleepTrackerScreen(
                    onMenu = { scope.launch { drawerState.open() } }
                )
            }
            composable("meal_tracker") {
                MealTrackerScreen(
                    onMenu = { scope.launch { drawerState.open() } }
                )
            }
            composable("exercise_tracker") {
                ExerciseTrackerScreen(
                    onMenu = { scope.launch { drawerState.open() } }
                )
            }
            composable("category_tracker") {
                CategoryTrackerScreen(
                    onMenu = { scope.launch { drawerState.open() } }
                )
            }
        }
    }
}