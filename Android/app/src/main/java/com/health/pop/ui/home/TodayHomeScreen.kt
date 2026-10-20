package com.health.pop.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.health.pop.core.designsystem.component.QuietCard
import com.health.pop.core.designsystem.theme.PopcornAmber
import com.health.pop.core.designsystem.theme.PopcornAmberLight
import com.health.pop.core.designsystem.theme.PurpleGradientEnd
import com.health.pop.core.designsystem.theme.PurpleGradientStart
import com.health.pop.core.designsystem.theme.SteadyBlue
import com.health.pop.core.designsystem.theme.SteadyBlueLight
import com.health.pop.core.designsystem.theme.SuccessGreen
import com.health.pop.domain.usecase.FormatDurationUseCase
import com.health.pop.ui.onboarding.LocalOnboarding

private const val MAX_VISIBLE_TASKS = 4

private const val STEP_PLAN = "plan"
private const val STEP_FOCUS = "focus"
private const val STEP_JOURNAL = "journal"
private const val TIP_GETTING_STARTED = "home_getting_started"

@Composable
fun TodayHomeScreen(
    viewModel: HomeViewModel,
    onNavigateToFocus: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToAssessment: () -> Unit,
    onNavigateToPlan: () -> Unit,
    onNavigateToClassroom: () -> Unit,
    onShowGuide: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val plan by viewModel.currentPlan.collectAsState()
    val journal by viewModel.todayJournal.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val durationFormatter = remember { FormatDurationUseCase() }

    // Phiên tập trung đã hoàn thành (đo thật) và số phút tự khai trong nhật ký có thể trùng nhau,
    // nên lấy giá trị lớn hơn thay vì cộng dồn.
    val focusMinutes = maxOf(uiState.sessionFocusMinutes, journal.focusMinutes)
    val focusTime = durationFormatter.formatMinutesToText(focusMinutes)

    val displayName = user?.fullName?.trim().takeUnless { it.isNullOrBlank() }
    // Đang tải lần đầu: chưa biết người dùng đã có kế hoạch hay chưa nên không hiện trạng thái "chưa có kế hoạch".
    val planPending = uiState.isLoading && !plan.hasSavedPlan

    // Checklist "Bắt đầu với POP AI": chỉ ghi nhận / hiển thị khi đã tải xong dữ liệu để người dùng cũ không thấy nó nhấp nháy.
    val onboarding = LocalOnboarding.current
    val journalSaved = journal.id.isNotBlank() || journal.focusMinutes > 0 ||
        journal.distractors.isNotEmpty() || journal.tomorrowAdjustment.isNotBlank()
    val dataReady = !uiState.isLoading && !uiState.planUnavailable
    if (onboarding != null && dataReady) {
        LaunchedEffect(plan.hasSavedPlan) { if (plan.hasSavedPlan) onboarding.markStepDone(STEP_PLAN) }
        LaunchedEffect(focusMinutes > 0) { if (focusMinutes > 0) onboarding.markStepDone(STEP_FOCUS) }
        LaunchedEffect(journalSaved) { if (journalSaved) onboarding.markStepDone(STEP_JOURNAL) }
    }
    val planDone = plan.hasSavedPlan || onboarding?.isStepDone(STEP_PLAN) == true
    val focusDone = focusMinutes > 0 || onboarding?.isStepDone(STEP_FOCUS) == true
    val journalDone = journalSaved || onboarding?.isStepDone(STEP_JOURNAL) == true
    val showGettingStarted = onboarding != null && dataReady &&
        !onboarding.isTipDismissed(TIP_GETTING_STARTED) &&
        !(planDone && focusDone && journalDone)
    val gettingStartedSteps = listOf(
        GettingStartedStep("Tạo kế hoạch cá nhân", "Đặt giới hạn video và vài việc nhỏ", planDone, onNavigateToPlan),
        GettingStartedStep("Chạy phiên tập trung đầu tiên", "Bấm giờ một phiên ngắn cho một việc", focusDone, onNavigateToFocus),
        GettingStartedStep("Ghi nhật ký 3 phút", "Nhìn lại hôm nay, chọn một điều chỉnh cho ngày mai", journalDone, onNavigateToJournal)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        uiState.errorMessage?.let { message ->
            HomeErrorBanner(message = message, onRetry = viewModel::refresh)
        }

        // Purple dashboard hero, inspired by the reference's vivid summary screen.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(listOf(PurpleGradientStart, PurpleGradientEnd)),
                    shape = RoundedCornerShape(30.dp)
                )
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // weight(1f) để tên dài tự xuống dòng/cắt bớt thay vì đẩy avatar ra khỏi màn hình.
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "POP AI  ·  HÔM NAY",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.1.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Xin chào, ${displayName ?: "Bạn"} ✨",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HelpButton(onClick = onShowGuide)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName?.first()?.uppercase() ?: "P",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Thời gian tập trung hôm nay",
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = focusTime,
                color = Color.White,
                fontSize = 38.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Mỗi phút tập trung là một bước tiến.",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                    .padding(vertical = 14.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroStat(
                    modifier = Modifier.weight(1f),
                    value = if (plan.hasSavedPlan) "${plan.studyTasks.size}" else "—",
                    label = when {
                        plan.hasSavedPlan -> "Việc hôm nay"
                        planPending -> "Đang tải…"
                        uiState.planUnavailable -> "Chưa tải được"
                        else -> "Chưa có kế hoạch"
                    }
                )
                Box(Modifier.width(1.dp).height(34.dp).background(Color.White.copy(alpha = 0.28f)))
                HeroStat(
                    modifier = Modifier.weight(1f),
                    value = if (plan.hasSavedPlan) "${plan.dailyShortVideoLimitMinutes} phút" else "—",
                    label = "Giới hạn video"
                )
                Box(Modifier.width(1.dp).height(34.dp).background(Color.White.copy(alpha = 0.28f)))
                HeroStat(
                    modifier = Modifier.weight(1f),
                    value = if (plan.hasSavedPlan && plan.focusProtectionHours.isNotBlank()) plan.focusProtectionHours else "—",
                    label = "Giờ bảo vệ"
                )
            }
        }

        if (showGettingStarted) {
            GettingStartedCard(
                steps = gettingStartedSteps,
                onDismiss = { onboarding?.dismissTip(TIP_GETTING_STARTED) }
            )
        }

        Column {
            Text("Bắt đầu nhẹ nhàng", style = MaterialTheme.typography.titleLarge)
            Text(
                "Chọn một việc nhỏ cho hôm nay",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Tập trung",
                subtitle = "Bắt đầu một phiên",
                icon = { Icon(Icons.Default.Timer, contentDescription = null, tint = SteadyBlue) },
                iconBackground = SteadyBlueLight,
                onClick = onNavigateToFocus
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Nhật ký nhanh",
                subtitle = "Ghi lại cảm nhận",
                icon = { Icon(Icons.Default.Book, contentDescription = null, tint = PopcornAmber) },
                iconBackground = PopcornAmberLight,
                onClick = onNavigateToJournal
            )
        }

        QuietCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Nhiệm vụ học tập",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = when {
                        plan.hasSavedPlan -> "${plan.studyTasks.size} việc"
                        planPending -> "Đang tải"
                        uiState.planUnavailable -> "Lỗi tải"
                        else -> "Chưa lưu"
                    },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            when {
                plan.studyTasks.isNotEmpty() -> {
                    plan.studyTasks.take(MAX_VISIBLE_TASKS).forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Dấu tròn rỗng: nhiệm vụ chưa hoàn thành (trước đây dùng dấu tích như đã xong).
                            Icon(
                                imageVector = Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = task,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (plan.studyTasks.size > MAX_VISIBLE_TASKS) {
                        TextButton(onClick = onNavigateToPlan) {
                            Text("Xem tất cả ${plan.studyTasks.size} việc")
                        }
                    }
                }
                planPending -> HomeHint("Đang tải kế hoạch của bạn…")
                uiState.planUnavailable -> HomeHint("Chưa tải được kế hoạch. Hãy bấm \"Thử lại\" ở phía trên.")
                plan.hasSavedPlan -> {
                    HomeHint("Chưa có nhiệm vụ nào. Hãy chọn một việc nhỏ để bắt đầu!")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onNavigateToPlan) { Text("Thêm nhiệm vụ") }
                }
                else -> {
                    HomeHint("Bạn chưa lưu kế hoạch cá nhân. Hãy tạo kế hoạch để theo dõi mục tiêu của mình.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onNavigateToPlan) { Text("Tạo kế hoạch") }
                }
            }
        }

        QuietCard(onClick = onNavigateToClassroom) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(SteadyBlueLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = SteadyBlue)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lớp học", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Chọn lớp, xem thông báo từ giáo viên",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Mở lớp học",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Dùng màu theo theme: màu cứng 0xFFF0EEFF khiến chữ sáng trên nền sáng (khó đọc) ở chế độ tối.
        Card(
            onClick = onNavigateToAssessment,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hiểu thêm về sự chú ý", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Bài tự đánh giá ngắn · 20 câu",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Mở bài đánh giá",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun HomeErrorBanner(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall
        )
        TextButton(
            onClick = onRetry,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer)
        ) {
            Text("Thử lại")
        }
    }
}

