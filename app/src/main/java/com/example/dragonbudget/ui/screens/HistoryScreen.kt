package com.example.dragonbudget.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dragonbudget.AppContainer
import com.example.dragonbudget.data.*
import com.example.dragonbudget.ui.components.SoftCard
import com.example.dragonbudget.ui.theme.*
import com.example.dragonbudget.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    appContainer: AppContainer,
    onBack: () -> Unit,
    onPurchaseClick: (Long) -> Unit = {}
) {
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.Factory(appContainer)
    )
    val purchases by viewModel.purchases.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var editTarget by remember { mutableStateOf<Purchase?>(null) }

    editTarget?.let { p ->
        EditPurchaseDialog(
            purchase = p,
            onDismiss = { editTarget = null },
            onSave = { updated ->
                viewModel.updatePurchase(updated)
                editTarget = null
            },
            categoryOptions = categories.map { it.name }
        )
    }

    Scaffold(
        containerColor = DragonBackground
    ) { padding ->
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
                        text = "HISTORY",
                        style = DragonTypography.headlineLarge,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.width(48.dp)) // Balance the back button
                }
            }

            // ── Summary Card ──
            item {
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Total Spent",
                            style = DragonTypography.headlineMedium,
                            fontSize = 22.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "\$${String.format("%.2f", totalSpent)}",
                            style = DragonTypography.headlineLarge,
                            fontSize = 48.sp,
                            color = HealthRed
                        )
                    }
                }
            }

            // ── Category Filters ──
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SoftFilterChip(
                        label = "All",
                        selected = selectedCategory == null,
                        onClick = { viewModel.filterByCategory(null) }
                    )
                    categories.forEach { cat ->
                        SoftFilterChip(
                            label = "${cat.iconEmoji} ${cat.name}",
                            selected = selectedCategory == cat.name,
                            onClick = { viewModel.filterByCategory(if (selectedCategory == cat.name) null else cat.name) }
                        )
                    }
                }
            }

            // ── Purchase List ──
            if (purchases.isEmpty()) {
                item {
                    Text(
                        "No purchases found.",
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        style = DragonTypography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = TextMuted
                    )
                }
            }

            items(purchases, key = { it.id }) { purchase ->
                SwipeablePurchaseRow(
                    purchase = purchase,
                    onTap = { onPurchaseClick(purchase.id) },
                    onDelete = { viewModel.deletePurchase(purchase) },
                    onEdit = { editTarget = purchase }
                )
            }
        }
    }
}

@Composable
fun SoftFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    SoftCard(
        cornerRadius = 12.dp,
        backgroundColor = if (selected) AccentBeige else DragonSurface,
        onClick = onClick
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = DragonTypography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else TextPrimary
        )
    }
}
