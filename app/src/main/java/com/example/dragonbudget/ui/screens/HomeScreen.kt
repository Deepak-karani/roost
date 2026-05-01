package com.example.dragonbudget.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dragonbudget.AppContainer
import com.example.dragonbudget.R
import com.example.dragonbudget.data.*
import com.example.dragonbudget.engine.DragonStateEngine
import com.example.dragonbudget.ui.components.SoftCard
import com.example.dragonbudget.ui.components.SoftProgressBar
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
    val moneyLeftRatio by viewModel.moneyLeftRatio.collectAsState()

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf("") }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = DragonSurface,
            title = { Text("Name your dragon", fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    singleLine = true,
                    placeholder = { Text("e.g. Smaug, Toothless, Roostie…") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameDragon(renameValue)
                    showRenameDialog = false
                }) { Text("Save", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = DragonBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddPurchase,
                containerColor = AccentBeige,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Purchase", modifier = Modifier.size(32.dp))
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // ── Title ──
                item {
                    Text(
                        text = "HOME",
                        style = DragonTypography.headlineLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // ── Dragon Hero Card ──
                item {
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.padding(24.dp)) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Dragon name (tap to rename)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            renameValue = dragon.name
                                            showRenameDialog = true
                                        }
                                ) {
                                    Text(
                                        dragon.name,
                                        style = DragonTypography.headlineMedium,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Rename dragon",
                                        modifier = Modifier.size(18.dp),
                                        tint = TextSecondary
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                // Dragon sprite reflects health AND money-left:
                                // when the wallet is empty, the dragon goes to sleep.
                                Image(
                                    painter = painterResource(
                                        id = DragonStateEngine.getDragonDrawable(
                                            dragon.health,
                                            moneyLeftRatio
                                        )
                                    ),
                                    contentDescription = "Dragon at health ${dragon.health}",
                                    modifier = Modifier.height(140.dp)
                                )
                                
                                Spacer(Modifier.height(16.dp))

                                // HP Bar
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("HP: ", style = DragonTypography.titleMedium, fontWeight = FontWeight.Black)
                                    SoftProgressBar(
                                        progress = dragon.health / 100f,
                                        label = "",
                                        modifier = Modifier.width(140.dp)
                                    )
                                }
                                
                                Spacer(Modifier.height(8.dp))

                                // XP Bar
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("XP: ", style = DragonTypography.titleMedium, fontWeight = FontWeight.Black)
                                    SoftProgressBar(
                                        progress = dragon.xp / 100f,
                                        label = "",
                                        modifier = Modifier.width(140.dp)
                                    )
                                }
                            }
                            
                            // Level label in the corner
                            Text(
                                "Level ${dragon.level}",
                                style = DragonTypography.headlineMedium,
                                modifier = Modifier.align(Alignment.BottomEnd),
                                color = TextPrimary,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // ── Action Row ──
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Weekly Budget Card
                        SoftCard(
                            modifier = Modifier.weight(1.3f),
                            onClick = onNavigateToBudgets
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Weekly Budget",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "\$${String.format("%.0f", totalBudget - totalSpent)}",
                                    style = DragonTypography.headlineLarge,
                                    fontSize = 42.sp
                                )
                                Text(
                                    "left of \$${String.format("%.0f", totalBudget)}",
                                    fontSize = 18.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Ask AI Card
                        SoftCard(
                            modifier = Modifier.weight(0.7f),
                            onClick = onNavigateToAskDragon
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.ChatBubble,
                                    contentDescription = "Ask AI",
                                    modifier = Modifier.size(60.dp),
                                    tint = TextPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Ask AI",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                            }
                        }
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
                            style = DragonTypography.headlineMedium,
                            fontSize = 22.sp
                        )
                        Text(
                            "See All",
                            modifier = Modifier.clickable { onNavigateToHistory() },
                            style = DragonTypography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }

                // ── Activity Items ──
                items(recentPurchases.take(3)) { purchase ->
                    ActivityCard(purchase)
                }
            }

            // (corner dragon removed — the + FAB owns the bottom-right
            //  area; the hero card already shows the live sprite)
        }
    }
}

@Composable
fun ActivityCard(purchase: Purchase) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.US) }
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    purchase.merchant,
                    style = DragonTypography.titleLarge,
                    fontSize = 20.sp
                )
                Text(
                    dateFormat.format(Date(purchase.timestamp)),
                    style = DragonTypography.bodyMedium,
                    fontSize = 14.sp
                )
            }
            Text(
                "-\$${String.format("%.2f", purchase.amount)}",
                style = DragonTypography.titleLarge,
                fontSize = 20.sp
            )
        }
    }
}
