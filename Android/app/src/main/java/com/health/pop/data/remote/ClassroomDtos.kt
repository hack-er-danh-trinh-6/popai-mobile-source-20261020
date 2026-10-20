package com.health.pop.data.remote

// Gson có thể gán null cho trường bị thiếu, nên các trường đều nullable và được chuẩn hoá ở repository.
data class SchoolDto(
    val id: String? = null,
    val name: String? = null,
    val classCount: Int? = null
)

data class ClassOptionDto(
    val id: String? = null,
    val name: String? = null,
    val teacherName: String? = null
)

data class ClassMembershipDto(
    val status: String? = null,
    val classId: String? = null,
    val className: String? = null,
    val schoolName: String? = null,
    val teacherName: String? = null
)

data class JoinClassRequestDto(
    val classId: String
)

data class ClassNotificationDto(
    val id: String? = null,
    val title: String? = null,
    val body: String? = null,
    val className: String? = null,
    val senderName: String? = null,
    val createdAt: String? = null,
    val read: Boolean = false
)

data class ClassInboxDto(
    val unreadCount: Int? = null,
    val items: List<ClassNotificationDto>? = null
)

data class MarkReadRequestDto(
    val ids: List<String>? = null,
    val all: Boolean? = null
)
