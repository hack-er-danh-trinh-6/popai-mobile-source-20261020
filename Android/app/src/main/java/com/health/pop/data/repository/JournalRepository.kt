package com.health.pop.data.repository

import com.health.pop.core.network.ApiResult
import com.health.pop.core.network.RetrofitClient
import com.health.pop.core.security.TokenManager
import com.health.pop.data.remote.PopApiService
import com.health.pop.data.remote.QuickJournalDto
import com.health.pop.data.remote.apiErrorMessage
import com.health.pop.domain.model.QuickJournal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JournalRepository(private val tokenManager: TokenManager) {

    private val apiService: PopApiService by lazy {
        RetrofitClient.getClient(tokenManager).create(PopApiService::class.java)
    }

    private val _todayJournal = MutableStateFlow(QuickJournal(date = getTodayDateString()))
    val todayJournal: StateFlow<QuickJournal> = _todayJournal.asStateFlow()

    // Locale.US để luôn ra chữ số Latin: máy dùng chữ số khác (Ả Rập, Hindi...) sẽ bị server từ chối ngày không hợp lệ.
    fun getTodayDateString(): String = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())

    /** Xoá nhật ký đang giữ trong bộ nhớ (khi đăng xuất / đổi tài khoản). */
    fun reset() {
        _todayJournal.value = QuickJournal(date = getTodayDateString())
    }

    suspend fun fetchTodayJournal(): ApiResult<QuickJournal?> {
        return try {
            val date = getTodayDateString()
            val response = apiService.getTodayJournal(date)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val journal = QuickJournal(
                    id = body.id,
                    date = body.date,
                    estimatedShortVideoMinutes = body.estimatedShortVideoMinutes,
                    focusMinutes = body.focusMinutes,
                    interruptionCount = body.interruptionCount,
                    distractors = body.distractors,
                    mostProductiveSession = body.mostProductiveSession,
                    tomorrowAdjustment = body.tomorrowAdjustment
                )
                _todayJournal.value = journal
                ApiResult.Success(journal)
            } else if (response.code() == 404) {
                val empty = QuickJournal(date = date)
                _todayJournal.value = empty
                ApiResult.Success(null)
            } else {
                ApiResult.Error(response.apiErrorMessage("Không thể tải nhật ký hôm nay."), response.code())
            }
        } catch (_: Exception) {
            ApiResult.Error("Không thể kết nối máy chủ để tải nhật ký hôm nay.")
        }
    }

    suspend fun saveJournal(journal: QuickJournal): ApiResult<Unit> {
        val updatedJournal = journal.copy(date = getTodayDateString())
        return try {
            val dto = QuickJournalDto(
                date = updatedJournal.date,
                estimatedShortVideoMinutes = updatedJournal.estimatedShortVideoMinutes,
                focusMinutes = updatedJournal.focusMinutes,
                interruptionCount = updatedJournal.interruptionCount,
                distractors = updatedJournal.distractors,
                mostProductiveSession = updatedJournal.mostProductiveSession,
                tomorrowAdjustment = updatedJournal.tomorrowAdjustment
            )
            val response = apiService.submitJournal(dto)
            if (response.isSuccessful) {
                _todayJournal.value = updatedJournal
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(response.apiErrorMessage("Lưu nhật ký thất bại trên máy chủ."), response.code())
            }
        } catch (_: Exception) {
            ApiResult.Error("Không thể kết nối máy chủ. Nhật ký chưa được lưu.")
        }
    }
}
