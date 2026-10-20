package com.health.pop.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.pop.core.network.ApiResult
import com.health.pop.data.repository.PlanRepository
import com.health.pop.domain.model.PersonalPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PlanSaveState {
    object Idle : PlanSaveState()
    object Saving : PlanSaveState()
    data class Success(val plan: PersonalPlan, val isDemoData: Boolean) : PlanSaveState()
    data class Error(val message: String) : PlanSaveState()
}

class PlanViewModel(private val repository: PlanRepository) : ViewModel() {

    val currentPlan: StateFlow<PersonalPlan> = repository.currentPlan
    private val _saveState = MutableStateFlow<PlanSaveState>(PlanSaveState.Idle)
    val saveState: StateFlow<PlanSaveState> = _saveState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = repository.fetchPlan()) {
                is ApiResult.Error -> _saveState.value = PlanSaveState.Error(result.message)
                else -> Unit
            }
        }
    }

    fun updatePlan(
        dailyLimit: Int,
        protectionHours: String,
        sessionDuration: Int,
        targetDays: Int,
        studyTasks: List<String>
    ) {
        val newPlan = repository.currentPlan.value.copy(
            dailyShortVideoLimitMinutes = dailyLimit,
            focusProtectionHours = protectionHours,
            sessionDurationMinutes = sessionDuration,
            targetDays = targetDays,
            studyTasks = studyTasks
        )
        _saveState.value = PlanSaveState.Saving
        viewModelScope.launch {
            when (val result = repository.savePlan(newPlan)) {
                is ApiResult.Success -> _saveState.value = PlanSaveState.Success(result.data, false)
                is ApiResult.Error -> _saveState.value = PlanSaveState.Error(result.message)
                else -> _saveState.value = PlanSaveState.Error("Máy chủ chưa sẵn sàng.")
            }
        }
    }
}
