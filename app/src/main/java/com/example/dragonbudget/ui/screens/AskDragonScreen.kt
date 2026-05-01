package com.example.dragonbudget.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.dragonbudget.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dragonbudget.AppContainer
import com.example.dragonbudget.data.AIAdvice
import com.example.dragonbudget.ui.components.SoftCard
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
    val dragonName by viewModel.dragonName.collectAsState()
    val dragonHealth by viewModel.dragonHealth.collectAsState()
    val moneyLeftRatio by viewModel.moneyLeftRatio.collectAsState()

    var question by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()

    val quickQuestions = listOf(
        "Why is my dragon tired?",
        "Budget?",
        "How do I level up?",
        "Can I afford food today?",
    )

    Scaffold(
        containerColor = DragonBackground
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp)
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
                            text = "ASK DRAGON",
                            style = DragonTypography.headlineLarge,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.width(48.dp))
                    }
                }

                // ── Quick Questions ──
                item {
                    Column {
                        Text(
                            "Quick Questions",
                            style = DragonTypography.headlineMedium,
                            fontSize = 22.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            quickQuestions.forEach { q ->
                                SoftCard(
                                    cornerRadius = 16.dp,
                                    onClick = { viewModel.askDragon(q) }
                                ) {
                                    Text(
                                        q,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        style = DragonTypography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // ── History ──
                item {
                    Text(
                        "History",
                        style = DragonTypography.headlineMedium,
                        fontSize = 22.sp
                    )
                }

                items(recentAdvice) { advice ->
                    AdviceCard(advice)
                }

                // Current Response
                if (response.isNotBlank() || isLoading) {
                    item {
                        AdviceCard(
                            AIAdvice(
                                prompt = if (isLoading) "Thinking..." else "Latest Question",
                                response = if (isLoading) "$dragonName is thinking..." else response
                            )
                        )
                    }
                }
            }

            // ── Small Dragon in Corner (mirrors health + money-left) ──
            Image(
                painter = painterResource(
                    id = com.example.dragonbudget.engine.DragonStateEngine.dragonFrameToDrawable(
                        com.example.dragonbudget.engine.DragonStateEngine.getDragonFrameForHealth(
                            (moneyLeftRatio.coerceIn(0f, 1f) * 100).toInt()
                        )
                    )
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp)
            )

            // ── Bottom Input ──
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
            ) {
                SoftCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Ask your dragon...",
                                style = DragonTypography.titleLarge,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(Color(0xFFB5AD9E), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = question,
                                    onValueChange = { question = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    ),
                                    cursorBrush = SolidColor(TextPrimary),
                                    singleLine = true
                                )
                            }
                        }
                        
                        Spacer(Modifier.width(12.dp))
                        
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    if (question.isNotBlank()) {
                                        viewModel.askDragon(question)
                                        question = ""
                                    }
                                },
                            tint = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdviceCard(advice: AIAdvice) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.US) }
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                advice.prompt,
                style = DragonTypography.headlineMedium,
                fontSize = 22.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                advice.response,
                style = DragonTypography.bodyLarge,
                fontSize = 18.sp,
                color = TextSecondary,
                lineHeight = 24.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                dateFormat.format(Date(advice.timestamp)),
                style = DragonTypography.bodyMedium,
                color = TextMuted
            )
        }
    }
}
