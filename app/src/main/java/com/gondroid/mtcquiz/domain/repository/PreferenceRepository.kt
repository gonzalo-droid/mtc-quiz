package com.gondroid.mtcquiz.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferenceRepository {

    val darkModeFlow: Flow<Boolean>

    suspend fun setDarkMode(
        enabled: Boolean
    )

    val userNameFlow: Flow<String>

    suspend fun setUserName(
        name: String
    )

    val photoUrlFlow: Flow<String>

    suspend fun setPhotoUrl(
        url: String
    )

    val isLoggedInFlow: Flow<Boolean>

    suspend fun setIsLoggedIn(
        isLoggedIn: Boolean
    )

    val isOnboardingShownFlow: Flow<Boolean>

    suspend fun setIsOnboardingShown(
        isOnboardingShown: Boolean
    )


}