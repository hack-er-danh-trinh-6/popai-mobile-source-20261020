package com.health.pop.domain.model

data class AIAdvice(
    val id: String = "",
    val adviceText: String = "",
    val actionableSteps: List<String> = emptyList(),
    val timestamp: String = "",
    val isSavedToPlan: Boolean = false,
    // true khi server thực sự đã dùng nhật ký hôm nay làm ngữ cảnh.
    val reflectionUsed: Boolean = false
)
