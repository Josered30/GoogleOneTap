package com.example.googleonetap.models

class CredentialsResult(
    val credentials: Credentials,
    val emailError: String,
    val passwordError: String,
    val valid: Boolean
)