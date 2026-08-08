package com.gondroid.core.data.billing

import android.app.Activity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import com.gondroid.core.data.analytics.AnalyticsManager
import com.gondroid.core.domain.model.BillingPeriod
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * [BillingClient] itself is mocked (via the injected [BillingClientFactory]), so Robolectric is
 * no longer needed to construct [PremiumRepositoryImpl] itself. It's still required, though,
 * because the tests build *real* Play Billing param objects (e.g. [BillingFlowParams] in the
 * `launchSubscription` tests) to exercise the actual flow, and those builders touch Android
 * framework internals (`android.text.TextUtils`) that throw "not mocked" under a plain JVM unit
 * test without Robolectric's shadow implementations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PremiumRepositoryImplTest {

    /** Fake in-memory DataStore<Preferences>, same style as the rest of core:data's tests avoid a real DataStore. */
    private class FakeDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(mutablePreferencesOf())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private lateinit var dataStore: FakeDataStore
    private lateinit var analyticsManager: AnalyticsManager
    private lateinit var billingClient: BillingClient
    private lateinit var capturedListener: PurchasesUpdatedListener
    private lateinit var repository: PremiumRepositoryImpl

    /**
     * [PremiumRepositoryImpl] takes its background [kotlinx.coroutines.CoroutineScope] as a
     * constructor param (see the `@ApplicationScope` binding in `CoroutineScopeModule`) instead
     * of building one internally, precisely so a test can hand it this [TestScope] — its
     * fire-and-forget `scope.launch { ... }` calls (DataStore writes, cache-seeding in `init`)
     * become children of this scope, and `testScope.runTest { advanceUntilIdle() }` can see and
     * wait for them. A real `Dispatchers.IO`-backed scope built inside the class would be
     * invisible to `advanceUntilIdle()` and make these two tests flaky/failing.
     */
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun buildRepository(): PremiumRepositoryImpl {
        val factory = BillingClientFactory { listener ->
            capturedListener = listener
            billingClient
        }
        return PremiumRepositoryImpl(dataStore, analyticsManager, testScope, factory)
    }

    @Before
    fun setUp() {
        dataStore = FakeDataStore()
        analyticsManager = mockk(relaxed = true)
        billingClient = mockk(relaxed = true) {
            every { isReady } returns true
        }
        repository = buildRepository()
    }

    @Test
    fun `billingPeriodFor maps known product ids`() {
        assertThat(repository.billingPeriodFor(PremiumRepositoryImpl.PRODUCT_ID_MONTHLY))
            .isEqualTo(BillingPeriod.MONTHLY)
        assertThat(repository.billingPeriodFor(PremiumRepositoryImpl.PRODUCT_ID_ANNUAL))
            .isEqualTo(BillingPeriod.ANNUAL)
        assertThat(repository.billingPeriodFor("unknown_product")).isNull()
    }

    @Test
    fun `mapToSubscriptionPlan builds domain plan from ProductDetails pricing phase`() {
        val pricingPhase = mockk<ProductDetails.PricingPhase> {
            every { formattedPrice } returns "S/ 9.90"
        }
        val pricingPhases = mockk<ProductDetails.PricingPhases> {
            every { pricingPhaseList } returns listOf(pricingPhase)
        }
        val offerDetails = mockk<ProductDetails.SubscriptionOfferDetails> {
            every { this@mockk.pricingPhases } returns pricingPhases
        }
        val details = mockk<ProductDetails> {
            every { productId } returns PremiumRepositoryImpl.PRODUCT_ID_MONTHLY
            every { subscriptionOfferDetails } returns listOf(offerDetails)
        }

        val plan = repository.mapToSubscriptionPlan(details)

        assertThat(plan).isNotNull()
        assertThat(plan!!.productId).isEqualTo(PremiumRepositoryImpl.PRODUCT_ID_MONTHLY)
        assertThat(plan.billingPeriod).isEqualTo(BillingPeriod.MONTHLY)
        assertThat(plan.formattedPrice).isEqualTo("S/ 9.90")
    }

    @Test
    fun `mapToSubscriptionPlan returns null when there is no offer`() {
        val details = mockk<ProductDetails> {
            every { productId } returns PremiumRepositoryImpl.PRODUCT_ID_ANNUAL
            every { subscriptionOfferDetails } returns null
        }

        assertThat(repository.mapToSubscriptionPlan(details)).isNull()
    }

    @Test
    fun `isPremiumFlow seeds from cached DataStore value on init`() = testScope.runTest {
        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply { this[PremiumRepositoryImpl.IS_PREMIUM_CACHED_KEY] = true }
        }
        val seededRepository = buildRepository()
        advanceUntilIdle()

        assertThat(seededRepository.isPremiumFlow.value).isTrue()
    }

    @Test
    fun `updateIsPremium writes through to DataStore`() = testScope.runTest {
        repository.updateIsPremium(true)
        advanceUntilIdle()

        val cached = dataStore.data.first()[PremiumRepositoryImpl.IS_PREMIUM_CACHED_KEY] ?: false
        assertThat(cached).isTrue()
        assertThat(repository.isPremiumFlow.value).isTrue()
    }

    // --- loadAvailablePlans ---

    private fun productDetailsFor(productId: String, price: String): ProductDetails {
        val pricingPhase = mockk<ProductDetails.PricingPhase> {
            every { formattedPrice } returns price
        }
        val pricingPhases = mockk<ProductDetails.PricingPhases> {
            every { pricingPhaseList } returns listOf(pricingPhase)
        }
        val offerDetails = mockk<ProductDetails.SubscriptionOfferDetails> {
            every { this@mockk.pricingPhases } returns pricingPhases
            every { offerToken } returns "offer-token-$productId"
        }
        return mockk(relaxed = true) {
            every { this@mockk.productId } returns productId
            every { subscriptionOfferDetails } returns listOf(offerDetails)
        }
    }

    private fun stubQueryProductDetails(responseCode: Int, details: List<ProductDetails> = emptyList()) {
        val listenerSlot = slot<ProductDetailsResponseListener>()
        every { billingClient.queryProductDetailsAsync(any(), capture(listenerSlot)) } answers {
            val result = BillingResult.newBuilder().setResponseCode(responseCode).build()
            listenerSlot.captured.onProductDetailsResponse(
                result,
                QueryProductDetailsResult.create(details, emptyList()),
            )
        }
    }

    @Test
    fun `loadAvailablePlans populates availablePlansFlow on success`() = testScope.runTest {
        val monthly = productDetailsFor(PremiumRepositoryImpl.PRODUCT_ID_MONTHLY, "S/ 9.90")
        val annual = productDetailsFor(PremiumRepositoryImpl.PRODUCT_ID_ANNUAL, "S/ 29.90")
        stubQueryProductDetails(BillingClient.BillingResponseCode.OK, listOf(monthly, annual))

        repository.loadAvailablePlans()
        advanceUntilIdle()

        assertThat(repository.availablePlansFlow.first().map { it.productId }).containsExactly(
            PremiumRepositoryImpl.PRODUCT_ID_MONTHLY,
            PremiumRepositoryImpl.PRODUCT_ID_ANNUAL,
        )
    }

    @Test
    fun `loadAvailablePlans leaves availablePlansFlow empty when query fails`() = testScope.runTest {
        stubQueryProductDetails(BillingClient.BillingResponseCode.ERROR)

        repository.loadAvailablePlans()
        advanceUntilIdle()

        assertThat(repository.availablePlansFlow.first()).isEmpty()
    }

    @Test
    fun `loadAvailablePlans does nothing when billing client fails to connect`() = testScope.runTest {
        every { billingClient.isReady } returns false
        every { billingClient.startConnection(any()) } answers {
            val listener = firstArg<BillingClientStateListener>()
            val result = BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
                .build()
            listener.onBillingSetupFinished(result)
        }

        repository.loadAvailablePlans()
        advanceUntilIdle()

        verify(exactly = 0) { billingClient.queryProductDetailsAsync(any(), any()) }
        assertThat(repository.availablePlansFlow.first()).isEmpty()
    }

    // --- launchSubscription ---

    @Test
    fun `launchSubscription launches billing flow with cached product and returns true`() = testScope.runTest {
        val monthly = productDetailsFor(PremiumRepositoryImpl.PRODUCT_ID_MONTHLY, "S/ 9.90")
        stubQueryProductDetails(BillingClient.BillingResponseCode.OK, listOf(monthly))
        every {
            billingClient.launchBillingFlow(any(), any())
        } returns BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
        repository.loadAvailablePlans()
        advanceUntilIdle()
        val activity = mockk<Activity>()

        val launched = repository.launchSubscription(activity, PremiumRepositoryImpl.PRODUCT_ID_MONTHLY)
        advanceUntilIdle()

        assertThat(launched).isTrue()
        val paramsSlot = slot<BillingFlowParams>()
        verify { billingClient.launchBillingFlow(activity, capture(paramsSlot)) }
        verify { analyticsManager.logSubscribeClicked(PremiumRepositoryImpl.PRODUCT_ID_MONTHLY) }
    }

    @Test
    fun `launchSubscription loads plans first when cache is empty`() = testScope.runTest {
        val annual = productDetailsFor(PremiumRepositoryImpl.PRODUCT_ID_ANNUAL, "S/ 29.90")
        stubQueryProductDetails(BillingClient.BillingResponseCode.OK, listOf(annual))
        every {
            billingClient.launchBillingFlow(any(), any())
        } returns BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
        val activity = mockk<Activity>()

        val launched = repository.launchSubscription(activity, PremiumRepositoryImpl.PRODUCT_ID_ANNUAL)
        advanceUntilIdle()

        assertThat(launched).isTrue()
        verify { billingClient.queryProductDetailsAsync(any(), any()) }
    }

    @Test
    fun `launchSubscription returns false when product is not found in Play Console`() = testScope.runTest {
        stubQueryProductDetails(BillingClient.BillingResponseCode.OK, emptyList())
        val activity = mockk<Activity>()

        val launched = repository.launchSubscription(activity, "unknown_product")
        advanceUntilIdle()

        assertThat(launched).isFalse()
        verify(exactly = 0) { billingClient.launchBillingFlow(any(), any()) }
    }

    @Test
    fun `launchSubscription returns false when billing client fails to connect`() = testScope.runTest {
        every { billingClient.isReady } returns false
        every { billingClient.startConnection(any()) } answers {
            val listener = firstArg<BillingClientStateListener>()
            val result = BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
                .build()
            listener.onBillingSetupFinished(result)
        }
        val activity = mockk<Activity>()

        val launched = repository.launchSubscription(activity, PremiumRepositoryImpl.PRODUCT_ID_MONTHLY)
        advanceUntilIdle()

        assertThat(launched).isFalse()
    }

    // --- purchasesUpdatedListener / handlePurchase ---

    private fun purchaseMock(
        state: Int = Purchase.PurchaseState.PURCHASED,
        acknowledged: Boolean = false,
        products: List<String> = listOf(PremiumRepositoryImpl.PRODUCT_ID_MONTHLY),
        token: String = "purchase-token",
    ): Purchase = mockk {
        every { purchaseState } returns state
        every { isAcknowledged } returns acknowledged
        every { this@mockk.products } returns products
        every { purchaseToken } returns token
    }

    @Test
    fun `purchasesUpdatedListener OK marks premium true and acknowledges unacknowledged purchase`() = testScope.runTest {
        val listenerSlot = slot<AcknowledgePurchaseResponseListener>()
        every { billingClient.acknowledgePurchase(any(), capture(listenerSlot)) } answers {
            listenerSlot.captured.onAcknowledgePurchaseResponse(
                BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build(),
            )
        }
        val purchase = purchaseMock(acknowledged = false)
        val okResult = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()

        capturedListener.onPurchasesUpdated(okResult, listOf(purchase))
        advanceUntilIdle()

        assertThat(repository.isPremiumFlow.value).isTrue()
        verify { analyticsManager.logPurchaseCompleted(PremiumRepositoryImpl.PRODUCT_ID_MONTHLY) }
        val paramsSlot = slot<AcknowledgePurchaseParams>()
        verify { billingClient.acknowledgePurchase(capture(paramsSlot), any()) }
    }

    @Test
    fun `purchasesUpdatedListener does not re-acknowledge an already-acknowledged purchase`() = testScope.runTest {
        val purchase = purchaseMock(acknowledged = true)
        val okResult = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()

        capturedListener.onPurchasesUpdated(okResult, listOf(purchase))
        advanceUntilIdle()

        assertThat(repository.isPremiumFlow.value).isTrue()
        verify(exactly = 0) { billingClient.acknowledgePurchase(any(), any()) }
    }

    @Test
    fun `purchasesUpdatedListener USER_CANCELED logs analytics without granting premium`() = testScope.runTest {
        val canceledResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.USER_CANCELED)
            .build()

        capturedListener.onPurchasesUpdated(canceledResult, emptyList())
        advanceUntilIdle()

        assertThat(repository.isPremiumFlow.value).isFalse()
        verify { analyticsManager.logPurchaseCanceled("unknown") }
    }

    @Test
    fun `purchasesUpdatedListener error logs analytics without granting premium`() = testScope.runTest {
        val errorResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.ERROR)
            .build()

        capturedListener.onPurchasesUpdated(errorResult, emptyList())
        advanceUntilIdle()

        assertThat(repository.isPremiumFlow.value).isFalse()
        verify {
            analyticsManager.logPurchaseFailed(
                productId = "unknown",
                errorCode = BillingClient.BillingResponseCode.ERROR,
            )
        }
    }

    // --- refreshPurchaseState / restorePurchases ---

    private fun stubQueryPurchases(responseCode: Int, purchases: List<Purchase> = emptyList()) {
        val listenerSlot = slot<PurchasesResponseListener>()
        every { billingClient.queryPurchasesAsync(any(), capture(listenerSlot)) } answers {
            val result = BillingResult.newBuilder().setResponseCode(responseCode).build()
            listenerSlot.captured.onQueryPurchasesResponse(result, purchases)
        }
    }

    @Test
    fun `refreshPurchaseState sets isPremium true when an active matching subscription exists`() = testScope.runTest {
        val activePurchase = purchaseMock(state = Purchase.PurchaseState.PURCHASED)
        stubQueryPurchases(BillingClient.BillingResponseCode.OK, listOf(activePurchase))

        repository.refreshPurchaseState()
        advanceUntilIdle()

        assertThat(repository.isPremiumFlow.value).isTrue()
        val paramsSlot = slot<QueryPurchasesParams>()
        verify { billingClient.queryPurchasesAsync(capture(paramsSlot), any()) }
    }

    @Test
    fun `refreshPurchaseState sets isPremium false when no active matching subscription exists`() = testScope.runTest {
        stubQueryPurchases(BillingClient.BillingResponseCode.OK, emptyList())

        repository.refreshPurchaseState()
        advanceUntilIdle()

        assertThat(repository.isPremiumFlow.value).isFalse()
    }

    @Test
    fun `refreshPurchaseState leaves isPremium unchanged when query fails`() = testScope.runTest {
        repository.updateIsPremium(true)
        advanceUntilIdle()
        stubQueryPurchases(BillingClient.BillingResponseCode.ERROR)

        repository.refreshPurchaseState()
        advanceUntilIdle()

        assertThat(repository.isPremiumFlow.value).isTrue()
    }

    @Test
    fun `restorePurchases delegates to refreshPurchaseState and logs analytics`() = testScope.runTest {
        val activePurchase = purchaseMock(state = Purchase.PurchaseState.PURCHASED)
        stubQueryPurchases(BillingClient.BillingResponseCode.OK, listOf(activePurchase))

        repository.restorePurchases()
        advanceUntilIdle()

        verify { analyticsManager.logRestoreClicked() }
        verify { analyticsManager.logRestoreCompleted(true) }
        assertThat(repository.isPremiumFlow.value).isTrue()
    }
}
