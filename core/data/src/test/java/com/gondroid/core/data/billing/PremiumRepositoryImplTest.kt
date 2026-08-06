package com.gondroid.core.data.billing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.android.billingclient.api.ProductDetails
import com.gondroid.core.data.analytics.AnalyticsManager
import com.gondroid.core.domain.model.BillingPeriod
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
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
 * Robolectric is required here (not plain JUnit, unlike the rest of core:data's tests) because
 * constructing [PremiumRepositoryImpl] builds a real Play Billing `BillingClient`
 * (`BillingClient.newBuilder(context)...build()`), which touches Android framework classes
 * unavailable on a plain JVM classpath.
 */
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

    private lateinit var context: Context
    private lateinit var dataStore: FakeDataStore
    private lateinit var analyticsManager: AnalyticsManager
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

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        dataStore = FakeDataStore()
        analyticsManager = mockk(relaxed = true)
        repository = PremiumRepositoryImpl(context, dataStore, analyticsManager, testScope)
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
        val seededRepository = PremiumRepositoryImpl(context, dataStore, analyticsManager, testScope)
        advanceUntilIdle()

        assertThat((seededRepository.isPremiumFlow as kotlinx.coroutines.flow.StateFlow<Boolean>).value).isTrue()
    }

    @Test
    fun `updateIsPremium writes through to DataStore`() = testScope.runTest {
        repository.updateIsPremium(true)
        advanceUntilIdle()

        val cached = dataStore.data.first()[PremiumRepositoryImpl.IS_PREMIUM_CACHED_KEY] ?: false
        assertThat(cached).isTrue()
        assertThat((repository.isPremiumFlow as kotlinx.coroutines.flow.StateFlow<Boolean>).value).isTrue()
    }
}
