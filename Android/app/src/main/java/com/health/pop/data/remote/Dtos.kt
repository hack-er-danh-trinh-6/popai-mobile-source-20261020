package com.health.pop.data.remote

data class RegisterRequestDto(
    val email: String,
    val password: String,
    val fullName: String
)

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class RefreshRequestDto(
    val refreshToken: String
)

data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto
)

data class UserDto(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String = "STUDENT"
)

data class AssessmentAnswerDto(
    val questionId: String,
    val score: Int
)

data class AssessmentSubmissionDto(
    val answers: List<AssessmentAnswerDto>
)

data class AssessmentResultDto(
    val id: String,
    val completedAt: String,
    val scores: Map<String, Float>,
    val note: String
)

data class PersonalPlanDto(
    val dailyShortVideoLimitMinutes: Int,
    val trackedPlatforms: List<String>,
    val focusProtectionHours: String,
    val studyTasks: List<String>,
    val sessionDurationMinutes: Int,
    val targetDays: Int
)

data class FocusSessionRequestDto(
    val clientSessionId: String,
    val taskName: String,
    val targetMinutes: Int,
    val actualMinutes: Int,
    val interruptionCount: Int,
    val interruptionReasons: List<String>,
    val selfRating: Int,
    val status: String
)

data class QuickJournalDto(
    val date: String,
    val estimatedShortVideoMinutes: Int,
    val focusMinutes: Int,
    val interruptionCount: Int,
    val distractors: List<String>,
    val mostProductiveSession: String,
    val tomorrowAdjustment: String
)

data class QuickJournalResponseDto(
    val id: String,
    val date: String,
    val estimatedShortVideoMinutes: Int,
    val focusMinutes: Int,
    val interruptionCount: Int,
    val distractors: List<String>,
    val mostProductiveSession: String,
    val tomorrowAdjustment: String
)

data class ProgressSummaryDto(
    val daysRange: Int,
    val avgShortVideoMinutes: Int,
    val totalFocusMinutes: Int,
    val completedSessionsCount: Int,
    val totalInterruptions: Int,
    val avgFocusRating: Float,
    val targetVideoLimitMinutes: Int,
    val hasSavedPlan: Boolean = false,
    val streaksDays: Int,
    val highlights: List<String>,
    val areasToAdjust: List<String>
)

data class AIAdviceRequestDto(
    val prompt: String,
    val includeReflection: Boolean = false,
    val reflectionDate: String? = null,
    // IANA time zone của thiết bị để server tính đúng "7 ngày gần nhất" theo ngày lịch của người dùng.
    val timeZone: String? = null
)

// Gson có thể gán null cho trường bị thiếu dù kiểu Kotlin là non-null, nên các trường đều nullable.
data class AIAdviceResponseDto(
    val adviceText: String? = null,
    val actionableSteps: List<String>? = null,
    val reflectionUsed: Boolean = false
)
