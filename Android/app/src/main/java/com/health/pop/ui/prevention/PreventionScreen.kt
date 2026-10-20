package com.health.pop.ui.prevention

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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.health.pop.core.designsystem.component.PopcornMotif
import com.health.pop.core.designsystem.component.QuietCard
import com.health.pop.core.designsystem.theme.PopcornAmber
import com.health.pop.core.designsystem.theme.SteadyBlue

@Composable
fun PreventionScreen(
    viewModel: PreventionViewModel,
    onStartAssessment: () -> Unit
) {
    val history by viewModel.historyState.collectAsState()

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
                    text = "Hiểu về Popcorn Brain",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Nhận diện xao nhãng & Rèn luyện sự tập trung",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PopcornMotif()
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Popcorn Brain Education Card
        QuietCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = PopcornAmber
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Popcorn Brain là gì?",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Khi thường xuyên xem video ngắn (TikTok, Reels, Shorts), não bộ quen với nhịp kích thích siêu nhanh, nhảy liên tục như những hạt bỏng ngô (popcorn). Khi quay lại đọc sách hoặc giải toán, não sẽ thấy 'chậm' và sốt ruột.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "POP AI giúp bạn rèn luyện lại khả năng tập trung sâu từng bước một — không cấm dùng thiết bị, không ép buộc, luôn nhẹ nhàng khích lệ.",
                style = MaterialTheme.typography.bodyMedium,
                color = SteadyBlue
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Take Assessment Action
        QuietCard {
            Text(
                text = "Bài tự đánh giá 20 câu",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Đánh giá 4 chỉ số: VN (video ngắn), PT (phụ thuộc thiết bị), PB (phân mảnh chú ý), TT (tập trung sâu). Tự theo dõi tiến trình trước/sau rèn luyện.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onStartAssessment,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SteadyBlue)
            ) {
                Icon(imageVector = Icons.Default.Assignment, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bắt đầu làm bài đánh giá")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Assessment History
        Text(
            text = "Lịch sử đánh giá của bạn",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (history.isEmpty()) {
            QuietCard {
                Text(
                    text = "Chưa có lượt đánh giá nào. Bạn hãy làm bài đánh giá đầu tiên nhé!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            history.forEach { item ->
                QuietCard(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.completedAt,
                            style = MaterialTheme.typography.titleMedium,
                            color = SteadyBlue
                        )
                        Text(
                            text = item.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text("VN: ${item.vnScore}/5")
                        Text("PT: ${item.ptScore}/5")
                        Text("PB: ${item.pbScore}/5")
                        Text("TT: ${item.ttScore}/5")
                    }
                }
            }
        }
    }
}
