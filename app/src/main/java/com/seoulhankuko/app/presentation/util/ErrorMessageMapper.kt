package com.seoulhankuko.app.presentation.util

import com.seoulhankuko.app.domain.exception.AppException

/**
 * Utility class to convert exceptions to user-friendly error messages
 */
object ErrorMessageMapper {
    
    /**
     * Maps AppException to user-friendly error messages
     */
    fun getErrorMessage(exception: AppException): String {
        return when (exception) {
            // Network exceptions
            is AppException.NetworkException.NoInternetConnection -> 
                "No internet connection. Please check your network settings."
            is AppException.NetworkException.ServerUnavailable -> 
                "Server is temporarily unavailable. Please try again later."
            is AppException.NetworkException.RequestTimeout -> 
                "Request timeout. Please check your connection and try again."
            is AppException.NetworkException.HostNotFound -> 
                "Cannot connect to server. Please check your internet connection."
            
            // Authentication exceptions
            is AppException.AuthException.InvalidCredentials -> 
                "Invalid email or password. Please check your credentials."
            is AppException.AuthException.UserNotFound -> 
                "User not found. Please check your email address."
            is AppException.AuthException.EmailAlreadyExists -> 
                "Email already exists. Please use a different email address."
            is AppException.AuthException.UsernameAlreadyExists -> 
                "Username already exists. Please choose a different username."
            is AppException.AuthException.TokenExpired -> 
                "Session expired. Please login again."
            is AppException.AuthException.AccountLocked -> 
                "Account is locked. Please contact support for assistance."
            
            // Data exceptions
            is AppException.DataException.DataNotFound -> 
                "Requested data not found. Please try again."
            is AppException.DataException.DataCorrupted -> 
                "Data error occurred. Please try again."
            is AppException.DataException.StorageError -> 
                "Storage error. Please try again."
            
            // Validation exceptions
            is AppException.ValidationException.InvalidEmail -> 
                "Invalid email format. Please enter a valid email address."
            is AppException.ValidationException.WeakPassword -> 
                "Password is too weak. Please use at least 8 characters with numbers and letters."
            is AppException.ValidationException.RequiredFieldMissing -> 
                "Please fill in all required fields."
            
            // Unexpected errors
            is AppException.UnexpectedError -> 
                "An unexpected error occurred. Please try again."
        }
    }
    
    /**
     * Maps generic Throwable to user-friendly error messages
     */
    fun getErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is AppException -> getErrorMessage(throwable)
            else -> "An unexpected error occurred. Please try again."
        }
    }
    
    /**
     * Gets a short error message for UI display
     */
    fun getShortErrorMessage(exception: AppException): String {
        return when (exception) {
            is AppException.NetworkException.NoInternetConnection -> "No internet connection"
            is AppException.NetworkException.ServerUnavailable -> "Server unavailable"
            is AppException.NetworkException.RequestTimeout -> "Request timeout"
            is AppException.NetworkException.HostNotFound -> "Connection failed"
            is AppException.AuthException.InvalidCredentials -> "Invalid credentials"
            is AppException.AuthException.UserNotFound -> "User not found"
            is AppException.AuthException.EmailAlreadyExists -> "Email exists"
            is AppException.AuthException.UsernameAlreadyExists -> "Username exists"
            is AppException.AuthException.TokenExpired -> "Session expired"
            is AppException.AuthException.AccountLocked -> "Account locked"
            is AppException.DataException.DataNotFound -> "Data not found"
            is AppException.DataException.DataCorrupted -> "Data error"
            is AppException.DataException.StorageError -> "Storage error"
            is AppException.ValidationException.InvalidEmail -> "Invalid email"
            is AppException.ValidationException.WeakPassword -> "Weak password"
            is AppException.ValidationException.RequiredFieldMissing -> "Missing fields"
            is AppException.UnexpectedError -> "Unexpected error"
        }
    }
}
