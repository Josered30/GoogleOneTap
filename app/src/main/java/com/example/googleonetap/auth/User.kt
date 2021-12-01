package com.example.googleonetap.auth

class User(val token: String, val authState: AuthState) {
    constructor() : this(token = "", authState = AuthState.EMPTY)
}
