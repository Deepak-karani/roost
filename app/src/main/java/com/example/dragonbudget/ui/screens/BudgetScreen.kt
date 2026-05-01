package com.example.dragonbudget.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dragonbudget.AppContainer
import com.example.dragonbudget.data.*
import com.example.dragonbudget.ui.theme.*
import com.example.dragonbudget.viewmodel.BudgetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    appContainer: AppContainer,
    onBack: () -> Unit
) {
    val viewModel: BudgetViewModel = viewModel(
        factory = BudgetViewModel.Factory(appContainer)
    )
    val categories by viewModel.categoriesWithSpent.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Budgets") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            Icons.Default.DeleteSweep, 
                            contentDescription = "Reset All",
                            tint = HealthRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DragonDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = DragonDark
    ) { padding ->

        // Reset confirmation dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset Budget?", fontWeight = FontWeight.Bold) },
                text = { Text("This will clear all purchases and reset your dragon's health back to 100. This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetBudget()
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HealthRed)
                    ) {
                        Text("Reset", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = DragonSurface
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            // ── Lifestyle Header ──
            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        "Budget Goals",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Customize limits for your lifestyle",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        TextButton(
                            onClick = { showResetDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = HealthRed)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset Week", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── Lifestyle Presets ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Categories.LIFESTYLE_PRESETS.keys.forEach { preset ->
                        FilterChip(
                            selected = false, // We don't track the active preset, just apply it
                            onClick = { viewModel.applyLifestylePreset(preset) },
                            label = { Text(preset) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DragonSurface,
                                labelColor = TextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = DragonBorder
                            )
                        )
                    }
                }
            }
            
            // ── Categories List ──
            items(categories) { cat ->
                BudgetCategoryCard(
                    category = cat,
                    onUpdateLimit = { newLimit ->
                        viewModel.updateLimit(cat.name, newLimit)
                    },
                    onUpdateSpent = { newSpent ->
                        viewModel.adjustCategorySpent(cat.name, newSpent)
                    }
                )
            }
        }
    }
}

@Composable
fun BudgetCategoryCard(
    category: BudgetCategoryWithSpent,
    onUpdateLimit: (Double) -> Unit,
    onUpdateSpent: (Double) -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }
    var editLimitValue by remember { mutableStateOf("") }
    var editSpentValue by remember { mutableStateOf("") }

    val progressColor = when {
        category.percentUsed >= 1.0f -> HealthRed
        category.percentUsed >= 0.8f -> HealthAmber
        else -> TealAccent
    }

    val animatedProgress by animateFloatAsState(
        targetValue = category.percentUsed.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress_anim"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DragonCard, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Emoji bubble
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DragonBorder.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(category.iconEmoji, fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        category.name,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                    Text(
                        "\$${String.format("%.0f", category.spentAmount)} of \$${String.format("%.0f", category.weeklyLimit)}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "\$${String.format("%.0f", category.remaining)}",
                    fontWeight = FontWeight.Bold,
                    color = if (category.remaining <= 0) HealthRed else TealAccent,
                    fontSize = 18.sp
                )
                Text(
                    if (category.remaining > 0) "left" else "over",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = DragonBorder,
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${(category.percentUsed * 100).toInt()}%",
                color = progressColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            TextButton(
                onClick = {
                    editLimitValue = String.format("%.0f", category.weeklyLimit)
                    editSpentValue = String.format("%.0f", category.spentAmount)
                    showEdit = !showEdit
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("Edit", color = ElectricBlue, fontSize = 12.sp)
            }
        }

        if (showEdit) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editLimitValue,
                        onValueChange = { editLimitValue = it },
                        label = { Text("Weekly Limit") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editSpentValue,
                        onValueChange = { editSpentValue = it },
                        label = { Text("Spent to Date") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Reset Spent Button
                    OutlinedButton(
                        onClick = {
                            onUpdateSpent(0.0)
                            showEdit = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DragonOrange)
                    ) {
                        Text("Reset Spent", fontSize = 12.sp)
                    }

                    // Save Button
                    Button(
                        onClick = {
                            val newLimit = editLimitValue.toDoubleOrNull()
                            val newSpent = editSpentValue.toDoubleOrNull()
                            if (newLimit != null) onUpdateLimit(newLimit)
                            if (newSpent != null) onUpdateSpent(newSpent)
                            showEdit = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Changes", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
