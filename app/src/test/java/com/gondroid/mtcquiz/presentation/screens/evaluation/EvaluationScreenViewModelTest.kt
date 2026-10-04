package com.gondroid.mtcquiz.presentation.screens.evaluation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.gondroid.core.domain.model.Evaluation
import com.gondroid.core.domain.repository.PreferenceRepository
import com.gondroid.evaluation.presentation.EvaluationEvent
import com.gondroid.evaluation.presentation.EvaluationScreenViewModel
import com.gondroid.mtcquiz.presentation.screens.QuizRepositoryFake
import com.gondroid.mtcquiz.util.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EvaluationScreenViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = QuizRepositoryFake()

    private fun createViewModel(): EvaluationScreenViewModel {
        val preferenceRepository: PreferenceRepository = mockk(relaxed = true) {
            every { timeToFinishEvaluationFlow } returns flowOf("40")
        }
        return EvaluationScreenViewModel(
            savedStateHandle = SavedStateHandle(mapOf("categoryId" to "1")),
            repository = repository,
            preferenceRepository = preferenceRepository
        )
    }

    private suspend fun savedEvaluationOf(vm: EvaluationScreenViewModel): Evaluation {
        var evaluationId = ""
        vm.event.test {
            vm.saveExam()
            evaluationId = (awaitItem() as EvaluationEvent.EvaluationCreated).evaluationId
        }
        return repository.getEvaluationById(evaluationId)!!
    }

    @Test
    fun `a verified answer counts even if the exam ends before moving on`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        // Timer runs out right after "Verificar", before "Siguiente".
        vm.verifyAnswer(isCorrect = true, option = "París")
        val evaluation = savedEvaluationOf(vm)

        assertThat(evaluation.totalCorrect).isEqualTo(1)
        assertThat(evaluation.totalIncorrect).isEqualTo(1)
        assertThat(evaluation.questionResults).hasSize(1)
        assertThat(evaluation.questionResults.single().option).isEqualTo("París")
    }

    @Test
    fun `verifying the same question twice records it once`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.verifyAnswer(isCorrect = true, option = "París")
        vm.verifyAnswer(isCorrect = false, option = "Roma")
        val evaluation = savedEvaluationOf(vm)

        assertThat(evaluation.questionResults).hasSize(1)
        assertThat(evaluation.questionResults.single().isCorrect).isTrue()
    }

    @Test
    fun `each verified question is recorded and the last one finishes the exam`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.verifyAnswer(isCorrect = true, option = "París")
        assertThat(vm.state.value.isFinishExam).isFalse()
        vm.nextQuestion()
        vm.verifyAnswer(isCorrect = false, option = "Español")
        assertThat(vm.state.value.isFinishExam).isTrue()

        val evaluation = savedEvaluationOf(vm)
        assertThat(evaluation.questionResults.map { it.isCorrect }).containsExactly(true, false).inOrder()
        assertThat(evaluation.questionResults.last().correctAnswer).isEqualTo("Portugués")
    }
}
