package com.example.dragonbudget.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
    val overallBudget by viewModel.overallBudget.collectAsState()
    val allocatedTotal by viewModel.allocatedTotal.collectAsState()
    val unallocated by viewModel.unallocated.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var addCategoryError by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    var showOverallEdit by remember { mutableStateOf(false) }
    var overallEditValue by remember { mutableStateOf("") }
    var overallEditError by remember { mutableStateOf<String?>(null) }

    if (showOverallEdit) {
        AlertDialog(
            onDismissRequest = {
                showOverallEdit = false
                overallEditError = null
            },
            containerColor = DragonSurface,
            title = { Text("Overall weekly budget", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = overallEditValue,
                        onValueChange = { overallEditValue = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Amount ($)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        )
                    )
                    overallEditError?.let {
                        Text(it, color = HealthRed, style = DragonTypography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amt = overallEditValue.toDoubleOrNull() ?: 0.0
                    viewModel.setOverallBudget(amt) { res ->
                        when (res) {
                            is com.example.dragonbudget.viewmodel.SetOverallResult.Accepted -> {
                                showOverallEdit = false
                                overallEditError = null
                            }
                            is com.example.dragonbudget.viewmodel.SetOverallResult.Invalid ->
                                overallEditError = "Enter an amount greater than $0."
                            is com.example.dragonbudget.viewmodel.SetOverallResult.BelowAllocations ->
                                overallEditError = "You've already allocated $${String.format("%.2f", res.allocated)} across categories. Lower a category limit first."
                        }
                    }
                }) { Text("Save", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOverallEdit = false
                    overallEditError = null
                }) { Text("Cancel") }
            }
        )
    }

    pendingDelete?.let { catName ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = DragonSurface,
            title = { Text("Delete \"$catName\"?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Existing purchases tagged with this category will keep " +
                        "their tag, but the category will disappear from the Budget screen."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(catName)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HealthRed)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = {
                showAddCategoryDialog = false
                addCategoryError = null
            },
            errorMessage = addCategoryError,
            onCreate = { name, emoji ->
                viewModel.addCategory(name, emoji) { res ->
                    when (res) {
                        is com.example.dragonbudget.viewmodel.AddCategoryResult.Accepted -> {
                            showAddCategoryDialog = false
                            addCategoryError = null
                        }
                        is com.example.dragonbudget.viewmodel.AddCategoryResult.NameTaken ->
                            addCategoryError = "A category named \"$name\" already exists."
                        is com.example.dragonbudget.viewmodel.AddCategoryResult.OverAllocated ->
                            addCategoryError = "You've already allocated everything. Free up $${String.format("%.2f", res.maxAllowed)} first."
                    }
                }
            }
        )
    }

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

            // ── Overall Budget Card ──
            item {
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Overall budget",
                                    style = DragonTypography.bodyMedium,
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "$${String.format("%.0f", overallBudget)}",
                                    style = DragonTypography.headlineLarge,
                                    fontSize = 36.sp
                                )
                            }
                            IconButton(onClick = {
                                overallEditValue = String.format("%.0f", overallBudget)
                                overallEditError = null
                                showOverallEdit = true
                            }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit overall budget",
                                    tint = TextSecondary
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Allocated", style = DragonTypography.bodyMedium, color = TextMuted, fontSize = 12.sp)
                                Text(
                                    "$${String.format("%.0f", allocatedTotal)}",
                                    style = DragonTypography.bodyLarge,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Remaining", style = DragonTypography.bodyMedium, color = TextMuted, fontSize = 12.sp)
                                Text(
                                    "$${String.format("%.0f", unallocated)}",
                                    style = DragonTypography.bodyLarge,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = if (unallocated <= 0.0) TextMuted else TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // ── Categories List ──
            items(categories, key = { it.name }) { cat ->
                SwipeToDeleteCategory(
                    onDelete = { pendingDelete = cat.name }
                ) {
                    BudgetCategoryCard(
                        category = cat,
                        unallocated = unallocated,
                        onUpdateLimit = { newLimit, onResult ->
                            viewModel.updateLimit(cat.name, newLimit) { res ->
                                onResult(res)
                            }
                        }
                    )
                }
            }

            // ── Add Category Button ──
            item {
                SoftCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = AccentBeige,
                    onClick = { showAddCategoryDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 18.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Add category",
                            style = DragonTypography.headlineMedium,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }
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
    unallocated: Double = 0.0,
    onUpdateLimit: (Double, (com.example.dragonbudget.viewmodel.UpdateLimitResult) -> Unit) -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }
    var editLimitValue by remember { mutableStateOf("") }
    var editError by remember { mutableStateOf<String?>(null) }

    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side absorbs extra width so long category names
                // (e.g. "Entertainment") don't crash into the right column.
                Row(
                    modifier = Modifier.weight(1f, fill = true),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(category.iconEmoji, fontSize = 26.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            category.name,
                            style = DragonTypography.headlineMedium,
                            fontSize = 18.sp,
                            maxLines = 1
                        )
                        Text(
                            "\$${String.format("%.0f", category.spentAmount)} spent",
                            style = DragonTypography.bodyMedium,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "\$${String.format("%.0f", category.weeklyLimit)}",
                            style = DragonTypography.headlineMedium,
                            fontSize = 22.sp,
                            color = if (category.percentUsed >= 1f) HealthRed else TextPrimary,
                            maxLines = 1
                        )
                        Text(
                            "Limit",
                            style = DragonTypography.bodySmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.width(4.dp))
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

            Spacer(Modifier.height(12.dp))

            // Bar without a label — the surrounding "$X spent / $X limit"
            // numbers above already say everything. No more "USED:" wrap.
            SoftProgressBar(
                progress = category.percentUsed,
                label = "",
                modifier = Modifier.fillMaxWidth()
            )

            if (showEdit) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Up to $${String.format("%.0f", category.weeklyLimit + unallocated)} available",
                    style = DragonTypography.bodyMedium,
                    color = TextMuted,
                    fontSize = 12.sp
                )
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
                            onValueChange = {
                                editLimitValue = it
                                editError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            val newLimit = editLimitValue.toDoubleOrNull() ?: return@Button
                            onUpdateLimit(newLimit) { res ->
                                when (res) {
                                    is com.example.dragonbudget.viewmodel.UpdateLimitResult.Accepted -> {
                                        showEdit = false
                                        editError = null
                                    }
                                    is com.example.dragonbudget.viewmodel.UpdateLimitResult.Rejected ->
                                        editError = "Only $${String.format("%.0f", res.maxAllowed)} left to allocate."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBeige),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
                editError?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = HealthRed, style = DragonTypography.bodyMedium, fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Swipe-to-delete wrapper for budget category cards.
 *
 * Uses EndToStart (the iOS-Mail "swipe leftward to reveal red Delete on the
 * right") which is what the rest of the app does on Home / History rows.
 * Keeping all swipe-to-delete gestures consistent across the app.
 *
 * The dismiss state is intercepted so the row snaps back — actual deletion
 * happens via the parent's confirm dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteCategory(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete(); false
            } else false
        },
        positionalThreshold = { distance -> distance * 0.4f }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(HealthRed, RoundedCornerShape(20.dp))
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, "Delete category", tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    ) {
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, emoji: String) -> Unit,
    errorMessage: String? = null
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf(Categories.EMOJI_PALETTE.first()) }

    // If the user types a name we know about, suggest its default emoji
    // (e.g. typing "Coffee" pre-picks ☕). They can still override.
    LaunchedEffect(name) {
        val suggested = Categories.DEFAULT_EMOJIS[name.trim()]
        if (suggested != null && suggested in Categories.EMOJI_PALETTE) {
            emoji = suggested
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DragonSurface,
        title = { Text("New category", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category name") },
                    placeholder = { Text("e.g. Coffee, Pets, Rent") },
                    singleLine = true
                )
                Text(
                    "Pick an emoji",
                    style = DragonTypography.bodyMedium,
                    color = TextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Categories.EMOJI_PALETTE.forEach { e ->
                        SoftCard(
                            cornerRadius = 12.dp,
                            backgroundColor = if (e == emoji) AccentBeige else DragonSurface,
                            onClick = { emoji = e }
                        ) {
                            Text(
                                e,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 22.sp
                            )
                        }
                    }
                }
                if (errorMessage != null) {
                    Text(
                        errorMessage,
                        color = HealthRed,
                        style = DragonTypography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.trim().isNotEmpty()) onCreate(name.trim(), emoji)
                }
            ) { Text("Create", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
