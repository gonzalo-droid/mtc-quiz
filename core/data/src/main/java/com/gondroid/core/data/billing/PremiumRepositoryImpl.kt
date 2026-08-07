package com.gondroid.core.data.billing

import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.gondroid.core.data.analytics.AnalyticsManager
import com.gondroid.core.data.di.ApplicationScope
import com.gondroid.core.domain.model.BillingPeriod
import com.gondroid.core.domain.model.SubscriptionPlan
import com.gondroid.core.domain.repository.PremiumRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PremiumRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val analyticsManager: AnalyticsManager,
    @ApplicationScope private val scope: CoroutineScope,
) : PremiumRepository, BillingLauncher {

    companion object {
        const val PRODUCT_ID_MONTHLY = "mtcquiz_premium_monthly"
        const val PRODUCT_ID_ANNUAL = "mtcquiz_premium_annual"
        private val PRODUCT_IDS = listOf(PRODUCT_ID_MONTHLY, PRODUCT_ID_ANNUAL)
        val IS_PREMIUM_CACHED_KEY = booleanPreferencesKey("cached_is_premium")
    }

    private val _isPremium = MutableStateFlow(false)
    override val isPremiumFlow: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _availablePlans = MutableStateFlow<List<SubscriptionPlan>>(emptyList())
    override val availablePlansFlow: Flow<List<SubscriptionPlan>> = _availablePlans.asStateFlow()

    /** ProductDetails reales cacheados desde la última consulta a Play, para lanzar la compra sin re-consultar. */
    private val productDetailsCache = mutableMapOf<String, ProductDetails>()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase -> handlePurchase(purchase) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Timber.d("Billing: user canceled subscription flow")
                analyticsManager.logPurchaseCanceled(purchases?.firstOrNull()?.products?.firstOrNull() ?: "unknown")
            }
            else -> {
                Timber.e("Billing: purchase failed with code ${billingResult.responseCode}")
                analyticsManager.logPurchaseFailed(
                    productId = purchases?.firstOrNull()?.products?.firstOrNull() ?: "unknown",
                    errorCode = billingResult.responseCode,
                )
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    init {
        scope.launch {
            _isPremium.value = dataStore.data.map { it[IS_PREMIUM_CACHED_KEY] ?: false }.first()
        }
    }

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        return suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        if (cont.isActive) cont.resume(true)
                    } else {
                        Timber.e("Billing: connection failed with code ${result.responseCode}")
                        if (cont.isActive) cont.resume(false)
                    }
                }
                override fun onBillingServiceDisconnected() {
                    Timber.w("Billing: service disconnected")
                }
            })
        }
    }

    override suspend fun loadAvailablePlans() {
        if (!ensureConnected()) return

        val productList = PRODUCT_IDS.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        val result: ProductDetailsResult = billingClient.queryProductDetails(params)

        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.e("Billing: failed to query product details: ${result.billingResult.debugMessage}")
            return
        }

        val plans = result.productDetailsList.orEmpty().mapNotNull { details ->
            productDetailsCache[details.productId] = details
            mapToSubscriptionPlan(details)
        }
        _availablePlans.value = plans
    }

    internal fun mapToSubscriptionPlan(details: ProductDetails): SubscriptionPlan? {
        val offer = details.subscriptionOfferDetails?.firstOrNull() ?: return null
        val pricingPhase = offer.pricingPhases.pricingPhaseList.firstOrNull() ?: return null
        val period = billingPeriodFor(details.productId) ?: return null
        return SubscriptionPlan(
            productId = details.productId,
            billingPeriod = period,
            formattedPrice = pricingPhase.formattedPrice,
        )
    }

    internal fun billingPeriodFor(productId: String): BillingPeriod? = when (productId) {
        PRODUCT_ID_MONTHLY -> BillingPeriod.MONTHLY
        PRODUCT_ID_ANNUAL -> BillingPeriod.ANNUAL
        else -> null
    }

    override suspend fun launchSubscription(activity: Activity, productId: String): Boolean {
        if (!ensureConnected()) return false

        var productDetails = productDetailsCache[productId]
        if (productDetails == null) {
            loadAvailablePlans()
            productDetails = productDetailsCache[productId]
        }
        if (productDetails == null) {
            Timber.e("Billing: product '$productId' not found in Play Console")
            return false
        }

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            Timber.e("Billing: no offer found for product '$productId'")
            return false
        }

        analyticsManager.logSubscribeClicked(productId)

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        val flowResult = billingClient.launchBillingFlow(activity, flowParams)
        return flowResult.responseCode == BillingClient.BillingResponseCode.OK
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            updateIsPremium(true)
            analyticsManager.logPurchaseCompleted(purchase.products.firstOrNull() ?: "unknown")

            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(params) { result ->
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        Timber.e("Billing: failed to acknowledge purchase: ${result.debugMessage}")
                    }
                }
            }
        }
    }

    internal fun updateIsPremium(value: Boolean) {
        _isPremium.value = value
        scope.launch {
            dataStore.edit { prefs -> prefs[IS_PREMIUM_CACHED_KEY] = value }
        }
    }

    override suspend fun refreshPurchaseState() {
        if (!ensureConnected()) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = billingClient.queryPurchasesAsync(params)

        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val hasActiveSub = result.purchasesList.any { purchase ->
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.products.any { it in PRODUCT_IDS }
            }
            updateIsPremium(hasActiveSub)
            Timber.d("Billing: refreshed purchase state, isPremium=$hasActiveSub")
        } else {
            Timber.e("Billing: failed to query purchases: ${result.billingResult.debugMessage}")
        }
    }

    override suspend fun restorePurchases() {
        analyticsManager.logRestoreClicked()
        refreshPurchaseState()
        analyticsManager.logRestoreCompleted(_isPremium.value)
    }
}
