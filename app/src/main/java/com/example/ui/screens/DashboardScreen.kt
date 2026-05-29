package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var isCatalogModalOpen by remember { mutableStateOf(false) }
    var isCameraScannerOpen by remember { mutableStateOf(false) }
    var isPaymentExpanded by remember { mutableStateOf(false) }

    var cartTabOffset by remember { mutableStateOf(Offset.Zero) }
    var payTabOffset by remember { mutableStateOf(Offset.Zero) }

    // Request Camera permission dynamically
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) {
                viewModel.updateBarcodeScannerToggle(false)
                Toast.makeText(context, "Camera permission is required for barcode scanning", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Synchronize permission request if user enables barcode scanning
    LaunchedEffect(state.settings.isBarcodeScannerEnabled) {
        if (state.settings.isBarcodeScannerEnabled && !hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // Main checkout structure
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Left Column: Active Cart Session & Store Context (takes up 100% space as it's fullscreen)
        if (!isPaymentExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxSize(), // Full screen card
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with Store Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.settings.storeLogoUri.isNotEmpty()) {
                            AsyncImage(
                                model = state.settings.storeLogoUri,
                                contentDescription = "Store Logo",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column {
                            Text(
                                text = state.settings.storeName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Platform: ${state.settings.storeType.name} Checkout",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // Quick Barcode Scanning toggle icon indicator
                    IconButton(
                        onClick = {
                            val next = !state.settings.isBarcodeScannerEnabled
                            viewModel.updateBarcodeScannerToggle(next)
                        },
                        modifier = Modifier.testTag("barcode_camera_toggle")
                    ) {
                        Icon(
                            imageVector = if (state.settings.isBarcodeScannerEnabled) Icons.Default.QrCodeScanner else Icons.Default.Search,
                            contentDescription = "Toggle Scan Mode",
                            tint = if (state.settings.isBarcodeScannerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Store-Type Specific Header Custom Widgets
                StoreTypeSpecificHeaderControls(state, viewModel)

                // Compact Action Bar: Compact Search & View Catalog Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compact Search Input (for scanning or quick filtering)
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(46.dp)
                            .testTag("product_search_input"),
                        placeholder = { Text("Filter or scan code...", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = OutlinedTextFieldDefaults.colors()
                    )

                    // Browse Catalog Button - styled nicely with a custom icon
                    FilledTonalButton(
                        onClick = { isCatalogModalOpen = true },
                        modifier = Modifier
                            .height(46.dp)
                            .testTag("browse_catalog_button"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Browse Catalog",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Product List",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Camera Scanner Scan Button - visible ONLY if barcode scanner is enabled!
                    if (state.settings.isBarcodeScannerEnabled) {
                        IconButton(
                            onClick = { isCameraScannerOpen = true },
                            modifier = Modifier
                                .size(46.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                .testTag("scan_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Scan Barcode with Camera",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Cashier Cart Session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Spacious Active Cart items grid/list
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    if (state.activeCart.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Cart is currently empty",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.activeCart) { item ->
                                CartRowItem(item, viewModel)
                            }
                        }
                    }
                }
            }
        }

        // LAYER 2: Floating Compact Overlays (drawn last so they hover cleanly on top of the fullscreen panels)

        // 2A. Floating compact Cart tab (shown at CenterStart when payment console is fullscreen)
        if (isPaymentExpanded) {
            Card(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .offset {
                        IntOffset(
                            cartTabOffset.x.roundToInt(),
                            cartTabOffset.y.roundToInt()
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            cartTabOffset = Offset(
                                x = cartTabOffset.x + dragAmount.x,
                                y = cartTabOffset.y + dragAmount.y
                            )
                        }
                    }
                    .width(72.dp)
                    .wrapContentHeight()
                    .zIndex(10f)
                    .clickable { isPaymentExpanded = false },
                shape = RoundedCornerShape(36.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.TopEnd,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        IconButton(
                            onClick = { isPaymentExpanded = false },
                            modifier = Modifier.testTag("compact_cart_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Open Cart",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        val totalQuantity = state.activeCart.sumOf { it.quantity }.toInt()
                        if (totalQuantity > 0) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(MaterialTheme.colorScheme.error, RoundedCornerShape(10.dp))
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = totalQuantity.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = "Cart",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 2B. Floating compact Pay tab (shown at CenterEnd when active cart is fullscreen)
        if (!isPaymentExpanded) {
            Card(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .offset {
                        IntOffset(
                            payTabOffset.x.roundToInt(),
                            payTabOffset.y.roundToInt()
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            payTabOffset = Offset(
                                x = payTabOffset.x + dragAmount.x,
                                y = payTabOffset.y + dragAmount.y
                            )
                        }
                    }
                    .width(72.dp)
                    .wrapContentHeight()
                    .zIndex(10f)
                    .clickable { isPaymentExpanded = true },
                shape = RoundedCornerShape(36.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { isPaymentExpanded = true },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .testTag("compact_payment_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Open Payment Console",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = "Pay",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Right Column: Checkout billing details, totals, and quick tender actions which expands full screen when active
        if (isPaymentExpanded) {
            Card(
                modifier = Modifier
                    .fillMaxSize(), // Full screen card: inactive card floats over it
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Payment Console",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { isPaymentExpanded = !isPaymentExpanded }
                    ) {
                        Icon(
                            imageVector = if (isPaymentExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Toggle Fullfullscreen Console",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Subtotal, Discount & Checkout calculation dashboard
                val subtotal = state.activeCart.sumOf { it.total }
                
                // Extra modifiers applying on subtotal dynamically
                var storeModifiedSubtotal = subtotal
                if (state.settings.storeType == StoreType.CAR_WASH) {
                    val factor = when {
                        state.activeVehicleSize.contains("Large", ignoreCase = true) -> 1.5
                        state.activeVehicleSize.contains("Medium", ignoreCase = true) -> 1.0
                        else -> 0.8
                    }
                    storeModifiedSubtotal *= factor
                }
                if (state.settings.storeType == StoreType.TECH_REPAIR) {
                    val labor = state.activeLaborFee.toDoubleOrNull() ?: 0.0
                    storeModifiedSubtotal += labor
                }
                if (state.settings.storeType == StoreType.WATER_STATION) {
                    storeModifiedSubtotal += state.activeWaterSurcharge
                }

                val finalTotal = (storeModifiedSubtotal - state.cartDiscount).coerceAtLeast(0.0)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal:", style = MaterialTheme.typography.bodyMedium)
                        Text("₱${String.format("%.2f", subtotal)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }

                    if (state.settings.storeType == StoreType.CAR_WASH) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Veh. Multiplier (${state.activeVehicleSize}):", style = MaterialTheme.typography.bodySmall)
                            Text("₱${String.format("%.2f", storeModifiedSubtotal)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (state.settings.storeType == StoreType.TECH_REPAIR && (state.activeLaborFee.toDoubleOrNull() ?: 0.0) > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Service Labor Added:", style = MaterialTheme.typography.bodySmall)
                            Text("+₱${state.activeLaborFee}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (state.settings.storeType == StoreType.WATER_STATION && state.activeWaterSurcharge > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Delivery Surcharge:", style = MaterialTheme.typography.bodySmall)
                            Text("+₱${state.activeWaterSurcharge}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Discount (₱):", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.applyCartDiscount((state.cartDiscount - 10).coerceAtLeast(0.0)) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "sub discount", modifier = Modifier.size(18.dp))
                            }
                            Text("₱${state.cartDiscount.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                            IconButton(
                                onClick = { viewModel.applyCartDiscount(state.cartDiscount + 10) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "add discount", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GRAND TOTAL:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("₱${String.format("%.2f", finalTotal)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.weight(0.1f))

                // Cash Payment Change Core calculator 
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.amountGivenInput,
                        onValueChange = { viewModel.setAmountGiven(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("cash_given_input"),
                        label = { Text("Amount Rendered / Given (₱)", style = MaterialTheme.typography.bodyMedium) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        singleLine = true
                    )

                    // Quick Tender denominations row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val notes = listOf(20.0, 50.0, 100.0, 500.0, 1000.0)
                        notes.forEach { note ->
                            Button(
                                onClick = { viewModel.setAmountGiven(note.toString()) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            ) {
                                Text("₱${note.toInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Dynamically calculate and cleanly display the exact change return value
                    val cashPaid = state.amountGivenInput.toDoubleOrNull() ?: 0.0
                    val changeDue = if (cashPaid >= finalTotal) cashPaid - finalTotal else 0.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CHANGE DUE:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = "₱${String.format("%.2f", changeDue)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.2f))

                // Final Action buttons Row - right-aligned to avoid wide distorted button widths
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clear Session item Button
                    OutlinedButton(
                        onClick = { viewModel.clearCart() },
                        modifier = Modifier
                            .width(130.dp)
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Cart", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Execute Checkout Invoice Action
                    Button(
                        onClick = {
                            viewModel.processCheckout()
                        },
                        modifier = Modifier
                            .width(160.dp)
                            .height(44.dp)
                            .testTag("checkout_transact_button"),
                        shape = RoundedCornerShape(8.dp),
                        enabled = state.activeCart.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Transact POS", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

    // Modal Camera scanner dialog centered
    if (isCameraScannerOpen && state.settings.isBarcodeScannerEnabled) {
        AlertDialog(
            onDismissRequest = { isCameraScannerOpen = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Barcode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { isCameraScannerOpen = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close Scanner")
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        BarcodeScannerCameraView(
                            onBarcodeReceived = { barcode ->
                                viewModel.handleScannedBarcode(barcode)
                                // Play audial beep
                                try {
                                    val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                Toast.makeText(context, "Scanned: $barcode", Toast.LENGTH_SHORT).show()
                            }
                        )
                        // Aiming Overlay grid border
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(Color.Transparent)
                                .align(Alignment.Center)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = Color.Transparent,
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            ) {}
                        }
                    } else {
                        Text("Camera permission not granted", color = Color.White)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { isCameraScannerOpen = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Modal Store Product Catalog Dialog
    if (isCatalogModalOpen) {
        AlertDialog(
            onDismissRequest = { isCatalogModalOpen = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Browse Store Catalog",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { isCatalogModalOpen = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close catalog")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Small local search bar
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("modal_product_search_input"),
                        placeholder = { Text("Search catalog products...", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true
                    )

                    // Category Filters Row
                    val modalCategories = listOf("All") + state.products.map { it.category }.distinct()
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        modalCategories.forEach { cat ->
                            val isSelected = state.selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onCategorySelected(cat) },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // Product Grid
                    val filteredProducts = state.products.filter { prod ->
                        val matchesCat = state.selectedCategory == "All" || prod.category == state.selectedCategory
                        val matchesQuery = prod.name.contains(state.searchQuery, ignoreCase = true) ||
                                prod.category.contains(state.searchQuery, ignoreCase = true) ||
                                (prod.barcode?.contains(state.searchQuery, ignoreCase = true) == true)
                        matchesCat && matchesQuery
                    }

                    if (filteredProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No compatible products found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 130.dp),
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredProducts) { product ->
                                ProductCatalogCard(product, state, viewModel)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { isCatalogModalOpen = false },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Done")
                }
            }
        )
    }

    // Receipt Log Completed dialog popup overlay 
    state.activeReceipt?.let { receipt ->
        AlertDialog(
            onDismissRequest = { viewModel.clearCart() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checkout Completed")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("TRANS-ID: ${receipt.id}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider()
                    
                    val items = JsonUtils.listFromJson(receipt.itemsJson)
                    LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                        items(items) { item ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                val qtyFormatted = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format("%.2f", item.quantity)
                                Text("${item.product.name} (x$qtyFormatted)", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1.2f))
                                Text("₱${String.format("%.2f", item.total)}", modifier = Modifier.weight(0.8f))
                            }
                        }
                    }
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal:")
                        Text("₱${String.format("%.2f", receipt.subtotal)}")
                    }
                    if (receipt.discount > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount applied:")
                            Text("-₱${String.format("%.2f", receipt.discount)}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOTAL BILL:", fontWeight = FontWeight.Bold)
                        Text("₱${String.format("%.2f", receipt.total)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Amount Rendered:")
                        Text("₱${String.format("%.2f", receipt.amountGiven)}")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Return Change Due:", fontWeight = FontWeight.SemiBold)
                        Text("₱${String.format("%.2f", receipt.changeDue)}", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.triggerBluetoothThermalPrint(receipt)
                    }
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print ESC/POS")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.clearCart() }) {
                    Text("Close Invoice")
                }
            }
        )
    }
}

@Composable
fun ProductCatalogCard(
    product: Product,
    state: PosUiState,
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    var showQuickDetailsDialog by remember { mutableStateOf(false) }

    // Bakery markdown 50% discount mod
    var isBakeryClearanceActive by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
            .clickable {
                if (state.settings.storeType == StoreType.BAKERY) {
                    showQuickDetailsDialog = true
                } else if (state.settings.storeType == StoreType.SARI_SARI) {
                    // Show Tingi options selector dialog
                    showQuickDetailsDialog = true
                } else {
                    viewModel.addToCart(product, 1.0)
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!product.imageUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = product.imageUri,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                // Bakery 50% clearance flash tag
                if (state.settings.storeType == StoreType.BAKERY && isBakeryClearanceActive) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.error)
                            .padding(4.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text("50% OFF", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }

            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val priceToDisplay = if (state.settings.storeType == StoreType.BAKERY && isBakeryClearanceActive) {
                        product.retailPrice * 0.5
                    } else {
                        product.retailPrice
                    }

                    Text(
                        text = "₱${String.format("%.2f", priceToDisplay)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Quick click add badge for other store types
                    if (state.settings.storeType != StoreType.BAKERY && state.settings.storeType != StoreType.SARI_SARI) {
                        IconButton(
                            onClick = {
                                viewModel.addToCart(product, 1.0)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Add to cart", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    // Overlay details dialog helper for Bakery Clearance modifier and Sari-sari Tingi fractions
    if (showQuickDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showQuickDetailsDialog = false },
            title = { Text(product.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.settings.storeType == StoreType.BAKERY) {
                        Text("Bakery checkout modifier options:")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isBakeryClearanceActive = !isBakeryClearanceActive }
                                .padding(8.dp)
                        ) {
                            Checkbox(checked = isBakeryClearanceActive, onCheckedChange = { isBakeryClearanceActive = it })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply 50% Markdown Clearance Discount Modifier")
                        }
                        Text("Calculated: ₱${String.format("%.2f", if (isBakeryClearanceActive) product.retailPrice * 0.5 else product.retailPrice)}")
                    }

                    if (state.settings.storeType == StoreType.SARI_SARI) {
                        Text("Sari-Sari 'Tingi' fraction configurations:")
                        val portions = listOf(
                            "Full Pack / Pack Price" to 1.0,
                            "1/2 Pack Portion" to 0.5,
                            "1/4 Pack Portion" to 0.25,
                            "1/10 Pack Unit (Stick/Piece)" to 0.1,
                            "1/20 Pack Unit (Stick/Piece)" to 0.05
                        )
                        var selectedPortion by remember { mutableStateOf(portions[0]) }
                        
                        portions.forEach { port ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPortion = port }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedPortion == port, onClick = { selectedPortion = port })
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(port.first)
                                    Text("Retail Price: ₱${String.format("%.2f", product.retailPrice * port.second)}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.addToCart(
                                    product = product,
                                    quantity = selectedPortion.second,
                                    customPrice = product.retailPrice,
                                    optionText = selectedPortion.first
                                )
                                showQuickDetailsDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Tingi Item to Cart")
                        }
                    }
                }
            },
            confirmButton = {
                if (state.settings.storeType == StoreType.BAKERY) {
                    Button(
                        onClick = {
                            val price = if (isBakeryClearanceActive) product.retailPrice * 0.5 else product.retailPrice
                            viewModel.addToCart(
                                product = product,
                                quantity = 1.0,
                                customPrice = price,
                                optionText = if (isBakeryClearanceActive) "50% Clearance Markdown" else null
                            )
                            showQuickDetailsDialog = false
                        }
                    ) {
                        Text("Add to Basket")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickDetailsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CartRowItem(
    item: CartItem,
    viewModel: PosViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(text = item.product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!item.selectedOption.isNullOrEmpty()) {
                    Text(text = item.selectedOption, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
                Text(text = "₱${String.format("%.2f", item.displayPrice)} each", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }

            // Quantity increments / fractional volumes modifiers
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = { viewModel.updateCartQuantity(item, item.quantity - 1.0) }) {
                    Icon(Icons.Default.Remove, contentDescription = "sub qty", modifier = Modifier.size(18.dp))
                }
                
                val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format("%.2f", item.quantity)
                Text(text = qtyStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                IconButton(onClick = { viewModel.updateCartQuantity(item, item.quantity + 1.0) }) {
                    Icon(Icons.Default.Add, contentDescription = "add qty", modifier = Modifier.size(18.dp))
                }
            }

            Column(
                modifier = Modifier.weight(0.8f),
                horizontalAlignment = Alignment.End
            ) {
                Text(text = "₱${String.format("%.2f", item.total)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold)
                IconButton(
                    onClick = { viewModel.removeFromCart(item) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Delete item", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun StoreTypeSpecificHeaderControls(
    state: PosUiState,
    viewModel: PosViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Store-Type Specific Context Engine (${state.settings.storeType.name})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            when (state.settings.storeType) {
                StoreType.CAFE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Order Dining Table Selector:", style = MaterialTheme.typography.bodySmall)
                        val tables = listOf("Table 1", "Table 2", "Table 3", "Table 4", "Table 5", "Table 6")
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            Button(onClick = { expanded = true }, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text(state.activeTableNo, style = MaterialTheme.typography.bodySmall)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                tables.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t) },
                                        onClick = {
                                            viewModel.setStoreTypeOption(t)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                StoreType.CAR_WASH -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = state.activePlateNumber,
                                onValueChange = { viewModel.updatePlateNumber(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                label = { Text("Plate Number Tracker") },
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true
                            )

                            var expanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1.1f)) {
                                OutlinedTextField(
                                    value = state.activeVehicleSize,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Sizing Tier multiplier") },
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = null) } },
                                    modifier = Modifier.height(52.dp)
                                )
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    val tiers = listOf("Small (Sedan - 0.8x)", "Medium (Crossover - 1.0x)", "Large (SUV / Van - 1.5x)")
                                    tiers.forEach { tier ->
                                        DropdownMenuItem(
                                            text = { Text(tier) },
                                            onClick = {
                                                viewModel.setStoreTypeOption(tier)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                StoreType.TECH_REPAIR -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.activeSerialInput,
                            onValueChange = { viewModel.updateTechSerialInput(it) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            label = { Text("Device Serial Number") },
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = state.activeLaborFee,
                            onValueChange = { viewModel.updateTechLaborFee(it) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            label = { Text("Service Labor Fee (₱)") },
                            textStyle = MaterialTheme.typography.bodySmall,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }

                StoreType.LAUNDROMAT -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.activeMachineId,
                            onValueChange = { viewModel.updateLaundryMachine(it) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            label = { Text("Mach. Unit ID Assigned") },
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = state.activeLaundryWeight,
                            onValueChange = { viewModel.updateLaundryWeight(it) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            label = { Text("Total Weight (KG)") },
                            textStyle = MaterialTheme.typography.bodySmall,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }

                StoreType.WATER_STATION -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1.2f)) {
                            OutlinedTextField(
                                value = state.activeWaterOption,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Refill/Container Type") },
                                textStyle = MaterialTheme.typography.bodySmall,
                                trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = null) } },
                                modifier = Modifier.height(52.dp)
                            )
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                val options = listOf("Slim 5-Gal Refill", "Round 5-Gal Refill", "1-Gal Refill", "New Slim Bottle Pack")
                                options.forEach { o ->
                                    DropdownMenuItem(text = { Text(o) }, onClick = {
                                        viewModel.setStoreTypeOption(o)
                                        expanded = false
                                    })
                                }
                            }
                        }

                        OutlinedTextField(
                            value = state.activeWaterSurcharge.toString(),
                            onValueChange = { viewModel.updateWaterSurcharge(it.toDoubleOrNull() ?: 0.0) },
                            modifier = Modifier
                                .weight(0.8f)
                                .height(52.dp),
                            label = { Text("Deliv. Surcharge (₱)") },
                            textStyle = MaterialTheme.typography.bodySmall,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }

                StoreType.BAKERY -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Pre-Order Pick-Up timeline setup:", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = state.activeBakeryPreOrderDate,
                            onValueChange = { viewModel.updateBakeryPreOrderDate(it) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            label = { Text("Pickup Date") },
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                    }
                }

                else -> {
                    // Sari-sari shows notification about Tingi configurations clicked directly inside items 
                    Text(
                        text = "📌 Sari-Sari 'Tingi' is activated! Click any of the catalog items below to select custom part-portions, or log debt via the Utang ledger tab above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun BarcodeScannerCameraView(
    onBarcodeReceived: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    var lastScannedBarcode by remember { mutableStateOf("") }
    var lastScanTime by remember { mutableStateOf(0L) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner) {
        var isDisposed = false
        var isClosed = false
        var activeProvider: ProcessCameraProvider? = null
        var activePreview: Preview? = null
        var activeAnalysis: ImageAnalysis? = null

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            if (isDisposed) {
                try {
                    val provider = cameraProviderFuture.get()
                    provider.unbindAll()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return@addListener
            }
            try {
                val provider = cameraProviderFuture.get()
                activeProvider = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                activePreview = preview

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                activeAnalysis = imageAnalysis

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (isDisposed || isClosed) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        try {
                            synchronized(barcodeScanner) {
                                if (!isClosed && !isDisposed) {
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            if (isDisposed || isClosed) return@addOnSuccessListener
                                            for (barcode in barcodes) {
                                                val rawValue = barcode.rawValue
                                                if (!rawValue.isNullOrEmpty()) {
                                                    // Simple debounce mechanism to avoid rapid double scans (800ms window)
                                                    val now = System.currentTimeMillis()
                                                    if (rawValue != lastScannedBarcode || now - lastScanTime > 800) {
                                                        lastScannedBarcode = rawValue
                                                        lastScanTime = now
                                                        onBarcodeReceived(rawValue)
                                                    }
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            imageProxy.close()
                        }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            isDisposed = true
            isClosed = true
            try {
                activePreview?.setSurfaceProvider(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                activeAnalysis?.clearAnalyzer()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                activeProvider?.unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                cameraExecutor.shutdown()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                synchronized(barcodeScanner) {
                    barcodeScanner.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}
