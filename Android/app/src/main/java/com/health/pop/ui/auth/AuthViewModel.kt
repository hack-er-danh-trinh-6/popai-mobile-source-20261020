package com.health.pop.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.pop.core.network.ApiResult
import com.health.pop.data.repository.AuthRepository
import com.health.pop.domain.model.User
import com.health.pop.domain.usecase.ValidateAuthUseCase
import com.health.pop.domain.usecase.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User, val isDemoData: Boolean) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val validator = ValidateAuthUseCase()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    var emailState = MutableStateFlow("")
    var passwordState = MutableStateFlow("")
    var fullNameState = MutableStateFlow("")
    var isSignUpMode = MutableStateFlow(false)

    fun login() {
        val email = emailState.value
        val pass = passwordState.value

        val emailVal = validator.validateEmail(email)
        if (emailVal is ValidationResult.Error) {
            _uiState.value = AuthUiState.Error(emailVal.message)
            return
        }

        val passVal = validator.validatePassword(pass)
        if (passVal is ValidationResult.Error) {
            _uiState.value = AuthUiState.Error(passVal.message)
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = repository.login(email, pass)) {
                is ApiResult.Success -> {
                    _uiState.value = AuthUiState.Success(result.data, result.isDemoData)
                }
                is ApiResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
                else -> {
                    _uiState.value = AuthUiState.Error("Máy chủ chưa cấu hình. Không thể kết nối backend.")
                }
            }
        }
    }

    fun register() {
        val email = emailState.value
        val pass = passwordState.value
        val name = fullNameState.value

        val emailVal = validator.validateEmail(email)
        if (emailVal is ValidationResult.Error) {
            _uiState.value = AuthUiState.Error(emailVal.message)
            return
        }

        val passVal = validator.validatePassword(pass)
        if (passVal is ValidationResult.Error) {
            _uiState.value = AuthUiState.Error(passVal.message)
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = repository.register(email, pass, name)) {
                is ApiResult.Success -> {
                    _uiState.value = AuthUiState.Success(result.data, result.isDemoData)
                }
                is ApiResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
                else -> {
                    _uiState.value = AuthUiState.Error("Máy chủ chưa cấu hình. Không thể kết nối backend.")
                }
            }
        }
    }
}
