package com.health.pop.domain.model

data class School(
    val id: String,
    val name: String,
    val classCount: Int
)

data class ClassOption(
    val id: String,
    val name: String,
    val teacherName: String
)

enum class MembershipStatus { NONE, PENDING, APPROVED, REJECTED }

data class ClassMembership(
    val status: MembershipStatus = MembershipStatus.NONE,
    val classId: String = "",
    val className: String = "",
    val schoolName: String = "",
    val teacherName: String = ""
)

data class ClassNotification(
    val id: String,
    val title: String,
    val body: String,
    val className: String,
    val senderName: String,
    val createdAt: String,
    val read: Boolean
)

data class ClassInbox(
    val unreadCount: Int,
    val items: List<ClassNotification>
)
