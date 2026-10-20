package com.health.pop.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.health.pop.core.designsystem.component.DemoDataBanner
import com.health.pop.core.designsystem.component.PopcornMotif
import com.health.pop.core.designsystem.component.QuietCard
import com.health.pop.ui.onboarding.ScreenTip
import com.health.pop.core.designsystem.theme.PopcornAmber
import com.health.pop.core.designsystem.theme.SteadyBlue
import com.health.pop.domain.usecase.ValidateAiPromptUseCase

@Composable
fun POPAIChatScreen(
    viewModel: AIViewModel
) {
    val aiState by viewModel.aiState.collectAsState()
    val promptInput by viewModel.promptInput.collectAsState()
    val includeReflection by viewModel.includeReflection.collectAsState()
    val planActionMessage by viewModel.planActionMessage.collectAsState()
    val addedSteps by viewModel.addedSteps.collectAsState()
    val savingStep by viewModel.savingStep.collectAsState()
    val isLoading = aiState is AiState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "POP-AI Trợ lý học tập",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Gợi ý hành động nhỏ - Không phán xét",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PopcornMotif()
        }

        Spacer(modifier = Modifier.height(20.dp))

        ScreenTip(
            tipId = "ai",
            title = "Hỏi POP-AI như thế nào?",
            message = "Viết điều bạn đang gặp (ví dụ: “mình hay lướt Shorts khi học bài”) rồi bấm “Nhận gợi ý tập trung”. Ở mỗi gợi ý, bấm “Thêm” để đưa vào kế hoạch của bạn."
        )

        // Prompt Input Card
        QuietCard {
            Text(
                text = "Đặt câu hỏi hoặc yêu cầu lời khuyên từ POP-AI",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = promptInput,
                onValueChange = viewModel::onPromptChange,
                label = { Text("Ví dụ: Cho mình 2 cách giảm lướt Shorts khi học bài") },
                supportingText = {
                    Text("${promptInput.length}/${ValidateAiPromptUseCase.MAX_PROMPT_LENGTH}")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = includeReflection,
                    onCheckedChange = { viewModel.includeReflection.value = it },
                    enabled = !isLoading
                )
                Text(
                    text = "Đồng ý đính kèm nội dung phản tư nhật ký hôm nay",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.requestAdvice() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SteadyBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("POP-AI đang suy nghĩ...")
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nhận gợi ý tập trung")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        (aiState as? AiState.Error)?.let { error ->
            DemoDataBanner(errorMessage = error.message)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = { viewModel.requestAdvice() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Thử lại")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        planActionMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        (aiState as? AiState.Success)?.let { success ->
            val advice = success.advice

            QuietCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = PopcornAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gợi ý từ POP-AI (${advice.timestamp})", style = MaterialTheme.typography.titleLarge)
                }

                if (success.reflectionRequested && !advice.reflectionUsed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hôm nay bạn chưa có nhật ký nên POP-AI chưa dùng được phần phản tư.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (advice.adviceText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = advice.adviceText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (advice.actionableSteps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Hành động khuyến nghị (Bấm + để thêm vào kế hoạch):",
                        style = MaterialTheme.typography.titleMedium,
                        color = SteadyBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    advice.actionableSteps.forEach { step ->
                        val isAdded = step in addedSteps
                        val isSaving = savingStep == step
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• $step",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.addStepToPlan(step) },
                                enabled = !isAdded && savingStep == null
                            ) {
                                when {
                                    isSaving -> CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    isAdded -> {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Đã thêm")
                                    }
                                    else -> {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Thêm")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Gợi ý chỉ để bạn tự theo dõi, không phải chẩn đoán hay tư vấn y khoa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
