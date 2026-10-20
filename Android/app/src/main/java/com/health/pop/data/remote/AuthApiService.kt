package com.health.pop.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/mobile/v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<AuthResponseDto>

    @POST("api/mobile/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @GET("api/mobile/v1/auth/me")
    suspend fun getCurrentUser(): Response<UserDto>

    @POST("api/mobile/v1/auth/logout")
    suspend fun logout(): Response<Unit>
}
