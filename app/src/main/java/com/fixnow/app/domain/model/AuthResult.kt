package com.fixnow.app.domain.model

sealed class AuthResult {
    data class Success(val user: AuthUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
