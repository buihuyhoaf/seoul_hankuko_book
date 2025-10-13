package com.seoulhankuko.app.domain.exception

/**
 * Custom exceptions for SeoulHankukoBook app
 */
sealed class AppException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    
    /**
     * Network-related exceptions
     */
    sealed class NetworkException(message: String, cause: Throwable? = null) : AppException(message, cause) {
        class NoInternetConnection(cause: Throwable? = null) : NetworkException(
            "No internet connection available", cause
        )
        
        class ServerUnavailable(cause: Throwable? = null) : NetworkException(
            "Server is currently unavailable", cause
        )
        
        class RequestTimeout(cause: Throwable? = null) : NetworkException(
            "Request timeout - please try again", cause
        )
        
        class HostNotFound(cause: Throwable? = null) : NetworkException(
            "Cannot connect to server - please check your connection", cause
        )
    }
    
    /**
     * Authentication-related exceptions
     */
    sealed class AuthException(message: String, cause: Throwable? = null) : AppException(message, cause) {
        class InvalidCredentials(cause: Throwable? = null) : AuthException(
            "Invalid email or password", cause
        )
        
        class UserNotFound(cause: Throwable? = null) : AuthException(
            "User not found", cause
        )
        
        class EmailAlreadyExists(cause: Throwable? = null) : AuthException(
            "Email already exists", cause
        )
        
        class UsernameAlreadyExists(cause: Throwable? = null) : AuthException(
            "Username already exists", cause
        )
        
        class TokenExpired(cause: Throwable? = null) : AuthException(
            "Session expired - please login again", cause
        )
        
        class AccountLocked(cause: Throwable? = null) : AuthException(
            "Account is locked - please contact support", cause
        )
    }
    
    /**
     * Data-related exceptions
     */
    sealed class DataException(message: String, cause: Throwable? = null) : AppException(message, cause) {
        class DataNotFound(cause: Throwable? = null) : DataException(
            "Requested data not found", cause
        )
        
        class DataCorrupted(cause: Throwable? = null) : DataException(
            "Data is corrupted", cause
        )
        
        class StorageError(cause: Throwable? = null) : DataException(
            "Storage error occurred", cause
        )
    }
    
    /**
     * Validation-related exceptions
     */
    sealed class ValidationException(message: String, cause: Throwable? = null) : AppException(message, cause) {
        class InvalidEmail(cause: Throwable? = null) : ValidationException(
            "Invalid email format", cause
        )
        
        class WeakPassword(cause: Throwable? = null) : ValidationException(
            "Password is too weak", cause
        )
        
        class RequiredFieldMissing(fieldName: String, cause: Throwable? = null) : ValidationException(
            "Required field '$fieldName' is missing", cause
        )
    }
    
    /**
     * Generic unexpected exceptions
     */
    class UnexpectedError(message: String, cause: Throwable? = null) : AppException(message, cause)
}
