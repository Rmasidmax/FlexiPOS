package com.example.ui.screens
 
import android.net.Uri
import android.widget.Toast
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AppTheme
import com.example.data.model.StoreType
import com.example.ui.viewmodel.PosViewModel

@Composable
fun SettingsScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var storeNameInput by remember { mutableStateOf(state.settings.storeName) }
    var storeLogoInput by remember { mutableStateOf(state.settings.storeLogoUri) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                if (inputStream != null) {
                    val imagesDir = File(context.filesDir, "logo_images").apply { mkdirs() }
                    val fileName = "logo_${System.currentTimeMillis()}.jpg"
                    val file = File(imagesDir, fileName)
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    storeLogoInput = file.absolutePath
                    viewModel.updateLogoUri(file.absolutePath)
                    Toast.makeText(context, "Store logo updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to read logo image from storage", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import logo image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Synchronize inputs when settings update
    LaunchedEffect(state.settings) {
        storeNameInput = state.settings.storeName
        storeLogoInput = state.settings.storeLogoUri
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Central Settings Panel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configure store identity, change themes, toggles and hardware peripheral profiles",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Card: Identity & Branding Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Business Branding Identity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = storeNameInput,
                        onValueChange = {
                            storeNameInput = it
                            viewModel.updateStoreName(it)
                        },
                        label = { Text("Display Store Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_store_name_input"),
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = storeLogoInput,
                        onValueChange = {
                            storeLogoInput = it
                            viewModel.updateLogoUri(it)
                        },
                        label = { Text("Business Logo URI / Web URL path") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_store_logo_input"),
                        leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                logoPickerLauncher.launch("image/*")
                            },
                            modifier = Modifier.weight(1f).testTag("settings_upload_logo_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Logo Image", style = MaterialTheme.typography.bodySmall)
                        }

                        if (storeLogoInput.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    storeLogoInput = ""
                                    viewModel.updateLogoUri("")
                                },
                                modifier = Modifier.testTag("settings_clear_logo_btn"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Clear", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Logo Preview block
                    Text("Logo Preview:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (storeLogoInput.isNotEmpty()) {
                            AsyncImage(
                                model = storeLogoInput,
                                contentDescription = "Logo Render",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("No Custom Logo Assigned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }

            // Right Card: Global Theme Selector with Color Swatches
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dynamic Layout Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("Remaps primary and accent elements app-wide instantly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                    val themesList = listOf(
                        Triple(AppTheme.CLASSIC_NAVY, "Classic Navy", Color(0xFF1B365D)),
                        Triple(AppTheme.WARM_AMBER, "Warm Amber", Color(0xFFB45309)),
                        Triple(AppTheme.EMERALD_GREEN, "Emerald Green", Color(0xFF047857)),
                        Triple(AppTheme.MINIMALIST_DARK, "Minimalist Dark", Color(0xFF212529))
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        themesList.forEach { th ->
                            val isSelected = state.settings.activeTheme == th.first
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.updateTheme(th.first) }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(th.third)
                                    )
                                    Text(
                                        text = th.second,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Active Theme", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Bottom Settings Column: Barcode / Camera hardware toggle and Store Type select
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Lower: Camera Peripheral controller
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SettingsInputHdmi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hardware Integrations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text("ML Kit Barcode Recognition Rear Camera", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "When enabled: Mount CameraX backend to analyze back camera frames completely local and offline. When disabled, standard text search controls take over.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Switch(
                            checked = state.settings.isBarcodeScannerEnabled,
                            onCheckedChange = { viewModel.updateBarcodeScannerToggle(it) },
                            modifier = Modifier.testTag("barcode_scanner_switch_toggle")
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text("Scan Specific Barcode Only Once", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "When enabled: scanning the same barcode multiple times will not increment its quantity or duplicate it in the cart. A warning is reported instead.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Switch(
                            checked = state.settings.scanBarcodeOnlyOnce,
                            onCheckedChange = { viewModel.updateScanBarcodeOnlyOnceToggle(it) },
                            modifier = Modifier.testTag("scan_only_once_switch_toggle")
                        )
                    }
                }
            }

            // Right Lower: Store Type radical switcher profile (7 types)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DashboardCustomize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Business Type Configuration Selector", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "RADICALLY reconfigures checkout inputs, custom multipliers and workflows instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    var dropdownExpanded by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("store_type_selector_dropdown_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Storefront, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Core Profile Mode: " + state.settings.storeType.name,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StoreType.values().forEach { st ->
                                val descText = when (st) {
                                    StoreType.SARI_SARI -> "Sari-Sari: Enables Tingi & Utang Ledger"
                                    StoreType.CAFE -> "Cafe: Tables Selectors & Category pill tabs"
                                    StoreType.TECH_REPAIR -> "Tech-Repair: Serials Tracking & custom Labor"
                                    StoreType.CAR_WASH -> "Car-Wash: Sizing tiers (S-M-L) & License Plates"
                                    StoreType.LAUNDROMAT -> "Laundromat: Weight scaling factors (kg) & Machines"
                                    StoreType.WATER_STATION -> "Water Refill: Sizes dropdown & delivery charges"
                                    StoreType.BAKERY -> "Bakery: Timeline pickers & 50% Markdown cleared flags"
                                }
                                DropdownMenuItem(
                                    text = { Text(descText) },
                                    onClick = {
                                        viewModel.updateStoreType(st)
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Diagnostics and About card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Diagnostics & System Specs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "View Technical specs, OS diagnostics, SQLite/Room db states, and Support service hotlines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    var showAboutDialog by remember { mutableStateOf(false) }

                    Button(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("open_about_diagnostics_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Diagnostics Panel")
                    }

                    if (showAboutDialog) {
                        AlertDialog(
                            onDismissRequest = { showAboutDialog = false },
                            title = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("System Specifications")
                                    }
                                    IconButton(onClick = { showAboutDialog = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close specifications")
                                    }
                                }
                            },
                            text = {
                                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                                    AboutScreen(modifier = Modifier.fillMaxWidth())
                                }
                            },
                            confirmButton = {
                                Button(onClick = { showAboutDialog = false }) {
                                    Text("Close")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
