package com.gondroid.questionreview.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gondroid.core.domain.repository.FeatureFlagsRepository
import com.gondroid.core.domain.repository.PremiumRepository
import com.gondroid.core.domain.repository.QuizRepository
import com.gondroid.core.presentation.ui.QuestionsScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class QuestionsScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository,
    premiumRepository: PremiumRepository,
    featureFlagsRepository: FeatureFlagsRepository,
    @Named("admobStudyBannerId") val bannerAdId: String
) : ViewModel() {

    private var _state = MutableStateFlow(QuestionsState())
    val state = _state.asStateFlow()

    private val data = savedStateHandle.toRoute<QuestionsScreenRoute>()

    init {
        premiumRepository.isPremiumFlow.onEach { isPremium ->
            _state.update { it.copy(isPremium = isPremium) }
        }.launchIn(viewModelScope)

        featureFlagsRepository.studyBannerInterval.onEach { interval ->
            _state.update { it.copy(bannerInterval = interval) }
        }.launchIn(viewModelScope)

        data.categoryId.let {
            viewModelScope.launch {
                repository.getCategoryById(it)?.let { category ->
                    _state.update {
                        it.copy(category = category)
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.getQuestionsByCategory(
                categoryId = _state.value.category.id,
                pathJson = _state.value.category.pathJson
            )
                .collect { questions ->
                    _state.update {
                        it.copy(questions = questions)
                    }
                }
        }
    }
}
