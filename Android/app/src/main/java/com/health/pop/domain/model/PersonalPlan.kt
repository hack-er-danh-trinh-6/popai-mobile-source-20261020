package com.health.pop.domain.model

data class PersonalPlan(
    val dailyShortVideoLimitMinutes: Int = 30,
    val trackedPlatforms: List<String> = emptyList(),
    val focusProtectionHours: String = "",
    val studyTasks: List<String> = emptyList(),
    val sessionDurationMinutes: Int = 25,
    val targetDays: Int = 30,
    val hasSavedPlan: Boolean = false
)
