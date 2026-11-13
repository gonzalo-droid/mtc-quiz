package com.gondroid.mtcquiz.presentation.screens

import com.gondroid.core.domain.model.PreferencesEvaluation
import com.gondroid.core.domain.repository.PreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class PreferenceRepositoryFake : PreferenceRepository {
    private val _currentStreakFlow = MutableStateFlow(0)
    private val _lastStudyDateFlow = MutableStateFlow(0L)
    private val _userNameFlow = MutableStateFlow("Usuario de prueba")
    private val _darkModeFlow = MutableStateFlow(true)
    private val _themeModeFlow = MutableStateFlow("system")
    private val _isLoggedInFlow = MutableStateFlow(false)
    private val _isOnboardingShownFlow = MutableStateFlow(false)
    private val _photoUrlFlow = MutableStateFlow("image.png")
    private val _numberQuestionsFlow = MutableStateFlow("40")
    private val _percentageToApprovedEvaluationFlow = MutableStateFlow("70")
    private val _timeToFinishEvaluationFlow = MutableStateFlow("40")

    override val currentStreakFlow: Flow<Int> = _currentStreakFlow
    override val lastStudyDateFlow: Flow<Long> = _lastStudyDateFlow
    override suspend fun recordStudySession() {}

    override val darkModeFlow: Flow<Boolean> = _darkModeFlow

    override suspend fun setDarkMode(enabled: Boolean) {
        _darkModeFlow.value = enabled
    }

    override val themeModeFlow: Flow<String> = _themeModeFlow

    override suspend fun setThemeMode(mode: String) {
        _themeModeFlow.value = mode
    }

    override val userNameFlow: Flow<String> = _userNameFlow

    override suspend fun setUserName(name: String) {
        _userNameFlow.value = name
    }

    override val photoUrlFlow: Flow<String> = _photoUrlFlow

    override suspend fun setPhotoUrl(url: String) {
        _photoUrlFlow.value = url
    }

    override val isLoggedInFlow: Flow<Boolean> = _isLoggedInFlow

    override suspend fun setIsLoggedIn(isLoggedIn: Boolean) {
        _isLoggedInFlow.value = isLoggedIn
    }

    override val isOnboardingShownFlow: Flow<Boolean> = _isOnboardingShownFlow

    override suspend fun setIsOnboardingShown(isOnboardingShown: Boolean) {
        _isOnboardingShownFlow.value = isOnboardingShown
    }

    override val numberQuestionsFlow: Flow<String> = _numberQuestionsFlow

    override suspend fun setNumberQuestions(value: String) {
        _numberQuestionsFlow.value = value
    }

    override val percentageToApprovedEvaluationFlow: Flow<String> =
        _percentageToApprovedEvaluationFlow

    override suspend fun setPercentageToApprovedEvaluation(value: String) {
        _percentageToApprovedEvaluationFlow.value = value
    }

    override val timeToFinishEvaluationFlow: Flow<String> = _timeToFinishEvaluationFlow

    override suspend fun setTimeToFinishEvaluation(value: String) {
        _timeToFinishEvaluationFlow.value = value
    }

    override suspend fun getInitEvaluation(): PreferencesEvaluation = PreferencesEvaluation(
        numberQuestions = "40",
        percentageToApprovedEvaluation = "70",
        timeToFinishEvaluation = "40"
    )

    override suspend fun setPreferencesEvaluation(data: PreferencesEvaluation): Boolean = true

    override suspend fun logout() {}
}
