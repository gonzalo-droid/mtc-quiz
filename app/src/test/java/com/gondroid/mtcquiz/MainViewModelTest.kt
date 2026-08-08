package com.gondroid.mtcquiz

import app.cash.turbine.test
import com.gondroid.core.domain.repository.PremiumRepository
import com.gondroid.mtcquiz.presentation.screens.PreferenceRepositoryFake
import com.gondroid.mtcquiz.util.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var preferenceRepository: PreferenceRepositoryFake
    private lateinit var premiumRepository: PremiumRepository
    private val isPremiumFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        preferenceRepository = PreferenceRepositoryFake()
        premiumRepository = mockk(relaxed = true) {
            every { isPremiumFlow } returns this@MainViewModelTest.isPremiumFlow
        }
    }

    private fun createViewModel() = MainViewModel(
        preferenceRepository = preferenceRepository,
        premiumRepository = premiumRepository,
    )

    @Test
    fun `init combines isLoggedIn and isOnboardingShown into state`() = runTest {
        preferenceRepository.setIsLoggedIn(true)
        preferenceRepository.setIsOnboardingShown(true)

        val vm = createViewModel()
        advanceUntilIdle()

        assertThat(vm.state.value.isLoggedIn).isTrue()
        assertThat(vm.state.value.isOnboardingShown).isTrue()
        assertThat(vm.state.value.isLoading).isFalse()
    }

    @Test
    fun `state updates when PreferenceRepository emits new isLoggedIn value`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        assertThat(vm.state.value.isLoggedIn).isFalse()

        preferenceRepository.setIsLoggedIn(true)
        advanceUntilIdle()

        assertThat(vm.state.value.isLoggedIn).isTrue()
    }

    @Test
    fun `isDarkMode exposes PreferenceRepository darkModeFlow`() = runTest {
        preferenceRepository.setDarkMode(true)

        val vm = createViewModel()

        vm.isDarkMode.test {
            assertThat(awaitItem()).isFalse() // seeded default before WhileSubscribed catches up
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `themeMode exposes PreferenceRepository themeModeFlow`() = runTest {
        preferenceRepository.setThemeMode("dark")

        val vm = createViewModel()

        vm.themeMode.test {
            assertThat(awaitItem()).isEqualTo("system") // seeded default before WhileSubscribed catches up
            assertThat(awaitItem()).isEqualTo("dark")
        }
    }

    @Test
    fun `isPremium exposes PremiumRepository isPremiumFlow`() = runTest {
        isPremiumFlow.value = true

        val vm = createViewModel()

        vm.isPremium.test {
            assertThat(awaitItem()).isFalse() // seeded default before WhileSubscribed catches up
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `isOnboardingShown exposes PreferenceRepository isOnboardingShownFlow`() = runTest {
        preferenceRepository.setIsOnboardingShown(true)

        val vm = createViewModel()
        advanceUntilIdle()

        assertThat(vm.isOnboardingShown.value).isTrue()
    }

    @Test
    fun `onOnboardingComplete marks onboarding as shown`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onOnboardingComplete()
        advanceUntilIdle()

        assertThat(vm.isOnboardingShown.value).isTrue()
    }

    @Test
    fun `init triggers a purchase state refresh so isPremium is up to date on app start`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coVerify { premiumRepository.refreshPurchaseState() }
    }
}
