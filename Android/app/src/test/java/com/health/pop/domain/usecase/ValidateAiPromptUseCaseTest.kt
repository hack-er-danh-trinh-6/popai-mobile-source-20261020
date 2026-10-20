package com.health.pop.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateAiPromptUseCaseTest {

    private val validator = ValidateAiPromptUseCase()

    @Test
    fun resolvePrompt_blankOrWhitespace_usesDefaultPrompt() {
        assertEquals(ValidateAiPromptUseCase.DEFAULT_PROMPT, validator.resolvePrompt(""))
        assertEquals(ValidateAiPromptUseCase.DEFAULT_PROMPT, validator.resolvePrompt("   \n  "))
    }

    @Test
    fun resolvePrompt_trimsUserInput() {
        assertEquals("Giảm lướt Shorts", validator.resolvePrompt("  Giảm lướt Shorts \n"))
    }

    @Test
    fun limit_capsLengthToServerLimit() {
        val tooLong = "a".repeat(ValidateAiPromptUseCase.MAX_PROMPT_LENGTH + 50)
        assertEquals(ValidateAiPromptUseCase.MAX_PROMPT_LENGTH, validator.limit(tooLong).length)
    }

    @Test
    fun limit_keepsShortInputUnchanged() {
        assertEquals("ngắn", validator.limit("ngắn"))
    }
}
