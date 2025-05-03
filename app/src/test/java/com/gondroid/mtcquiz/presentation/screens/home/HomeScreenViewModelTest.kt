package com.gondroid.mtcquiz.presentation.screens.home

import app.cash.turbine.test
import com.gondroid.mtcquiz.data.local.quiz.CLASS_A
import com.gondroid.mtcquiz.domain.models.Category
import com.gondroid.mtcquiz.presentation.screens.PreferenceRepositoryFake
import com.gondroid.mtcquiz.presentation.screens.QuizRepositoryFake
import com.gondroid.mtcquiz.util.MainDispatcherRule
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
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


    @Before
    fun setUp() {
        repository = QuizRepositoryFake()
        preferenceRepository = PreferenceRepositoryFake()
        viewModel = HomeScreenViewModel(
            repository = repository,
            preferenceRepository = preferenceRepository
        )
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
                classType = CLASS_A,
                description = "Es el más común y te permite manejar carros como sedanes, coupé , hatchback, convertibles, station wagon, SUV, Areneros, Pickup y furgones. Es necesaria para obtener las demás licencias de Clase A.",
                image = "a",
                pdf = "CLASE_A_I.pdf"
            ),
            Category(
                id = "2",
                title = "CLASE A - CATEGORIA II-A",
                category = "A-IIa",
                classType = CLASS_A,
                description = "Los mismos que A-1 y también carros oficiales de transporte de pasajeros como Taxis, Buses, Ambulancias y Transporte Interprovincial. Primero debes obtener la Licencia A-I",
                image = "a",
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
}