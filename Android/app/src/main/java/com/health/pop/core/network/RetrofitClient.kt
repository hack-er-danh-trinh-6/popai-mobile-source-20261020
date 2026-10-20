package com.health.pop.core.network

import com.health.pop.BuildConfig
import com.health.pop.core.security.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    @Volatile
    private var retrofit: Retrofit? = null

    fun getClient(tokenManager: TokenManager): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: buildClient(tokenManager).also { retrofit = it }
        }
    }

    private fun buildClient(tokenManager: TokenManager): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            // BASIC logs never include request bodies containing passwords or journal entries.
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(logging)
            .authenticator(SessionAuthenticator(tokenManager))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(55, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
