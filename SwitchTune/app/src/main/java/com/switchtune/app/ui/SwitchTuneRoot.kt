package com.switchtune.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.switchtune.app.ui.common.FullScreenLoading
import com.switchtune.app.ui.onboarding.OnboardingScreen
import com.switchtune.app.ui.paywall.PaywallScreen
import com.switchtune.app.ui.result.ResultScreen
import com.switchtune.app.ui.settings.SettingsScreen

private object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
}

@Composable
fun SwitchTuneRoot(viewModel: RootViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Start billing connection, and re-check purchases whenever we resume
    // (covers pending purchases completing while the app was backgrounded).
    LifecycleEventEffect(Lifecycle.Event.ON_CREATE) { viewModel.start() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPurchases() }

    when (state.gate) {
        StartGate.LOADING -> FullScreenLoading()

        StartGate.PAYWALL -> PaywallScreen(
            state = state.billing,
            onBuy = { activity -> viewModel.purchase(activity) },
            onRestore = { viewModel.refreshPurchases() },
            onClearError = { viewModel.clearBillingError() },
            onDebugUnlock = { viewModel.debugUnlock() },
        )

        StartGate.ONBOARDING -> OnboardingScreen(
            // When onboarding finishes, prefs flip and the gate moves to MAIN.
            onComplete = { },
        )

        StartGate.MAIN -> {
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
    }
}
