package com.health.pop.ui.classroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.pop.core.network.ApiResult
import com.health.pop.data.repository.ClassroomRepository
import com.health.pop.domain.model.ClassInbox
import com.health.pop.domain.model.ClassMembership
import com.health.pop.domain.model.ClassOption
import com.health.pop.domain.model.MembershipStatus
import com.health.pop.domain.model.School
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClassroomUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** true khi đã biết chắc trạng thái lớp của học sinh từ máy chủ (kể cả "chưa vào lớp nào"). */
    val membershipLoaded: Boolean = false,
    val membership: ClassMembership = ClassMembership(),
    val inbox: ClassInbox? = null,
    val schoolQuery: String = "",
    val schools: List<School> = emptyList(),
    val isLoadingSchools: Boolean = false,
    /** Lỗi khi tải danh sách trường; khác với "danh sách trống" (chưa có giáo viên khai báo). */
    val schoolsError: String? = null,
    val selectedSchool: School? = null,
    val classes: List<ClassOption> = emptyList(),
    val isLoadingClasses: Boolean = false,
    val classesError: String? = null,
    val isSubmitting: Boolean = false
)

class ClassroomViewModel(private val repository: ClassroomRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ClassroomUiState())
    val uiState: StateFlow<ClassroomUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var refreshJob: Job? = null

    init {
        refresh()
        searchSchools("", immediate = true)
    }

    /** Tải trạng thái lớp và hộp thư. [silent] = true khi làm mới ngầm để không nhấp nháy thanh tải. */
    fun refresh(silent: Boolean = false) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            if (!silent) _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val membership = repository.getMembership()) {
                is ApiResult.Success -> {
                    var inbox: ClassInbox? = null
                    if (membership.data.status == MembershipStatus.APPROVED) {
                        val inboxResult = repository.getInbox()
                        if (inboxResult is ApiResult.Success) inbox = inboxResult.data
                    }
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = null, membershipLoaded = true, membership = membership.data, inbox = inbox)
                    }
                    // Danh sách đã được hiển thị với nhãn "Mới"; đánh dấu đã đọc ở máy chủ để lần sau không còn là mới.
                    if (inbox != null && inbox.unreadCount > 0) repository.markAllRead()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = membership.message) }
                else -> _uiState.update { it.copy(isLoading = false, errorMessage = "Không thể tải thông tin lớp học.") }
            }
        }
    }

    fun onSchoolQueryChange(query: String) {
        _uiState.update { it.copy(schoolQuery = query) }
        searchSchools(query, immediate = false)
    }

    private fun searchSchools(query: String, immediate: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (!immediate) delay(300)
            _uiState.update { it.copy(isLoadingSchools = true, schoolsError = null) }
            when (val result = repository.searchSchools(query)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isLoadingSchools = false, schools = result.data, schoolsError = null)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isLoadingSchools = false, schools = emptyList(), schoolsError = result.message)
                }
                else -> _uiState.update { it.copy(isLoadingSchools = false) }
            }
        }
    }

    fun selectSchool(school: School) {
        _uiState.update {
            it.copy(selectedSchool = school, classes = emptyList(), isLoadingClasses = true, classesError = null, errorMessage = null)
        }
        viewModelScope.launch {
            when (val result = repository.getClasses(school.id)) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoadingClasses = false, classes = result.data) }
                is ApiResult.Error -> _uiState.update { it.copy(isLoadingClasses = false, classesError = result.message) }
                else -> _uiState.update { it.copy(isLoadingClasses = false) }
            }
        }
    }

    fun clearSchool() {
        _uiState.update { it.copy(selectedSchool = null, classes = emptyList(), classesError = null) }
    }

    /** Thử lại toàn bộ khi tải lỗi: trạng thái lớp của học sinh và danh sách trường. */
    fun retry() {
        refresh()
        searchSchools(_uiState.value.schoolQuery, immediate = true)
    }

    fun retrySchools() {
        searchSchools(_uiState.value.schoolQuery, immediate = true)
    }

    fun retryClasses() {
        _uiState.value.selectedSchool?.let { selectSchool(it) }
    }

    fun requestJoin(option: ClassOption) {
        if (_uiState.value.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.requestJoin(option.id)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isSubmitting = false, membershipLoaded = true, membership = result.data, selectedSchool = null, classes = emptyList())
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                else -> _uiState.update { it.copy(isSubmitting = false, errorMessage = "Không thể gửi yêu cầu vào lớp.") }
            }
        }
    }

    /** Huỷ yêu cầu đang chờ, rời lớp, hoặc đóng thông báo bị từ chối. */
    fun leaveClass() {
        if (_uiState.value.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.leaveClass()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isSubmitting = false, membershipLoaded = true, membership = ClassMembership(), inbox = null)
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                else -> _uiState.update { it.copy(isSubmitting = false, errorMessage = "Không thể thực hiện thao tác này.") }
            }
        }
    }
}
