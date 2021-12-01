package com.example.googleonetap.auth

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AuthManager(
    private val context: Context
) {
    companion object {
        const val SHARE_STORAGE_KEY = "SECURITY_STORAGE"
        const val TOKEN_KEY = "USER_TOKEN"
    }

    private val _stateSharedFlow: MutableSharedFlow<User> =
        MutableSharedFlow(extraBufferCapacity = 1)
    val stateSharedFlow = _stateSharedFlow.asSharedFlow()

    var currentValue: User = User()
        private set

    init {
        _stateSharedFlow.tryEmit(currentValue)
    }

    fun changeState(user: User) {
        currentValue = user
        _stateSharedFlow.tryEmit(user)
    }

    fun checkToken(): Boolean {
        val sharedPreferences =
            context.getSharedPreferences(SHARE_STORAGE_KEY, Context.MODE_PRIVATE)
        val token = sharedPreferences.getString(TOKEN_KEY, "")
        if (token != null && token.isNotEmpty()) {
            return true
        }
        return false
    }

    fun checkAuth(): Boolean {
        return (checkToken() && currentValue.authState == AuthState.AUTH)
    }

    fun setToken(token: String): Unit {
        val sharedPreferences =
            context.getSharedPreferences(SHARE_STORAGE_KEY, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(TOKEN_KEY, token)
            apply()
        }
    }

    fun getToken(): String {
        val sharedPreferences =
            context.getSharedPreferences(SHARE_STORAGE_KEY, Context.MODE_PRIVATE)
        return sharedPreferences.getString(TOKEN_KEY, "").orEmpty()
    }

    fun removeToken(): Unit {
        val sharedPreferences =
            context.getSharedPreferences(SHARE_STORAGE_KEY, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove(TOKEN_KEY)
            apply()
        }
    }
}
