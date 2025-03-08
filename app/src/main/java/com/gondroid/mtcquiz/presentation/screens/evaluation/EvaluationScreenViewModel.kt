package com.gondroid.mtcquiz.presentation.screens.evaluation


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class EvaluationScreenViewModel
@Inject
constructor() : ViewModel() {

    var state by mutableStateOf(EvaluationDataState())
        private set
}