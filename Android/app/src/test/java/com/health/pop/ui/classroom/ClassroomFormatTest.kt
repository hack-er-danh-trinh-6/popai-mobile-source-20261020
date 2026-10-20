package com.health.pop.ui.classroom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassroomFormatTest {

    @Test
    fun formatNotificationTime_returnsEmptyForInvalidInput() {
        assertEquals("", formatNotificationTime(""))
        assertEquals("", formatNotificationTime("không phải ngày giờ ở đây"))
    }

    @Test
    fun formatNotificationTime_formatsServerTimestamp() {
        val formatted = formatNotificationTime("2026-10-20T08:30:00.000Z")
        assertTrue(formatted.matches(Regex("\\d{2}/\\d{2} \\d{2}:\\d{2}")))
    }
}
