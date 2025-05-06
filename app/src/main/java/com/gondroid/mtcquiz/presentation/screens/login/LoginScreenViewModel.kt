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
import com.gondroid.mtcquiz.presentation.screens.questions.QuestionsDataState
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class LoginScreenViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var _state = MutableStateFlow(ConfigurationDataState())
    val state = _state.asStateFlow()


    private var _isLoggedIn = MutableStateFlow(authRepository.isUserLoggedIn())
    val isLoggedIn = _isLoggedIn.asStateFlow()

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
                Timber.e("Error: ${e.localizedMessage} ${e.message}")
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
                        Timber.tag("LoginVM").e(e, "Received an invalid google id token response")
                    }
                } else {
                    Timber.tag("LoginVM").e("Unexpected type of credential")
                }
            }

            else -> {
                Timber.tag("LoginVM").e("Unexpected type of credential")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _isLoggedIn.update { false }
        }
    }
}
