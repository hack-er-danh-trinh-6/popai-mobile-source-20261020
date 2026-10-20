package com.health.pop.domain.model

data class ProgressSummary(
    val daysRange: Int = 7,
    val avgShortVideoMinutes: Int = 0,
    val totalFocusMinutes: Int = 0,
    val completedSessionsCount: Int = 0,
    val totalInterruptions: Int = 0,
    val avgFocusRating: Float = 0f,
    val targetVideoLimitMinutes: Int = 0,
    val hasSavedPlan: Boolean = false,
    val streaksDays: Int = 0,
    val highlights: List<String> = emptyList(),
    val areasToAdjust: List<String> = emptyList()
)
