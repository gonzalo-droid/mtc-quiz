package com.gondroid.mtcquiz.presentation.screens.configuration


import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject


@HiltViewModel
class ConfigurationScreenViewModel
@Inject
constructor() : ViewModel() {

    private var _state = MutableStateFlow(ConfigurationDataState())
    val state = _state.asStateFlow()


}