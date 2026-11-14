package com.gondroid.configuration.presentation.premium

import android.app.Activity
import app.cash.turbine.test
import com.gondroid.configuration.presentation.FeatureFlagsRepositoryFake
import com.gondroid.core.data.analytics.AnalyticsManager
import com.gondroid.core.data.billing.BillingLauncher
import com.gondroid.core.domain.model.BillingPeriod
import com.gondroid.core.domain.model.SubscriptionPlan
import com.gondroid.core.domain.repository.PremiumRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PremiumViewModelTest {

    private val monthlyPlan =
        SubscriptionPlan("mtcquiz_premium_monthly", BillingPeriod.MONTHLY, "S/ 9.90")
    private val annualPlan =
        SubscriptionPlan("mtcquiz_premium_annual", BillingPeriod.ANNUAL, "S/ 29.90")

    private lateinit var premiumRepository: PremiumRepository
    private lateinit var billingLauncher: BillingLauncher
    private lateinit var analyticsManager: AnalyticsManager

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        premiumRepository = mockk(relaxed = true) {
            every { isPremiumFlow } returns MutableStateFlow(false)
            every { availablePlansFlow } returns MutableStateFlow(listOf(monthlyPlan, annualPlan))
        }
        billingLauncher = mockk(relaxed = true)
        analyticsManager = mockk(relaxed = true)
    }

    private fun createViewModel(
        featureFlagsRepository: FeatureFlagsRepositoryFake = FeatureFlagsRepositoryFake()
    ) = PremiumViewModel(
        premiumRepository,
        billingLauncher,
        analyticsManager,
        featureFlagsRepository
    )

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init logs paywall viewed and loads available plans`() = runTest {
        createViewModel()

        verify { analyticsManager.logPaywallViewed() }
        coVerify { premiumRepository.loadAvailablePlans() }
    }

    @Test
    fun `available plans default selection is the annual plan`() = runTest {
        val vm = createViewModel()

        assertThat(vm.selectedPlan.value).isEqualTo(annualPlan)
    }

    @Test
    fun `selectPlan updates the selected plan`() = runTest {
        val vm = createViewModel()

        vm.selectPlan(monthlyPlan)

        assertThat(vm.selectedPlan.value).isEqualTo(monthlyPlan)
    }

    @Test
    fun `subscribe launches the selected plan's product id`() = runTest {
        val activity = mockk<Activity>()
        coEvery { billingLauncher.launchSubscription(activity, any()) } returns true
        val vm = createViewModel()
        vm.selectPlan(monthlyPlan)

        vm.subscribe(activity)

        coVerify { billingLauncher.launchSubscription(activity, "mtcquiz_premium_monthly") }
    }

    @Test
    fun `restorePurchases sets restored message when isPremiumFlow reflects true`() = runTest {
        every { premiumRepository.isPremiumFlow } returns MutableStateFlow(true)
        val vm = createViewModel()

        vm.restorePurchases()

        assertThat(vm.restoreMessage.value).isEqualTo("Compra restaurada correctamente")
        coVerify { premiumRepository.restorePurchases() }
    }

    @Test
    fun `restorePurchases sets not found message when isPremiumFlow reflects false`() = runTest {
        every { premiumRepository.isPremiumFlow } returns MutableStateFlow(false)
        val vm = createViewModel()

        vm.restorePurchases()

        assertThat(vm.restoreMessage.value).isEqualTo("No se encontró ninguna suscripción activa")
        coVerify { premiumRepository.restorePurchases() }
    }

    @Test
    fun `paywall is available while the premium offer is enabled`() = runTest {
        val vm = createViewModel(FeatureFlagsRepositoryFake(isPremiumOfferEnabled = true))

        assertThat(vm.isPaywallAvailable.value).isTrue()
    }

    @Test
    fun `paywall closes for a non premium user when the offer is turned off remotely`() = runTest {
        val flags = FeatureFlagsRepositoryFake(isPremiumOfferEnabled = true)
        val vm = createViewModel(flags)

        vm.isPaywallAvailable.test {
            assertThat(awaitItem()).isTrue()
            flags.premiumOfferEnabled.value = false
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `paywall stays available for a paying user even with the offer off`() = runTest {
        every { premiumRepository.isPremiumFlow } returns MutableStateFlow(true)

        val vm = createViewModel(FeatureFlagsRepositoryFake(isPremiumOfferEnabled = false))

        assertThat(vm.isPaywallAvailable.value).isTrue()
    }

    @Test
    fun `subscribe does nothing when the offer is off and the user is not premium`() = runTest {
        val activity = mockk<Activity>()
        val vm = createViewModel(FeatureFlagsRepositoryFake(isPremiumOfferEnabled = false))

        vm.subscribe(activity)

        coVerify(exactly = 0) { billingLauncher.launchSubscription(any(), any()) }
        verify(exactly = 0) { analyticsManager.logPaywallViewed() }
    }
}
