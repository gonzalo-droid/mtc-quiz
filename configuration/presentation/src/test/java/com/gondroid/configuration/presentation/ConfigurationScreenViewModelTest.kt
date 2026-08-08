package com.gondroid.configuration.presentation

import com.gondroid.core.domain.repository.AuthRepository
import com.gondroid.core.domain.repository.PreferenceRepository
import com.gondroid.core.domain.repository.PremiumRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigurationScreenViewModelTest {

    private val darkModeFlow = MutableStateFlow(false)
    private val themeModeFlow = MutableStateFlow("system")
    private val isPremiumFlow = MutableStateFlow(false)

    private lateinit var authRepository: AuthRepository
    private lateinit var preferenceRepository: PreferenceRepository
    private lateinit var premiumRepository: PremiumRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        authRepository = mockk(relaxed = true)
        preferenceRepository = mockk(relaxed = true) {
            every { darkModeFlow } returns this@ConfigurationScreenViewModelTest.darkModeFlow
            every { themeModeFlow } returns this@ConfigurationScreenViewModelTest.themeModeFlow
        }
        premiumRepository = mockk(relaxed = true) {
            every { isPremiumFlow } returns this@ConfigurationScreenViewModelTest.isPremiumFlow
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ConfigurationScreenViewModel(
        repository = authRepository,
        preferenceRepository = preferenceRepository,
        premiumRepository = premiumRepository
    )

    @Test
    fun `state seeds isDarkMode and themeMode from PreferenceRepository on init`() = runTest {
        darkModeFlow.value = true
        themeModeFlow.value = "dark"

        val vm = createViewModel()

        assertThat(vm.state.value.isDarkMode).isTrue()
        assertThat(vm.state.value.themeMode).isEqualTo("dark")
    }

    @Test
    fun `state reflects isPremium from PremiumRepository`() = runTest {
        isPremiumFlow.value = true

        val vm = createViewModel()

        assertThat(vm.state.value.isPremium).isTrue()
    }

    @Test
    fun `state reflects isPremium changes from PremiumRepository flow emissions`() = runTest {
        val vm = createViewModel()
        assertThat(vm.state.value.isPremium).isFalse()

        isPremiumFlow.value = true

        assertThat(vm.state.value.isPremium).isTrue()
    }

    @Test
    fun `ToggleDarkMode action delegates to PreferenceRepository`() = runTest {
        val vm = createViewModel()

        vm.onAction(ConfigurationAction.ToggleDarkMode(true))

        coVerify { preferenceRepository.setDarkMode(true) }
    }

    @Test
    fun `SetThemeMode action delegates to PreferenceRepository`() = runTest {
        val vm = createViewModel()

        vm.onAction(ConfigurationAction.SetThemeMode("light"))

        coVerify { preferenceRepository.setThemeMode("light") }
    }

    @Test
    fun `logout calls AuthRepository and emits Success event`() = runTest {
        coEvery { authRepository.logout() } returns true
        val vm = createViewModel()

        val eventDeferred = async { vm.event.first() }
        vm.logout()

        assertThat(eventDeferred.await()).isEqualTo(ConfigurationEvent.Success)
        coVerify { authRepository.logout() }
    }
}
