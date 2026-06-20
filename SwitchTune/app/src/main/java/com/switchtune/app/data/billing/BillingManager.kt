package com.switchtune.app.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Google Play Billing Library 9 to sell SwitchTune as a single,
 * non-consumable one-time purchase ($1.95). The app content stays locked until
 * [BillingState.entitlement] is [Entitlement.PURCHASED].
 *
 * No subscriptions, no consumables, no free trial — per product rules.
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    enum class Entitlement { UNKNOWN, NOT_PURCHASED, PENDING, PURCHASED }
    enum class TransientError { CONNECTION, PURCHASE_FLOW, UNAVAILABLE }

    data class BillingState(
        val entitlement: Entitlement = Entitlement.UNKNOWN,
        val formattedPrice: String? = null,
        val error: TransientError? = null,
    )

    private val scope = CoroutineScope(SupervisorJob())
    private val _state = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = _state.asStateFlow()

    private var productDetails: ProductDetails? = null

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach { handlePurchase(it) }
            BillingClient.BillingResponseCode.USER_CANCELED -> { /* no-op: user backed out */ }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refreshPurchases()
            else -> _state.update { it.copy(error = TransientError.PURCHASE_FLOW) }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .enableAutoServiceReconnection()
        .build()

    /** Establishes the connection and loads product + entitlement state. Safe to call repeatedly. */
    fun start() {
        if (billingClient.isReady) {
            queryProductDetails()
            refreshPurchases()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    refreshPurchases()
                } else {
                    _state.update { it.copy(error = TransientError.CONNECTION) }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Auto-reconnection is enabled; nothing required here.
            }
        })
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(UNLOCK_PRODUCT_ID)
                        .setProductType(ProductType.INAPP)
                        .build(),
                ),
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = queryResult.productDetailsList.firstOrNull()
                productDetails = details
                val price = details?.oneTimePurchaseOfferDetailsList
                    ?.firstOrNull()?.formattedPrice
                    ?: details?.oneTimePurchaseOfferDetails?.formattedPrice
                _state.update { it.copy(formattedPrice = price, error = null) }
            } else {
                _state.update { it.copy(error = TransientError.UNAVAILABLE) }
            }
        }
    }

    /** Restores / re-checks the user's purchases (call on launch and on resume). */
    fun refreshPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            val relevant = purchases.filter { UNLOCK_PRODUCT_ID in it.products }
            if (relevant.isEmpty()) {
                _state.update { it.copy(entitlement = Entitlement.NOT_PURCHASED) }
                return@queryPurchasesAsync
            }
            relevant.forEach { handlePurchase(it) }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (!purchase.isAcknowledged) acknowledge(purchase)
                _state.update { it.copy(entitlement = Entitlement.PURCHASED, error = null) }
            }

            Purchase.PurchaseState.PENDING ->
                _state.update { it.copy(entitlement = Entitlement.PENDING) }

            else ->
                _state.update { it.copy(entitlement = Entitlement.NOT_PURCHASED) }
        }
    }

    private fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { /* result handled idempotently on next refresh */ }
    }

    /** Launches the Google Play purchase UI for the one-time unlock. */
    fun launchPurchase(activity: Activity) {
        val details = productDetails
        if (details == null) {
            _state.update { it.copy(error = TransientError.UNAVAILABLE) }
            queryProductDetails()
            return
        }
        val offerToken = details.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply { if (!offerToken.isNullOrBlank()) setOfferToken(offerToken) }
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.update { it.copy(error = TransientError.PURCHASE_FLOW) }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    /**
     * DEBUG-ONLY shortcut to unlock the app without a real Google Play purchase,
     * so the main flow can be tested locally before the Play Console product
     * exists. Guarded by BuildConfig.DEBUG at the call site; never reachable in
     * a release build.
     */
    fun debugUnlock() {
        _state.update { it.copy(entitlement = Entitlement.PURCHASED, error = null) }
    }

    companion object {
        /** Must match the one-time product ID created in Google Play Console. */
        const val UNLOCK_PRODUCT_ID = "switchtune_unlock"
    }
}
