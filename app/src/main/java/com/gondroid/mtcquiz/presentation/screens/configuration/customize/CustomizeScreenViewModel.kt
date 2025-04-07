package com.gondroid.mtcquiz.presentation.screens.configuration.customize


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class CustomizeScreenViewModel
@Inject
constructor() : ViewModel() {

    var state by mutableStateOf(CustomizeDataState())
        private set
}