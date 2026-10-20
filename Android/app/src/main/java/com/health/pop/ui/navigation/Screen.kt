package com.health.pop.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Auth : Screen("auth", "Xác thực")
    object Home : Screen("home", "Hôm nay")
    object Prevention : Screen("prevention", "Tự đánh giá")
    object Assessment : Screen("assessment", "Bài đánh giá 20 câu")
    object AssessmentResult : Screen("assessment_result", "Kết quả đánh giá")
    object Plan : Screen("plan", "Kế hoạch")
    object Focus : Screen("focus", "Tập trung")
    object Journal : Screen("journal", "Nhật ký")
    object Progress : Screen("progress", "Tiến bộ")
    object AI : Screen("ai", "POP-AI")
    object Classroom : Screen("classroom", "Lớp học")
}
