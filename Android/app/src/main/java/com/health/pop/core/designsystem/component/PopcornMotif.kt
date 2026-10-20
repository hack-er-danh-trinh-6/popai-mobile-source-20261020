package com.health.pop.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.health.pop.core.designsystem.theme.PopcornAmber

@Composable
fun PopcornMotif(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = PopcornAmber
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PopcornKernel(size = size, color = color)
        Spacer(modifier = Modifier.width(4.dp))
        PopcornKernel(size = size * 1.2f, color = color)
        Spacer(modifier = Modifier.width(4.dp))
        PopcornKernel(size = size, color = color)
    }
}

@Composable
private fun PopcornKernel(
    size: Dp,
    color: Color
) {
    Canvas(modifier = Modifier.size(size)) {
        val radius = size.toPx() / 2f
        val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
        
        // Draw 3 soft overlapping circles representing a popcorn kernel
        drawCircle(
            color = color,
            radius = radius * 0.55f,
            center = Offset(center.x - radius * 0.25f, center.y + radius * 0.1f)
        )
        drawCircle(
            color = color,
            radius = radius * 0.65f,
            center = Offset(center.x, center.y - radius * 0.2f)
        )
        drawCircle(
            color = color,
            radius = radius * 0.55f,
            center = Offset(center.x + radius * 0.25f, center.y + radius * 0.1f)
        )
    }
}
