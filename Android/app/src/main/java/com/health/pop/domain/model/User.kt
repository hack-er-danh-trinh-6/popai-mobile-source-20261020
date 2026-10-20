package com.health.pop.domain.model

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String = "STUDENT"
)
