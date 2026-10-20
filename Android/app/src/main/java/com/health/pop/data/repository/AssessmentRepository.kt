package com.health.pop.data.repository

import com.health.pop.core.network.ApiResult
import com.health.pop.core.network.RetrofitClient
import com.health.pop.core.security.TokenManager
import com.health.pop.data.local.MockDataProvider
import com.health.pop.data.remote.AssessmentAnswerDto
import com.health.pop.data.remote.AssessmentSubmissionDto
import com.health.pop.data.remote.PopApiService
import com.health.pop.data.remote.apiErrorMessage
import com.health.pop.domain.model.AssessmentQuestion
import com.health.pop.domain.model.AssessmentResult
import com.health.pop.domain.usecase.CalculateAssessmentScoresUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AssessmentRepository(private val tokenManager: TokenManager) {

    private val apiService: PopApiService by lazy {
        RetrofitClient.getClient(tokenManager).create(PopApiService::class.java)
    }
    private val calculateScoresUseCase = CalculateAssessmentScoresUseCase()
    private val _latestSubmission = MutableStateFlow<AssessmentResult?>(null)
    val latestSubmission: StateFlow<AssessmentResult?> = _latestSubmission.asStateFlow()

    fun getQuestions(): List<AssessmentQuestion> = MockDataProvider.assessmentQuestions

    fun clearLatestSubmission() {
        _latestSubmission.value = null
    }

    suspend fun submitAssessment(answers: Map<String, Int>): ApiResult<AssessmentResult> {
        val calculatedResult = calculateScoresUseCase(answers)
        return try {
            val dtoList = answers.map { AssessmentAnswerDto(it.key, it.value) }
            val response = apiService.submitAssessment(AssessmentSubmissionDto(dtoList))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val result = AssessmentResult(
                    id = body.id,
                    completedAt = body.completedAt,
                    vnScore = body.scores["VN"] ?: calculatedResult.vnScore,
                    ptScore = body.scores["PT"] ?: calculatedResult.ptScore,
                    pbScore = body.scores["PB"] ?: calculatedResult.pbScore,
                    ttScore = body.scores["TT"] ?: calculatedResult.ttScore,
                    note = body.note
                )
                _latestSubmission.value = result
                ApiResult.Success(result)
            } else {
                ApiResult.Error(response.apiErrorMessage("Không thể gửi bài đánh giá đến máy chủ."), response.code())
            }
        } catch (_: Exception) {
            ApiResult.Error("Không thể kết nối máy chủ. Bài đánh giá chưa được lưu.")
        }
    }

    suspend fun getAssessmentHistory(): ApiResult<List<AssessmentResult>> {
        return try {
            val response = apiService.getAssessmentHistory()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val list = body.map { dto ->
                    AssessmentResult(
                        id = dto.id,
                        completedAt = dto.completedAt,
                        vnScore = dto.scores["VN"] ?: 0f,
                        ptScore = dto.scores["PT"] ?: 0f,
                        pbScore = dto.scores["PB"] ?: 0f,
                        ttScore = dto.scores["TT"] ?: 0f,
                        note = dto.note
                    )
                }
                ApiResult.Success(list)
            } else {
                ApiResult.Error(response.apiErrorMessage("Không thể tải lịch sử đánh giá."), response.code())
            }
        } catch (_: Exception) {
            ApiResult.Error("Không thể kết nối máy chủ để tải lịch sử đánh giá.")
        }
    }
}
