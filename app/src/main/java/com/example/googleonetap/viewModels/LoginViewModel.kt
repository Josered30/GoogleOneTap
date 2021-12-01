package com.example.googleonetap.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.googleonetap.auth.AuthManager
import com.example.googleonetap.auth.AuthState
import com.example.googleonetap.auth.User
import com.example.googleonetap.models.Credentials
import com.example.googleonetap.models.CredentialsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class LoginViewModel @Inject constructor(private val authManager: AuthManager) : ViewModel() {

    private val _credentialsResult = MutableLiveData<CredentialsResult>()
    val credentialsResult: LiveData<CredentialsResult> = _credentialsResult

    private val _checkTokenResult = MutableLiveData<Boolean>()
    val checkTokenResult: LiveData<Boolean> = _checkTokenResult

    fun validateCredentials(credentials: Credentials) {
        val regex =
            Regex("(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")


        if (credentials.email.isNotEmpty() && regex.matches(credentials.email)) {
            _credentialsResult.postValue(CredentialsResult(credentials, "", "", true))
            return
        }

        var currentEmailError = credentialsResult.value?.emailError.orEmpty()
        var currentPasswordError = credentialsResult.value?.passwordError.orEmpty()

        if (credentials.email.isEmpty()) {
            currentEmailError = "Empty field"
        }

        if (credentials.password.isEmpty()) {
            currentPasswordError = "Empty field"
        }

        if (credentials.email.isNotEmpty() && !regex.matches(credentials.email)) {
            currentEmailError = "Email error"
        }

        _credentialsResult.postValue(
            CredentialsResult(
                credentials,
                currentEmailError,
                currentPasswordError,
                false
            )
        )
    }

    fun loginNewToken(credentials: Credentials) {
        // send credentials to server
        val token = "dfasdasdsa"
        val user = User(token, AuthState.AUTH)
        authManager.setToken(token)
        authManager.changeState(user)
    }

    fun loginOldToken(token: String) {
        authManager.setToken(token)
        val user = User(token, AuthState.AUTH)
        authManager.setToken(token)
        authManager.changeState(user)
    }

    fun loginGoogleToken(googleToken: String) {
        // send credentials to server
        val token = "dfasdasdsa"
        val user = User(token, AuthState.AUTH)
        authManager.setToken(token)
        authManager.changeState(user)
    }
}


