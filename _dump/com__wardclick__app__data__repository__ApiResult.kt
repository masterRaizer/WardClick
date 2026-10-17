package com.wardclick.app.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.io.IOException

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    data class Exception(val e: Throwable, val message: String) : ApiResult<Nothing>()
}

suspend fun <T> safeApiCall(
    timeoutMs: Long = 10000L,
    apiCall: suspend () -> T
): ApiResult<T> {
    return try {
        withTimeout(timeoutMs) {
            val response = apiCall()
            ApiResult.Success(response)
        }
    } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) {
            ApiResult.Exception(e, "Время ожидания истекло")
        } else {
            throw e
        }
    } catch (e: HttpException) {
        val errorMessage = when (e.code()) {
            400 -> "Неверный запрос"
            401 -> "Необходима авторизация"
            403 -> "Доступ запрещен"
            404 -> "Ресурс не найден"
            409 -> "Конфликт данных"
            in 500..599 -> "Ошибка сервера"
            else -> "Неизвестная ошибка сети"
        }
        ApiResult.Error(e.code(), errorMessage)
    } catch (e: IOException) {
        ApiResult.Exception(e, "Отсутствует подключение к интернету")
    } catch (e: Exception) {
        ApiResult.Exception(e, "Произошла неизвестная ошибка")
    }
}
