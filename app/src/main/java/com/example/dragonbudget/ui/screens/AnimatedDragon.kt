package com.example.dragonbudget.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.dragonbudget.R
import kotlinx.coroutines.delay

/**
 * Dragon animation states based on budget health.
 *
 * THRIVING (health > 70):  Shows money eyes frame (static)
 * HEALTHY  (health 40-70): Shows happy frame (static)
 * DROWSY   (health 20-40): Shows drowsy frame (static)
 * DYING    (health < 20):  Shows sleeping frame (static)
 */

enum class DragonAnimState {
    THRIVING,
    HEALTHY,
    DROWSY,
    DYING
}

/**
 * Sprite-based dragon that shows the right frame for the current health.
 * No endless animation — just a subtle breathing pulse.
 *
 * @param healthPercent 0f..1f representing dragon health
 * @param level Dragon level (affects scale)
 */
@Composable
fun AnimatedDragon(
    healthPercent: Float,
    level: Int = 1,
    modifier: Modifier = Modifier
) {
    val animState = when {
        healthPercent > 0.70f -> DragonAnimState.THRIVING
        healthPercent > 0.40f -> DragonAnimState.HEALTHY
        healthPercent > 0.20f -> DragonAnimState.DROWSY
        else -> DragonAnimState.DYING
    }

    // Pick a single frame based on state
    val frameRes = when (animState) {
        DragonAnimState.THRIVING -> R.drawable.dragon_money_1
        DragonAnimState.HEALTHY -> R.drawable.dragon_happy
        DragonAnimState.DROWSY -> R.drawable.dragon_drowsy
        DragonAnimState.DYING -> R.drawable.dragon_sleep_deep
    }

    // Gentle breathing pulse (subtle, not distracting)
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe_scale"
    )

    // Opacity dims when dying
    val dragonAlpha = when (animState) {
        DragonAnimState.DYING -> 0.65f
        DragonAnimState.DROWSY -> 0.85f
        else -> 1.0f
    }

    // Scale up with level
    val levelScale = 1.0f + (level - 1) * 0.02f

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = frameRes),
            contentDescription = "SnapDragon",
            modifier = Modifier
                .fillMaxHeight()
                .scale(breatheScale * levelScale)
                .alpha(dragonAlpha),
            contentScale = ContentScale.Fit
        )
    }
}
