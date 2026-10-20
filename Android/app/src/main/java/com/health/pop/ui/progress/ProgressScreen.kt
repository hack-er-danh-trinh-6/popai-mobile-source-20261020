package com.health.pop.ui.progress

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.health.pop.domain.usecase.FormatDurationUseCase

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel
) {
    val selectedDays by viewModel.selectedDays.collectAsState()
    val summary by viewModel.summaryState.collectAsState()
    val error by viewModel.errorState.collectAsState()
    val durationFormatter = FormatDurationUseCase()

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
                    text = "Tiến bộ cá nhân",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Theo dõi sự thay đổi & Hành trình rèn luyện",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PopcornMotif()
        }

        Spacer(modifier = Modifier.height(20.dp))

        ScreenTip(
            tipId = "progress",
            title = "Đọc màn Tiến bộ như thế nào?",
            message = "Chọn 7, 14, 21 hoặc 30 ngày để xem tổng phút tập trung, số phiên hoàn thành và chuỗi ngày tập trung. Dùng app đều vài ngày thì số liệu sẽ đầy đủ hơn."
        )

        // Timeframe Selector: 7, 14, 21, 30 days
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(7, 14, 21, 30).forEach { days ->
                FilterChip(
                    selected = (selectedDays == days),
                    onClick = { viewModel.loadProgress(days) },
                    label = { Text("$days ngày") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (error != null) {
            DemoDataBanner(errorMessage = error!!)
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (summary != null) {
            // Stats Overview Card
            QuietCard {
                Text(
                    text = "Thống kê $selectedDays ngày qua",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Tổng phút tập trung", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = durationFormatter.formatMinutesToText(summary!!.totalFocusMinutes),
                            style = MaterialTheme.typography.titleLarge,
                            color = SteadyBlue
                        )
                    }

                    Column {
                        Text("TB lướt video/ngày", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${summary!!.avgShortVideoMinutes} phút",
                            style = MaterialTheme.typography.titleLarge,
                            color = PopcornAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Số phiên hoàn thành", style = MaterialTheme.typography.bodyMedium)
                        Text("${summary!!.completedSessionsCount} phiên", style = MaterialTheme.typography.titleMedium)
                    }

                    Column {
                        Text("Số lần ngắt quãng", style = MaterialTheme.typography.bodyMedium)
                        Text("${summary!!.totalInterruptions} lần", style = MaterialTheme.typography.titleMedium)
                    }

                    Column {
                        Text("Chuỗi ngày tập trung", style = MaterialTheme.typography.bodyMedium)
                        Text("${summary!!.streaksDays} ngày", style = MaterialTheme.typography.titleMedium, color = SteadyBlue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Highlights
            QuietCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ThumbUp, contentDescription = null, tint = SteadyBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Điểm làm tốt của bạn", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))

                summary!!.highlights.forEach { item ->
                    Text("• $item", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Adjustments
            QuietCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = PopcornAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gợi ý điều chỉnh", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))

                summary!!.areasToAdjust.forEach { item ->
                    Text("• $item", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
