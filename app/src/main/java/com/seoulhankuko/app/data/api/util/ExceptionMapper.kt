package com.seoulhankuko.app.data.api.util

import com.seoulhankuko.app.domain.exception.AppException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Utility class to map network exceptions to app-specific exceptions
 */
object ExceptionMapper {
    
    /**
     * Maps network exceptions to app-specific exceptions
     */
    fun mapToAppException(throwable: Throwable): AppException {
        return when (throwable) {
            is HttpException -> mapHttpException(throwable)
            is SocketTimeoutException -> AppException.NetworkException.RequestTimeout(throwable)
            is UnknownHostException -> AppException.NetworkException.HostNotFound(throwable)
            is IOException -> AppException.NetworkException.NoInternetConnection(throwable)
            is AppException -> throwable
            else -> AppException.UnexpectedError(
                "Unexpected error: ${throwable.message}", 
                throwable
            )
        }
    }
    
    /**
     * Maps HTTP exceptions to app-specific exceptions based on status codes
     */
    private fun mapHttpException(httpException: HttpException): AppException {
        return when (httpException.code()) {
            400 -> AppException.ValidationException.InvalidEmail(httpException)
            401 -> AppException.AuthException.InvalidCredentials(httpException)
            403 -> AppException.AuthException.AccountLocked(httpException)
            404 -> AppException.AuthException.UserNotFound(httpException)
            409 -> {
                // Check response body to determine if it's email or username conflict
                val errorBody = httpException.response()?.errorBody()?.string() ?: ""
                when {
                    errorBody.contains("email", ignoreCase = true) -> 
                        AppException.AuthException.EmailAlreadyExists(httpException)
                    errorBody.contains("username", ignoreCase = true) -> 
                        AppException.AuthException.UsernameAlreadyExists(httpException)
                    else -> AppException.UnexpectedError("Conflict error", httpException)
                }
            }
            422 -> AppException.ValidationException.WeakPassword(httpException)
            429 -> AppException.NetworkException.ServerUnavailable(httpException)
            500, 502, 503, 504 -> AppException.NetworkException.ServerUnavailable(httpException)
            else -> AppException.UnexpectedError(
                "HTTP error ${httpException.code()}: ${httpException.message()}", 
                httpException
            )
        }
    }
    
    /**
     * Maps HTTP status codes to user-friendly error messages
     */
    fun getErrorMessage(httpCode: Int): String {
        return when (httpCode) {
            400 -> "Invalid request data"
            401 -> "Invalid credentials"
            403 -> "Access denied"
            404 -> "Resource not found"
            409 -> "Resource already exists"
            422 -> "Invalid data format"
            429 -> "Too many requests - please try again later"
            500 -> "Server error - please try again"
            502, 503, 504 -> "Service temporarily unavailable"
            else -> "Unknown error occurred"
        }
    }
}
