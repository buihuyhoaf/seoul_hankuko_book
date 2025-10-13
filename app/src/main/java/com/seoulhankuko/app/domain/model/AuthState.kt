package com.seoulhankuko.app.domain.model

sealed class AuthState {
    object Loading : AuthState()
    object SignedOut : AuthState()
    data class SignedIn(val userId: String) : AuthState()
}
