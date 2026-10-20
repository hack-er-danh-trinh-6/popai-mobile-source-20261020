package com.example.pop.ui.splash // TODO: đổi theo package của bạn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/* ───────────── Màu theo logo ───────────── */
private val PopBlue = Color(0xFF1456A8)
private val HeadColor = Color(0xFF2260B3)
private val BookColor = Color(0xFF2B66BC)
private val StripColor = Color(0xFF64B5FF)
private val TickColor = Color(0xFF2F77D9)
private val RingBrush = Brush.linearGradient(
    listOf(Color(0xFF4DA3E8), Color(0xFF1F7BDC)),
    start = Offset(600f, 250f), end = Offset(1330f, 800f)
)
private val LeafBrush = Brush.linearGradient(
    listOf(Color(0xFF66B06E), Color(0xFF3C9A80)),
    start = Offset(600f, 830f), end = Offset(950f, 1080f)
)
private val StarBrush = Brush.linearGradient(
    listOf(Color(0xFFFFD36B), Color(0xFFF5A234)),
    start = Offset(1160f, 250f), end = Offset(1340f, 445f)
)

/* ───────────── Hình học logo (toạ độ gốc 1930px của ảnh PNG) ───────────── */
private fun mx(x: Float, m: Boolean) = if (m) 1930f - x else x

private fun pagePath(m: Boolean) = Path().apply {
    moveTo(mx(675f, m), 648f)
    cubicTo(mx(800f, m), 670f, mx(910f, m), 720f, mx(955f, m), 770f)
    lineTo(mx(955f, m), 950f)
    cubicTo(mx(900f, m), 860f, mx(800f, m), 820f, mx(695f, m), 805f)
    close()
}

private fun stripPath(m: Boolean) = Path().apply {
    moveTo(mx(708f, m), 610f)
    cubicTo(mx(830f, m), 615f, mx(920f, m), 670f, mx(948f, m), 722f)
    cubicTo(mx(890f, m), 690f, mx(800f, m), 650f, mx(708f, m), 632f)
    close()
}

private fun leafPath(m: Boolean) = Path().apply {
    moveTo(mx(590f, m), 825f)
    cubicTo(mx(790f, m), 830f, mx(940f, m), 915f, mx(950f, m), 1075f)
    cubicTo(mx(800f, m), 1088f, mx(640f, m), 990f, mx(590f, m), 825f)
    close()
}

private fun veinPath(m: Boolean) = Path().apply {
    moveTo(mx(745f, m), 935f)
    cubicTo(mx(830f, m), 960f, mx(885f, m), 1010f, mx(902f, m), 1068f)
}

private fun starPath(cx: Float, cy: Float) = Path().apply {
    val k = 22f
    moveTo(cx, cy - 97f)
    quadraticBezierTo(cx + k, cy - k, cx + 92f, cy)
    quadraticBezierTo(cx + k, cy + k, cx, cy + 98f)
    quadraticBezierTo(cx - k, cy + k, cx - 92f, cy)
    quadraticBezierTo(cx - k, cy - k, cx, cy - 97f)
    close()
}

private val LeftPage = pagePath(false)
private val RightPage = pagePath(true)
private val LeftStrip = stripPath(false)
private val RightStrip = stripPath(true)
private val LeftLeaf = leafPath(false)
private val RightLeaf = leafPath(true)
private val LeftVein = veinPath(false)
private val RightVein = veinPath(true)
private val Star = starPath(1248f, 345f)
private val TickAngles = listOf(-150f, -90f, -30f)

/* ───────────── Trạng thái animation ───────────── */
private class SplashAnim {
    val ring = Animatable(0f)
    val ticks = List(3) { Animatable(0f) }
    val head = Animatable(0f)
    val book = Animatable(0f)
    val leaves = Animatable(0f)
    val star = Animatable(0f)
    val letters = List(5) { Animatable(0f) }
    val slogan = Animatable(0f)
    val exit = Animatable(0f)
}

