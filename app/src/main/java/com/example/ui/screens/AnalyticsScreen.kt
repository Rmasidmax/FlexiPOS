package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItem
import com.example.data.model.JsonUtils
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.SalesTrendPoint
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnalyticsScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var selectedInterval by remember { mutableStateOf("Weekly") } // "Weekly", "Monthly", "Annual"
    var isGraphTypeBar by remember { mutableStateOf(true) } // true: Bar, false: Line

    val trendPoints = viewModel.getHistoricalRevenueTrendPoints(selectedInterval)
    val bestSellers = viewModel.getTopSellingProducts()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top row title and actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Store Sales & Revenue Core Analytics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Live calculations of financial registers, items sold, and trend reports",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Export PDF button - styled and sized to be small and proportional with label
            FilledTonalButton(
                onClick = { viewModel.exportSalesReportPdf(context) },
                modifier = Modifier
                    .height(36.dp)
                    .testTag("export_pdf_button"),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "Print PDF Report",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Print PDF Report",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left block: Dynamic Canvas Graphs
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Filter options and Line vs Bar toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Days select
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Weekly", "Monthly", "Annual").forEach { intv ->
                                FilterChip(
                                    selected = selectedInterval == intv,
                                    onClick = { selectedInterval = intv },
                                    label = { Text(intv, fontSize = 12.sp) }
                                )
                            }
                        }

                        // Toggle line vs bar
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                                .padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Bar Chart",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isGraphTypeBar) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { isGraphTypeBar = true }
                                    .padding(vertical = 4.dp, horizontal = 10.dp)
                                    .size(18.dp),
                                tint = if (isGraphTypeBar) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
                            )
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = "Line Chart",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (!isGraphTypeBar) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { isGraphTypeBar = false }
                                    .padding(vertical = 4.dp, horizontal = 10.dp)
                                    .size(18.dp),
                                tint = if (!isGraphTypeBar) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Revenue graphs rendering Canvas
                    Text(
                        text = "Total Gross Revenue ($selectedInterval Overview)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    POSRevenueCustomCanvasGraph(points = trendPoints, isBar = isGraphTypeBar)
                }
            }

            // Right block: High-Velocity Trend items ranks
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🏆 High-Velocity Trends",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Fastest-moving items based on total sales quantites",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (bestSellers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No checkout logs recorded", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            bestSellers.forEachIndexed { idx, pair ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "#${idx + 1}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(pair.first, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    val formattedQty = if (pair.second % 1.0 == 0.0) pair.second.toInt().toString() else String.format("%.1f", pair.second)
                                    Text(
                                        text = "$formattedQty sold",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dual Report Matrix: Comprehensive structured Cashier Transactions Data Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Structured Ledger Matrix",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (state.receiptLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("Ledger data table is currently empty.", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    // Header cells
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    ) {
                        Text("Transaction ID", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 12.sp)
                        Text("Store Type", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 12.sp)
                        Text("Date & Time", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 12.sp)
                        Text("Items Sold", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("Checkout (₱)", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 12.sp, textAlign = TextAlign.End)
                    }

                    // Table Rows
                    val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    val rowsToShow = state.receiptLogs.take(15) // Limit screen load
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        rowsToShow.forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(log.id, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Text(log.storeType, modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall)
                                Text(df.format(Date(log.timestamp)), modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall)
                                
                                val itemsList = JsonUtils.listFromJson(log.itemsJson)
                                val qtyTotal = itemsList.sumOf { it.quantity }
                                val formattedQty = if (qtyTotal % 1.0 == 0.0) qtyTotal.toInt().toString() else String.format("%.2f", qtyTotal)
                                
                                Text(formattedQty, modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                                Text("₱${String.format("%.2f", log.total)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.primary)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

// Highly stylized Jetpack Compose screen Canvas graph mapping historical revenue 
@Composable
fun POSRevenueCustomCanvasGraph(
    points: List<SalesTrendPoint>,
    isBar: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val leftPadding = 50f
            val bottomPadding = 40f
            val rightPadding = 20f
            val topPadding = 20f

            val graphWidth = canvasWidth - leftPadding - rightPadding
            val graphHeight = canvasHeight - topPadding - bottomPadding

            // Core Math Range
            val maxObj = points.maxOfOrNull { it.revenue } ?: 1.0
            val maxRange = if (maxObj <= 0) 100.0 else maxObj * 1.15

            // Draw horizontal coordinate Grid lines 
            val intervals = 4
            for (i in 0..intervals) {
                val ratio = i.toFloat() / intervals
                val y = topPadding + graphHeight * (1f - ratio)
                drawLine(
                    color = gridColor,
                    start = Offset(leftPadding, y),
                    end = Offset(canvasWidth - rightPadding, y),
                    strokeWidth = 1f
                )
                
                // Text value coordinates - simulate locally
                // Note: DrawText is available in modern Canvas or via Native Canvas paint configs
            }

            // Draw data nodes
            if (points.isNotEmpty()) {
                val stepX = if (points.size > 1) graphWidth / (points.size - 1) else graphWidth
                val barWidth = (graphWidth / points.size) * 0.6f

                val pointsCoord = points.mapIndexed { idx, p ->
                    val ratio = (p.revenue / maxRange).toFloat()
                    val x = leftPadding + (idx * stepX)
                    val y = topPadding + graphHeight * (1f - ratio)
                    Offset(x, y)
                }

                if (isBar) {
                    // Render bar series
                    points.forEachIndexed { idx, p ->
                        val ratio = (p.revenue / maxRange).toFloat()
                        val h = graphHeight * ratio
                        val x = leftPadding + (idx * (graphWidth / points.size)) + ((graphWidth / points.size) - barWidth) / 2
                        val y = topPadding + graphHeight - h

                        // Drawing filled column
                        drawRect(
                            color = primaryColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, h)
                        )
                    }
                } else {
                    // Render line series
                    val path = Path()
                    pointsCoord.forEachIndexed { idx, offset ->
                        if (idx == 0) {
                            path.moveTo(offset.x, offset.y)
                        } else {
                            path.lineTo(offset.x, offset.y)
                        }
                    }

                    // Stroke line
                    drawPath(
                        path = path,
                        color = primaryColor,
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )

                    // Draw circles
                    pointsCoord.forEach { offset ->
                        drawCircle(
                            color = secondaryColor,
                            radius = 6f,
                            center = offset
                        )
                    }
                }
            }
        }

        // Minimalist Bottom coordinate legend simulation row 
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 38.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { p ->
                Text(
                    text = p.label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.width(42.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
