package com.health.pop.data.repository

import com.health.pop.core.network.ApiResult
import com.health.pop.core.network.RetrofitClient
import com.health.pop.core.security.TokenManager
import com.health.pop.data.remote.AIAdviceRequestDto
import com.health.pop.data.remote.PopApiService
import com.health.pop.data.remote.apiErrorMessage
import com.health.pop.domain.model.AIAdvice
import com.health.pop.domain.model.PersonalPlan
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AIRepository(
    private val tokenManager: TokenManager,
    private val planRepository: PlanRepository
) {

    private val apiService: PopApiService by lazy {
        RetrofitClient.getClient(tokenManager).create(PopApiService::class.java)
    }

    suspend fun getAdvice(prompt: String, includeReflection: Boolean = false): ApiResult<AIAdvice> {
        return try {
            val now = Date()
            // Locale.US để định dạng dd/MM/yyyy luôn dùng chữ số Latin, đúng định dạng server yêu cầu.
            val reflectionDate = if (includeReflection) {
                SimpleDateFormat("dd/MM/yyyy", Locale.US).format(now)
            } else null
            val response = apiService.requestAIAdvice(
                AIAdviceRequestDto(
                    prompt = prompt,
                    includeReflection = includeReflection,
                    reflectionDate = reflectionDate,
                    timeZone = TimeZone.getDefault().id
                )
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val adviceText = body.adviceText.orEmpty().trim()
                val steps = body.actionableSteps.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }
                if (adviceText.isEmpty() && steps.isEmpty()) {
                    return ApiResult.Error("POP-AI chưa trả về nội dung. Vui lòng thử lại.")
                }
                ApiResult.Success(
                    AIAdvice(
                        id = "ai_${now.time}",
                        adviceText = adviceText,
                        actionableSteps = steps,
                        timestamp = "Hôm nay, " + SimpleDateFormat("HH:mm", Locale.US).format(now),
                        reflectionUsed = body.reflectionUsed
                    )
                )
            } else {
                ApiResult.Error(response.apiErrorMessage("Không thể nhận tư vấn từ POP-AI."), response.code())
            }
        } catch (e: CancellationException) {
            // Không nuốt huỷ coroutine (ví dụ người dùng rời màn hình).
            throw e
        } catch (_: Exception) {
            ApiResult.Error("Không thể kết nối dịch vụ AI của máy chủ. Câu hỏi chưa được gửi.")
        }
    }

    suspend fun addActionToPlan(actionStep: String): ApiResult<PersonalPlan> =
        planRepository.addStudyTaskAndSave(actionStep)
}