/* ───────────── Màn hình chính ───────────── */
@Composable
fun PopSplashScreen(onFinished: () -> Unit) {
    val a = remember { SplashAnim() }

    val inf = rememberInfiniteTransition(label = "loop")
    val twinkle = inf.animateFloat(
        0.88f, 1.12f,
        infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "twinkle"
    )
    val sway = inf.animateFloat(
        -2.5f, 2.5f,
        infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway"
    )
    val clock = inf.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "clock"
    )

    LaunchedEffect(Unit) {
        launch { a.ring.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
        launch {
            delay(350)
            a.head.animateTo(1f, spring(0.5f, Spring.StiffnessLow))
        }
        a.ticks.forEachIndexed { i, t ->
            launch {
                delay(600L + i * 120L)
                t.animateTo(1f, spring(0.55f, Spring.StiffnessLow))
            }
        }
        launch {
            delay(700)
            a.book.animateTo(1f, spring(0.65f, Spring.StiffnessLow))
        }
        launch {
            delay(1000)
            a.leaves.animateTo(1f, spring(0.5f, Spring.StiffnessLow))
        }
        launch {
            delay(1300)
            a.star.animateTo(1f, spring(0.45f, Spring.StiffnessLow))
        }
        a.letters.forEachIndexed { i, l ->
            launch {
                delay(1500L + i * 110L)
                l.animateTo(1f, spring(0.5f, Spring.StiffnessMedium))
            }
        }
        launch {
            delay(2100)
            a.slogan.animateTo(1f, tween(500))
        }

        delay(3100)
        a.exit.animateTo(1f, tween(350))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - a.exit.value
                val s = 1f + 0.06f * a.exit.value
                scaleX = s; scaleY = s
            }
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFE8F2FF)))),
        contentAlignment = Alignment.Center
    ) {
        // Vầng sáng phía sau logo
        Box(
            Modifier
                .offset(y = (-70).dp)
                .size(360.dp)
                .graphicsLayer { alpha = a.head.value.coerceIn(0f, 1f) * (0.6f + 0.4f * (twinkle.value - 0.88f) / 0.24f) }
                .background(
                    Brush.radialGradient(listOf(Color(0x404DA3E8), Color.Transparent)),
                    CircleShape
                )
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(280.dp)) {
                val u = size.width / 880f
                withTransform({
                    scale(u, u, Offset.Zero)
                    translate(-530f, -225f)
                }) {
                    drawLogo(a, sway, twinkle)
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                PopLetter("P", a.letters[0])
                PopLetter(".", a.letters[1])
                ClockO(a.letters[2], clock)
                PopLetter(".", a.letters[3])
                PopLetter("P", a.letters[4])
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Bớt phân mảnh – Tập trung sâu – Học chủ động.",
                color = Color(0xFF1F5FB8),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = a.slogan.value
                    translationY = (1f - a.slogan.value) * 24f
                }
            )
        }
    }
}

