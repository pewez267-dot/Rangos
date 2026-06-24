package com.switchtune.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.switchtune.app.ui.result.ResultScreen
import com.switchtune.app.ui.settings.SettingsScreen

private object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
}

/**
 * App shell. The app opens directly into the main screen — no paywall and no
 * onboarding step. Monetisation is handled by Google Play as a paid app.
 */
@Composable
fun SwitchTuneRoot() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            ResultScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
