package com.gondroid.mtcquiz.presentation.screens.questionreview

import androidx.lifecycle.SavedStateHandle
import com.gondroid.core.domain.model.SubscriptionPlan
import com.gondroid.core.domain.repository.PremiumRepository
import com.gondroid.mtcquiz.presentation.screens.FeatureFlagsRepositoryFake
import com.gondroid.mtcquiz.presentation.screens.QuizRepositoryFake
import com.gondroid.mtcquiz.util.MainDispatcherRule
import com.gondroid.questionreview.presentation.QuestionsScreenViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuestionsScreenViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val premium = MutableStateFlow(false)

    private val premiumRepository = object : PremiumRepository {
        override val isPremiumFlow: StateFlow<Boolean> = premium
        override val availablePlansFlow: Flow<List<SubscriptionPlan>> = flowOf(emptyList())
        override suspend fun loadAvailablePlans() = Unit
        override suspend fun refreshPurchaseState() = Unit
        override suspend fun restorePurchases() = Unit
    }

    private fun createViewModel(flags: FeatureFlagsRepositoryFake = FeatureFlagsRepositoryFake()) =
        QuestionsScreenViewModel(
            savedStateHandle = SavedStateHandle(mapOf("categoryId" to "1")),
            repository = QuizRepositoryFake(),
            premiumRepository = premiumRepository,
            featureFlagsRepository = flags,
            bannerAdId = "test-banner-id"
        )

    @Test
    fun `free users get the remote banner interval`() = runTest {
        val vm = createViewModel(FeatureFlagsRepositoryFake(studyBannerInterval = 10))
        advanceUntilIdle()

        assertThat(vm.state.value.effectiveBannerInterval).isEqualTo(10)
    }

    @Test
    fun `premium users never get study banners`() = runTest {
        premium.value = true
        val vm = createViewModel(FeatureFlagsRepositoryFake(studyBannerInterval = 10))
        advanceUntilIdle()

        assertThat(vm.state.value.effectiveBannerInterval).isEqualTo(0)
    }

    @Test
    fun `a remote change of the interval reaches the screen`() = runTest {
        val flags = FeatureFlagsRepositoryFake(studyBannerInterval = 10)
        val vm = createViewModel(flags)
        advanceUntilIdle()

        flags.bannerInterval.value = 0
        advanceUntilIdle()

        assertThat(vm.state.value.effectiveBannerInterval).isEqualTo(0)
    }
}
