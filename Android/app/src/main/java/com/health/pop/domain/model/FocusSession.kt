package com.health.pop.domain.model

enum class FocusSessionState {
    NOT_STARTED,
    RUNNING,
    PAUSED,
    COMPLETED
}

data class FocusSession(
    val id: String = "",
    val taskName: String = "",
    val targetMinutes: Int = 25,
    val actualMinutes: Int = 0,
    val interruptionCount: Int = 0,
    val interruptionReasons: List<String> = emptyList(),
    val selfRating: Int = 0, // 1 to 5
    val state: FocusSessionState = FocusSessionState.NOT_STARTED,
    val isSynced: Boolean = true
)
