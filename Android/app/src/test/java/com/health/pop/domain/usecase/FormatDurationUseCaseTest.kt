package com.health.pop.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatDurationUseCaseTest {

    private val formatUseCase = FormatDurationUseCase()

    @Test
    fun testFormatTimerDisplay_formatsCorrectly() {
        assertEquals("00:00", formatUseCase.formatTimerDisplay(0))
        assertEquals("00:05", formatUseCase.formatTimerDisplay(5))
        assertEquals("01:30", formatUseCase.formatTimerDisplay(90))
        assertEquals("25:00", formatUseCase.formatTimerDisplay(1500))
    }

    @Test
    fun testFormatMinutesToText_formatsCorrectly() {
        assertEquals("0 phút", formatUseCase.formatMinutesToText(0))
        assertEquals("25 phút", formatUseCase.formatMinutesToText(25))
        assertEquals("1 giờ", formatUseCase.formatMinutesToText(60))
        assertEquals("1 giờ 15 phút", formatUseCase.formatMinutesToText(75))
    }
}
