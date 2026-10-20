package com.health.pop.ui.onboarding

import android.content.SharedPreferences
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Trạng thái hướng dẫn cho người dùng mới, lưu riêng theo từng tài khoản.
 *
 * Dùng SharedPreferences thường (không mã hoá) vì chỉ chứa cờ "đã xem", không có dữ liệu nhạy cảm.
 * Không đặt vào TokenManager vì TokenManager.clear() xoá sạch khi đăng xuất, khiến người dùng cũ bị xem lại hướng dẫn.
 */
class OnboardingState(
    private val prefs: SharedPreferences,
    userId: String
) {
    private val tourKey = "tour_seen_$userId"
    private val tipsKey = "tips_dismissed_$userId"
    private val stepsKey = "steps_done_$userId"

    /** Đã xem (hoặc bỏ qua) buổi giới thiệu lần đầu. */
    var tourSeen by mutableStateOf(prefs.getBoolean(tourKey, false))
        private set

    /** Người dùng tự mở lại hướng dẫn bằng nút "?" ở màn Hôm nay. */
    var manualTourOpen by mutableStateOf(false)
        private set

    private val dismissedTips = mutableStateListOf<String>().apply { addAll(readSet(tipsKey)) }
    private val doneSteps = mutableStateListOf<String>().apply { addAll(readSet(stepsKey)) }

    val isTourVisible: Boolean
        get() = !tourSeen || manualTourOpen

    fun openTour() {
        manualTourOpen = true
    }

    fun finishTour() {
        manualTourOpen = false
        if (!tourSeen) {
            tourSeen = true
            prefs.edit().putBoolean(tourKey, true).apply()
        }
    }

    fun isTipDismissed(tipId: String): Boolean = tipId in dismissedTips

    fun dismissTip(tipId: String) {
        if (tipId in dismissedTips) return
        dismissedTips.add(tipId)
        saveSet(tipsKey, dismissedTips)
    }

    fun isStepDone(stepId: String): Boolean = stepId in doneSteps

    /** Ghi nhớ bước "bắt đầu" đã hoàn thành để không bị tính lại từ đầu mỗi ngày. */
    fun markStepDone(stepId: String) {
        if (stepId in doneSteps) return
        doneSteps.add(stepId)
        saveSet(stepsKey, doneSteps)
    }

    private fun readSet(key: String): Set<String> =
        prefs.getStringSet(key, emptySet()).orEmpty()

    private fun saveSet(key: String, values: List<String>) {
        prefs.edit().putStringSet(key, values.toSet()).apply()
    }
}

/** null khi chưa đăng nhập: các thành phần hướng dẫn sẽ tự ẩn. */
val LocalOnboarding = compositionLocalOf<OnboardingState?> { null }
