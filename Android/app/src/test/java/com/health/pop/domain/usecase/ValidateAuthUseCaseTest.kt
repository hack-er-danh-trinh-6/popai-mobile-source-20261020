package com.health.pop.domain.usecase

import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateAuthUseCaseTest {

    private val validator = ValidateAuthUseCase()

    @Test
    fun testValidateEmail_emptyOrInvalid_returnsError() {
        val emptyRes = validator.validateEmail("")
        assertTrue(emptyRes is ValidationResult.Error)

        val invalidRes = validator.validateEmail("notanemail")
        assertTrue(invalidRes is ValidationResult.Error)
    }

    @Test
    fun testValidateEmail_valid_returnsSuccess() {
        val validRes = validator.validateEmail("hocsinh@example.com")
        assertTrue(validRes is ValidationResult.Success)
    }

    @Test
    fun testValidatePassword_short_returnsError() {
        val res = validator.validatePassword("12345")
        assertTrue(res is ValidationResult.Error)
    }

    @Test
    fun testValidatePassword_sufficientLength_returnsSuccess() {
        val res = validator.validatePassword("123456")
        assertTrue(res is ValidationResult.Success)
    }
}
