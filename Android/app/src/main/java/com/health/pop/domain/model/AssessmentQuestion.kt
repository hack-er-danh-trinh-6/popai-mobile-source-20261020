package com.health.pop.domain.model

enum class AssessmentGroup(val code: String, val titleName: String, val description: String) {
    VN("VN", "Thói quen xem video ngắn", "Tần suất lướt Shorts/Reels/TikTok và chuyển clip liên tục"),
    PT("PT", "Mức độ phụ thuộc thiết bị số", "Cảm giác cần mở điện thoại khi rảnh rỗi hoặc chờ đợi"),
    PB("PB", "Biểu hiện phân mảnh chú ý", "Khó duy trì đọc sách/bài dài, suy nghĩ hay bị ngắt quãng"),
    TT("TT", "Khả năng tập trung sâu", "Mức độ duy trì sự tập trung khi học tập và làm bài tập")
}

data class AssessmentQuestion(
    val id: String,
    val group: AssessmentGroup,
    val questionText: String
)
