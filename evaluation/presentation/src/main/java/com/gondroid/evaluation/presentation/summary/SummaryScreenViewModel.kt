package com.gondroid.evaluation.presentation.summary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gondroid.core.domain.repository.QuizRepository
import com.gondroid.core.presentation.ui.SummaryScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SummaryScreenViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository
) : ViewModel() {

    var state by mutableStateOf(SummaryState())
        private set

    private val data = savedStateHandle.toRoute<SummaryScreenRoute>()

    init {

        data.categoryId.let {
            viewModelScope.launch {
                repository.getCategoryById(it)?.let { category ->
                    state = state.copy(category = category)
                }
            }
        }

        data.evaluationId.let {
            viewModelScope.launch {
                repository.getEvaluationById(it)?.let { evaluation ->
                    state = state.copy(
                        evaluation = evaluation,
                        date = evaluation.date.let {
                            DATE_FORMATTER.format(it)
                                // Spanish leaves weekday and month lowercase; this string is
                                // shown on its own, so it reads better sentence-capitalised.
                                .replaceFirstChar { char -> char.uppercase() }
                        }
                    )
                }
            }
        }
    }

    private companion object {
        // Without an explicit locale this follows the device language, which rendered
        // "Sunday, September 06 2026" on an English device. Pinned to generic Spanish
        // rather than es-PE on purpose: es-PE is the only Spanish locale whose CLDR data
        // spells the ninth month "setiembre", and the more familiar "septiembre" was
        // preferred here.
        val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es"))
    }
}
