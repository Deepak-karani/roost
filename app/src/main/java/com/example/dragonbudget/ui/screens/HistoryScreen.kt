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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dragonbudget.AppContainer
import com.example.dragonbudget.data.*
import com.example.dragonbudget.ui.theme.*
import com.example.dragonbudget.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    appContainer: AppContainer,
    onBack: () -> Unit
) {
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.Factory(appContainer)
    )
    val purchases by viewModel.purchases.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Purchase History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    var showConfirm by remember { mutableStateOf(false) }
                    IconButton(onClick = { showConfirm = true }) {
                        Icon(Icons.Default.DeleteSweep, "Clear History", tint = HealthRed)
                    }
                    if (showConfirm) {
                        AlertDialog(
                            onDismissRequest = { showConfirm = false },
                            title = { Text("Clear All History?") },
                            text = { Text("This will delete all transactions and reset your dragon's health. This cannot be undone.") },
                            confirmButton = {
                                Button(onClick = { viewModel.clearAll(); showConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = HealthRed)) {
                                    Text("Clear All", color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
                            }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            // Summary card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DragonCard, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Spent", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            "\$${String.format("%.2f", totalSpent)}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${purchases.size}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text("purchases", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }

            // Category filters
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.filterByCategory(null) },
                        label = { Text("All", fontSize = 12.sp) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue.copy(alpha = 0.2f),
                            selectedLabelColor = ElectricBlue,
                            containerColor = DragonCard,
                            labelColor = TextSecondary
                        )
                    )
                    Categories.ALL.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                viewModel.filterByCategory(
                                    if (selectedCategory == cat) null else cat
                                )
                            },
                            label = {
                                Text(
                                    "${Categories.EMOJIS[cat] ?: ""} $cat",
                                    fontSize = 12.sp
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricBlue.copy(alpha = 0.2f),
                                selectedLabelColor = ElectricBlue,
                                containerColor = DragonCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            // Purchase list
            if (purchases.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DragonCard, RoundedCornerShape(16.dp))
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No purchases found.", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }

            items(purchases) { purchase ->
                PurchaseRow(purchase)
            }
        }
    }
}
