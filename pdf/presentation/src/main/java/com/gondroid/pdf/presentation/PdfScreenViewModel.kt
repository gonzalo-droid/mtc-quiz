package com.gondroid.pdf.presentation


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gondroid.core.domain.repository.QuizRepository
import com.gondroid.core.presentation.ui.PdfScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class PdfScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository
) : ViewModel() {

    private var _state = MutableStateFlow(PdfState())
    val state = _state.asStateFlow()


    private val data = savedStateHandle.toRoute<PdfScreenRoute>()

    init {
        data.categoryId.let {
            viewModelScope.launch {
                repository.getCategoryById(it)?.let { category ->
                    _state.update {
                        it.copy(category = category, isLoading = true)
                    }
                }
            }
        }
    }

    fun loading(isLoading: Boolean) {
        _state.update {
            it.copy(isLoading = isLoading)
        }
    }

    fun downloading(value: Boolean) {
        _state.update {
            it.copy(shouldDownload = value, isLoading = value)
        }
    }

}