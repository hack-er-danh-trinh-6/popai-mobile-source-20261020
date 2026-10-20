package com.health.pop.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.pop.core.network.ApiResult
import com.health.pop.data.repository.AuthRepository
import com.health.pop.data.repository.JournalRepository
import com.health.pop.data.repository.PlanRepository
import com.health.pop.data.repository.ProgressRepository
import com.health.pop.domain.model.PersonalPlan
import com.health.pop.domain.model.QuickJournal
import com.health.pop.domain.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    /** Chỉ true khi chưa từng tải thành công, để màn hình không nhấp nháy mỗi lần làm mới ngầm. */
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** Tải kế hoạch lỗi và chưa có dữ liệu kế hoạch nào: không được hiển thị như "chưa lưu kế hoạch". */
    val planUnavailable: Boolean = false,
    /** Tổng số phút của các phiên tập trung đã hoàn thành hôm nay (lấy từ máy chủ). */
    val sessionFocusMinutes: Int = 0
)

class HomeViewModel(
    authRepository: AuthRepository,
    private val planRepository: PlanRepository,
    private val journalRepository: JournalRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
    val currentPlan: StateFlow<PersonalPlan> = planRepository.currentPlan
    val todayJournal: StateFlow<QuickJournal> = journalRepository.todayJournal

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var planLoaded = false
    private var loadedCleanly = false

    init {
        refresh()
    }

    /**
     * Tải lại dữ liệu hôm nay. Được gọi khi mở Home, khi quay lại Home (sau phiên tập trung, lưu nhật ký,
     * lưu kế hoạch...), khi app trở lại foreground và khi người dùng bấm "Thử lại".
     * Bỏ qua nếu một lượt tải đang chạy để tránh gọi trùng.
     */
    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = !loadedCleanly) }
            coroutineScope {
                // Ba request độc lập nên chạy song song thay vì nối tiếp.
                val planDeferred = async { planRepository.fetchPlan() }
                val journalDeferred = async { journalRepository.fetchTodayJournal() }
                val progressDeferred = async { progressRepository.getProgressSummary(1) }

                val errors = mutableListOf<String>()
                var sessionMinutes: Int? = null

                val planResult = planDeferred.await()
                when (planResult) {
                    is ApiResult.Success -> planLoaded = true
                    is ApiResult.Error -> errors += planResult.message
                    else -> Unit
                }
                val journalResult = journalDeferred.await()
                if (journalResult is ApiResult.Error) errors += journalResult.message

                val progressResult = progressDeferred.await()
                when (progressResult) {
                    is ApiResult.Success -> sessionMinutes = progressResult.data.totalFocusMinutes
                    is ApiResult.Error -> errors += progressResult.message
                    else -> Unit
                }

                loadedCleanly = errors.isEmpty()
                val planUnavailable = planResult is ApiResult.Error &&
                    !planLoaded &&
                    !planRepository.currentPlan.value.hasSavedPlan

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = when (errors.size) {
                            0 -> null
                            1 -> errors.first()
                            else -> "Không thể tải đầy đủ dữ liệu hôm nay. Vui lòng kiểm tra kết nối và thử lại."
                        },
                        planUnavailable = planUnavailable,
                        // Giữ giá trị cũ nếu lần tải này lỗi, tránh tụt về 0 phút.
                        sessionFocusMinutes = sessionMinutes ?: it.sessionFocusMinutes
                    )
                }
            }
        }
    }
}
