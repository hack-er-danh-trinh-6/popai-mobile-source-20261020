package com.health.pop.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ClassroomApiService {

    @GET("api/mobile/v1/schools")
    suspend fun getSchools(@Query("q") query: String): Response<List<SchoolDto>>

    @GET("api/mobile/v1/schools/{schoolId}/classes")
    suspend fun getClasses(@Path("schoolId") schoolId: String): Response<List<ClassOptionDto>>

    @GET("api/mobile/v1/class/membership")
    suspend fun getMembership(): Response<ClassMembershipDto>

    @POST("api/mobile/v1/class/join-request")
    suspend fun requestJoin(@Body request: JoinClassRequestDto): Response<ClassMembershipDto>

    @DELETE("api/mobile/v1/class/membership")
    suspend fun leaveClass(): Response<Unit>

    @GET("api/mobile/v1/notifications")
    suspend fun getNotifications(): Response<ClassInboxDto>

    @POST("api/mobile/v1/notifications/deliver")
    suspend fun deliverNotifications(): Response<List<ClassNotificationDto>>

    @POST("api/mobile/v1/notifications/read")
    suspend fun markRead(@Body request: MarkReadRequestDto): Response<Unit>
}
