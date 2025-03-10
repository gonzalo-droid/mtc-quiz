package com.gondroid.mtcquiz.presentation.screens.home


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject


@HiltViewModel
class HomeScreenViewModel
@Inject
constructor() : ViewModel() {

    var state by mutableStateOf(HomeDataState())
        private set
}