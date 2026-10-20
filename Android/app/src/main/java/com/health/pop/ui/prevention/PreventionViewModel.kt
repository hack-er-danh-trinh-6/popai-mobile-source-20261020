package com.health.pop.ui.prevention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.pop.core.network.ApiResult
import com.health.pop.data.repository.AssessmentRepository
import com.health.pop.domain.model.AssessmentQuestion
import com.health.pop.domain.model.AssessmentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AssessmentSubmissionState {
    object Idle : AssessmentSubmissionState()
    object Submitting : AssessmentSubmissionState()
    data class Success(val result: AssessmentResult, val isDemoData: Boolean) : AssessmentSubmissionState()
    data class Error(val message: String) : AssessmentSubmissionState()
}

class PreventionViewModel(private val repository: AssessmentRepository) : ViewModel() {

    val questions: List<AssessmentQuestion> = repository.getQuestions()
    private val _answers = MutableStateFlow<MutableMap<String, Int>>(mutableMapOf())
    val answers: StateFlow<Map<String, Int>> = _answers.asStateFlow()
    private val _submissionState = MutableStateFlow<AssessmentSubmissionState>(
        repository.latestSubmission.value?.let { AssessmentSubmissionState.Success(it, false) }
            ?: AssessmentSubmissionState.Idle
    )
    val submissionState: StateFlow<AssessmentSubmissionState> = _submissionState.asStateFlow()
    private val _historyState = MutableStateFlow<List<AssessmentResult>>(emptyList())
    val historyState: StateFlow<List<AssessmentResult>> = _historyState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            when (val result = repository.getAssessmentHistory()) {
                is ApiResult.Success -> _historyState.value = result.data
                else -> Unit
            }
        }
    }

    fun setAnswer(questionId: String, score: Int) {
        val updated = _answers.value.toMutableMap()
        updated[questionId] = score
        _answers.value = updated
    }

    fun submitAssessment() {
        if (_answers.value.size < questions.size) {
            _submissionState.value = AssessmentSubmissionState.Error("Vui lòng trả lời đủ 20 câu hỏi trước khi gửi.")
            return
        }
        _submissionState.value = AssessmentSubmissionState.Submitting
        viewModelScope.launch {
            when (val result = repository.submitAssessment(_answers.value)) {
                is ApiResult.Success -> {
                    _submissionState.value = AssessmentSubmissionState.Success(result.data, result.isDemoData)
                    loadHistory()
                }
                is ApiResult.Error -> _submissionState.value = AssessmentSubmissionState.Error(result.message)
                else -> _submissionState.value = AssessmentSubmissionState.Error("Máy chủ chưa cấu hình.")
            }
        }
    }

    fun markSubmissionHandled() {
        _submissionState.value = AssessmentSubmissionState.Idle
    }

    fun resetSubmissionState() {
        _submissionState.value = AssessmentSubmissionState.Idle
        _answers.value = mutableMapOf()
        repository.clearLatestSubmission()
    }
}
