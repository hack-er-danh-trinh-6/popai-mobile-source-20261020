package com.health.pop.ui.classroom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.health.pop.core.designsystem.component.DemoDataBanner
import com.health.pop.core.designsystem.component.QuietCard
import com.health.pop.domain.model.ClassNotification
import com.health.pop.domain.model.ClassOption
import com.health.pop.domain.model.MembershipStatus
import com.health.pop.domain.model.School
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val PENDING_REFRESH_MS = 15_000L

private const val DATA_SHARING_NOTE =
    "Sau khi giáo viên duyệt, giáo viên xem được số phút tập trung, số phiên hoàn thành và số lần bị ngắt " +
        "của bạn (từ lúc được duyệt). Giáo viên không xem được nhật ký, bài tự đánh giá hay cuộc trò chuyện với POP-AI. " +
        "Bạn có thể rời lớp bất cứ lúc nào."

@Composable
fun ClassroomScreen(
    viewModel: ClassroomViewModel,
    isOnboarding: Boolean,
    onClose: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val status = state.membership.status
    var pendingClass by remember { mutableStateOf<ClassOption?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }

    // Khi đang chờ duyệt, tự làm mới để học sinh thấy ngay khi giáo viên duyệt.
    LaunchedEffect(status) {
        if (status == MembershipStatus.PENDING) {
            while (true) {
                delay(PENDING_REFRESH_MS)
                viewModel.refresh(silent = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Lớp học", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) {
                Text(if (isOnboarding && status != MembershipStatus.APPROVED) "Để sau" else "Xong")
            }
        }

        state.errorMessage?.let { message ->
            DemoDataBanner(errorMessage = message)
        }

        if (isOnboarding && state.membershipLoaded && status == MembershipStatus.NONE) {
            Text(
                "Chọn trường và lớp của bạn để gửi yêu cầu cho giáo viên. Bạn vẫn dùng đầy đủ POP AI nếu bỏ qua bước này.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when (status) {
            MembershipStatus.APPROVED -> {
                QuietCard {
                    Text("Bạn đang ở lớp ${state.membership.className}", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${state.membership.schoolName} · GV ${state.membership.teacherName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(DATA_SHARING_NOTE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { confirmLeave = true }, enabled = !state.isSubmitting) { Text("Rời lớp") }
                }
                InboxSection(items = state.inbox?.items.orEmpty())
            }

            MembershipStatus.PENDING -> {
                QuietCard {
                    Text("Đang chờ giáo viên duyệt", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Bạn đã gửi yêu cầu vào lớp ${state.membership.className} · ${state.membership.schoolName}. " +
                            "Khi được duyệt, trạng thái sẽ tự cập nhật ở đây.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = viewModel::leaveClass, enabled = !state.isSubmitting) { Text("Huỷ yêu cầu") }
                }
            }

            else -> {
                if (!state.membershipLoaded) {
                    // Chưa biết học sinh đã vào lớp hay chưa (đang tải hoặc tải lỗi): không hiện bộ chọn trường
                    // với dòng "chưa có trường nào" dễ gây hiểu nhầm; lỗi đã hiện ở thanh cảnh báo phía trên.
                    if (!state.isLoading) {
                        Button(onClick = viewModel::retry) { Text("Thử lại") }
                    }
                } else {
                    if (status == MembershipStatus.REJECTED) {
                        QuietCard {
                            Text("Yêu cầu chưa được duyệt", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Giáo viên chưa duyệt yêu cầu vào lớp ${state.membership.className} · ${state.membership.schoolName}. " +
                                    "Bạn có thể kiểm tra lại tên lớp và gửi yêu cầu khác.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    ClassPicker(
                        state = state,
                        onQueryChange = viewModel::onSchoolQueryChange,
                        onSelectSchool = viewModel::selectSchool,
                        onClearSchool = viewModel::clearSchool,
                        onChooseClass = { pendingClass = it },
                        onRetrySchools = viewModel::retrySchools,
                        onRetryClasses = viewModel::retryClasses
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    pendingClass?.let { option ->
        AlertDialog(
            onDismissRequest = { pendingClass = null },
            title = { Text("Gửi yêu cầu vào lớp ${option.name}?") },
            text = {
                Text(
                    "${state.selectedSchool?.name.orEmpty()} · GV ${option.teacherName}\n\n$DATA_SHARING_NOTE",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.requestJoin(option); pendingClass = null }) { Text("Gửi yêu cầu") }
            },
            dismissButton = { TextButton(onClick = { pendingClass = null }) { Text("Huỷ") } }
        )
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Rời lớp ${state.membership.className}?") },
            text = { Text("Giáo viên sẽ không còn xem được dữ liệu tập trung mới của bạn và bạn không nhận thông báo của lớp nữa.") },
            confirmButton = {
                Button(onClick = { viewModel.leaveClass(); confirmLeave = false }) { Text("Rời lớp") }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Ở lại") } }
        )
    }
}

@Composable
private fun ColumnScope.ClassPicker(
    state: ClassroomUiState,
    onQueryChange: (String) -> Unit,
    onSelectSchool: (School) -> Unit,
    onClearSchool: () -> Unit,
    onChooseClass: (ClassOption) -> Unit,
    onRetrySchools: () -> Unit,
    onRetryClasses: () -> Unit
) {
    val school = state.selectedSchool
    val schoolsError = state.schoolsError
    val classesError = state.classesError
    if (school == null) {
        Text("Chọn trường", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = state.schoolQuery,
            onValueChange = onQueryChange,
            label = { Text("Tìm tên trường") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (state.isLoadingSchools && state.schools.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (schoolsError != null) {
            LoadErrorNotice(message = schoolsError, onRetry = onRetrySchools)
        } else if (state.schools.isEmpty()) {
            Text(
                "Chưa tìm thấy trường nào. Hãy nhờ giáo viên khai báo trường và lớp trên trang giáo viên trước.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            state.schools.forEach { item ->
                QuietCard(onClick = { onSelectSchool(item) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${item.classCount} lớp",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Chọn lớp", style = MaterialTheme.typography.titleLarge)
                Text(school.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClearSchool) { Text("Đổi trường") }
        }
        if (state.isLoadingClasses) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (classesError != null) {
            LoadErrorNotice(message = classesError, onRetry = onRetryClasses)
        } else if (state.classes.isEmpty()) {
            Text("Trường này chưa có lớp nào.", style = MaterialTheme.typography.bodyMedium)
        } else {
            state.classes.forEach { option ->
                QuietCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(option.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "GV ${option.teacherName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(onClick = { onChooseClass(option) }, enabled = !state.isSubmitting) { Text("Vào lớp") }
                    }
                }
            }
        }
    }
}

/** Lỗi tải danh sách (mạng, máy chủ chưa cập nhật...) kèm nút thử lại; không nhầm với danh sách trống. */
@Composable
private fun LoadErrorNotice(message: String, onRetry: () -> Unit) {
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error
    )
    OutlinedButton(onClick = onRetry) { Text("Thử lại") }
}

@Composable
private fun ColumnScope.InboxSection(items: List<ClassNotification>) {
    Text("Thông báo từ giáo viên", style = MaterialTheme.typography.titleLarge)
    if (items.isEmpty()) {
        Text(
            "Chưa có thông báo nào.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    items.forEach { item ->
        QuietCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (item.read) FontWeight.Normal else FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (!item.read) {
                    Text("Mới", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(item.body, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            val meta = listOf(item.senderName, formatNotificationTime(item.createdAt)).filter { it.isNotBlank() }.joinToString(" · ")
            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Chuyển thời gian ISO-8601 (UTC) của máy chủ sang giờ địa phương dạng dd/MM HH:mm. */
internal fun formatNotificationTime(iso: String): String {
    if (iso.length < 19) return ""
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val date: Date = parser.parse(iso.substring(0, 19)) ?: return ""
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(date)
    } catch (_: Exception) {
        ""
    }
}