/* ───────────── Vẽ emblem (vòng, đầu, sách, lá, ngôi sao) ───────────── */
private fun DrawScope.drawLogo(a: SplashAnim, sway: State<Float>, twinkle: State<Float>) {
    // 1. Vòng cung được "vẽ" dần theo chiều kim đồng hồ (chừa khoảng trống cho ngôi sao)
    val rv = a.ring.value
    if (rv > 0f) {
        val total = 193f * rv
        val topLeft = Offset(965f - 375f, 640f - 375f)
        val sz = Size(750f, 750f)
        val st = Stroke(width = 56f, cap = StrokeCap.Round)
        val b = min(total, 140f)
        if (b > 1f) drawArc(RingBrush, 158f, b, false, topLeft, sz, style = st)
        val c = total - 140f
        if (c > 1f) drawArc(RingBrush, 329f, c, false, topLeft, sz, style = st)
    }

    // 2. Ba tia sáng (vạch đồng hồ) bung ra quanh đầu
    val center = Offset(965f, 640f)
    TickAngles.forEachIndexed { i, deg ->
        val p = a.ticks[i].value.coerceIn(0f, 1f)
        if (p > 0f) {
            val rad = Math.toRadians(deg.toDouble())
            val dir = Offset(cos(rad).toFloat(), sin(rad).toFloat())
            drawLine(
                TickColor,
                center + dir * 245f,
                center + dir * (245f + 72f * p),
                strokeWidth = 38f, cap = StrokeCap.Round, alpha = p
            )
        }
    }

    // 3. Đầu người nảy lên
    val h = a.head.value
    if (h > 0f) {
        val c = Offset(965f, 553f)
        scale(h, pivot = c) { drawCircle(HeadColor, 105f, c) }
    }

    // 4. Cuốn sách mở ra từ gáy
    val bk = a.book.value
    if (bk > 0f) {
        val al = bk.coerceIn(0f, 1f)
        val round = Stroke(width = 16f, join = StrokeJoin.Round)
        scale(scaleX = bk, scaleY = 0.6f + 0.4f * bk, pivot = Offset(965f, 800f)) {
            drawPath(LeftStrip, StripColor, alpha = al)
            drawPath(RightStrip, StripColor, alpha = al)
            drawPath(LeftPage, BookColor, alpha = al)
            drawPath(LeftPage, BookColor, alpha = al, style = round)
            drawPath(RightPage, BookColor, alpha = al)
            drawPath(RightPage, BookColor, alpha = al, style = round)
        }
    }

    // 5. Hai chiếc lá mọc lên từ gốc rồi đung đưa nhẹ
    val lf = a.leaves.value
    if (lf > 0f) {
        val pv = Offset(965f, 1080f)
        val al = lf.coerceIn(0f, 1f)
        rotate(sway.value, pv) {
            scale(lf, pivot = pv) {
                drawPath(LeftLeaf, LeafBrush, alpha = al)
                drawPath(RightLeaf, LeafBrush, alpha = al)
                val vein = Stroke(width = 24f, cap = StrokeCap.Round)
                drawPath(LeftVein, Color.White, alpha = al, style = vein)
                drawPath(RightVein, Color.White, alpha = al, style = vein)
            }
        }
    }

    // 6. Ngôi sao xoay vào + lấp lánh liên tục
    val sv = a.star.value
    if (sv > 0f) {
        val sc = Offset(1248f, 345f)
        rotate((1f - sv.coerceIn(0f, 1f)) * -180f, sc) {
            scale(sv * twinkle.value, pivot = sc) {
                drawPath(Star, StarBrush, alpha = sv.coerceIn(0f, 1f))
            }
        }
    }
}

/* ───────────── Chữ P / dấu chấm với hiệu ứng "pop" ───────────── */
@Composable
private fun PopLetter(text: String, anim: Animatable<Float, AnimationVector1D>) {
    Text(
        text = text,
        color = PopBlue,
        style = TextStyle(
            fontSize = 72.sp,
            lineHeight = 72.sp,
            fontWeight = FontWeight.Black,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                LineHeightStyle.Alignment.Center,
                LineHeightStyle.Trim.Both
            )
        ),
        modifier = Modifier.graphicsLayer {
            val v = anim.value
            alpha = v.coerceIn(0f, 1f)
            translationY = (1f - v) * 70f
            val s = 0.5f + 0.5f * v
            scaleX = s; scaleY = s
        }
    )
}

/* ───────────── Chữ "O" là đồng hồ có kim quay ───────────── */
@Composable
private fun ClockO(anim: Animatable<Float, AnimationVector1D>, angle: State<Float>) {
    Canvas(
        Modifier
            .offset(y = (-2).dp) // chỉnh nếu O lệch so với chữ P
            .size(54.dp)
            .graphicsLayer {
                val v = anim.value
                alpha = v.coerceIn(0f, 1f)
                translationY = (1f - v) * 70f
                val s = 0.5f + 0.5f * v
                scaleX = s; scaleY = s
            }
    ) {
        val r = size.minDimension / 2f
        val c = center
        drawCircle(PopBlue, r, c)
        drawCircle(Color.White, r * 0.66f, c)

        val w = r * 0.15f
        val m = Math.toRadians((angle.value - 90f).toDouble())          // kim phút
        val hr = Math.toRadians((angle.value / 12f + 30f).toDouble())   // kim giờ
        drawLine(
            PopBlue, c,
            c + Offset(cos(m).toFloat(), sin(m).toFloat()) * (r * 0.5f),
            strokeWidth = w, cap = StrokeCap.Round
        )
        drawLine(
            PopBlue, c,
            c + Offset(cos(hr).toFloat(), sin(hr).toFloat()) * (r * 0.34f),
            strokeWidth = w, cap = StrokeCap.Round
        )
    }
}
