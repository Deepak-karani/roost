package com.example.dragonbudget.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dragonbudget.AppContainer
import com.example.dragonbudget.data.*
import com.example.dragonbudget.engine.DragonStateEngine
import com.example.dragonbudget.ui.theme.*
import com.example.dragonbudget.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    appContainer: AppContainer,
    onNavigateToAddPurchase: () -> Unit,
    onNavigateToAskDragon: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(appContainer)
    )
    val dragon by viewModel.dragonState.collectAsState()
    val recentPurchases by viewModel.recentPurchases.collectAsState()
    val totalSpent by viewModel.totalSpentThisWeek.collectAsState()
    val totalBudget by viewModel.totalBudgetThisWeek.collectAsState()
    val topCat by viewModel.topCategory.collectAsState()

    Scaffold(
        containerColor = DragonDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddPurchase,
                containerColor = ElectricBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Purchase")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = 56.dp, bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Header ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "DragonBudget",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "Grow your dragon by spending wisely",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                    // Streak badge
                    if (dragon.streakDays > 0) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = DragonOrange.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "🔥 ${dragon.streakDays}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = DragonOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    // AI Status Indicator
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = (if (appContainer.liteRTLMManager.isEngineReady()) TealAccent else HealthRed).copy(alpha = 0.1f),
                        modifier = Modifier.clickable { onNavigateToAskDragon() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (appContainer.liteRTLMManager.isEngineReady()) TealAccent else HealthRed, CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (appContainer.liteRTLMManager.isEngineReady()) "NPU AI READY" else "NPU LOADING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (appContainer.liteRTLMManager.isEngineReady()) TealAccent else HealthRed
                            )
                        }
                    }
                }
            }

            // ── Dragon Hero Card ──
            item { DragonCard(dragon = dragon) }

            // ── Budget + Quick Actions Row ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Budget summary (takes 2/3)
                    BudgetMiniCard(
                        spent = totalSpent,
                        budget = totalBudget,
                        onClick = onNavigateToBudgets,
                        modifier = Modifier.weight(2f)
                    )
                    // Ask Dragon button (takes 1/3)
                    QuickActionCard(
                        icon = Icons.Default.ChatBubble,
                        label = "Ask AI",
                        accentColor = DragonOrange,
                        onClick = onNavigateToAskDragon,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Insights Section ──
            if (topCat != null) {
                item {
                    TopCategoryHighlight(category = topCat!!)
                }
            }

            // ── Recent Activity Header ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Activity",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    if (recentPurchases.isNotEmpty()) {
                        TextButton(onClick = onNavigateToHistory) {
                            Text("See All", color = ElectricBlue, fontSize = 13.sp)
                        }
                    }
                }
            }

            // ── Purchase List ──
            if (recentPurchases.isEmpty()) {
                item {
                    EmptyStateCard()
                }
            }

            items(recentPurchases, key = { it.id }) { purchase ->
                Box(modifier = Modifier.animateItem()) {
                    PurchaseRow(purchase)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Dragon Hero Card
// ──────────────────────────────────────────────

@Composable
fun DragonCard(dragon: DragonState) {
    val animatedHealth by animateFloatAsState(
        targetValue = dragon.health / 100f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "health_anim"
    )
    val animatedXp by animateFloatAsState(
        targetValue = dragon.xp / 100f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "xp_anim"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(DragonStateEngine.getHealthColor(dragon.health)).copy(alpha = 0.08f),
                            DragonCard,
                            DragonCard
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Dragon sprite
            AnimatedDragon(
                healthPercent = dragon.health / 100f,
                level = dragon.level,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .fillMaxHeight(0.65f)
                    .offset(y = (-20).dp)
            )

            // Stats overlay at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Name + Level row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${DragonStateEngine.getMoodEmoji(dragon.mood)} ${dragon.name}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DragonOrange.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "LV ${dragon.level}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = DragonOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Health bar
                StatBar(
                    label = "HP",
                    progress = animatedHealth,
                    color = Color(DragonStateEngine.getHealthColor(dragon.health)),
                    value = "${dragon.health}"
                )

                Spacer(Modifier.height(6.dp))

                // XP bar
                StatBar(
                    label = "XP",
                    progress = animatedXp,
                    color = DragonGold,
                    value = "${dragon.xp}"
                )
            }
        }
    }
}

@Composable
private fun StatBar(label: String, progress: Float, color: Color, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(24.dp)
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = DragonBorder,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            value,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            modifier = Modifier.width(24.dp)
        )
    }
}

// ──────────────────────────────────────────────
// Budget Mini Card
// ──────────────────────────────────────────────

@Composable
fun BudgetMiniCard(spent: Double, budget: Double, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val remaining = (budget - spent).coerceAtLeast(0.0)
    val percentUsed = if (budget > 0) (spent / budget).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = percentUsed.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "budget_anim"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DragonCard),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Weekly Budget", color = TextSecondary, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "\$${String.format("%.0f", remaining)}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (percentUsed > 0.8f) HealthRed else TealAccent
            )
            Text(
                "left of \$${String.format("%.0f", budget)}",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (percentUsed > 0.8f) HealthRed else TealAccent,
                trackColor = DragonBorder
            )
        }
    }
}

// ──────────────────────────────────────────────
// Quick Action Card
// ──────────────────────────────────────────────

@Composable
fun QuickActionCard(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DragonCard),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                label,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ──────────────────────────────────────────────
// Empty State
// ──────────────────────────────────────────────

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DragonCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🐉", fontSize = 36.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "No purchases yet",
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Scan a receipt or add a purchase to start tracking",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

// ──────────────────────────────────────────────
// Purchase Row
// ──────────────────────────────────────────────

@Composable
fun PurchaseRow(purchase: Purchase) {
    val emoji = Categories.EMOJIS[purchase.category] ?: "📦"
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.US) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DragonCard, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Category icon bubble
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DragonBorder.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(emoji, fontSize = 18.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    purchase.merchant,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    dateFormat.format(Date(purchase.timestamp)),
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
        Text(
            "-\$${String.format("%.2f", purchase.amount)}",
            fontWeight = FontWeight.SemiBold,
            color = HealthRed.copy(alpha = 0.85f),
            fontSize = 15.sp
        )
    }
}
@Composable
fun TopCategoryHighlight(category: BudgetCategoryWithSpent) {
    val percent = if (category.weeklyLimit > 0) category.spentAmount / category.weeklyLimit else 0.0
    val color = when {
        percent > 0.9 -> HealthRed
        percent > 0.7 -> DragonOrange
        else -> TealAccent
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = DragonSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DragonBorder)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon Circle
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(category.iconEmoji, fontSize = 24.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Highest Spending",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    category.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$${String.format("%.0f", category.spentAmount)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    "${String.format("%.0f", percent * 100)}% of limit",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
