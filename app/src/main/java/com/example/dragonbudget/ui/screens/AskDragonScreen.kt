package com.example.dragonbudget.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
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
import com.example.dragonbudget.data.AIAdvice
import com.example.dragonbudget.ui.theme.*
import com.example.dragonbudget.viewmodel.AskDragonViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AskDragonScreen(
    appContainer: AppContainer,
    onBack: () -> Unit
) {
    val viewModel: AskDragonViewModel = viewModel(
        factory = AskDragonViewModel.Factory(appContainer)
    )
    val response by viewModel.response.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val recentAdvice by viewModel.recentAdvice.collectAsState()

    var question by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()

    val quickQuestions = listOf(
        "Why is my dragon tired?",
        "Can I afford food today?",
        "What am I overspending on?",
        "How do I level up?",
    )

    // Auto-scroll to bottom when response arrives or loading starts
    LaunchedEffect(response, isLoading) {
        if (response.isNotBlank() || isLoading) {
            val count = scrollState.layoutInfo.totalItemsCount
            if (count > 0) {
                scrollState.animateScrollToItem(count - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Ask SnapDragon", fontSize = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (appContainer.liteRTLMManager.isEngineReady()) TealAccent else HealthRed, CircleShape)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (appContainer.liteRTLMManager.isEngineReady()) "AI Ready" else "AI Initializing...",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DragonDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            // Input bar pinned to bottom with IME awareness
            Surface(
                color = DragonSurface,
                tonalElevation = 3.dp,
                modifier = Modifier.imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        placeholder = { Text("Ask your dragon…", color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Send
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSend = {
                                if (question.isNotBlank() && !isLoading) {
                                    viewModel.askDragon(question)
                                    question = ""
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = DragonBorder,
                            cursorColor = ElectricBlue,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        )
                    )
                    FilledIconButton(
                        onClick = {
                            if (question.isNotBlank()) {
                                viewModel.askDragon(question)
                                question = ""
                            }
                        },
                        enabled = question.isNotBlank() && !isLoading,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = DragonOrange,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Ask")
                    }
                }
            }
        },
        containerColor = DragonDark
    ) { padding ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
        ) {
            // Quick question chips
            item {
                Text("Quick Questions", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickQuestions.forEach { q ->
                        SuggestionChip(
                            onClick = {
                                viewModel.askDragon(q)
                            },
                            label = { Text(q, fontSize = 12.sp) },
                            shape = RoundedCornerShape(20.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = DragonCard,
                                labelColor = TextPrimary
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = DragonBorder
                            )
                        )
                    }
                }
            }

            // Loading state
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DragonCard, RoundedCornerShape(20.dp))
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = DragonOrange,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "SnapDragon is thinking…",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // AI Response
            if (response.isNotBlank()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                DragonOrange.copy(alpha = 0.08f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Text(
                            "🐉 SnapDragon",
                            fontWeight = FontWeight.Bold,
                            color = DragonOrange,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            response,
                            color = if (response.contains("Engine not initialized")) HealthRed else TextPrimary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                        
                        if (response.contains("Engine not initialized")) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "To fix this, push the model via ADB:",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "adb push gemma-4-E2B-it.litertlm /sdcard/Download/",
                                    color = ElectricBlue,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { appContainer.initAction?.invoke() },
                                colors = ButtonDefaults.buttonColors(containerColor = DragonOrange),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Retry Scan for Model", fontSize = 12.sp)
                            }
                        }
                        
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "⚡ On-device AI • LiteRT NPU",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Previous advice
            if (recentAdvice.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("History", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                items(recentAdvice) { advice ->
                    AdviceHistoryRow(advice)
                }
            }
        }
    }
}

@Composable
fun AdviceHistoryRow(advice: AIAdvice) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DragonCard, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            advice.prompt,
            color = ElectricBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            advice.response,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            maxLines = 3
        )
        Spacer(Modifier.height(6.dp))
        Text(
            dateFormat.format(Date(advice.timestamp)),
            color = TextMuted,
            fontSize = 11.sp
        )
    }
}
