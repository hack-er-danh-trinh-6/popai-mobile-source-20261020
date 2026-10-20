package com.health.pop.core.network

import com.google.gson.Gson
import com.health.pop.BuildConfig
import com.health.pop.core.security.TokenManager
import com.health.pop.data.remote.AuthResponseDto
import com.health.pop.data.remote.RefreshRequestDto
import com.health.pop.domain.model.User
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit

class SessionAuthenticator(private val tokenManager: TokenManager) : Authenticator {

    private val gson = Gson()
    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) > 1) return null
        val originalRequest = response.request
        val sentAccessToken = originalRequest.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.takeIf { it.isNotBlank() }
            ?: return null

        synchronized(this) {
            val currentAccessToken = tokenManager.getAccessToken()
            if (!currentAccessToken.isNullOrBlank() && currentAccessToken != sentAccessToken) {
                return originalRequest.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            val refreshToken = tokenManager.getRefreshToken()?.takeIf { it.isNotBlank() } ?: return null
            val json = gson.toJson(RefreshRequestDto(refreshToken))
            val refreshRequest = Request.Builder()
                .url(BuildConfig.BASE_URL + "api/mobile/v1/auth/refresh")
                .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            return try {
                refreshClient.newCall(refreshRequest).execute().use { refreshResponse ->
                    if (!refreshResponse.isSuccessful) {
                        if (refreshResponse.code == 401) tokenManager.clear()
                        return null
                    }
                    val responseBody = refreshResponse.body?.string() ?: return null
                    val auth = runCatching { gson.fromJson(responseBody, AuthResponseDto::class.java) }.getOrNull()
                        ?: return null
                    if (auth.accessToken.isBlank() || auth.refreshToken.isBlank()) return null
                    tokenManager.saveTokens(auth.accessToken, auth.refreshToken)
                    tokenManager.saveUser(User(auth.user.id, auth.user.email, auth.user.fullName, auth.user.role))
                    originalRequest.newBuilder()
                        .header("Authorization", "Bearer ${auth.accessToken}")
                        .build()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count += 1
            prior = prior.priorResponse
        }
        return count
    }
}
