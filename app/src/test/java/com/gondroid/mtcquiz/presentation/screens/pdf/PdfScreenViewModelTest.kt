package com.gondroid.mtcquiz.presentation.screens.pdf

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.gondroid.core.data.ads.AdsManager
import com.gondroid.mtcquiz.presentation.screens.QuizRepositoryFake
import com.gondroid.mtcquiz.util.MainDispatcherRule
import com.gondroid.pdf.presentation.PdfEvent
import com.gondroid.pdf.presentation.PdfScreenViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
    private fun createViewModel(): PdfScreenViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("categoryId" to "1"))
        return PdfScreenViewModel(
            savedStateHandle = savedStateHandle,
            repository = QuizRepositoryFake(),
            adsManager = adsManager,
        )
    }

    @Test
    fun `onDownloadClicked records download and emits StartDownload when cap not met`() = runTest {
        coEvery { adsManager.shouldShowPdfInterstitial() } returns false
        val vm = createViewModel()
        vm.events.test {
            vm.onDownloadClicked()
            assertThat(awaitItem()).isEqualTo(PdfEvent.StartDownload)
        }
        coVerify { adsManager.recordPdfDownload() }
    }

    @Test
    fun `onDownloadClicked emits ShowInterstitial when cap is met`() = runTest {
        coEvery { adsManager.shouldShowPdfInterstitial() } returns true
        val vm = createViewModel()
        vm.events.test {
            vm.onDownloadClicked()
            assertThat(awaitItem()).isEqualTo(PdfEvent.ShowInterstitial)
        }
        coVerify { adsManager.recordPdfDownload() }
    }

    @Test
    fun `onInterstitialClosed emits StartDownload`() = runTest {
        val vm = createViewModel()
        vm.events.test {
            vm.onInterstitialClosed()
            assertThat(awaitItem()).isEqualTo(PdfEvent.StartDownload)
        }
    }
}
