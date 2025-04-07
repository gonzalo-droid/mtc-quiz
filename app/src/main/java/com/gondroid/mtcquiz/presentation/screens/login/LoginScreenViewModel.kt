package com.gondroid.mtcquiz.presentation.screens.login


import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CustomCredential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.mtcquiz.domain.repository.AuthRepository
import com.gondroid.mtcquiz.presentation.screens.configuration.ConfigurationDataState
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LoginScreenViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {


    var state by mutableStateOf(ConfigurationDataState())
        private set

    var isLoggedIn by mutableStateOf(authRepository.isUserLoggedIn())
        private set

    var errorMessage by mutableStateOf<String?>(null)

    fun signInWithGoogle(idToken: String) {
        authRepository.signInWithGoogle(idToken) { success, error ->
            if (success) {
                isLoggedIn = true
                errorMessage = null
            } else {
                errorMessage = error
            }
        }
    }

    fun launchGoogleSignIn(activity: Activity?) {
        viewModelScope.launch {
            try {
                val result = authRepository.getGoogleClient(activity)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)

                    signInWithGoogle(googleIdTokenCredential.idToken)
                }
            } catch (e: Exception) {
                Log.e("LoginVM", "Error: ${e.localizedMessage}")
            }
        }
    }

    fun logout() {
        authRepository.logout()
        isLoggedIn = false
    }
}
