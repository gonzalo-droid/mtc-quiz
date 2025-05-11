package com.gondroid.mtcquiz.domain.repository

import com.gondroid.mtcquiz.domain.models.PreferencesEvaluation
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

    val numberQuestionsFlow: Flow<String>

    suspend fun setNumberQuestions(
        value: String
    )

    val percentageToApprovedEvaluationFlow: Flow<String>

    suspend fun setPercentageToApprovedEvaluation(
        value: String
    )

    val timeToFinishEvaluationFlow: Flow<String>

    suspend fun setTimeToFinishEvaluation(
        value: String
    )

    suspend fun getInitEvaluation(): PreferencesEvaluation

    suspend fun setPreferencesEvaluation(data: PreferencesEvaluation) : Boolean

    suspend fun logout()

}