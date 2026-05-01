package com.example.dragonbudget.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.dragonbudget.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.dragonbudget.ui.components.SoftCard

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
    val isScanning by viewModel.isScanning.collectAsState()
    val receiptScan by viewModel.receiptScan.collectAsState()
    val scanError by viewModel.scanError.collectAsState()
    val dragonHealth by viewModel.dragonHealth.collectAsState()
    val moneyLeftRatio by viewModel.moneyLeftRatio.collectAsState()

    var merchant by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Categories.FOOD) }
    var note by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    var isCameraOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> if (granted) isCameraOpen = true }
    )

    // Navigate back after save
    LaunchedEffect(saved) {
        if (saved) onBack()
    }

    Scaffold(
        containerColor = DragonBackground
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp)
            ) {
                // ── Title ──
                item {
                    Text(
                        text = "ADD PURCHASES",
                        style = DragonTypography.headlineLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // ── Scan Button ──
                item {
                    SoftCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                isCameraOpen = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Scan Receipt (AI)",
                                style = DragonTypography.headlineMedium,
                                fontSize = 22.sp
                            )
                        }
                    }
                }

                // ── Scan Loading ──
                if (isScanning) {
                    item {
                        SoftCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 3.dp,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    "Reading receipt with Gemma…",
                                    style = DragonTypography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // ── Scan Error ──
                if (!isScanning && scanError != null && receiptScan?.items.isNullOrEmpty()) {
                    item {
                        SoftCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    "Scan failed",
                                    style = DragonTypography.headlineMedium,
                                    fontSize = 20.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    scanError ?: "",
                                    style = DragonTypography.bodyLarge,
                                    color = TextMuted
                                )
                                Spacer(Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SoftCard(
                                        cornerRadius = 12.dp,
                                        backgroundColor = AccentBeige,
                                        onClick = {
                                            viewModel.clearScan()
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                isCameraOpen = true
                                            } else {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        }
                                    ) {
                                        Text(
                                            "Try Again",
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                            style = DragonTypography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    SoftCard(
                                        cornerRadius = 12.dp,
                                        onClick = { viewModel.clearScan() }
                                    ) {
                                        Text(
                                            "Dismiss",
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                            style = DragonTypography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Scanned Receipt Results ──
                val scan = receiptScan
                if (!isScanning && scan != null && scan.items.isNotEmpty()) {
                    item {
                        SoftCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            scan.merchant.ifBlank { "Receipt" },
                                            style = DragonTypography.headlineMedium,
                                            fontSize = 22.sp
                                        )
                                        Text(
                                            "${scan.items.size} items detected",
                                            style = DragonTypography.bodyLarge,
                                            color = TextMuted,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Text(
                                        "$${String.format("%.2f", scan.total)}",
                                        style = DragonTypography.headlineMedium,
                                        fontSize = 22.sp
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(scan.items) { index, item ->
                        SoftCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 14.dp,
                            onClick = { viewModel.toggleItemSelection(index) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.selected,
                                    onCheckedChange = { viewModel.toggleItemSelection(index) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = TextPrimary,
                                        uncheckedColor = TextMuted
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.itemName,
                                        style = DragonTypography.bodyLarge,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.selected) TextPrimary else TextMuted
                                    )
                                    Text(
                                        "${Categories.EMOJIS[item.suggestedCategory] ?: "📦"} ${item.suggestedCategory}",
                                        style = DragonTypography.bodyLarge,
                                        fontSize = 13.sp,
                                        color = TextMuted
                                    )
                                }
                                Text(
                                    "$${String.format("%.2f", item.price)}",
                                    style = DragonTypography.bodyLarge,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.selected) TextPrimary else TextMuted
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SoftCard(
                                modifier = Modifier.weight(1f),
                                backgroundColor = AccentBeige,
                                onClick = {
                                    val selectedCount = scan.items.count { it.selected }
                                    if (selectedCount > 0) {
                                        viewModel.saveAllScannedItems(scan.merchant, scan.items)
                                    }
                                }
                            ) {
                                val selectedCount = scan.items.count { it.selected }
                                Text(
                                    "Save $selectedCount item${if (selectedCount == 1) "" else "s"}",
                                    modifier = Modifier
                                        .padding(vertical = 14.dp)
                                        .fillMaxWidth(),
                                    style = DragonTypography.headlineMedium,
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            SoftCard(
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.clearScan() }
                            ) {
                                Text(
                                    "Discard",
                                    modifier = Modifier
                                        .padding(vertical = 14.dp)
                                        .fillMaxWidth(),
                                    style = DragonTypography.headlineMedium,
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // ── Manual entry form ──
                // Hidden while a scan is in progress or showing results — the
                // scanned-receipt section above replaces this whole block.
                val showManualForm = !isScanning && receiptScan == null
                if (showManualForm) {
                    item {
                        SoftInputField(value = merchant, onValueChange = { merchant = it }, placeholder = "Merchant")
                    }
                    item {
                        SoftInputField(value = amount, onValueChange = { amount = it }, placeholder = "Amount ($)", keyboardType = KeyboardType.Decimal)
                    }
                    item {
                        SoftCategoryPicker(
                            selected = selectedCategory,
                            expanded = categoryExpanded,
                            onExpand = { categoryExpanded = it },
                            onSelect = { selectedCategory = it }
                        )
                    }
                    item {
                        SoftInputField(value = note, onValueChange = { note = it }, placeholder = "Note (optional)")
                    }

                    // ── Save Button ──
                    item {
                        SoftCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = AccentBeige,
                            onClick = {
                                val amt = amount.toDoubleOrNull() ?: 0.0
                                if (merchant.isNotBlank() && amt > 0) {
                                    viewModel.savePurchase(merchant, amt, selectedCategory, note)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "Save Purchase",
                                    style = DragonTypography.headlineMedium,
                                    fontSize = 24.sp,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.width(12.dp))
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    modifier = Modifier.size(36.dp),
                                    tint = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // ── Small Dragon in Corner (mirrors health + money-left) ──
            Image(
                painter = painterResource(
                    id = com.example.dragonbudget.engine.DragonStateEngine
                        .getDragonDrawable(dragonHealth, moneyLeftRatio)
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp)
            )
            
            // ── Camera Overlay ──
            if (isCameraOpen) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    CameraPreviewView(
                        modifier = Modifier.fillMaxSize(),
                        onImageCaptured = { uri ->
                            isCameraOpen = false
                            viewModel.scanReceipt(uri)
                        },
                        onError = { isCameraOpen = false }
                    )
                    IconButton(
                        onClick = { isCameraOpen = false },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SoftInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(placeholder, style = DragonTypography.bodyLarge, color = TextMuted)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                cursorBrush = SolidColor(TextPrimary),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType)
            )
        }
    }
}

@Composable
fun SoftCategoryPicker(
    selected: String,
    expanded: Boolean,
    onExpand: (Boolean) -> Unit,
    onSelect: (String) -> Unit
) {
    Box {
        SoftCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onExpand(true) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${Categories.EMOJIS[selected] ?: ""} $selected",
                    style = DragonTypography.bodyLarge,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(32.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpand(false) },
            modifier = Modifier.background(DragonSurface)
        ) {
            Categories.ALL.forEach { category ->
                DropdownMenuItem(
                    text = { Text("${Categories.EMOJIS[category]} $category", fontWeight = FontWeight.Bold) },
                    onClick = {
                        onSelect(category)
                        onExpand(false)
                    }
                )
            }
        }
    }
}

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

    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var minZoom by remember { mutableStateOf(1f) }
    var maxZoom by remember { mutableStateOf(1f) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    try {
                        cameraProvider.unbindAll()
                        val cam = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                        camera = cam
                        cam.cameraInfo.zoomState.observe(lifecycleOwner) { state ->
                            minZoom = state.minZoomRatio
                            maxZoom = state.maxZoomRatio
                            zoomRatio = state.zoomRatio
                        }
                    } catch (exc: Exception) { Log.e("Camera", "Failed", exc) }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Zoom controls (right edge) ──
        if (maxZoom > minZoom) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${String.format("%.1f", zoomRatio)}x",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = {
                        val next = (zoomRatio + 0.5f).coerceAtMost(maxZoom)
                        camera?.cameraControl?.setZoomRatio(next)
                    }
                ) { Icon(Icons.Default.Add, "Zoom in", tint = Color.White) }

                Slider(
                    value = zoomRatio,
                    onValueChange = { v -> camera?.cameraControl?.setZoomRatio(v) },
                    valueRange = minZoom..maxZoom,
                    modifier = Modifier
                        .height(180.dp)
                        .width(40.dp)
                        .graphicsLayer { rotationZ = -90f },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )

                IconButton(
                    onClick = {
                        val next = (zoomRatio - 0.5f).coerceAtLeast(minZoom)
                        camera?.cameraControl?.setZoomRatio(next)
                    }
                ) { Icon(Icons.Default.Remove, "Zoom out", tint = Color.White) }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .size(80.dp)
                .background(Color.White, CircleShape)
                .clickable {
                    val file = java.io.File(context.cacheDir, "scan.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                    imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                onImageCaptured(android.net.Uri.fromFile(file))
                            }
                            override fun onError(exc: ImageCaptureException) {
                                onError(exc)
                            }
                        })
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Camera, null, modifier = Modifier.size(40.dp), tint = Color.Black)
        }
    }
}
