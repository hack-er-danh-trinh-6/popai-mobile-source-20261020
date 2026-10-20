package com.health.pop.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private object FloatingBarColors {
    val Bar = Color(0xFF2F2F4A)
    val Indicator = Color(0xFFEDEAFF)
    val Selected = Color(0xFF6C5CE7)
    val Unselected = Color(0xFFADAAC4)
    val Fab = Color(0xFF6C5CE7)
}

data class NavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

private val bottomNavItems = listOf(
    NavItem(Screen.Home, Icons.Rounded.Home, "Hôm nay"),
    NavItem(Screen.Plan, Icons.Rounded.Checklist, "Kế hoạch"),
    NavItem(Screen.Focus, Icons.Rounded.Timer, "Tập trung"),
    NavItem(Screen.Journal, Icons.AutoMirrored.Rounded.MenuBook, "Nhật ký"),
    NavItem(Screen.Progress, Icons.Rounded.BarChart, "Tiến bộ")
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit
) {
    val exactIndex = bottomNavItems.indexOfFirst { it.screen.route == currentRoute }
    val selectedIndex = when {
        currentRoute == Screen.AI.route -> -1
        exactIndex >= 0 -> exactIndex
        else -> 0
    }

    FloatingBottomBar(
        items = bottomNavItems,
        selectedIndex = selectedIndex,
        onSelect = { index -> onNavigate(bottomNavItems[index].screen) },
        fabIcon = Icons.Rounded.AutoAwesome,
        fabSelected = currentRoute == Screen.AI.route,
        onFabClick = { onNavigate(Screen.AI) }
    )
}

@Composable
private fun FloatingBottomBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    fabIcon: ImageVector,
    fabSelected: Boolean,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barHeight = 68.dp
    val fabSize = 56.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(barHeight),
            shape = RoundedCornerShape(barHeight / 2),
            color = FloatingBarColors.Bar,
            shadowElevation = 10.dp
        ) {
            BoxWithConstraints(Modifier.padding(6.dp)) {
                val itemWidth = maxWidth / items.size
                val indicatorVisible = selectedIndex in items.indices
                val indicatorColor by animateColorAsState(
                    targetValue = if (indicatorVisible) FloatingBarColors.Indicator else FloatingBarColors.Bar,
                    animationSpec = tween(250),
                    label = "floatingNavIndicatorColor"
                )
                val indicatorX by animateDpAsState(
                    targetValue = itemWidth * selectedIndex.coerceIn(items.indices),
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f),
                    label = "floatingNavIndicator"
                )

                Box(
                    Modifier
                        // Đọc indicatorX trong lambda (pha layout) để lò xo trượt không kéo cả thanh recompose mỗi khung hình.
                        .offset { IntOffset(indicatorX.roundToPx(), 0) }
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(indicatorColor)
                )

                Row(Modifier.fillMaxSize()) {
                    items.forEachIndexed { index, item ->
                        NavTab(
                            item = item,
                            selected = index == selectedIndex,
                            onClick = { onSelect(index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        CircleFab(
            icon = fabIcon,
            size = fabSize,
            selected = fabSelected,
            onClick = onFabClick
        )
    }
}

@Composable
private fun NavTab(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint by animateColorAsState(
        targetValue = if (selected) FloatingBarColors.Selected else FloatingBarColors.Unselected,
        animationSpec = tween(250),
        label = "floatingNavTint"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.18f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 400f),
        label = "floatingNavIconScale"
    )
    val lift by animateDpAsState(
        targetValue = if (selected) (-1).dp else 1.dp,
        animationSpec = spring(stiffness = 500f),
        label = "floatingNavLift"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier
                .offset(y = lift)
                .size(24.dp)
                .scale(iconScale)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = item.label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun CircleFab(
    icon: ImageVector,
    size: Dp,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "floatingNavFabPress"
    )
    val breathe by rememberInfiniteTransition(label = "floatingNavFabBreathe").animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingNavFabBreatheValue"
    )

    Surface(
        modifier = Modifier
            .size(size)
            .scale(pressScale)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        shape = CircleShape,
        color = if (selected) FloatingBarColors.Indicator else FloatingBarColors.Fab,
        shadowElevation = 8.dp
    ) {
        val contentColor = if (selected) FloatingBarColors.Selected else Color.White
        // Có nhãn chữ "POP-AI" để người dùng mới biết nút tròn này là gì (trước đây chỉ có biểu tượng).
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                // Hiệu ứng "thở" lặp vô hạn: đọc trong graphicsLayer (pha vẽ) thay vì recompose liên tục trên mọi tab.
                modifier = Modifier.size(22.dp).graphicsLayer {
                    scaleX = breathe
                    scaleY = breathe
                }
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "POP-AI",
                color = contentColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}
