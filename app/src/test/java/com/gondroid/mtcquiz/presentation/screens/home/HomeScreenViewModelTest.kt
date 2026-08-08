package com.gondroid.mtcquiz.presentation.screens.home

import app.cash.turbine.test
import com.gondroid.core.domain.model.Category
import com.gondroid.core.domain.model.SubscriptionPlan
import com.gondroid.core.domain.repository.PremiumRepository
import com.gondroid.home.presentation.HomeScreenViewModel
import com.gondroid.mtcquiz.presentation.screens.PreferenceRepositoryFake
import com.gondroid.mtcquiz.presentation.screens.QuizRepositoryFake
import com.gondroid.mtcquiz.util.MainDispatcherRule
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenViewModelTest {

    /**
     * Test Dispatcher in ViewModel
     */
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: HomeScreenViewModel
    private lateinit var repository: QuizRepositoryFake
    private lateinit var preferenceRepository: PreferenceRepositoryFake

    private fun fakePremiumRepository(isPremiumFlow: StateFlow<Boolean> = MutableStateFlow(false)) =
        object : PremiumRepository {
            override val isPremiumFlow: StateFlow<Boolean> = isPremiumFlow
            override val availablePlansFlow: Flow<List<SubscriptionPlan>> = flowOf(emptyList())
            override suspend fun loadAvailablePlans() = Unit
            override suspend fun refreshPurchaseState() = Unit
            override suspend fun restorePurchases() = Unit
        }

    private fun createViewModel(premiumRepository: PremiumRepository = fakePremiumRepository()) =
        HomeScreenViewModel(
            repository = repository,
            preferenceRepository = preferenceRepository,
            premiumRepository = premiumRepository,
            bannerAdId = "test-banner-id"
        )

    @Before
    fun setUp() {
        repository = QuizRepositoryFake()
        preferenceRepository = PreferenceRepositoryFake()
        viewModel = createViewModel()
    }

    @Test
    fun `state should contain categories and username when ViewModel is initialized`() = runTest {
        // Advance coroutines manually to let flows emit
        advanceUntilIdle()

        val expectedCategories = listOf(
            Category(
                id = "1",
                title = "CLASE A - CATEGORIA I",
                category = "A-I",
                classType = "A",
                description = "Es el más común y te permite manejar carros como sedanes, coupé , hatchback, convertibles, station wagon, SUV, Areneros, Pickup y furgones. Es necesaria para obtener las demás licencias de Clase A.",
                pdf = "CLASE_A_I.pdf"
            ),
            Category(
                id = "2",
                title = "CLASE A - CATEGORIA II-A",
                category = "A-IIa",
                classType = "A",
                description = "Los mismos que A-1 y también carros oficiales de transporte de pasajeros como Taxis, Buses, Ambulancias y Transporte Interprovincial. Primero debes obtener la Licencia A-I",
                pdf = "CLASE_A_I.pdf"
            ),
        )

        Truth.assertThat(viewModel.state.value.categories).isEqualTo(expectedCategories)
        Truth.assertThat(viewModel.state.value.userName).isEqualTo("Usuario de prueba")
    }

    @Test
    fun `state should update when PreferenceRepository emits new username`() = runTest {
        advanceUntilIdle() // Advance coroutines manually to let flows emit
        preferenceRepository.setUserName("Nuevo Usuario")
        advanceUntilIdle()
        Truth.assertThat(viewModel.state.value.userName).isEqualTo("Nuevo Usuario")
    }

    @Test
    fun `ViewModel emits state with categories and username using Turbine`() = runTest {
        viewModel.state.test {
            val state = awaitItem()
            Truth.assertThat(state.categories).hasSize(2)
            Truth.assertThat(state.userName).isEqualTo("Usuario de prueba")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state reflects isPremium from PremiumRepository`() = runTest {
        val vm = createViewModel(premiumRepository = fakePremiumRepository(MutableStateFlow(true)))
        advanceUntilIdle()

        Truth.assertThat(vm.state.value.isPremium).isTrue()
    }

    @Test
    fun `state reflects isPremium changes from PremiumRepository flow emissions`() = runTest {
        val isPremiumFlow = MutableStateFlow(false)
        val vm = createViewModel(premiumRepository = fakePremiumRepository(isPremiumFlow))
        advanceUntilIdle()
        Truth.assertThat(vm.state.value.isPremium).isFalse()

        isPremiumFlow.value = true
        advanceUntilIdle()

        Truth.assertThat(vm.state.value.isPremium).isTrue()
    }
}