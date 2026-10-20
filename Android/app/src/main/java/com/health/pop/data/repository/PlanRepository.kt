package com.health.pop.data.repository

import com.health.pop.core.network.ApiResult
import com.health.pop.core.network.RetrofitClient
import com.health.pop.core.security.TokenManager
import com.health.pop.data.remote.PersonalPlanDto
import com.health.pop.data.remote.PopApiService
import com.health.pop.data.remote.apiErrorMessage
import com.health.pop.domain.model.PersonalPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlanRepository(private val tokenManager: TokenManager) {

    private val apiService: PopApiService by lazy {
        RetrofitClient.getClient(tokenManager).create(PopApiService::class.java)
    }
    private val _currentPlan = MutableStateFlow(PersonalPlan())
    val currentPlan: StateFlow<PersonalPlan> = _currentPlan.asStateFlow()

    /** Xoá kế hoạch đang giữ trong bộ nhớ (khi đăng xuất / đổi tài khoản). */
    fun reset() {
        _currentPlan.value = PersonalPlan()
    }

    suspend fun fetchPlan(): ApiResult<PersonalPlan> {
        return try {
            val response = apiService.getPersonalPlan()
            val dto = response.body()
            if (response.isSuccessful && dto != null) {
                val plan = dto.toDomain()
                _currentPlan.value = plan
                ApiResult.Success(plan)
            } else if (response.code() == 404) {
                val empty = PersonalPlan()
                _currentPlan.value = empty
                ApiResult.Success(empty)
            } else {
                ApiResult.Error(response.apiErrorMessage("Không thể tải kế hoạch từ máy chủ."), response.code())
            }
        } catch (_: Exception) {
            ApiResult.Error("Không thể kết nối máy chủ để tải kế hoạch.")
        }
    }

    suspend fun savePlan(plan: PersonalPlan): ApiResult<PersonalPlan> {
        return try {
            val response = apiService.savePersonalPlan(plan.toDto())
            val dto = response.body()
            if (response.isSuccessful && dto != null) {
                val saved = dto.toDomain()
                _currentPlan.value = saved
                ApiResult.Success(saved)
            } else {
                ApiResult.Error(response.apiErrorMessage("Lưu kế hoạch thất bại trên máy chủ."), response.code())
            }
        } catch (_: Exception) {
            ApiResult.Error("Không thể kết nối máy chủ. Kế hoạch chưa được lưu.")
        }
    }

    suspend fun addStudyTaskAndSave(task: String): ApiResult<PersonalPlan> {
        val normalized = task.trim()
        if (normalized.isBlank()) return ApiResult.Error("Nhiệm vụ không được để trống.")
        val loaded = fetchPlan()
        if (loaded !is ApiResult.Success) return loaded
        val current = loaded.data
        if (current.studyTasks.any { it.equals(normalized, ignoreCase = true) }) return ApiResult.Success(current)
        return savePlan(current.copy(studyTasks = current.studyTasks + normalized))
    }

    private fun PersonalPlanDto.toDomain() = PersonalPlan(
        dailyShortVideoLimitMinutes = dailyShortVideoLimitMinutes,
        trackedPlatforms = trackedPlatforms,
        focusProtectionHours = focusProtectionHours,
        studyTasks = studyTasks,
        sessionDurationMinutes = sessionDurationMinutes,
        targetDays = targetDays,
        hasSavedPlan = true
    )

    private fun PersonalPlan.toDto() = PersonalPlanDto(
        dailyShortVideoLimitMinutes = dailyShortVideoLimitMinutes,
        trackedPlatforms = trackedPlatforms,
        focusProtectionHours = focusProtectionHours,
        studyTasks = studyTasks,
        sessionDurationMinutes = sessionDurationMinutes,
        targetDays = targetDays
    )
}
