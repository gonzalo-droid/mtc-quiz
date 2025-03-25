package com.gondroid.mtcquiz.presentation.screens.evaluation


import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gondroid.mtcquiz.domain.models.QuestionResult
import com.gondroid.mtcquiz.domain.repository.QuizRepository
import com.gondroid.mtcquiz.presentation.navigation.EvaluationScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val data = savedStateHandle.toRoute<EvaluationScreenRoute>()


    init {

        data.categoryId.let {
            viewModelScope.launch {
                repository.getCategoryById(it)?.let { category ->
                    state = state.copy(category = category)
                }
            }
        }

        viewModelScope.launch {
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
            answerWasVerified = true
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
            Log.d("onClickItem", result.toString())
            _resultsList.add(result)
        }

    }
}