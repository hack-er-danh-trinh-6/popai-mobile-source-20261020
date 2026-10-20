package com.health.pop.domain.usecase

import java.util.Locale

class FormatDurationUseCase {

    /**
     * Formats total seconds into MM:SS display format.
     */
    fun formatTimerDisplay(totalSeconds: Int): String {
        if (totalSeconds <= 0) return "00:00"
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    /**
     * Formats total minutes into readable Vietnamese string.
     */
    fun formatMinutesToText(totalMinutes: Int): String {
        if (totalMinutes <= 0) return "0 phút"
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours > 0 && mins > 0 -> "$hours giờ $mins phút"
            hours > 0 -> "$hours giờ"
            else -> "$mins phút"
        }
    }
}
