package com.health.pop.data.repository

import com.health.pop.core.network.ApiResult
import com.health.pop.core.network.RetrofitClient
import com.health.pop.core.security.TokenManager
import com.health.pop.data.remote.FocusSessionRequestDto
import com.health.pop.data.remote.PopApiService
import com.health.pop.data.remote.apiErrorMessage
import com.health.pop.domain.model.FocusSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FocusRepository(private val tokenManager: TokenManager) {

    private val apiService: PopApiService by lazy {
        RetrofitClient.getClient(tokenManager).create(PopApiService::class.java)
    }
    private val _currentSession = MutableStateFlow(FocusSession())
    val currentSession: StateFlow<FocusSession> = _currentSession.asStateFlow()

    fun updateSession(session: FocusSession) {
        _currentSession.value = session
    }

    suspend fun syncSessionToServer(session: FocusSession): ApiResult<Unit> {
        return try {
            val dto = FocusSessionRequestDto(
                clientSessionId = session.id,
                taskName = session.taskName,
                targetMinutes = session.targetMinutes,
                actualMinutes = session.actualMinutes,
                interruptionCount = session.interruptionCount,
                interruptionReasons = session.interruptionReasons,
                selfRating = session.selfRating,
                status = session.state.name
            )
            val response = apiService.submitFocusSession(dto)
            if (response.isSuccessful) {
                markSyncStateIfCurrent(session, synced = true)
                ApiResult.Success(Unit)
            } else {
                markSyncStateIfCurrent(session, synced = false)
                ApiResult.Error(response.apiErrorMessage("Phiên chưa được đồng bộ với máy chủ."), response.code())
            }
        } catch (_: Exception) {
            markSyncStateIfCurrent(session, synced = false)
            ApiResult.Error("Không thể kết nối máy chủ. Phiên tập trung chưa được đồng bộ.")
        }
    }

    private fun markSyncStateIfCurrent(session: FocusSession, synced: Boolean) {
        val current = _currentSession.value
        if (current.id == session.id && current.copy(isSynced = false) == session.copy(isSynced = false)) {
            _currentSession.value = current.copy(isSynced = synced)
        }
    }
}
