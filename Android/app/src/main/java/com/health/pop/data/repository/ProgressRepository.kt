package com.health.pop.data.repository

import com.health.pop.core.network.ApiResult
import com.health.pop.core.network.RetrofitClient
import com.health.pop.core.security.TokenManager
import com.health.pop.data.remote.PopApiService
import com.health.pop.data.remote.apiErrorMessage
import com.health.pop.domain.model.ProgressSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ProgressRepository(private val tokenManager: TokenManager) {

    private val apiService: PopApiService by lazy {
        RetrofitClient.getClient(tokenManager).create(PopApiService::class.java)
    }

    suspend fun getProgressSummary(daysRange: Int): ApiResult<ProgressSummary> {
        return try {
            val endDate = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
            val timeZone = TimeZone.getDefault().id
            val response = apiService.getProgressSummary(daysRange, endDate, timeZone)
            val dto = response.body()
            if (response.isSuccessful && dto != null) {
                ApiResult.Success(
                    ProgressSummary(
                        daysRange = dto.daysRange,
                        avgShortVideoMinutes = dto.avgShortVideoMinutes,
                        totalFocusMinutes = dto.totalFocusMinutes,
                        completedSessionsCount = dto.completedSessionsCount,
                        totalInterruptions = dto.totalInterruptions,
                        avgFocusRating = dto.avgFocusRating,
                        targetVideoLimitMinutes = dto.targetVideoLimitMinutes,
                        hasSavedPlan = dto.hasSavedPlan,
                        streaksDays = dto.streaksDays,
                        highlights = dto.highlights,
                        areasToAdjust = dto.areasToAdjust
                    )
                )
            } else {
                ApiResult.Error(response.apiErrorMessage("Không thể tải tiến độ từ máy chủ."), response.code())
            }
        } catch (_: Exception) {
            ApiResult.Error("Không thể kết nối máy chủ để tải tiến độ.")
        }
    }
}
