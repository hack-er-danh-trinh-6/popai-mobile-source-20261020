package com.health.pop.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.pop.core.network.ApiResult
import com.health.pop.data.repository.AIRepository
import com.health.pop.domain.model.AIAdvice
import com.health.pop.domain.usecase.ValidateAiPromptUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AiState {
    object Idle : AiState()
    object Loading : AiState()
    data class Success(
        val advice: AIAdvice,
        val isDemoData: Boolean,
        // Người dùng có bật đính kèm nhật ký cho lần hỏi này hay không (để báo nếu hôm nay chưa có nhật ký).
        val reflectionRequested: Boolean = false
    ) : AiState()
    data class Error(val message: String) : AiState()
}

class AIViewModel(
    private val repository: AIRepository,
    private val promptValidator: ValidateAiPromptUseCase = ValidateAiPromptUseCase()
) : ViewModel() {

    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()
    private val _planActionMessage = MutableStateFlow<String?>(null)
    val planActionMessage: StateFlow<String?> = _planActionMessage.asStateFlow()

    // Các bước đã lưu vào kế hoạch và bước đang được lưu (để hiện trạng thái trên nút "Thêm").
    private val _addedSteps = MutableStateFlow<Set<String>>(emptySet())
    val addedSteps: StateFlow<Set<String>> = _addedSteps.asStateFlow()
    private val _savingStep = MutableStateFlow<String?>(null)
    val savingStep: StateFlow<String?> = _savingStep.asStateFlow()

    var promptInput = MutableStateFlow("")
    var includeReflection = MutableStateFlow(false)

    fun onPromptChange(value: String) {
        promptInput.value = promptValidator.limit(value)
    }

    fun requestAdvice() {
        // Chặn gửi trùng khi đang chờ phản hồi.
        if (_aiState.value is AiState.Loading) return
        val input = promptValidator.resolvePrompt(promptInput.value)
        val reflectionRequested = includeReflection.value
        _planActionMessage.value = null
        _addedSteps.value = emptySet()
        _aiState.value = AiState.Loading
        viewModelScope.launch {
            when (val result = repository.getAdvice(input, reflectionRequested)) {
                is ApiResult.Success -> _aiState.value =
                    AiState.Success(result.data, result.isDemoData, reflectionRequested)
                is ApiResult.Error -> _aiState.value = AiState.Error(result.message)
                is ApiResult.ServerUnconfigured -> _aiState.value = AiState.Error("Máy chủ chưa sẵn sàng.")
                is ApiResult.NetworkError -> _aiState.value =
                    AiState.Error("Không thể kết nối mạng. Vui lòng kiểm tra kết nối và thử lại.")
            }
        }
    }

    fun addStepToPlan(step: String) {
        if (step in _addedSteps.value || _savingStep.value != null) return
        _planActionMessage.value = null
        _savingStep.value = step
        viewModelScope.launch {
            try {
                val message = when (val result = repository.addActionToPlan(step)) {
                    is ApiResult.Success -> {
                        _addedSteps.value = _addedSteps.value + step
                        "Đã lưu vào kế hoạch cá nhân."
                    }
                    is ApiResult.Error -> result.message
                    else -> "Không thể lưu nhiệm vụ vào kế hoạch."
                }
                _planActionMessage.value = message
            } finally {
                _savingStep.value = null
            }
        }
    }
}
