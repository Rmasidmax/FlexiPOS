package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Product
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InventoryScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // SAF Launchers for DB backup/restore
    val dbExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri ->
            uri?.let {
                try {
                    val outputStream = context.contentResolver.openOutputStream(it)
                    if (outputStream != null) {
                        viewModel.exportFullBackupDatabaseStream(outputStream)
                    } else {
                        Toast.makeText(context, "Failed to open output write channel", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    val dbImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    if (inputStream != null) {
                        viewModel.restoreFullBackupDatabaseStream(inputStream)
                    } else {
                        Toast.makeText(context, "Failed to open input read channel", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Import error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    // Admin Access Control login portal view
    if (!state.isAdminLoggedIn) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Admin Security Portal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Access is restricted to authorized owners. Enter pass-credential to edit catalog inventory and restore backups.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = state.adminPasswordInput,
                        onValueChange = { viewModel.onAdminPasswordInputChanged(it) },
                        label = { Text("Enter Security Password") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_password_input"),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )

                    state.adminLoginError?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.processAdminLogin() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_login_submit_button")
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify Credentials")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Text(
                    //     text = "Hint: Default security key is 'admin123' or '1234'",
                    //     style = MaterialTheme.typography.labelSmall,
                    //     color = MaterialTheme.colorScheme.outline
                    // )
                }
            }
        }
        return
    }

    // Authenticated Admin Dashboard Layout 
    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedProductToEdit by remember { mutableStateOf<Product?>(null) }
    var isTrashPageOpen by remember { mutableStateOf(false) }

    // Raised states for product entry form dialog so they are accessible to launchers
    var nameInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("") }
    var costInput by remember { mutableStateOf("") }
    var retailInput by remember { mutableStateOf("") }
    var barcodeInput by remember { mutableStateOf("") }
    var logoInput by remember { mutableStateOf("") }

    var isDialogCameraScannerOpen by remember { mutableStateOf(false) }

    // Dynamic Camera Permission handling
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
            if (isGranted) {
                isDialogCameraScannerOpen = true
            } else {
                Toast.makeText(context, "Camera permission is required for barcode scanning in products", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Image Picker Launcher to import image files from local device storage
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                if (inputStream != null) {
                    val imagesDir = File(context.filesDir, "product_images").apply { mkdirs() }
                    val fileName = "product_${System.currentTimeMillis()}.jpg"
                    val file = File(imagesDir, fileName)
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    logoInput = file.absolutePath
                    Toast.makeText(context, "Image successfully imported from storage", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to read image content from storage", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            if (isTrashPageOpen) {
                // Header of Separate Product Trash Archive Page
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { isTrashPageOpen = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .testTag("trash_back_button")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Go back to inventory")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Deactivated Products Trash Bin",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Permanently wipe products or restore them back to standard inventory list",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.logoutAdmin() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Exit Access Mode", tint = MaterialTheme.colorScheme.error)
                    }
                }

                val deletedProducts = state.allProductsAdmin.filter { it.isDeleted }

                if (deletedProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Your Product Trash is clean!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Deactivating any item in the inventory area will store it safely here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(deletedProducts) { product ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("trash_item_row_${product.id}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Display product image in Trash list (the request requires display images in product lists)
                                        if (!product.imageUri.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = product.imageUri,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = product.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                textDecoration = TextDecoration.LineThrough
                                            )
                                            Text(
                                                text = "Category: ${product.category}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            Text(
                                                text = "Cost: ₱${"%.2f".format(product.costPrice)} • Retail: ₱${"%.2f".format(product.retailPrice)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Action buttons: Restore vs. Permanently delete
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilledTonalButton(
                                            onClick = { viewModel.restoreProduct(product.id) },
                                            modifier = Modifier.testTag("restore_btn_${product.id}"),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = "Restore", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore", style = MaterialTheme.typography.bodySmall)
                                        }

                                        Button(
                                            onClick = { viewModel.hardDeleteProduct(product.id) },
                                            modifier = Modifier.testTag("hard_delete_btn_${product.id}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteForever, contentDescription = "Delete Forever", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Wipe", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Main Active Inventory view
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Authorized Operations Manager",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Register products, track pricing, and perform backups offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(onClick = { viewModel.logoutAdmin() }) {
                            Icon(Icons.Default.Logout, contentDescription = "Exit Access Mode", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Open New Product input Modal
                        Button(
                            onClick = {
                                selectedProductToEdit = null
                                nameInput = ""
                                categoryInput = ""
                                costInput = ""
                                retailInput = ""
                                barcodeInput = ""
                                logoInput = ""
                                showAddEditDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_product_fab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Item")
                        }

                        // Switch to Separate Trash Products sub-page
                        val deletedCount = state.allProductsAdmin.count { it.isDeleted }
                        OutlinedButton(
                            onClick = { isTrashPageOpen = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("view_trash_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "View Trash", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Trash Archive ($deletedCount)")
                        }
                    }
                }

                // BACKUP & DISASTER RECOVERY UTILITIES section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Disaster Recovery Database Maintenance Engine (Local SAF)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Save store's entire state to downloads or restore previous structural backup files locally and offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // EXPORT LOCAL BACKUP via CreateDocument SAF
                            Button(
                                onClick = {
                                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                                    dbExportLauncher.launch("flexi_backup_${ts}.db")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export Backup", style = MaterialTheme.typography.bodySmall)
                            }

                            // RESTORE LOCAL BACKUP via GetContent file chooser SAF
                            Button(
                                onClick = {
                                    dbImportLauncher.launch("*/*")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore Backup", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                val activeProducts = state.allProductsAdmin.filter { !it.isDeleted }

                // Listing active items only
                if (activeProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active products exist in the database. Register a new item above.", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeProducts) { product ->
                            AdminProductRow(
                                product = product,
                                onEdit = {
                                    selectedProductToEdit = product
                                    nameInput = product.name
                                    categoryInput = product.category ?: ""
                                    costInput = product.costPrice.toString()
                                    retailInput = product.retailPrice.toString()
                                    barcodeInput = product.barcode ?: ""
                                    logoInput = product.imageUri ?: ""
                                    showAddEditDialog = true
                                },
                                onSoftDelete = {
                                    viewModel.softDeleteProduct(product.id)
                                }
                            )
                        }
                    }
                }
            }
        }

    // Modal dialog for Add / Edit Item
    if (showAddEditDialog) {
        var validationError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = { Text(if (selectedProductToEdit == null) "Add New Product" else "Modify Product Details") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        if (validationError != null) {
                            Text(
                                text = validationError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Product Name *") },
                            modifier = Modifier.fillMaxWidth().testTag("product_name_input_field"),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = categoryInput,
                            onValueChange = { categoryInput = it },
                            label = { Text("Category (e.g. Pastry, Beverages, Service)") },
                            modifier = Modifier.fillMaxWidth().testTag("product_category_input_field"),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = costInput,
                            onValueChange = { costInput = it },
                            label = { Text("Cost Price (₱) *") },
                            modifier = Modifier.fillMaxWidth().testTag("product_cost_input_field"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = retailInput,
                            onValueChange = { retailInput = it },
                            label = { Text("Retail Selling Price (₱) *") },
                            modifier = Modifier.fillMaxWidth().testTag("product_retail_input_field"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = barcodeInput,
                                onValueChange = { barcodeInput = it },
                                label = { Text("Scan/Type Barcode String") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("product_barcode_input_field"),
                                singleLine = true
                            )
                            
                            // Barcode scanning camera icon button beside the barcode field
                            IconButton(
                                onClick = {
                                    if (hasCameraPermission) {
                                        isDialogCameraScannerOpen = true
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                    .testTag("dialog_barcode_scan_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Scan Barcode with Camera",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (logoInput.isNotEmpty()) {
                                        AsyncImage(
                                            model = logoInput,
                                            contentDescription = "Product Image Preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Product Visual Asset",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                imagePickerLauncher.launch("image/*")
                                            },
                                            modifier = Modifier.testTag("dialog_choose_image_btn"),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoLibrary,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                             Spacer(modifier = Modifier.width(6.dp))
                                            Text("Upload Image", style = MaterialTheme.typography.bodySmall)
                                        }

                                        if (logoInput.isNotEmpty()) {
                                             OutlinedButton(
                                                 onClick = { logoInput = "" },
                                                 modifier = Modifier.testTag("dialog_clear_image_btn"),
                                                 contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                             ) {
                                                 Text("Clear", style = MaterialTheme.typography.bodySmall)
                                             }
                                        }
                                    }
                                }
                             }
                             
                             OutlinedTextField(
                                 value = logoInput,
                                 onValueChange = { logoInput = it },
                                 label = { Text("Image Uri / Location link path") },
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .testTag("product_image_input_field"),
                                 singleLine = true
                             )
                         }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Require zero-null text field validations on vital fields
                        val cost = costInput.toDoubleOrNull()
                        val retail = retailInput.toDoubleOrNull()
                        if (nameInput.trim().isEmpty()) {
                            validationError = "Product Name is required."
                        } else if (cost == null || cost <= 0) {
                            validationError = "Valid Cost Price greater than 0 is required."
                        } else if (retail == null || retail <= 0) {
                            validationError = "Valid Retail Price greater than 0 is required."
                        } else {
                            viewModel.addOrUpdateProduct(
                                id = selectedProductToEdit?.id ?: 0,
                                name = nameInput,
                                category = categoryInput,
                                costPrice = cost,
                                retailPrice = retail,
                                barcode = barcodeInput,
                                imageUri = logoInput
                            )
                            showAddEditDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_product_confirmation_button")
                ) {
                    Text("Save Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Camera scanner dialog centered for inserting/editing product barcodes
    if (isDialogCameraScannerOpen && state.settings.isBarcodeScannerEnabled) {
        AlertDialog(
            onDismissRequest = { isDialogCameraScannerOpen = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Product Barcode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { isDialogCameraScannerOpen = false }) {
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
                                barcodeInput = barcode
                                // Play standard audial beep
                                try {
                                    val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                                    toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                Toast.makeText(context, "Barcode Scanned: $barcode", Toast.LENGTH_SHORT).show()
                                isDialogCameraScannerOpen = false
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
                                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            ) {}
                        }
                    } else {
                        Text("Camera permission not granted", color = Color.White)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { isDialogCameraScannerOpen = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminProductRow(
    product: Product,
    onEdit: () -> Unit,
    onSoftDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_item_row_${product.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (product.isDeleted) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!product.imageUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = product.imageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(45.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Inventory, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (product.isDeleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = product.category ?: "General",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = "Cost: ₱${"%.2f".format(product.costPrice)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        Text(
                            text = "Retail: ₱${"%.2f".format(product.retailPrice)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        if (!product.barcode.isNullOrEmpty()) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            Text(
                                text = "Barcode: ${product.barcode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Product", tint = MaterialTheme.colorScheme.primary)
                }

                if (!product.isDeleted) {
                    // Soft archive triggers standard warning logic
                    IconButton(onClick = onSoftDelete) {
                        Icon(Icons.Default.Archive, contentDescription = "Soft Archive Product", tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Soft-Archived", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
        }
    }
}
