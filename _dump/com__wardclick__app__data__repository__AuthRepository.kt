package com.wardclick.app.data.repository

import com.wardclick.app.data.local.SessionManager
import com.wardclick.app.data.model.AuthResponse
import com.wardclick.app.data.model.LoginRequest
import com.wardclick.app.data.model.RegisterRequest
import com.wardclick.app.data.remote.ApiService

class AuthRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {
    suspend fun login(request: LoginRequest): ApiResult<AuthResponse> {
        val result = safeApiCall { apiService.login(request) }
        if (result is ApiResult.Success) {
            val response = result.data
            sessionManager.saveSession(
                response.token,
                response.userId,
                response.email,
                response.fullName,
                response.role
            )
        }
        return result
    }

    suspend fun register(request: RegisterRequest): ApiResult<AuthResponse> {
        val result = safeApiCall { apiService.register(request) }
        if (result is ApiResult.Success) {
            val response = result.data
            sessionManager.saveSession(
                response.token,
                response.userId,
                response.email,
                response.fullName,
                response.role
            )
        }
        return result
    }

    fun logout() {
        sessionManager.clear()
    }
}
