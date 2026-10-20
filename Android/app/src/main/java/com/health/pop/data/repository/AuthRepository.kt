package com.health.pop.data.repository

import com.health.pop.core.network.ApiResult
import com.health.pop.core.network.RetrofitClient
import com.health.pop.core.security.TokenManager
import com.health.pop.data.remote.AuthApiService
import com.health.pop.data.remote.LoginRequestDto
import com.health.pop.data.remote.RegisterRequestDto
import com.health.pop.data.remote.UserDto
import com.health.pop.data.remote.apiErrorMessage
import com.health.pop.domain.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthRepository(private val tokenManager: TokenManager) {

    private val apiService: AuthApiService by lazy {
        RetrofitClient.getClient(tokenManager).create(AuthApiService::class.java)
    }
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Đặt true ngay sau khi đăng ký thành công để app hỏi học sinh chọn trường và lớp một lần.
    @Volatile
    private var classSetupRequested = false
    private val _currentUser = MutableStateFlow(restoreCachedUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        if (!tokenManager.getAccessToken().isNullOrBlank()) {
            repositoryScope.launch { refreshCurrentUser() }
        }
    }

    private fun restoreCachedUser(): User? {
        val token = tokenManager.getAccessToken()
        if (token.isNullOrBlank() || token.startsWith("demo_")) {
            tokenManager.clear()
            return null
        }
        return tokenManager.getUser()
    }

    fun consumeClassSetupRequest(): Boolean {
        val requested = classSetupRequested
        classSetupRequested = false
        return requested
    }

    suspend fun refreshCurrentUser() {
        if (tokenManager.getAccessToken().isNullOrBlank()) return
        try {
            val response = apiService.getCurrentUser()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                updateCurrentUser(body)
            } else if (response.code() == 401) {
                tokenManager.clear()
                _currentUser.value = null
            }
        } catch (_: Exception) {
            // Keep the previously authenticated profile while offline; protected writes still require the server.
        }
    }

    suspend fun login(email: String, pass: String): ApiResult<User> {
        return try {
            val response = apiService.login(LoginRequestDto(email, pass))
            val body = response.body()
            if (response.isSuccessful && body != null && body.user.role == "TEACHER") {
                ApiResult.Error("Đây là tài khoản giáo viên. Giáo viên vui lòng đăng nhập trên trang web giáo viên.")
            } else if (response.isSuccessful && body != null) {
                tokenManager.saveTokens(body.accessToken, body.refreshToken)
                val user = body.user.toDomain()
                tokenManager.saveUser(user)
                _currentUser.value = user
                ApiResult.Success(user)
            } else {
                ApiResult.Error(response.apiErrorMessage("Đăng nhập thất bại."), response.code())
            }
        } catch (_: Exception) {
            ApiResult.Error("Không thể kết nối máy chủ. Vui lòng thử lại.")
        }
    }

    suspend fun register(email: String, pass: String, fullName: String): ApiResult<User> {
        return try {
            val response = apiService.register(RegisterRequestDto(email, pass, fullName))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                tokenManager.saveTokens(body.accessToken, body.refreshToken)
                val user = body.user.toDomain()
                tokenManager.saveUser(user)
                classSetupRequested = true
                _currentUser.value = user
                ApiResult.Success(user)
            } else {
                ApiResult.Error(response.apiErrorMessage("Đăng ký thất bại."), response.code())
            }
        } catch (_: Exception) {
            ApiResult.Error("Không thể kết nối máy chủ. Vui lòng thử lại.")
        }
    }

    suspend fun logout() {
        try {
            apiService.logout()
        } catch (_: Exception) {
            // Local credentials are cleared even if the server cannot be reached.
        }
        tokenManager.clear()
        _currentUser.value = null
    }

    private fun updateCurrentUser(dto: UserDto) {
        val user = dto.toDomain()
        tokenManager.saveUser(user)
        _currentUser.value = user
    }

    private fun UserDto.toDomain() = User(id, email, fullName, role)
}
