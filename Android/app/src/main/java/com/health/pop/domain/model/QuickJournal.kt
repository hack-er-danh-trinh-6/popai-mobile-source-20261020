package com.health.pop.domain.model

data class QuickJournal(
    val id: String = "",
    val date: String = "",
    val estimatedShortVideoMinutes: Int = 0,
    val focusMinutes: Int = 0,
    val interruptionCount: Int = 0,
    val distractors: List<String> = emptyList(),
    val mostProductiveSession: String = "",
    val tomorrowAdjustment: String = ""
)
