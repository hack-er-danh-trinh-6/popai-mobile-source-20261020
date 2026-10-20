package com.health.pop.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.health.pop.core.designsystem.component.DemoDataBanner
import com.health.pop.core.designsystem.component.PopcornMotif
import com.health.pop.core.designsystem.component.QuietCard
import com.health.pop.ui.onboarding.ScreenTip
import com.health.pop.core.designsystem.theme.SteadyBlue

@Composable
fun PersonalPlanScreen(
    viewModel: PlanViewModel
) {
    val plan by viewModel.currentPlan.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    var dailyLimit by remember(plan) { mutableStateOf(plan.dailyShortVideoLimitMinutes.toString()) }
    var protectionHours by remember(plan) { mutableStateOf(plan.focusProtectionHours) }
    var selectedDuration by remember(plan) { mutableStateOf(plan.sessionDurationMinutes) }
    var selectedTargetDays by remember(plan) { mutableStateOf(plan.targetDays) }

    val studyTasks = remember(plan) { mutableStateListOf(*plan.studyTasks.toTypedArray()) }
    var newTaskText by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

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
                    text = "Kế hoạch cá nhân",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Lập mục tiêu tập trung & Quản lý nhịp sinh hoạt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PopcornMotif()
        }

        Spacer(modifier = Modifier.height(20.dp))

        ScreenTip(
            tipId = "plan",
            title = "Kế hoạch dùng để làm gì?",
            message = "Đặt giới hạn phút lướt video, khung giờ cần tập trung và vài việc nhỏ. Nhớ bấm “Lưu kế hoạch cá nhân” ở cuối trang — các việc bạn thêm sẽ hiện ở màn Hôm nay."
        )

        if (!plan.hasSavedPlan) {
            Text(
                "Bạn chưa lưu kế hoạch nào. Các giá trị ban đầu chỉ là gợi ý, chưa phải dữ liệu của bạn.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Daily Video Limit
        QuietCard {
            Text("Giới hạn phút video ngắn mỗi ngày (phút)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = dailyLimit,
                onValueChange = { dailyLimit = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Protection Hours
        QuietCard {
            Text("Khung giờ bảo vệ tập trung", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = protectionHours,
                onValueChange = { protectionHours = it },
                label = { Text("Ví dụ: 19:00 - 21:00") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Session Duration Choices: 10, 15, 25, 30, 45 mins
        QuietCard {
            Text("Thời lượng mỗi phiên tập trung", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(10, 15, 25, 30, 45).forEach { duration ->
                    FilterChip(
                        selected = (selectedDuration == duration),
                        onClick = { selectedDuration = duration },
                        label = { Text("$duration phút") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Target Duration: 7 or 30 days
        QuietCard {
            Text("Thời hạn mục tiêu kế hoạch", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = (selectedTargetDays == 7),
                    onClick = { selectedTargetDays = 7 },
                    label = { Text("7 ngày") }
                )
                FilterChip(
                    selected = (selectedTargetDays == 30),
                    onClick = { selectedTargetDays = 30 },
                    label = { Text("30 ngày") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Study Tasks
        QuietCard {
            Text("Danh sách nhiệm vụ học tập", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            studyTasks.forEachIndexed { index, task ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• $task",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = { studyTasks.removeAt(index) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Xóa")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    label = { Text("Thêm nhiệm vụ mới") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    if (newTaskText.isNotBlank()) {
                        studyTasks.add(newTaskText.trim())
                        newTaskText = ""
                    }
                }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (formError != null) {
            Text(formError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (saveState is PlanSaveState.Error) {
            DemoDataBanner(errorMessage = (saveState as PlanSaveState.Error).message)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (saveState is PlanSaveState.Saving) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Button(
                onClick = {
                    val limit = dailyLimit.toIntOrNull()
                    formError = when {
                        limit == null || limit !in 0..1440 -> "Giới hạn video phải là số phút từ 0 đến 1440."
                        protectionHours.length > 100 -> "Khung giờ bảo vệ tối đa 100 ký tự."
                        studyTasks.size > 30 || studyTasks.any { it.trim().length > 200 } -> "Mỗi kế hoạch tối đa 30 nhiệm vụ, mỗi nhiệm vụ tối đa 200 ký tự."
                        else -> null
                    }
                    if (formError == null) {
                        viewModel.updatePlan(
                            dailyLimit = limit!!,
                            protectionHours = protectionHours,
                            sessionDuration = selectedDuration,
                            targetDays = selectedTargetDays,
                            studyTasks = studyTasks.toList()
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lưu kế hoạch cá nhân")
            }
        }
    }
}
