package com.health.pop.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.health.pop.R
import com.health.pop.core.designsystem.component.PopcornMotif
import com.health.pop.core.designsystem.theme.PurpleGradientEnd
import com.health.pop.core.designsystem.theme.PurpleGradientStart

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(PurpleGradientStart, PurpleGradientEnd)))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 74.dp, y = (-64).dp)
                .size(230.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-90).dp, y = 90.dp)
                .size(270.dp)
                .background(Color.White.copy(alpha = 0.07f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.pop_app_logo),
                contentDescription = "Logo P.O.P.",
                modifier = Modifier.size(172.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(30.dp))
            Text(
                text = "POP AI",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Chăm sóc bạn,\nnhẹ nhàng mỗi ngày.",
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 17.sp,
                lineHeight = 25.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(38.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(23.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "ĐANG CHUẨN BỊ KHÔNG GIAN CỦA BẠN",
                color = Color.White.copy(alpha = 0.74f),
                fontSize = 10.sp,
                letterSpacing = 1.1.sp,
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PopcornMotif(size = 8.dp, color = Color.White.copy(alpha = 0.82f))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "MỘT NHỊP NHỎ · MỘT NGÀY NHẸ HƠN",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 9.sp,
                letterSpacing = 0.8.sp
            )
        }
    }
}
