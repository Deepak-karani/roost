package com.example.dragonbudget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dragonbudget.ui.theme.*

/**
 * A custom modifier that applies a dual shadow (light top-left, dark bottom-right)
 * to achieve the Neumorphic / Soft UI look from the screenshots.
 */
fun Modifier.softShadow(
    cornerRadius: Dp = 20.dp,
    spread: Dp = 1.dp,
    blur: Dp = 8.dp,
    shadowColorDark: Color = DragonShadowDark,
    shadowColorLight: Color = DragonShadowLight,
    offsetX: Dp = 6.dp,
    offsetY: Dp = 6.dp
) = this.drawBehind {
    val transparentColor = Color.Transparent.toArgb()
    val shadowColorDarkArgb = shadowColorDark.toArgb()
    val shadowColorLightArgb = shadowColorLight.toArgb()

    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.style = android.graphics.Paint.Style.FILL

        // ── Dark Shadow (Bottom Right) ──
        frameworkPaint.color = transparentColor
        frameworkPaint.setShadowLayer(
            blur.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            shadowColorDarkArgb
        )
        canvas.drawRoundRect(
            0f, 0f, size.width, size.height,
            cornerRadius.toPx(), cornerRadius.toPx(),
            paint
        )

        // ── Light Shadow (Top Left) ──
        frameworkPaint.setShadowLayer(
            blur.toPx(),
            -offsetX.toPx(),
            -offsetY.toPx(),
            shadowColorLightArgb
        )
        canvas.drawRoundRect(
            0f, 0f, size.width, size.height,
            cornerRadius.toPx(), cornerRadius.toPx(),
            paint
        )
    }
}

/**
 * The standard container for all items in the new UI.
 * Matches the thick borders and soft shadows from the screenshots.
 */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    backgroundColor: Color = DragonSurface,
    borderColor: Color = Color(0xFF555555), // Dark grey border as seen in images
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val baseModifier = modifier
        .softShadow(cornerRadius = cornerRadius)
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
        .clip(RoundedCornerShape(cornerRadius))
        .background(backgroundColor)
        .border(2.5.dp, borderColor, RoundedCornerShape(cornerRadius))

    Box(
        modifier = baseModifier,
        content = content
    )
}

/**
 * Custom Progress Bar matching the screenshot's blocky look.
 */
@Composable
fun SoftProgressBar(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Only render the label slot when there's actually a label —
        // otherwise we render an awkward stranded ":" or eat width
        // unnecessarily on small phone screens.
        if (label.isNotBlank()) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BarBackground)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(10.dp))
                    .background(HpColor)
            )
        }
    }
}
