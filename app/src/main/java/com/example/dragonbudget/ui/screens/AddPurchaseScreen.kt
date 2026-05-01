package com.example.dragonbudget.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dragonbudget.AppContainer
import com.example.dragonbudget.data.Categories
import com.example.dragonbudget.ui.theme.*
import com.example.dragonbudget.viewmodel.AddPurchaseViewModel
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPurchaseScreen(
    appContainer: AppContainer,
    onBack: () -> Unit
) {
    val viewModel: AddPurchaseViewModel = viewModel(
        factory = AddPurchaseViewModel.Factory(appContainer)
    )
    val saved by viewModel.saved.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val receiptScan by viewModel.receiptScan.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanError by viewModel.scanError.collectAsState()

    var merchant by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Categories.FOOD) }
    var note by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    var isCameraOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                isCameraOpen = true
            }
        }
    )

    // Auto-fill from single scan result
    LaunchedEffect(scanResult) {
        scanResult?.let {
            merchant = it.merchant
            amount = String.format("%.2f", it.amount)
            selectedCategory = it.suggestedCategory
        }
    }

    // Auto-fill merchant from multi-item scan
    LaunchedEffect(receiptScan) {
        receiptScan?.let {
            merchant = it.merchant
        }
    }

    // Navigate back after save
    LaunchedEffect(saved) {
        if (saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Purchase") },
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
        containerColor = DragonDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isCameraOpen) {
                // ── Live Camera Preview ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    CameraPreviewView(
                        modifier = Modifier.fillMaxSize(),
                        onImageCaptured = { uri ->
                            isCameraOpen = false
                            viewModel.scanReceipt(uri)
                        },
                        onError = {
                            isCameraOpen = false
                        }
                    )

                    // Close Camera Button
                    IconButton(
                        onClick = { isCameraOpen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Camera", tint = Color.White)
                    }
                }
            } else if (receiptScan != null && receiptScan!!.items.isNotEmpty()) {
                // ── Multi-Item Scan Results ──
                ScannedItemsList(
                    receiptScan = receiptScan!!,
                    merchant = merchant,
                    onMerchantChange = { merchant = it },
                    onToggleItem = { index -> viewModel.toggleItemSelection(index) },
                    onSaveAll = {
                        viewModel.saveAllScannedItems(merchant, receiptScan!!.items)
                    },
                    onScanAgain = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            isCameraOpen = true
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // ── Manual Entry Form ──
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Scan Receipt Button
                    item {
                        OutlinedButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    isCameraOpen = true
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TealAccent),
                            enabled = !isScanning
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = TealAccent,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Scanning receipt...")
                            } else {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("📷 Scan Receipt (AI)", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Scan result feedback
                    if (scanResult != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "✅ AI detected: ${scanResult!!.merchant} — \$${String.format("%.2f", scanResult!!.amount)}",
                                    modifier = Modifier.padding(12.dp),
                                    color = TealAccent,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    if (scanError != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ElectricBlue.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "⚠️ $scanError",
                                    modifier = Modifier.padding(12.dp),
                                    color = ElectricBlue,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Manual form fields
                    item {
                        OutlinedTextField(
                            value = merchant,
                            onValueChange = { merchant = it },
                            label = { Text("Merchant") },
                            placeholder = { Text("e.g. Chipotle, Target") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = DragonBorder,
                                focusedLabelColor = ElectricBlue,
                                cursorColor = ElectricBlue,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount ($)") },
                            placeholder = { Text("0.00") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = DragonBorder,
                                focusedLabelColor = ElectricBlue,
                                cursorColor = ElectricBlue,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                            )
                        )
                    }

                    item {
                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = "${Categories.EMOJIS[selectedCategory] ?: ""} $selectedCategory",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricBlue,
                                    unfocusedBorderColor = DragonBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                Categories.ALL.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text("${Categories.EMOJIS[category]} $category") },
                                        onClick = {
                                            selectedCategory = category
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = DragonBorder,
                                focusedLabelColor = ElectricBlue,
                                cursorColor = ElectricBlue,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                            )
                        )
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val amt = amount.toDoubleOrNull() ?: 0.0
                                if (merchant.isNotBlank() && amt > 0) {
                                    viewModel.savePurchase(merchant, amt, selectedCategory, note)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(16.dp),
                            enabled = merchant.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0
                        ) {
                            Text(
                                "Save Purchase",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Scanned Items List (multi-item view)
// ──────────────────────────────────────────────

@Composable
fun ScannedItemsList(
    receiptScan: com.example.dragonbudget.data.ReceiptScanResult,
    merchant: String,
    onMerchantChange: (String) -> Unit,
    onToggleItem: (Int) -> Unit,
    onSaveAll: () -> Unit,
    onScanAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCount = receiptScan.items.count { it.selected }
    val selectedTotal = receiptScan.items.filter { it.selected }.sumOf { it.price }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header
        Card(
            colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "🧾 ${receiptScan.items.size} items detected",
                    color = TealAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Receipt total: \$${String.format("%.2f", receiptScan.total)} • Confidence: ${String.format("%.0f", receiptScan.confidence * 100)}%",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Merchant field (editable)
        OutlinedTextField(
            value = merchant,
            onValueChange = onMerchantChange,
            label = { Text("Merchant") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricBlue,
                unfocusedBorderColor = DragonBorder,
                focusedLabelColor = ElectricBlue,
                cursorColor = ElectricBlue,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
            )
        )

        // Items list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(receiptScan.items) { index, item ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.selected) DragonSurface else DragonDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.selected,
                            onCheckedChange = { onToggleItem(index) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = TealAccent,
                                uncheckedColor = DragonBorder
                            )
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.itemName,
                                color = if (item.selected) TextPrimary else TextSecondary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                "${Categories.EMOJIS[item.suggestedCategory] ?: "📦"} ${item.suggestedCategory}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            "\$${String.format("%.2f", item.price)}",
                            color = if (item.selected) TealAccent else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // Bottom bar: selected total + save button
        Card(
            colors = CardDefaults.cardColors(containerColor = DragonSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "$selectedCount items selected",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        "\$${String.format("%.2f", selectedTotal)}",
                        color = TealAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onScanAgain,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Rescan", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onSaveAll,
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedCount > 0
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Save $selectedCount Items",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Camera Preview with Capture & Zoom
// ──────────────────────────────────────────────

@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    
    // Zoom state
    var zoomLevel by remember { mutableFloatStateOf(0f) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val executor = ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        cameraControl = camera.cameraControl
                    } catch (exc: Exception) {
                        Log.e("CameraPreview", "Use case binding failed", exc)
                    }
                }, executor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Zoom Slider Overlay (Right Side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .fillMaxHeight(0.6f)
                .width(44.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = { 
                zoomLevel = (zoomLevel + 0.1f).coerceAtMost(1f)
                cameraControl?.setLinearZoom(zoomLevel)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
            }
            
            Slider(
                value = zoomLevel,
                onValueChange = { 
                    zoomLevel = it
                    cameraControl?.setLinearZoom(it)
                },
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        rotationZ = -90f
                    },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = ElectricBlue,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            
            IconButton(onClick = { 
                zoomLevel = (zoomLevel - 0.1f).coerceAtLeast(0f)
                cameraControl?.setLinearZoom(zoomLevel)
            }) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
            }
        }

        // Capture Button Overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable {
                        val photoFile = java.io.File(
                            context.cacheDir,
                            "receipt_${System.currentTimeMillis()}.jpg"
                        )
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        imageCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    onImageCaptured(Uri.fromFile(photoFile))
                                }

                                override fun onError(exc: ImageCaptureException) {
                                    onError(exc)
                                }
                            }
                        )
                    },
                color = Color.White,
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(4.dp, ElectricBlue)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Capture",
                        tint = ElectricBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
