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
    var editTarget by remember { mutableStateOf<Purchase?>(null) }
    val homeCategories by viewModel.allCategories.collectAsState()
    val needsOverallBudget by viewModel.needsOverallBudget.collectAsState()
    var firstLaunchBudgetInput by remember { mutableStateOf("") }

    if (needsOverallBudget) {
        AlertDialog(
            onDismissRequest = { /* not dismissable — must enter a budget */ },
            containerColor = DragonSurface,
            title = {
                Text(
                    "Welcome to Roost",
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "What's your weekly budget? Categories you create will share this overall amount."
                    )
                    OutlinedTextField(
                        value = firstLaunchBudgetInput,
                        onValueChange = { input ->
                            firstLaunchBudgetInput = input.filter { c -> c.isDigit() || c == '.' }
                        },
                        label = { Text("Weekly budget ($)") },
                        placeholder = { Text("e.g. 500") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        )
                    )
                }
            },
            confirmButton = {
                val parsed = firstLaunchBudgetInput.toDoubleOrNull() ?: 0.0
                TextButton(
                    enabled = parsed > 0,
                    onClick = {
                        viewModel.setOverallWeeklyBudget(parsed)
                        firstLaunchBudgetInput = ""
                    }
                ) {
                    Text("Set budget", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    editTarget?.let { p ->
        EditPurchaseDialog(
            purchase = p,
            onDismiss = { editTarget = null },
            onSave = { updated ->
                viewModel.updatePurchase(updated)
                editTarget = null
            },
            categoryOptions = homeCategories.map { it.name }
        )
    }

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
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
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

                                // Dragon sprite is driven directly by the
                                // user's remaining-budget percentage (0..100).
                                val budgetHealth = DragonStateEngine.budgetHealth(totalSpent, totalBudget)
                                val dragonFrame = DragonStateEngine.getDragonFrameForHealth(budgetHealth)
                                val mood = DragonStateEngine.getMoodLabel(budgetHealth)
                                val budgetUsedPercent = if (totalBudget > 0)
                                    ((totalSpent / totalBudget) * 100).toInt().coerceAtLeast(0)
                                else 0

                                Image(
                                    painter = painterResource(
                                        id = DragonStateEngine.dragonFrameToDrawable(dragonFrame)
                                    ),
                                    contentDescription = "${dragon.name} — $mood",
                                    modifier = Modifier.height(180.dp)
                                )

                                Spacer(Modifier.height(12.dp))

                                // Health bar (budget-derived)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("HP: ", style = DragonTypography.titleMedium, fontWeight = FontWeight.Black)
                                    SoftProgressBar(
                                        progress = budgetHealth / 100f,
                                        label = "",
                                        modifier = Modifier.width(140.dp)
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                // XP bar (still from existing engine)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("XP: ", style = DragonTypography.titleMedium, fontWeight = FontWeight.Black)
                                    SoftProgressBar(
                                        progress = dragon.xp / 100f,
                                        label = "",
                                        modifier = Modifier.width(140.dp)
                                    )
                                }

                                Spacer(Modifier.height(14.dp))

                                // Info block: health %, budget used %, mood, explanation
                                // Two compact stat columns side by side, with
                                // generous spacing — keeps long mood strings
                                // (e.g. "Calm and thriving") from colliding.
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "Health",
                                            style = DragonTypography.bodyMedium,
                                            color = TextMuted,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            "$budgetHealth%",
                                            style = DragonTypography.bodyLarge,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = TextPrimary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "Budget used",
                                            style = DragonTypography.bodyMedium,
                                            color = TextMuted,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            "$budgetUsedPercent%",
                                            style = DragonTypography.bodyLarge,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = TextPrimary
                                        )
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                Text(
                                    mood,
                                    style = DragonTypography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = if (totalBudget > 0)
                                        "You used $budgetUsedPercent% of your weekly budget, so ${dragon.name} has $budgetHealth% energy left."
                                    else
                                        "Set a weekly budget so ${dragon.name} can react to your spending.",
                                    style = DragonTypography.bodyMedium,
                                    color = TextMuted,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                )
                            }
                            
                            // Level label — moved to top-right so it doesn't
                            // collide with the info block at the card bottom.
                            Text(
                                "Lv ${dragon.level}",
                                style = DragonTypography.headlineMedium,
                                modifier = Modifier.align(Alignment.TopEnd),
                                color = TextSecondary,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
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
                items(recentPurchases.take(3), key = { it.id }) { purchase ->
                    SwipeablePurchaseRow(
                        purchase = purchase,
                        onDelete = { viewModel.deletePurchase(purchase) },
                        onEdit = { editTarget = purchase }
                    )
                }
            }

            // (corner dragon removed — the + FAB owns the bottom-right
            //  area; the hero card already shows the live sprite)
        }
    }
}

@Composable
fun ActivityCard(purchase: Purchase, onClick: (() -> Unit)? = null) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.US) }
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        onClick = onClick
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

/**
 * iOS-style swipeable row.
 *
 *   • swipe left  -> reveals red Delete background, deletes when fully swiped
 *   • swipe right -> reveals beige Edit background, opens edit dialog
 *
 * Uses Material3 SwipeToDismissBox under the hood. The `confirmValueChange`
 * lambda fires the action and returns false so the card snaps back into place
 * (we do the destructive work imperatively, the UI just reflects the new
 * Flow<List<Purchase>> from the DAO once Room re-emits).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeablePurchaseRow(
    purchase: Purchase,
    onTap: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); false }
                SwipeToDismissBoxValue.StartToEnd -> { onEdit(); false }
                else -> false
            }
        },
        positionalThreshold = { distance -> distance * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val dir = dismissState.dismissDirection
            val bg = when (dir) {
                SwipeToDismissBoxValue.EndToStart -> HealthRed
                SwipeToDismissBoxValue.StartToEnd -> AccentBeige
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(bg, RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp),
                contentAlignment = when (dir) {
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.Center
                }
            ) {
                when (dir) {
                    SwipeToDismissBoxValue.EndToStart ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Delete", color = Color.White, fontWeight = FontWeight.Black)
                        }
                    SwipeToDismissBoxValue.StartToEnd ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, "Edit", tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Edit", color = Color.White, fontWeight = FontWeight.Black)
                        }
                    else -> Unit
                }
            }
        }
    ) {
        ActivityCard(purchase, onClick = onTap)
    }
}

/**
 * Inline editor for an existing Purchase. Pre-fills merchant, amount,
 * category. Note field omitted to keep the dialog small (note is rarely
 * the thing the user wants to fix from a swipe-edit).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPurchaseDialog(
    purchase: Purchase,
    onDismiss: () -> Unit,
    onSave: (Purchase) -> Unit,
    categoryOptions: List<String> = listOf(Categories.FOOD)
) {
    var merchant by remember { mutableStateOf(purchase.merchant) }
    var amount by remember { mutableStateOf(String.format("%.2f", purchase.amount)) }
    var category by remember { mutableStateOf(purchase.category) }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DragonSurface,
        title = { Text("Edit purchase", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount ($)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${Categories.emojiFor(category)} $category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categoryOptions.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${Categories.emojiFor(cat)} $cat") },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.toDoubleOrNull() ?: purchase.amount
                if (merchant.isNotBlank() && amt > 0) {
                    onSave(purchase.copy(merchant = merchant.trim(), amount = amt, category = category))
                }
            }) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
