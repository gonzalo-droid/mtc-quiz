package com.gondroid.evaluation.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.core.database.dao.DismissedQuestionDao
import com.gondroid.core.database.entity.DismissedQuestionEntity
import com.gondroid.core.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewErrorsViewModel @Inject constructor(
    private val repository: QuizRepository,
    private val dismissedDao: DismissedQuestionDao
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewErrorsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAllEvaluations(),
                dismissedDao.getAllDismissedIds()
            ) { evaluations, dismissedKeys ->
                frequentErrors(evaluations, dismissedKeys.toSet())
            }.collect { frequentErrors ->
                _state.update {
                    it.copy(
                        frequentErrors = frequentErrors,
                        isLoading = false
                    )
                }
            }
        }
    }

    /** [key] is the error's [errorKey], stored in the `question_id` column. */
    fun dismissQuestion(key: Int) = viewModelScope.launch {
        dismissedDao.dismiss(DismissedQuestionEntity(key))
    }
}
