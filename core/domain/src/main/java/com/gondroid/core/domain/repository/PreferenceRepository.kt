package com.gondroid.core.domain.repository

import com.gondroid.core.domain.model.PreferencesEvaluation
import kotlinx.coroutines.flow.Flow

interface PreferenceRepository {

    val currentStreakFlow: Flow<Int>
    val lastStudyDateFlow: Flow<Long>
    suspend fun recordStudySession()

    val darkModeFlow: Flow<Boolean>
    val themeModeFlow: Flow<String>

    suspend fun setDarkMode(
        enabled: Boolean
    )

    suspend fun setThemeMode(mode: String)

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

    suspend fun setPreferencesEvaluation(data: PreferencesEvaluation): Boolean

    suspend fun logout()

}