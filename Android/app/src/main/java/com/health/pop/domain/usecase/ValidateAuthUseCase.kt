package com.health.pop.domain.usecase

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

class ValidateAuthUseCase {

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")

    fun validateEmail(email: String): ValidationResult {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult.Error("Vui lòng nhập địa chỉ email")
        }
        if (!emailRegex.matches(trimmed)) {
            return ValidationResult.Error("Địa chỉ email không đúng định dạng")
        }
        return ValidationResult.Success
    }

    fun validatePassword(password: String): ValidationResult {
        if (password.length < 6) {
            return ValidationResult.Error("Mật khẩu phải có ít nhất 6 ký tự")
        }
        return ValidationResult.Success
    }
}
