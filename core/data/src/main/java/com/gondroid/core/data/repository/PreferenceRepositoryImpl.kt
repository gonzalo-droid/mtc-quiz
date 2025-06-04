package com.gondroid.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject


class PreferenceRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferenceRepository {

    companion object {
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_enabled")
        val USER_NAME = stringPreferencesKey("user_name")
        val PHOTO_URL = stringPreferencesKey("photo_url")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val IS_ONBOARDING_SHOWN = booleanPreferencesKey("is_onboarding_shown")

        var NUMBER_QUESTIONS = stringPreferencesKey("number_questions")
        var PERCENTAGE_TO_APPROVED_EVALUATION =
            stringPreferencesKey("percentage_to_approved_evaluation")
        val TIME_TO_FINISH_EVALUATION = stringPreferencesKey("time_to_finish_evaluation")
    }


    // DarkMode
    override val darkModeFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }

    override suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }
    // UserName

    override suspend fun setUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[USER_NAME] = name
        }
    }

    override val userNameFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[USER_NAME] ?: ""
        }

    // Photo URL
    override suspend fun setPhotoUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[PHOTO_URL] = url
        }
    }

    override val photoUrlFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[PHOTO_URL] ?: ""
        }

    // Logged In
    override suspend fun setIsLoggedIn(isLoggedIn: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = isLoggedIn
        }
    }

    override val isLoggedInFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[IS_LOGGED_IN] ?: false
        }

    // Onboarding Show
    override suspend fun setIsOnboardingShown(isOnboardingShown: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_ONBOARDING_SHOWN] = isOnboardingShown
        }
    }

    override val isOnboardingShownFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[IS_ONBOARDING_SHOWN] ?: false
        }


    // Setting evaluation
    override val numberQuestionsFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[NUMBER_QUESTIONS] ?: "40"
        }

    override suspend fun setNumberQuestions(value: String) {
        dataStore.edit { preferences ->
            preferences[NUMBER_QUESTIONS] = value
        }
    }

    override val percentageToApprovedEvaluationFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[PERCENTAGE_TO_APPROVED_EVALUATION] ?: "80"
        }

    override suspend fun setPercentageToApprovedEvaluation(value: String) {
        dataStore.edit { preferences ->
            preferences[PERCENTAGE_TO_APPROVED_EVALUATION] = value
        }
    }

    override suspend fun setTimeToFinishEvaluation(value: String) {
        dataStore.edit { preferences ->
            preferences[TIME_TO_FINISH_EVALUATION] = value
        }
    }

    override val timeToFinishEvaluationFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[TIME_TO_FINISH_EVALUATION] ?: "40"
        }

    override suspend fun getInitEvaluation(): PreferencesEvaluation {
        val numberQuestions = numberQuestionsFlow.first()
        val timeToFinish = timeToFinishEvaluationFlow.first()
        val percentage = percentageToApprovedEvaluationFlow.first()

        return PreferencesEvaluation(
            numberQuestions = numberQuestions.toString(),
            timeToFinishEvaluation = timeToFinish.toString(),
            percentageToApprovedEvaluation = percentage.toString()
        )
    }

    override suspend fun setPreferencesEvaluation(data: PreferencesEvaluation): Boolean {
        return try {
            setNumberQuestions(data.numberQuestions)
            setTimeToFinishEvaluation(data.timeToFinishEvaluation)
            setPercentageToApprovedEvaluation(data.percentageToApprovedEvaluation)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun logout() {
        setUserName("")
        setPhotoUrl("")
        setIsLoggedIn(false)
        setIsOnboardingShown(true)
    }
}
