package com.gondroid.mtcquiz.presentation.screens.questions


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class QuestionsScreenViewModel
@Inject
constructor() : ViewModel() {

    var state by mutableStateOf(QuestionsDataState())
        private set
}