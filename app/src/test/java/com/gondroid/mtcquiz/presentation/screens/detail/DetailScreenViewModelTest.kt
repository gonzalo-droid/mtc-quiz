package com.gondroid.mtcquiz.presentation.screens.detail

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.gondroid.core.data.ads.AdsManager
import com.gondroid.core.data.billing.BillingManager
import com.gondroid.detail.presentation.DetailEvent
import com.gondroid.detail.presentation.DetailScreenViewModel
import com.gondroid.mtcquiz.presentation.screens.QuizRepositoryFake
import com.gondroid.mtcquiz.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DetailScreenViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val adsManager: AdsManager = mockk(relaxed = true)

    private fun createViewModel(
        isPremium: Boolean = false,
        categoryId: String = "1",
    ): DetailScreenViewModel {
        val billingManager = object : BillingManager {
            override val isPremiumFlow: Flow<Boolean> = flowOf(isPremium)
            override suspend fun launchSubscription(activity: Activity): Boolean = false
            override suspend fun refreshPurchaseState() = Unit
            override suspend fun restorePurchases() = Unit
        }
        val savedStateHandle = SavedStateHandle(mapOf("categoryId" to categoryId))
        return DetailScreenViewModel(
            savedStateHandle = savedStateHandle,
            repository = QuizRepositoryFake(),
            billingManager = billingManager,
            adsManager = adsManager,
            bannerAdId = "test-banner-id",
        )
    }

    @Test
    fun `state loads the category matching the route's categoryId`() = runTest {
        val vm = createViewModel(categoryId = "1")
        advanceUntilIdle()
        assertThat(vm.state.value.category.id).isEqualTo("1")
        assertThat(vm.state.value.category.title).isEqualTo("CLASE A - CATEGORIA I")
    }

    @Test
    fun `state reflects isPremium from BillingManager`() = runTest {
        val vm = createViewModel(isPremium = true)
        advanceUntilIdle()
        assertThat(vm.state.value.isPremium).isTrue()
    }

    @Test
    fun `state defaults isPremium to false when BillingManager reports not premium`() = runTest {
        val vm = createViewModel(isPremium = false)
        advanceUntilIdle()
        assertThat(vm.state.value.isPremium).isFalse()
    }

    @Test
    fun `onStartEvaluation navigates directly when interstitial should not show`() = runTest {
        coEvery { adsManager.shouldShowEvaluationInterstitial() } returns false
        val vm = createViewModel()
        vm.events.test {
            vm.onStartEvaluation("1")
            assertThat(awaitItem()).isEqualTo(DetailEvent.NavigateToEvaluation("1"))
        }
        coVerify { adsManager.recordEvaluationStart() }
    }

    @Test
    fun `onStartEvaluation shows interstitial event when interstitial should show`() = runTest {
        coEvery { adsManager.shouldShowEvaluationInterstitial() } returns true
        val vm = createViewModel()
        vm.events.test {
            vm.onStartEvaluation("1")
            assertThat(awaitItem()).isEqualTo(DetailEvent.ShowEvaluationInterstitial)
        }
        coVerify { adsManager.recordEvaluationStart() }
    }

    @Test
    fun `onInterstitialClosed navigates to evaluation`() = runTest {
        val vm = createViewModel()
        vm.events.test {
            vm.onInterstitialClosed("1")
            assertThat(awaitItem()).isEqualTo(DetailEvent.NavigateToEvaluation("1"))
        }
    }
}
