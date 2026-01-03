package com.ikorihn.obsiditter.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ikorihn.obsiditter.data.Prefs
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
    }
}