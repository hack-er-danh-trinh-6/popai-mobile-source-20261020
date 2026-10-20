package com.health.pop.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.health.pop.domain.model.QuickJournal

@Composable
fun QuickJournalScreen(
    viewModel: JournalViewModel
) {
    val journal by viewModel.todayJournal.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    var shortVideoMins by remember(journal) { mutableStateOf(journal.estimatedShortVideoMinutes.toString()) }
    var focusMins by remember(journal) { mutableStateOf(journal.focusMinutes.toString()) }
    var interruptions by remember(journal) { mutableStateOf(journal.interruptionCount.toString()) }
    var productiveTime by remember(journal) { mutableStateOf(journal.mostProductiveSession) }
    var tomorrowAdj by remember(journal) { mutableStateOf(journal.tomorrowAdjustment) }
    var formError by remember { mutableStateOf<String?>(null) }

    val distractorOptions = listOf("Mạng xã hội", "Thông báo game", "Tiếng ồn", "Bạn bè rủ rê", "Khác")
    val selectedDistractors = remember(journal) { mutableStateListOf(*journal.distractors.toTypedArray()) }

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
                    text = "Nhật ký nhanh",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Hoàn thành trong 3 phút - Ngày ${journal.date}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PopcornMotif()
        }

        Spacer(modifier = Modifier.height(20.dp))

        ScreenTip(
            tipId = "journal",
            title = "Nhật ký nhanh – chỉ 3 phút",
            message = "Mỗi cuối ngày, điền số phút lướt video, số phút tập trung, điều làm bạn xao nhãng và một điều muốn chỉnh cho ngày mai, rồi bấm “Lưu nhật ký ngày hôm nay”. POP-AI có thể dùng nhật ký này (nếu bạn đồng ý) để gợi ý sát hơn."
        )

        // Video & Focus Stats
        QuietCard {
            Text("Số phút lướt video ngắn ước tính hôm nay", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = shortVideoMins,
                onValueChange = { shortVideoMins = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Số phút tập trung hoàn thành", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = focusMins,
                onValueChange = { focusMins = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Số lần bị ngắt quãng", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = interruptions,
                onValueChange = { interruptions = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Distractor Factors
        QuietCard {
            Text("Yếu tố gây xao nhãng hôm nay", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                distractorOptions.forEach { option ->
                    val isSelected = selectedDistractors.contains(option)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) selectedDistractors.remove(option) else selectedDistractors.add(option)
                        },
                        label = { Text(option) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Productive Time & Tomorrow Adjustment
        QuietCard {
            Text("Khung giờ tập trung hiệu quả nhất", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = productiveTime,
                onValueChange = { productiveTime = it },
                label = { Text("Ví dụ: 19:30 - 20:00") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Một điều muốn điều chỉnh ngày mai", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = tomorrowAdj,
                onValueChange = { tomorrowAdj = it },
                label = { Text("Ví dụ: Để điện thoại ở chế độ im lặng khi học bài") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (formError != null) {
            Text(formError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (saveState is JournalSaveState.Error) {
            DemoDataBanner(errorMessage = (saveState as JournalSaveState.Error).message)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (saveState is JournalSaveState.Saving) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Button(
                onClick = {
                    val videoMinutes = shortVideoMins.toIntOrNull()
                    val focusMinutes = focusMins.toIntOrNull()
                    val interruptionCount = interruptions.toIntOrNull()
                    formError = when {
                        videoMinutes == null || videoMinutes !in 0..1440 -> "Phút video phải là số từ 0 đến 1440."
                        focusMinutes == null || focusMinutes !in 0..1440 -> "Phút tập trung phải là số từ 0 đến 1440."
                        interruptionCount == null || interruptionCount !in 0..10000 -> "Số lần gián đoạn phải là số từ 0 đến 10000."
                        else -> null
                    }
                    if (formError == null) {
                        viewModel.saveJournal(
                            QuickJournal(
                                date = journal.date,
                                estimatedShortVideoMinutes = videoMinutes!!,
                                focusMinutes = focusMinutes!!,
                                interruptionCount = interruptionCount!!,
                                distractors = selectedDistractors.toList(),
                                mostProductiveSession = productiveTime,
                                tomorrowAdjustment = tomorrowAdj
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lưu nhật ký ngày hôm nay")
            }
        }
    }
}
