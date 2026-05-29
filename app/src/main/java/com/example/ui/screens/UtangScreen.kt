package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.UtangRecord
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UtangScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    var showAddDebtDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredRecords = state.utangRecords.filter {
        it.customerName.contains(searchQuery, ignoreCase = true) ||
        it.contactNo.contains(searchQuery, ignoreCase = true) ||
        it.notes.contains(searchQuery, ignoreCase = true)
    }

    val totalOutstandingDebt = state.utangRecords.sumOf { it.amount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Panel (now Top): Stats & New Debt submission form 
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Local Utang Ledger Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Total outstanding debt summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CreditCardOff, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TOTAL ACTIVE OUTSTANDING DEBT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₱${String.format("%.2f", totalOutstandingDebt)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "${state.utangRecords.size} customers with pending payments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // Quick Actions card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Quick Debt Manager", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Sari-Sari stores utilize the utang ledger to log sales where payment is promised at a later time. Clear lines in single taps when paid.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Button(
                        onClick = { showAddDebtDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("open_log_debt_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log Customer Debt")
                    }
                }
            }
        }

        // Right Panel (now Bottom): Searchable Ledger List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("utang_ledger_search"),
                placeholder = { Text("Search customer name, contact or notes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            if (filteredRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching names found." else "No utang entries registered in ledger. High-five!",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredRecords) { record ->
                        UtangRowCard(record = record, onPayOff = { viewModel.payUtang(record.id) })
                    }
                }
            }
        }
    }

    // Modal dialog to add a new Utang Record
    if (showAddDebtDialog) {
        var custName by remember { mutableStateOf("") }
        var phoneNo by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        var noteStr by remember { mutableStateOf("") }
        var valError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDebtDialog = false },
            title = { Text("Add New Ledger Debt Entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (valError != null) {
                        Text(text = valError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedTextField(
                        value = custName,
                        onValueChange = { custName = it },
                        modifier = Modifier.fillMaxWidth().testTag("utang_customer_name_field"),
                        label = { Text("Customer Full Name *") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phoneNo,
                        onValueChange = { phoneNo = it },
                        modifier = Modifier.fillMaxWidth().testTag("utang_customer_phone_field"),
                        label = { Text("Contact Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        modifier = Modifier.fillMaxWidth().testTag("utang_customer_amount_field"),
                        label = { Text("Debt Amount (₱) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        modifier = Modifier.fillMaxWidth().testTag("utang_customer_notes_field"),
                        label = { Text("Sari-sari item list / credit details notes") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull()
                        if (custName.trim().isEmpty()) {
                            valError = "Customer Name is required."
                        } else if (amount == null || amount <= 0) {
                            valError = "A valid outstanding credit amount is required."
                        } else {
                            viewModel.createUtangRecord(custName, phoneNo, amount, noteStr)
                            showAddDebtDialog = false
                        }
                    },
                    modifier = Modifier.testTag("utang_confirm_submit_button")
                ) {
                    Text("Register Debt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDebtDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UtangRowCard(
    record: UtangRecord,
    onPayOff: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("utang_row_${record.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = record.customerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                if (record.contactNo.isNotEmpty()) {
                    Text(
                        text = "📞 ${record.contactNo}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (record.notes.isNotEmpty()) {
                    Text(
                        text = "📝 Note: ${record.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(record.timestamp))
                Text(
                    text = "Logged: $format",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(0.8f)
            ) {
                Text(
                    text = "₱${String.format("%.2f", record.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )

                // Single tap cutoff trigger paid debt 
                Button(
                    onClick = onPayOff,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Settle", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
