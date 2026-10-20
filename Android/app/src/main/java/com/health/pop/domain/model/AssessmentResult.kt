package com.health.pop.domain.model

data class AssessmentResult(
    val id: String = "",
    val completedAt: String = "",
    val vnScore: Float = 0f,
    val ptScore: Float = 0f,
    val pbScore: Float = 0f,
    val ttScore: Float = 0f,
    val note: String = "Chỉ để tự theo dõi, không phải chẩn đoán"
)
