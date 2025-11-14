package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.api.model.MistakesListResponse
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.data.api.util.ExceptionMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MistakesRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getUserMistakes(
        username: String,
        page: Int = 1,
        itemsPerPage: Int = 20,
        questionType: String? = null,
        token: String? = null
    ): Result<MistakesListResponse> {
        return try {
            val response = apiService.getUserMistakes(
                username = username,
                page = page,
                itemsPerPage = itemsPerPage,
                questionType = questionType,
                token = token
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get mistakes"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
}

