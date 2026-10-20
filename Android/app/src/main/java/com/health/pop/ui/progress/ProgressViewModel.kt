package com.health.pop.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.pop.core.network.ApiResult
import com.health.pop.data.repository.ProgressRepository
import com.health.pop.domain.model.ProgressSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProgressViewModel(private val repository: ProgressRepository) : ViewModel() {

    private val _selectedDays = MutableStateFlow(7)
    val selectedDays: StateFlow<Int> = _selectedDays.asStateFlow()
    private val _summaryState = MutableStateFlow<ProgressSummary?>(null)
    val summaryState: StateFlow<ProgressSummary?> = _summaryState.asStateFlow()
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    init {
        loadProgress(7)
    }

    fun loadProgress(days: Int) {
        _selectedDays.value = days
        _errorState.value = null
        viewModelScope.launch {
            when (val result = repository.getProgressSummary(days)) {
                is ApiResult.Success -> _summaryState.value = result.data
                is ApiResult.Error -> {
                    _summaryState.value = null
                    _errorState.value = result.message
                }
                else -> {
                    _summaryState.value = null
                    _errorState.value = "Không thể tải tiến độ từ máy chủ."
                }
            }
        }
    }
}
