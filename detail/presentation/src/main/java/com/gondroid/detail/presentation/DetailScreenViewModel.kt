package com.gondroid.detail.presentation


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gondroid.domain.repository.QuizRepository
import com.gondroid.mtcquiz.domain.repository.QuizRepository
import com.gondroid.mtcquiz.presentation.navigation.DetailScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class DetailScreenViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository
) : ViewModel() {


    private var _state = MutableStateFlow(DetailDataState())
    val state = _state.asStateFlow()

    private val data = savedStateHandle.toRoute<DetailScreenRoute>()
    init {
        data.categoryId.let {
            viewModelScope.launch {
                repository.getCategoryById(it)?.let { category ->
                    _state.update {
                        it.copy(category = category)
                    }
                }

            }
        }
    }
}