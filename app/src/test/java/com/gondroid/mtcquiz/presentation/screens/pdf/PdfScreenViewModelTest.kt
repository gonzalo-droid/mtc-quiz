package com.gondroid.mtcquiz.presentation.screens.pdf

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.gondroid.core.data.ads.AdsManager
import com.gondroid.core.data.ads.RewardedOutcome
import com.gondroid.mtcquiz.presentation.screens.FeatureFlagsRepositoryFake
import com.gondroid.mtcquiz.presentation.screens.QuizRepositoryFake
import com.gondroid.mtcquiz.util.MainDispatcherRule
import com.gondroid.pdf.presentation.PdfEvent
import com.gondroid.pdf.presentation.PdfScreenViewModel
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PdfScreenViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val adsManager: AdsManager = mockk(relaxed = true)

    /**
     * PdfScreenViewModel uses savedStateHandle.toRoute<PdfScreenRoute>() which reads
     * the "categoryId" key directly from the SavedStateHandle using kotlinx.serialization.
     * QuizRepositoryFake has categories with ids "1" and "2".
     */
    private fun createViewModel(
        featureFlagsRepository: FeatureFlagsRepositoryFake = FeatureFlagsRepositoryFake()
    ): PdfScreenViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("categoryId" to "1"))
        return PdfScreenViewModel(
            savedStateHandle = savedStateHandle,
            repository = QuizRepositoryFake(),
            adsManager = adsManager,
            featureFlagsRepository = featureFlagsRepository
        )
    }

    @Test
    fun `onDownloadClicked downloads directly when no rewarded ad is ready`() = runTest {
        every { adsManager.isPdfRewardedReady() } returns false
        val vm = createViewModel()
        vm.events.test {
            vm.onDownloadClicked()
            assertThat(awaitItem()).isEqualTo(PdfEvent.StartDownload)
        }
        assertThat(vm.state.value.showRewardDialog).isFalse()
    }

    @Test
    fun `onDownloadClicked asks before the rewarded ad when one is ready`() = runTest {
        every { adsManager.isPdfRewardedReady() } returns true
        val vm = createViewModel()
        vm.events.test {
            vm.onDownloadClicked()
            advanceUntilIdle()
            assertThat(vm.state.value.showRewardDialog).isTrue()
            expectNoEvents()
        }
    }

    @Test
    fun `watching the ad closes the dialog and shows the rewarded ad`() = runTest {
        every { adsManager.isPdfRewardedReady() } returns true
        val vm = createViewModel()
        vm.onDownloadClicked()
        advanceUntilIdle()
        vm.events.test {
            vm.onWatchRewardedClicked()
            assertThat(awaitItem()).isEqualTo(PdfEvent.ShowRewarded)
        }
        assertThat(vm.state.value.showRewardDialog).isFalse()
    }

    @Test
    fun `cancelling the dialog downloads nothing`() = runTest {
        every { adsManager.isPdfRewardedReady() } returns true
        val vm = createViewModel()
        vm.events.test {
            vm.onDownloadClicked()
            advanceUntilIdle()
            vm.onRewardDialogDismissed()
            assertThat(vm.state.value.showRewardDialog).isFalse()
            expectNoEvents()
        }
    }

    @Test
    fun `earning the reward starts the download`() = runTest {
        val vm = createViewModel()
        vm.events.test {
            vm.onRewardedResult(RewardedOutcome.EARNED)
            assertThat(awaitItem()).isEqualTo(PdfEvent.StartDownload)
        }
    }

    @Test
    fun `an ad that fails to show still lets the user download`() = runTest {
        val vm = createViewModel()
        vm.events.test {
            vm.onRewardedResult(RewardedOutcome.UNAVAILABLE)
            assertThat(awaitItem()).isEqualTo(PdfEvent.StartDownload)
        }
    }

    @Test
    fun `closing the ad early does not download`() = runTest {
        val vm = createViewModel()
        vm.events.test {
            vm.onRewardedResult(RewardedOutcome.SKIPPED)
            assertThat(awaitItem()).isEqualTo(PdfEvent.RewardSkipped)
        }
    }

    @Test
    fun `state exposes the premium offer flag so the dialog can hide Premium`() = runTest {
        val flags = FeatureFlagsRepositoryFake(isPremiumOfferEnabled = true)
        val vm = createViewModel(featureFlagsRepository = flags)
        advanceUntilIdle()
        assertThat(vm.state.value.isPremiumOfferEnabled).isTrue()

        flags.premiumOfferEnabled.value = false
        advanceUntilIdle()

        assertThat(vm.state.value.isPremiumOfferEnabled).isFalse()
    }
}
