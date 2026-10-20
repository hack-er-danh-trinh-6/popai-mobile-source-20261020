package com.health.pop.ui.prevention

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.health.pop.core.designsystem.component.DemoDataBanner
import com.health.pop.core.designsystem.component.QuietCard
import com.health.pop.core.designsystem.theme.PopcornAmber
import com.health.pop.core.designsystem.theme.SteadyBlue
import com.health.pop.domain.model.AssessmentGroup

@Composable
fun AssessmentResultScreen(
    viewModel: PreventionViewModel,
    onDone: () -> Unit
) {
    val submissionState by viewModel.submissionState.collectAsState()

    val result = (submissionState as? AssessmentSubmissionState.Success)?.result
    val error = (submissionState as? AssessmentSubmissionState.Error)?.message

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        if (error != null) {
            DemoDataBanner(errorMessage = error)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = "Kết quả tự đánh giá",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Điểm trung bình theo nhóm (Thang điểm 1 - 5)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (result != null) {
            QuietCard {
                Text("VN - Thói quen video ngắn: ${result.vnScore} / 5", style = MaterialTheme.typography.titleMedium, color = PopcornAmber)
                Text(AssessmentGroup.VN.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))

                Text("PT - Phụ thuộc thiết bị: ${result.ptScore} / 5", style = MaterialTheme.typography.titleMedium, color = PopcornAmber)
                Text(AssessmentGroup.PT.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))

                Text("PB - Phân mảnh chú ý: ${result.pbScore} / 5", style = MaterialTheme.typography.titleMedium, color = PopcornAmber)
                Text(AssessmentGroup.PB.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))

                Text("TT - Tập trung sâu: ${result.ttScore} / 5", style = MaterialTheme.typography.titleMedium, color = SteadyBlue)
                Text(AssessmentGroup.TT.description, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            QuietCard {
                Text(
                    text = "Lưu ý quan trọng:",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = result.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.resetSubmissionState()
                onDone()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Quay về màn hình chính")
        }
    }
}
