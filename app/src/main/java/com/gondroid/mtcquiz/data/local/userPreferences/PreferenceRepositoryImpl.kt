package com.gondroid.mtcquiz.data.local.userPreferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gondroid.mtcquiz.domain.repository.PreferenceRepository
import kotlinx.coroutines.flow.Flow
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
    }


    override val darkModeFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }

    override suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    override suspend fun setUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[USER_NAME] = name
        }
    }

    override suspend fun setPhotoUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[PHOTO_URL] = url
        }
    }

    override suspend fun setIsLoggedIn(isLoggedIn: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = isLoggedIn
        }
    }

    override suspend fun setIsOnboardingShown(isOnboardingShown: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_ONBOARDING_SHOWN] = isOnboardingShown
        }
    }

    override val userNameFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[USER_NAME] ?: ""
        }

    override val photoUrlFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[PHOTO_URL] ?: ""
        }

    override val isLoggedInFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[IS_LOGGED_IN] ?: false
        }

    override val isOnboardingShownFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[IS_ONBOARDING_SHOWN] ?: false
        }


}
