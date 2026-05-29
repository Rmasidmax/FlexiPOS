package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.*
import com.example.ui.viewmodel.PosViewModel
import com.example.data.model.StoreType

class MainActivity : ComponentActivity() {
    private val viewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            MyApplicationTheme(theme = state.settings.activeTheme) {
                var currentTab by remember { mutableStateOf(0) }

                // Safe fallback: redirect away from Sari-sari Utang ledger if StoreType switches profiles
                LaunchedEffect(state.settings.storeType) {
                    if (state.settings.storeType != StoreType.SARI_SARI && currentTab == 2) {
                        currentTab = 0
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Checkout Area") },
                                label = { Text("Checkout") }
                            )
                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = { Icon(Icons.Default.Inventory, contentDescription = "Inventory Area") },
                                label = { Text("Inventory") }
                            )
                            if (state.settings.storeType == StoreType.SARI_SARI) {
                                NavigationBarItem(
                                    selected = currentTab == 2,
                                    onClick = { currentTab = 2 },
                                    icon = { Icon(Icons.Default.Receipt, contentDescription = "Utang Ledger") },
                                    label = { Text("Utang") }
                                )
                            }
                            NavigationBarItem(
                                selected = currentTab == 3,
                                onClick = { currentTab = 3 },
                                icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics Core Reports") },
                                label = { Text("Analytics") }
                            )
                            NavigationBarItem(
                                selected = currentTab == 4,
                                onClick = { currentTab = 4 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Branding Settings") },
                                label = { Text("Settings") }
                            )
                        }
                    }
                ) { innerPadding ->
                    // Notification dispatchers
                    LaunchedEffect(state.error, state.successMessage) {
                        state.error?.let {
                            Toast.makeText(this@MainActivity, "⚠️ Error: $it", Toast.LENGTH_LONG).show()
                            viewModel.clearNotifications()
                        }
                        state.successMessage?.let {
                            Toast.makeText(this@MainActivity, "✅ $it", Toast.LENGTH_SHORT).show()
                            viewModel.clearNotifications()
                        }
                    }

                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            0 -> DashboardScreen(viewModel = viewModel)
                            1 -> InventoryScreen(viewModel = viewModel)
                            2 -> UtangScreen(viewModel = viewModel)
                            3 -> AnalyticsScreen(viewModel = viewModel)
                            4 -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
