package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.widthIn(max = 680.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "FlexiPOS Local Business POS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "High-Velocity Offline-First Merchant Workspace",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            HorizontalDivider()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Card: Application Profile Specs
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AppSettingsAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Application Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        DetailSpecRow("App Name", "FlexiPOS Mobile")
                        DetailSpecRow("Release Build", "v2.4.6-LTS")
                        DetailSpecRow("Build Version Code", "104")
                        DetailSpecRow("Target SDK Framework", "Android SDK 36")
                    }
                }

                // Right Card: Hardware & OS Architecture Summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("System Architecture Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        DetailSpecRow("Device Model", "${Build.MANUFACTURER} ${Build.MODEL}")
                        DetailSpecRow("Android API Version", "API ${Build.VERSION.SDK_INT}")
                        DetailSpecRow("CPU ABI Platform", Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a")
                        DetailSpecRow("Db Storage Engine", "SQLite / Room v2.7.0")
                    }
                }
            }

            // Developer profile card block
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Technical Developer Profile & Credentials",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "For custom feature configurations, local printer calibrations, Bluetooth hardware linkages, or premium cloud backups synchronization, contact technical support.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Email Desk Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Technical Support Email", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("buildnchill.tech@gmail.com", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                            }
                        }

                        // Hotlines Desk Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Contact Support Hotline", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("+63 992 207 3188", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Explicitly render a standardized copyright legal compliance line docked cleanly at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "© 2026 FlexiPOS Offline POS Systems Inc. | Standardized Copyright Legal Compliance. All Rights Reserved. Fully Encrypted Secure Storage.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DetailSpecRow(label: String, valText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(text = valText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}
