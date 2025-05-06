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
import com.gondroid.mtcquiz.presentation.screens.detail.DetailDataState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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

    private var _state = MutableStateFlow(EvaluationDataState())
    val state = _state.asStateFlow()

    private val _resultsList = mutableListOf<QuestionResult>()
    val resultsList: List<QuestionResult> get() = _resultsList

    private var eventChannel = Channel<EvaluationEvent>()
    val event = eventChannel.receiveAsFlow()

    private val data = savedStateHandle.toRoute<EvaluationScreenRoute>()

    init {
        viewModelScope.launch {
            data.categoryId.let {
                repository.getCategoryById(it)?.let { category ->
                    _state.update {
                        it.copy(category = category)
                    }
                }
            }

            repository.getQuestionsByCategory(data.categoryId)
                .collect { questions ->
                    _state.update {
                        it.copy(questions = questions, question = questions[_state.value.indexQuestion])
                    }

                }
        }
    }

    fun nextQuestion() {
        val index = _state.value.indexQuestion.inc()
        _state.update {
            it.copy(
                answerWasSelected = false,
                answerWasVerified = false,
                indexQuestion = index,
                question = _state.value.questions[index])
        }
    }

    fun verifyAnswer() {
        _state.value = _state.value.copy(
            answerWasSelected = true,
            answerWasVerified = true,
            isFinishExam = _state.value.indexQuestion == _state.value.questions.size.dec()
        )
    }

    fun saveAnswer(isCorrect: Boolean, option: String) {
        if (_state.value.answerWasVerified) {
            val result = QuestionResult(
                id = UUID.randomUUID().toString(),
                questionId = _state.value.question.id,
                question = _state.value.question.title,
                option = option,
                isCorrect = isCorrect
            )
            _resultsList.add(result)
        }
    }

    fun saveExam() {
        val correctAnswers = _resultsList.count { it.isCorrect }
        val totalTask = _state.value.questions.size
        val incorrectAnswers = totalTask - correctAnswers

        val percentage = (correctAnswers / totalTask.toFloat()).times(100).toInt()

        viewModelScope.launch {
            val evaluation = Evaluation(
                id = UUID.randomUUID().toString(),
                categoryId = _state.value.category.id,
                categoryTitle = _state.value.category.title,
                totalCorrect = correctAnswers,
                totalIncorrect = incorrectAnswers,
                totalQuestions = _state.value.questions.size,
                state = if (percentage >= 80) EvaluationState.APPROVED else EvaluationState.REJECTED
            )
            repository.saveEvaluation(evaluation = evaluation)

            eventChannel.send(EvaluationEvent.EvaluationCreated(evaluationId = evaluation.id))
        }
    }
}