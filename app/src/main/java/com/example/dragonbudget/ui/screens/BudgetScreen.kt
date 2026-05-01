package com.example.dragonbudget.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dragonbudget.AppContainer
import com.example.dragonbudget.data.*
import com.example.dragonbudget.ui.components.SoftCard
import com.example.dragonbudget.ui.components.SoftProgressBar
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
        containerColor = DragonBackground
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp)
        ) {
            // ── Title with Back Button ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "WEEKLY BUDGETS",
                        style = DragonTypography.headlineLarge,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.width(48.dp))
                }
            }

            // ── Categories List ──
            items(categories) { cat ->
                BudgetCategoryCard(
                    category = cat,
                    onUpdateLimit = { newLimit ->
                        viewModel.updateLimit(cat.name, newLimit)
                    }
                )
            }

            // ── Reset Button ──
            item {
                SoftCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = HealthRed,
                    onClick = { showResetDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Reset Budget & Activity",
                            style = DragonTypography.headlineMedium,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetCategoryCard(
    category: BudgetCategoryWithSpent,
    onUpdateLimit: (Double) -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }
    var editLimitValue by remember { mutableStateOf("") }

    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.iconEmoji, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            category.name,
                            style = DragonTypography.headlineMedium,
                            fontSize = 20.sp
                        )
                        Text(
                            "\$${String.format("%.0f", category.spentAmount)} spent",
                            style = DragonTypography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "\$${String.format("%.0f", category.weeklyLimit)}",
                            style = DragonTypography.headlineMedium,
                            fontSize = 24.sp,
                            color = if (category.percentUsed >= 1f) HealthRed else TextPrimary
                        )
                        Text(
                            "Limit",
                            style = DragonTypography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            editLimitValue = String.format("%.0f", category.weeklyLimit)
                            showEdit = !showEdit
                        }
                    ) {
                        Icon(
                            imageVector = if (showEdit) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = if (showEdit) "Cancel edit" else "Edit limit",
                            tint = TextSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SoftProgressBar(
                progress = category.percentUsed,
                label = "USED",
                modifier = Modifier.fillMaxWidth()
            )

            if (showEdit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(Color(0xFFB5AD9E), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = editLimitValue,
                            onValueChange = { editLimitValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                    
                    Button(
                        onClick = {
                            val newLimit = editLimitValue.toDoubleOrNull()
                            if (newLimit != null) onUpdateLimit(newLimit)
                            showEdit = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBeige),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
