package com.health.pop.ui.prevention

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.health.pop.core.designsystem.component.DemoDataBanner
import com.health.pop.core.designsystem.component.QuietCard
import com.health.pop.core.designsystem.theme.SteadyBlue

@Composable
fun AssessmentScreen(
    viewModel: PreventionViewModel,
    onSubmitted: () -> Unit
) {
    val questions = viewModel.questions
    val answers by viewModel.answers.collectAsState()
    val submissionState by viewModel.submissionState.collectAsState()

    LaunchedEffect(submissionState) {
        if (submissionState is AssessmentSubmissionState.Success) {
            viewModel.markSubmissionHandled()
            onSubmitted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Bài tự đánh giá 20 câu",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Thang điểm 1 (Rất ít/Không bao giờ) đến 5 (Rất thường xuyên/Rất nhiều)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        questions.forEachIndexed { index, question ->
            QuietCard(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = "Câu ${index + 1} [${question.group.code}]: ${question.questionText}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                val selectedScore = answers[question.id] ?: 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { score ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RadioButton(
                                selected = (selectedScore == score),
                                onClick = { viewModel.setAnswer(question.id, score) }
                            )
                            Text(
                                text = "$score",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (submissionState is AssessmentSubmissionState.Error) {
            DemoDataBanner(errorMessage = (submissionState as AssessmentSubmissionState.Error).message)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (submissionState is AssessmentSubmissionState.Submitting) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Button(
                onClick = { viewModel.submitAssessment() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Hoàn thành bài đánh giá (${answers.size}/20)")
            }
        }
    }
}
