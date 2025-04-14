package com.gondroid.mtcquiz.presentation.screens.login


import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.mtcquiz.domain.repository.AuthRepository
import com.gondroid.mtcquiz.presentation.screens.configuration.ConfigurationDataState
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
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

    private var eventChannel = Channel<LoginEvent>()
    val event = eventChannel.receiveAsFlow()


    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            val isSuccessful = authRepository.signInWithGoogle(idToken)
            if (isSuccessful) {
                eventChannel.send(LoginEvent.Success)
            } else {
                eventChannel.send(LoginEvent.Fail)
            }
        }
    }

    fun launchGoogleSignIn(context: Context) {
        viewModelScope.launch {
            try {
                handleSignIn(authRepository.getGoogleClient(context))
            } catch (e: GetCredentialException) {
                eventChannel.send(LoginEvent.Fail)
                Log.e("LoginVM", "Error: ${e.localizedMessage} ${e.message}")
            }
        }
    }

    fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential

        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)

                        signInWithGoogle(googleIdTokenCredential.idToken)

                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("LoginVM", "Received an invalid google id token response", e)
                    }
                } else {
                    Log.e("LoginVM", "Unexpected type of credential")
                }
            }

            else -> {
                Log.e("LoginVM", "Unexpected type of credential")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            isLoggedIn = false
        }
    }
}
