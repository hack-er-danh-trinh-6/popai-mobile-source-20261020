package com.health.pop.ui.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.health.pop.core.designsystem.component.DemoDataBanner
import com.health.pop.core.designsystem.component.PopcornMotif
import com.health.pop.core.designsystem.component.QuietCard
import com.health.pop.ui.onboarding.ScreenTip
import com.health.pop.core.designsystem.theme.PopcornAmber
import com.health.pop.core.designsystem.theme.SteadyBlue
import com.health.pop.domain.model.FocusSessionState

@Composable
fun FocusTimerScreen(
    viewModel: FocusViewModel
) {
    val session by viewModel.currentSession.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    var interruptionReasonText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Đồng hồ tập trung",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = session.taskName.ifEmpty { "Nhiệm vụ học tập" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PopcornMotif()
        }

        Spacer(modifier = Modifier.height(30.dp))

        ScreenTip(
            tipId = "focus",
            title = "Cách dùng đồng hồ tập trung",
            message = "Bấm “Bắt đầu” để chạy một phiên cho việc đang chọn. Khi đang chạy, nếu bị xao nhãng hãy ghi lại lý do ở thẻ “Ghi nhận gián đoạn”. Xong thì bấm “Hoàn thành” và chấm sao cho mức tập trung của bạn."
        )

        // Progress Ring Container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            val totalSecs = (session.targetMinutes * 60).coerceAtLeast(1)
            val progress = (remainingSeconds.toFloat() / totalSecs.toFloat()).coerceIn(0f, 1f)

            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = PopcornAmber,
                strokeWidth = 12.dp,
                trackColor = MaterialTheme.colorScheme.outline
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = viewModel.formatUseCase.formatTimerDisplay(remainingSeconds),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = when (session.state) {
                        FocusSessionState.NOT_STARTED -> "Sẵn sàng"
                        FocusSessionState.RUNNING -> "Đang tập trung..."
                        FocusSessionState.PAUSED -> "Tạm dừng"
                        FocusSessionState.COMPLETED -> "Hoàn thành phiên!"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = SteadyBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Timer Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (session.state == FocusSessionState.RUNNING) {
                Button(
                    onClick = { viewModel.pauseTimer() },
                    colors = ButtonDefaults.buttonColors(containerColor = PopcornAmber)
                ) {
                    Icon(imageVector = Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tạm dừng")
                }
            } else if (session.state != FocusSessionState.COMPLETED) {
                Button(
                    onClick = { viewModel.startTimer() },
                    colors = ButtonDefaults.buttonColors(containerColor = SteadyBlue)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (session.state == FocusSessionState.PAUSED) "Tiếp tục" else "Bắt đầu")
                }
            }

            if (session.state == FocusSessionState.RUNNING || session.state == FocusSessionState.PAUSED) {
                OutlinedButton(onClick = { viewModel.completeSession() }) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hoàn thành")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Interruption Tracker
        if (session.state == FocusSessionState.RUNNING || session.state == FocusSessionState.PAUSED) {
            QuietCard {
                Text(
                    text = "Ghi nhận gián đoạn (${session.interruptionCount} lần)",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = interruptionReasonText,
                        onValueChange = { interruptionReasonText = it },
                        label = { Text("Lý do (Ví dụ: Tin nhắn, Tiếng ồn)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        viewModel.recordInterruption(interruptionReasonText)
                        interruptionReasonText = ""
                    }) {
                        Text("Ghi lại")
                    }
                }
            }
        }

        // Post-Session Rating & Feedback
        if (session.state == FocusSessionState.COMPLETED) {
            QuietCard {
                Text(
                    text = "Đánh giá mức độ tập trung phiên này",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { viewModel.setSelfRating(star) }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$star sao",
                                tint = if (star <= session.selfRating) PopcornAmber else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Chúc mừng bạn đã hoàn thành phiên tập trung!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SteadyBlue
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.startNewSession() }) {
                    Text("Bắt đầu phiên mới")
                }
            }
        }

        if (syncMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            DemoDataBanner(errorMessage = syncMessage)
        }
    }
}