@Composable
private fun HomeHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun HeroStat(modifier: Modifier, value: String, label: String) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // "Giờ bảo vệ" là chuỗi nhập tự do (tối đa 100 ký tự) nên phải căn giữa và giới hạn số dòng.
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.76f),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    iconBackground: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(iconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) { icon() }
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class GettingStartedStep(
    val title: String,
    val hint: String,
    val done: Boolean,
    val onClick: () -> Unit
)

/** Nút "?" trong phần đầu màn Hôm nay: mở lại hướng dẫn sử dụng bất cứ lúc nào. */
@Composable
private fun HelpButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClickLabel = "Mở hướng dẫn sử dụng", role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Hướng dẫn sử dụng" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "?",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GettingStartedCard(
    steps: List<GettingStartedStep>,
    onDismiss: () -> Unit
) {
    val doneCount = steps.count { it.done }
    QuietCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Bắt đầu với POP AI", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Làm lần lượt vài việc nhỏ để POP AI hiểu và hỗ trợ bạn tốt hơn · $doneCount/${steps.size}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            TextButton(onClick = onDismiss) { Text("Ẩn") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { doneCount / steps.size.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
        )
        Spacer(modifier = Modifier.height(12.dp))
        steps.forEachIndexed { index, step ->
            if (index > 0) Spacer(modifier = Modifier.height(8.dp))
            GettingStartedRow(number = index + 1, step = step)
        }
    }
}

@Composable
private fun GettingStartedRow(number: Int, step: GettingStartedStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = step.onClick)
            .background(
                if (step.done) Color.Transparent
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    if (step.done) SuccessGreen else MaterialTheme.colorScheme.primary,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (step.done) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Đã xong",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = "$number",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (step.done) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = step.hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!step.done) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
