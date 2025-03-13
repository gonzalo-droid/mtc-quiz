package com.gondroid.mtcquiz.presentation.screens.pdf


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class PdfScreenViewModel
@Inject
constructor() : ViewModel() {

    var state by mutableStateOf(PdfDataState())
        private set

    init {

    }
}