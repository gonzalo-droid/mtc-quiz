package com.gondroid.mtcquiz.presentation.screens.evaluation


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gondroid.mtcquiz.domain.models.Evaluation
import com.gondroid.mtcquiz.domain.models.EvaluationState
import com.gondroid.mtcquiz.domain.models.QuestionResult
import com.gondroid.mtcquiz.domain.repository.QuizRepository
import com.gondroid.mtcquiz.presentation.navigation.EvaluationScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class EvaluationScreenViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository
) : ViewModel() {

    var state by mutableStateOf(EvaluationDataState())
        private set

    private val _resultsList = mutableListOf<QuestionResult>()
    val resultsList: List<QuestionResult> get() = _resultsList

    private var eventChannel = Channel<EvaluationEvent>()
    val event = eventChannel.receiveAsFlow()

    private val data = savedStateHandle.toRoute<EvaluationScreenRoute>()


    init {

        viewModelScope.launch {

            data.categoryId.let {
                repository.getCategoryById(it)?.let { category ->
                    state = state.copy(category = category)
                }
            }

            repository.getQuestionsByCategory("categoryId")
                .collect { questions ->
                    state =
                        state.copy(questions = questions, question = questions[state.indexQuestion])
                }
        }
    }

    fun nextQuestion() {
        val index = state.indexQuestion.inc()
        state = state.copy(
            answerWasSelected = false,
            answerWasVerified = false,
            indexQuestion = index,
            question = state.questions[index]
        )
    }

    fun verifyAnswer() {
        state = state.copy(
            answerWasSelected = true,
            answerWasVerified = true,
            isFinishExam = state.indexQuestion == state.questions.size.dec()
        )
    }

    fun saveAnswer(isCorrect: Boolean, option: String) {
        if (state.answerWasVerified) {
            val result = QuestionResult(
                id = UUID.randomUUID().toString(),
                questionId = state.question.id,
                question = state.question.title,
                option = option,
                isCorrect = isCorrect
            )
            _resultsList.add(result)
        }

        val correctAnswers = _resultsList.count { it.isCorrect }
        val incorrectAnswers = _resultsList.count { !it.isCorrect }

        state = state.copy(
            correctAnswers = correctAnswers,
            incorrectAnswers = incorrectAnswers,
        )
    }

    fun saveExam() {
        val correctAnswers = _resultsList.count { it.isCorrect }
        val incorrectAnswers = _resultsList.count { !it.isCorrect }
        val totalTask = state.questions.size

        val percentage = (correctAnswers / totalTask.toFloat()).times(100).toInt()

        viewModelScope.launch {
            val evaluation = Evaluation(
                id = UUID.randomUUID().toString(),
                categoryId = state.category.id,
                categoryTitle = state.category.title,
                totalCorrect = correctAnswers,
                totalIncorrect = incorrectAnswers,
                totalQuestions = state.questions.size,
                state = if (percentage >= 80) EvaluationState.APPROVED else EvaluationState.REJECTED
            )
            repository.saveEvaluation(evaluation = evaluation)

            eventChannel.send(EvaluationEvent.EvaluationCreated(evaluationId = evaluation.id))
        }
    }
}