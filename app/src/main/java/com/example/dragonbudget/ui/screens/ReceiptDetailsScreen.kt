package com.example.dragonbudget.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dragonbudget.AppContainer
import com.example.dragonbudget.data.Categories
import com.example.dragonbudget.ui.components.SoftCard
import com.example.dragonbudget.ui.theme.*
import com.example.dragonbudget.viewmodel.ReceiptDetailsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailsScreen(
    appContainer: AppContainer,
    purchaseId: Long,
    onBack: () -> Unit
) {
    val viewModel: ReceiptDetailsViewModel = viewModel(
        factory = ReceiptDetailsViewModel.Factory(appContainer, purchaseId)
    )
    val purchase by viewModel.purchase.collectAsState()
    val items by viewModel.items.collectAsState()

    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.US) }

    Scaffold(containerColor = DragonBackground) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "RECEIPT",
                        style = DragonTypography.headlineLarge,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.width(48.dp))
                }
            }

            // ── Header card: merchant + total + date + category ──
            item {
                val p = purchase
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            p?.merchant ?: "—",
                            style = DragonTypography.headlineMedium,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            p?.let { dateFormat.format(Date(it.timestamp)) } ?: "",
                            style = DragonTypography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                p?.category?.let {
                                    "${Categories.EMOJIS[it] ?: "📦"} $it"
                                } ?: "",
                                style = DragonTypography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Text(
                                p?.let { "\$${String.format("%.2f", it.amount)}" } ?: "",
                                style = DragonTypography.headlineLarge,
                                fontSize = 36.sp,
                                color = HealthRed
                            )
                        }
                    }
                }
            }

            // ── Items list ──
            item {
                Text(
                    if (items.isEmpty()) "No items archived" else "Items (${items.size})",
                    style = DragonTypography.headlineMedium,
                    fontSize = 22.sp
                )
            }

            if (items.isEmpty()) {
                item {
                    Text(
                        "This purchase wasn't scanned, so there are no line items archived. " +
                            "When you scan a receipt with the camera, every detected item gets stored here.",
                        style = DragonTypography.bodyLarge,
                        color = TextMuted
                    )
                }
            } else {
                items(items) { item ->
                    SoftCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.itemName,
                                    style = DragonTypography.bodyLarge,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${Categories.EMOJIS[item.category] ?: "📦"} ${item.category}",
                                    style = DragonTypography.bodyMedium,
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            }
                            Text(
                                "\$${String.format("%.2f", item.price)}",
                                style = DragonTypography.bodyLarge,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Items-sum vs purchase-total reconciliation hint, only when
                // they differ enough to be visible (tax, fees, OCR rounding).
                item {
                    val itemsSum = items.sumOf { it.price }
                    val total = purchase?.amount ?: 0.0
                    val diff = total - itemsSum
                    if (kotlin.math.abs(diff) >= 0.01) {
                        SoftCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 14.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Items subtotal",
                                        style = DragonTypography.bodyLarge,
                                        color = TextSecondary
                                    )
                                    Text(
                                        "\$${String.format("%.2f", itemsSum)}",
                                        style = DragonTypography.bodyLarge,
                                        color = TextSecondary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Tax / fees",
                                        style = DragonTypography.bodyLarge,
                                        color = TextSecondary
                                    )
                                    Text(
                                        "\$${String.format("%.2f", diff)}",
                                        style = DragonTypography.bodyLarge,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
