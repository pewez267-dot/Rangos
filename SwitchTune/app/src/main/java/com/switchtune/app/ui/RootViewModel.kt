package com.switchtune.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.switchtune.app.data.billing.BillingManager
import com.switchtune.app.data.prefs.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class StartGate { LOADING, PAYWALL, ONBOARDING, MAIN }

data class RootUiState(
    val gate: StartGate = StartGate.LOADING,
    val billing: BillingManager.BillingState = BillingManager.BillingState(),
)

@HiltViewModel
class RootViewModel @Inject constructor(
    private val billingManager: BillingManager,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<RootUiState> =
        combine(billingManager.state, userPreferencesRepository.preferences) { billing, prefs ->
            val gate = when (billing.entitlement) {
                BillingManager.Entitlement.UNKNOWN -> StartGate.LOADING
                BillingManager.Entitlement.PURCHASED ->
                    if (prefs.onboardingComplete) StartGate.MAIN else StartGate.ONBOARDING
                // PENDING and NOT_PURCHASED both keep the content locked behind the paywall.
                else -> StartGate.PAYWALL
            }
            RootUiState(gate = gate, billing = billing)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RootUiState(),
        )

    fun start() = billingManager.start()

    fun refreshPurchases() = billingManager.refreshPurchases()

    fun purchase(activity: android.app.Activity) = billingManager.launchPurchase(activity)

    fun clearBillingError() = billingManager.clearError()

    /** DEBUG-only: unlock without a real purchase (used by the paywall debug button). */
    fun debugUnlock() = billingManager.debugUnlock()
}
