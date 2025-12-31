package com.ikorihn.obsiditter.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ikorihn.obsiditter.data.Prefs
import com.ikorihn.obsiditter.ui.add.AddNoteScreen
import com.ikorihn.obsiditter.ui.edit.EditNoteScreen
import com.ikorihn.obsiditter.ui.home.HomeScreen
import com.ikorihn.obsiditter.ui.settings.SettingsScreen

@Composable
fun ObsiditterApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val prefs = remember { Prefs(context) }
    
    val startDestination = if (prefs.storageUri == null) "settings" else "home"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("home") {
            HomeScreen(
                onAddNote = { navController.navigate("add") },
                onEditNote = { date, index -> navController.navigate("edit/$date/$index") },
                onSettings = { navController.navigate("settings") }
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
        composable("add") {
            AddNoteScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            "edit/{date}/{index}",
            arguments = listOf(
                navArgument("date") { type = NavType.StringType },
                navArgument("index") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val index = backStackEntry.arguments?.getInt("index") ?: -1
            EditNoteScreen(
                date = date,
                index = index,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}