package com.wardclick.app.data.remote

import android.content.Context
import android.content.Intent
import com.wardclick.app.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val sessionManager: SessionManager,
    private val context: Context
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionManager.getToken()
        val originalRequest = chain.request()

        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        val response = chain.proceed(request)

        if (response.code == 401 && !token.isNullOrEmpty()) {
            sessionManager.clear()
            val intent = Intent("com.wardclick.app.ACTION_LOGOUT")
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
        }

        return response
    }
}
