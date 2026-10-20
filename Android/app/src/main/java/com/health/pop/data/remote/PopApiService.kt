package com.health.pop.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PopApiService {

    @POST("api/mobile/v1/assessment/submit")
    suspend fun submitAssessment(@Body submission: AssessmentSubmissionDto): Response<AssessmentResultDto>

    @GET("api/mobile/v1/assessment/history")
    suspend fun getAssessmentHistory(): Response<List<AssessmentResultDto>>

    @GET("api/mobile/v1/plan")
    suspend fun getPersonalPlan(): Response<PersonalPlanDto>

    @POST("api/mobile/v1/plan")
    suspend fun savePersonalPlan(@Body plan: PersonalPlanDto): Response<PersonalPlanDto>

    @POST("api/mobile/v1/focus/session")
    suspend fun submitFocusSession(@Body session: FocusSessionRequestDto): Response<Unit>

    @POST("api/mobile/v1/journal")
    suspend fun submitJournal(@Body journal: QuickJournalDto): Response<Unit>

    @GET("api/mobile/v1/journal/today")
    suspend fun getTodayJournal(@Query("date") date: String): Response<QuickJournalResponseDto>

    @GET("api/mobile/v1/progress/summary")
    suspend fun getProgressSummary(
        @Query("days") days: Int,
        @Query("endDate") endDate: String,
        @Query("timeZone") timeZone: String
    ): Response<ProgressSummaryDto>

    @POST("api/mobile/v1/ai/advice")
    suspend fun requestAIAdvice(@Body request: AIAdviceRequestDto): Response<AIAdviceResponseDto>
}
