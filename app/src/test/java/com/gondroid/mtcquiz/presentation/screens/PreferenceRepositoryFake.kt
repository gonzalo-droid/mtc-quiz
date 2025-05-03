package com.gondroid.mtcquiz.presentation.screens

import com.gondroid.mtcquiz.domain.repository.PreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class PreferenceRepositoryFake : PreferenceRepository {
    private val _userNameFlow = MutableStateFlow("Usuario de prueba")
    private val _darkModeFlow = MutableStateFlow(true)
    private val _isLoggedIn = MutableStateFlow(false)
    private val _isOnboardingShownFlow = MutableStateFlow(false)
    private val _photoUrlFlow = MutableStateFlow("image.png")

    override val darkModeFlow: MutableStateFlow<Boolean> = _darkModeFlow

    override suspend fun setDarkMode(enabled: Boolean) {
        _darkModeFlow.value = enabled
    }

    override val userNameFlow: Flow<String> = _userNameFlow

    override suspend fun setUserName(name: String) {
        _userNameFlow.value = name
    }


    override val photoUrlFlow: Flow<String> = _photoUrlFlow

    override suspend fun setPhotoUrl(url: String) {
        _photoUrlFlow.value = url
    }

    override val isLoggedInFlow: Flow<Boolean> = _isLoggedIn

    override suspend fun setIsLoggedIn(isLoggedIn: Boolean) {
        _isLoggedIn.value = isLoggedIn
    }

    override val isOnboardingShownFlow: Flow<Boolean> = _isOnboardingShownFlow

    override suspend fun setIsOnboardingShown(isOnboardingShown: Boolean) {
        _isOnboardingShownFlow.value = isOnboardingShown
    }
}