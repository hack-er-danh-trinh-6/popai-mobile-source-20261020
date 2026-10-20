package com.health.pop.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.pop.core.network.ApiResult
import com.health.pop.data.repository.FocusRepository
import com.health.pop.domain.model.FocusSession
import com.health.pop.domain.model.FocusSessionState
import com.health.pop.domain.usecase.FormatDurationUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class FocusViewModel(
    private val repository: FocusRepository,
    private val defaultTargetMinutes: Int = 25,
    private val defaultTaskName: String = ""
) : ViewModel() {

    val currentSession: StateFlow<FocusSession> = repository.currentSession
    val formatUseCase = FormatDurationUseCase()
    private val _remainingSeconds = MutableStateFlow(25 * 60)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()
    private val syncMutex = Mutex()
    private var timerJob: Job? = null

    init {
        val existing = repository.currentSession.value
        when {
            existing.id.isBlank() || existing.state == FocusSessionState.COMPLETED -> {
                initSession(defaultTaskName, defaultTargetMinutes)
            }
            else -> {
                if (existing.state == FocusSessionState.RUNNING) {
                    val paused = existing.copy(state = FocusSessionState.PAUSED)
                    repository.updateSession(paused)
                    syncSession(paused)
                }
                _remainingSeconds.value =
                    ((existing.targetMinutes - existing.actualMinutes).coerceAtLeast(0) * 60)
            }
        }
    }

    fun initSession(taskName: String = defaultTaskName, targetMinutes: Int = defaultTargetMinutes) {
        val safeTargetMinutes = targetMinutes.coerceIn(1, 600)
        val session = FocusSession(
            id = UUID.randomUUID().toString(),
            taskName = taskName.trim().ifEmpty { "Tập trung học tập" },
            targetMinutes = safeTargetMinutes,
            actualMinutes = 0,
            interruptionCount = 0,
            state = FocusSessionState.NOT_STARTED
        )
        repository.updateSession(session)
        _remainingSeconds.value = safeTargetMinutes * 60
        _syncMessage.value = null
    }

    fun startNewSession() = initSession()

    fun startTimer() {
        val current = currentSession.value
        if (current.state == FocusSessionState.RUNNING || current.state == FocusSessionState.COMPLETED) return
        val running = current.copy(state = FocusSessionState.RUNNING)
        repository.updateSession(running)
        syncSession(running)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0 && currentSession.value.state == FocusSessionState.RUNNING) {
                delay(1000)
                _remainingSeconds.value -= 1
            }
            if (_remainingSeconds.value <= 0) completeSession()
        }
    }

    fun pauseTimer() {
        val current = currentSession.value
        if (current.state != FocusSessionState.RUNNING) return
        timerJob?.cancel()
        val paused = current.copy(
            actualMinutes = elapsedMinutes(current.targetMinutes, _remainingSeconds.value),
            state = FocusSessionState.PAUSED
        )
        repository.updateSession(paused)
        syncSession(paused)
    }

    fun recordInterruption(reason: String) {
        val current = currentSession.value
        if (reason.isBlank()) return
        val updated = current.copy(
            actualMinutes = elapsedMinutes(current.targetMinutes, _remainingSeconds.value),
            interruptionCount = current.interruptionCount + 1,
            interruptionReasons = current.interruptionReasons + reason.trim()
        )
        repository.updateSession(updated)
        syncSession(updated)
    }

    fun completeSession() {
        timerJob?.cancel()
        val current = currentSession.value
        if (current.state == FocusSessionState.COMPLETED) return
        val updated = current.copy(
            actualMinutes = elapsedMinutes(current.targetMinutes, _remainingSeconds.value),
            state = FocusSessionState.COMPLETED
        )
        repository.updateSession(updated)
        syncSession(updated)
    }

    fun setSelfRating(rating: Int) {
        if (rating !in 1..5) return
        val updated = currentSession.value.copy(selfRating = rating)
        repository.updateSession(updated)
        if (updated.state == FocusSessionState.COMPLETED) syncSession(updated)
    }

    private fun elapsedMinutes(targetMinutes: Int, remainingSeconds: Int): Int {
        val elapsedSeconds = (targetMinutes * 60 - remainingSeconds).coerceIn(0, targetMinutes * 60)
        return (elapsedSeconds / 60).coerceAtMost(targetMinutes)
    }

    private fun syncSession(session: FocusSession) {
        viewModelScope.launch {
            syncMutex.withLock {
                when (val result = repository.syncSessionToServer(session)) {
                    is ApiResult.Error -> _syncMessage.value = result.message
                    else -> _syncMessage.value = null
                }
            }
        }
    }
}
