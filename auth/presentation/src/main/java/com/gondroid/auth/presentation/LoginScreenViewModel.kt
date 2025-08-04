package com.gondroid.auth.presentation


import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.core.domain.repository.AuthRepository
import com.gondroid.auth.domain.provider.AuthProvider
import com.gondroid.auth.domain.provider.AuthResult
import com.gondroid.core.presentation.ui.UiText
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class LoginScreenViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private var _uiState =
        MutableStateFlow(LoginState(error = "Google authentication not available"))
    var uiState = _uiState.asStateFlow()

    private var eventChannel = Channel<LoginEvent>()
    val event = eventChannel.receiveAsFlow()


    init {
        loadAvailableProviders()
    }


    private fun loadAvailableProviders() {
        viewModelScope.launch {
            try {
                val providers = authRepository.getAvailableProviders()
                _uiState.value = _uiState.value.copy(availableProviders = providers)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load providers: ${e.message}"
                )
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            val isSuccessful = authRepository.signInWithGoogle(idToken)
            if (isSuccessful) {
                eventChannel.send(LoginEvent.LoginSuccess)
            } else {
                // eventChannel.send(LoginEvent.Error)
            }
        }
    }

    fun authenticateWithGoogle() {
        viewModelScope.launch {
            val googleProvider = _uiState.value.availableProviders
                .filterIsInstance<AuthProvider.Google>()
                .firstOrNull()

            googleProvider?.let { authenticateWithProvider(it) }
                ?: run {
                    eventChannel.send(
                        LoginEvent.Error(
                            UiText.StringResource(R.string.provider_google_not_avaible)
                        )
                    )
                }
        }
    }

    fun authenticateWithProvider(provider: AuthProvider) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = authRepository.authenticate(provider)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                    )
                }

                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                    )
                }

                is AuthResult.Cancelled -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                    )
                }
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
}
