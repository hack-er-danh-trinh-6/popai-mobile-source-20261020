package com.health.pop.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.health.pop.core.designsystem.component.QuietCard
import com.health.pop.core.designsystem.theme.PurpleGradientEnd
import com.health.pop.core.designsystem.theme.PurpleGradientStart
import kotlinx.coroutines.launch

private data class TourRow(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private data class TourPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val rows: List<TourRow> = emptyList(),
    val footnote: String? = null
)

private val tourPages = listOf(
    TourPage(
        icon = Icons.Rounded.Psychology,
        title = "Chào mừng đến với POP AI",
        body = "Xem video ngắn (TikTok, Reels, Shorts) liên tục khiến đầu óc quen nhảy từ thứ này sang thứ khác " +
            "như những hạt bắp rang — gọi là “Popcorn Brain”. Khi đó học bài hay đọc sách sẽ thấy chậm và dễ sốt ruột.",
        footnote = "POP AI giúp bạn luyện lại sự tập trung từng bước nhỏ mỗi ngày — không cấm dùng điện thoại, không phán xét."
    ),
    TourPage(
        icon = Icons.Rounded.Checklist,
        title = "Bắt đầu chỉ với 3 bước",
        body = "Bạn không cần làm hết trong một lần. Đi theo thứ tự này là dễ nhất:",
        rows = listOf(
            TourRow(
                Icons.Rounded.Psychology,
                "1. Tự đánh giá",
                "Làm bài 20 câu ở thẻ “Hiểu thêm về sự chú ý” trên màn Hôm nay để biết mức xao nhãng hiện tại của mình."
            ),
            TourRow(
                Icons.Rounded.Checklist,
                "2. Lập kế hoạch",
                "Đặt giới hạn phút lướt video, khung giờ cần tập trung và vài việc nhỏ cần làm."
            ),
            TourRow(
                Icons.Rounded.Timer,
                "3. Tập trung & ghi nhật ký",
                "Bấm giờ một phiên học ngắn, cuối ngày ghi nhật ký trong 3 phút."
            )
        )
    ),
    TourPage(
        icon = Icons.Rounded.Home,
        title = "Thanh điều hướng phía dưới",
        body = "Chạm vào từng mục để chuyển màn hình:",
        rows = listOf(
            TourRow(Icons.Rounded.Home, "Hôm nay", "Tổng quan trong ngày và các lối tắt."),
            TourRow(Icons.Rounded.Checklist, "Kế hoạch", "Giới hạn video, giờ bảo vệ tập trung, việc cần làm."),
            TourRow(Icons.Rounded.Timer, "Tập trung", "Đồng hồ cho từng phiên học."),
            TourRow(Icons.AutoMirrored.Rounded.MenuBook, "Nhật ký", "Ghi lại 3 phút mỗi cuối ngày."),
            TourRow(Icons.Rounded.BarChart, "Tiến bộ", "Xem sự thay đổi trong 7–30 ngày."),
            TourRow(Icons.Rounded.AutoAwesome, "Nút POP-AI (bên phải)", "Hỏi POP-AI để nhận gợi ý hành động nhỏ.")
        )
    ),
    TourPage(
        icon = Icons.Rounded.AutoAwesome,
        title = "Sẵn sàng rồi!",
        body = "Hãy bắt đầu bằng việc nhỏ nhất: tạo kế hoạch hoặc chạy thử một phiên tập trung ngắn. " +
            "Bạn có thể mở lại hướng dẫn này bất cứ lúc nào bằng nút “?” ở màn Hôm nay.",
        footnote = "POP AI là công cụ tự theo dõi, không phải chẩn đoán hay tư vấn y khoa."
    )
)

/**
 * Buổi giới thiệu toàn màn hình. [onFinish] được gọi khi người dùng bấm "Bắt đầu", "Bỏ qua"
 * hoặc nhấn Back ở trang đầu.
 */
@Composable
fun OnboardingTour(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { tourPages.size })
    val scope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    val isLast = currentPage == tourPages.lastIndex

    BackHandler {
        if (currentPage > 0) {
            scope.launch { pagerState.animateScrollToPage(currentPage - 1) }
        } else {
            onFinish()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isLast) {
                    TextButton(onClick = onFinish) { Text("Bỏ qua") }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { index ->
                TourPageContent(tourPages[index])
            }

            PageDots(count = tourPages.size, current = currentPage)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage > 0) {
                    TextButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(currentPage - 1) }
                    }) { Text("Quay lại") }
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = {
                    if (isLast) {
                        onFinish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                    }
                }) {
                    Text(if (isLast) "Bắt đầu" else "Tiếp tục")
                }
            }
        }
    }
}

@Composable
private fun TourPageContent(page: TourPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(
                    brush = Brush.linearGradient(listOf(PurpleGradientStart, PurpleGradientEnd)),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (page.rows.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            QuietCard {
                page.rows.forEachIndexed { index, row ->
                    if (index > 0) Spacer(modifier = Modifier.height(14.dp))
                    TourRowItem(row)
                }
            }
        }

        page.footnote?.let { footnote ->
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = footnote,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TourRowItem(row: TourRow) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = row.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = row.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = row.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val inactive = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        val active = MaterialTheme.colorScheme.primary
        repeat(count) { index ->
            val dotWidth by animateDpAsState(
                targetValue = if (index == current) 22.dp else 8.dp,
                label = "tourDotWidth"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .height(8.dp)
                    .width(dotWidth)
                    .clip(CircleShape)
                    .background(if (index == current) active else inactive)
            )
        }
    }
}
