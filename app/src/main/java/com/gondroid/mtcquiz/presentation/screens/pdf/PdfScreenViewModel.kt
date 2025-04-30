package com.gondroid.mtcquiz.presentation.screens.pdf


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gondroid.mtcquiz.domain.repository.QuizRepository
import com.gondroid.mtcquiz.presentation.navigation.PdfScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class PdfScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository
) : ViewModel() {
    var state by mutableStateOf(PdfDataState())
        private set

    private val data = savedStateHandle.toRoute<PdfScreenRoute>()

    init {
        data.categoryId.let {
            viewModelScope.launch {
                repository.getCategoryById(it)?.let { category ->
                    state = state.copy(category = category, isLoading = true)
                }
            }
        }
    }

    fun loading(isLoading: Boolean) {
        state = state.copy(isLoading = isLoading)
    }

    fun downloading(value: Boolean) {
        state = state.copy(isLoading = value, shouldDownload = value)
    }

}