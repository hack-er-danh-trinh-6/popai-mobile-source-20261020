package com.health.pop.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.pop.core.network.ApiResult
import com.health.pop.data.repository.JournalRepository
import com.health.pop.domain.model.QuickJournal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class JournalSaveState {
    object Idle : JournalSaveState()
    object Saving : JournalSaveState()
    data class Success(val isDemoData: Boolean) : JournalSaveState()
    data class Error(val message: String) : JournalSaveState()
}

class JournalViewModel(private val repository: JournalRepository) : ViewModel() {

    val todayJournal: StateFlow<QuickJournal> = repository.todayJournal
    private val _saveState = MutableStateFlow<JournalSaveState>(JournalSaveState.Idle)
    val saveState: StateFlow<JournalSaveState> = _saveState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = repository.fetchTodayJournal()) {
                is ApiResult.Error -> _saveState.value = JournalSaveState.Error(result.message)
                else -> Unit
            }
        }
    }

    fun saveJournal(journal: QuickJournal) {
        _saveState.value = JournalSaveState.Saving
        viewModelScope.launch {
            when (val result = repository.saveJournal(journal)) {
                is ApiResult.Success -> _saveState.value = JournalSaveState.Success(false)
                is ApiResult.Error -> _saveState.value = JournalSaveState.Error(result.message)
                else -> _saveState.value = JournalSaveState.Error("Máy chủ chưa sẵn sàng.")
            }
        }
    }
}
