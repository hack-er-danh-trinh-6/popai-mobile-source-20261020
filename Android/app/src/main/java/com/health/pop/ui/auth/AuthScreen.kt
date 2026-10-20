package com.health.pop.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.health.pop.core.designsystem.component.DemoDataBanner
import com.health.pop.core.designsystem.component.PopcornMotif
import com.health.pop.core.designsystem.component.QuietCard

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val email by viewModel.emailState.collectAsState()
    val password by viewModel.passwordState.collectAsState()
    val fullName by viewModel.fullNameState.collectAsState()
    val isSignUpMode by viewModel.isSignUpMode.collectAsState()

    if (uiState is AuthUiState.Success) {
        onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PopcornMotif(size = 20.dp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "POP AI",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Đồng hành tập trung & Rèn luyện chú ý",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        QuietCard {
            Text(
                text = if (isSignUpMode) "Đăng ký tài khoản POP" else "Đăng nhập học sinh",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isSignUpMode) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { viewModel.fullNameState.value = it },
                    label = { Text("Họ và tên") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.emailState.value = it },
                label = { Text("Email học sinh") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.passwordState.value = it },
                label = { Text("Mật khẩu") },
                supportingText = if (isSignUpMode) {
                    { Text("Ít nhất 6 ký tự") }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState is AuthUiState.Error) {
                DemoDataBanner(errorMessage = (uiState as AuthUiState.Error).message)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(
                    onClick = {
                        if (isSignUpMode) viewModel.register() else viewModel.login()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSignUpMode) "Tạo tài khoản" else "Đăng nhập")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isSignUpMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = { viewModel.isSignUpMode.value = false }) {
                        Text("Đã có tài khoản? Đăng nhập")
                    }
                }
            } else {
                // Người dùng mới thường không thấy nút chữ nhỏ nên dùng nút viền đầy đủ chiều rộng.
                OutlinedButton(
                    onClick = { viewModel.isSignUpMode.value = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Lần đầu dùng? Tạo tài khoản")
                }
            }
        }
    }
}
