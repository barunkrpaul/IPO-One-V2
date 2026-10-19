package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.LiveIpoItem
import com.example.data.model.PanProfile
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IpoDetailSheet(
    ipo: LiveIpoItem,
    isWatched: Boolean,
    panProfiles: List<PanProfile>,
    onToggleWatch: () -> Unit,
    onSubmitBid: (panHolderName: String, panNumber: String, category: String, lots: Int, upiId: String) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedLots by remember { mutableIntStateOf(1) }
    var selectedCategory by remember { mutableStateOf("Retail") }
    var selectedPanIndex by remember { mutableIntStateOf(0) }
    var customHolderName by remember { mutableStateOf(panProfiles.firstOrNull()?.holderName ?: "") }
    var customPanNumber by remember { mutableStateOf(panProfiles.firstOrNull()?.panNumber ?: "") }
    var customUpiId by remember { mutableStateOf(panProfiles.firstOrNull()?.upiId ?: "") }
    var showApplyForm by remember { mutableStateOf(false) }

    val lotShares = if (ipo.lotSize > 0) ipo.lotSize else 1
    val totalShares = lotShares * selectedLots
    val totalInvestment = totalShares * ipo.issuePrice
    val estTotalProfit = (ipo.gmpAmount * totalShares).roundToInt()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusBadge(statusCode = ipo.statusCode, statusLabel = ipo.statusLabel)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${ipo.category} • ${ipo.listingExchange}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = ipo.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Sector: ${ipo.sector} • Updated: ${ipo.updatedOn}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(
                        onClick = onToggleWatch,
                        modifier = Modifier.testTag("detail_watchlist_button")
                    ) {
                        Icon(
                            imageVector = if (isWatched) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Toggle Watchlist",
                            tint = if (isWatched) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("detail_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close details"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // InvestorGain GMP Hero Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LIVE INVESTORGAIN GMP",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "+₹${formatDecimal(ipo.gmpAmount)} (${String.format("%.2f", ipo.gmpPercent)}%)",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (ipo.gmpRangeText.isNotBlank()) {
                                Text(
                                    text = "GMP Range: ${ipo.gmpRangeText}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row {
                                repeat(5) { idx ->
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = if (idx < ipo.fireRating) Color(0xFFFF9800) else Color(0xFF334155),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Est. Listing: ₹${formatDecimal(ipo.estimatedListingPrice)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Issue Price: ₹${ipo.issuePrice}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Key Issue Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricBox(
                    label = "Issue Size",
                    value = ipo.issueSizeCr,
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    label = "Lot Size",
                    value = "${ipo.lotSize} Shares",
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    label = "P/E Ratio",
                    value = ipo.peRatio,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dates Schedule Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ScheduleDateColumn("Open Date", ipo.openDate)
                    ScheduleDateColumn("Close Date", ipo.closeDate)
                    ScheduleDateColumn("Allotment (BoA)", ipo.boaDate)
                    ScheduleDateColumn("Listing Date", ipo.listingDate)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live InvestorGain Subscription Breakdown
            Text(
                text = "Live Subscription Breakdown (InvestorGain)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SubscriptionBarRow("Total Subscription", ipo.totalSubscription, MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    SubscriptionBarRow("QIB (Institutional)", ipo.qibSubscription, MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.height(8.dp))
                    SubscriptionBarRow("NII (Total HNI)", ipo.niiSubscription, MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    SubscriptionBarRow("sHNI (₹2L - ₹10L)", ipo.shniSubscription, MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    SubscriptionBarRow("bHNI (> ₹10L)", ipo.bhniSubscription, MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    SubscriptionBarRow("Retail (RII)", ipo.riiSubscription, MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Lot & GMP Profit Calculator
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Multi-Lot GMP Profit Calculator",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Lots: $selectedLots ($totalShares Shares) • Blocked Capital: ₹$totalInvestment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = selectedLots.toFloat(),
                        onValueChange = { selectedLots = it.roundToInt().coerceIn(1, 25) },
                        valueRange = 1f..25f,
                        steps = 23,
                        modifier = Modifier.testTag("lot_calculator_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estimated Listing Gain ($selectedLots Lot${if (selectedLots > 1) "s" else ""}):",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "+₹$estTotalProfit",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Log IPO Bid / Apply with PAN Vault
            if (!showApplyForm) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showApplyForm = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("open_apply_form_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log / Track IPO Bid")
                    }

                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ipo.investorGainUrl))
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("open_investorgain_url_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("InvestorGain Report")
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Track IPO Application in Cloud Vault",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (panProfiles.isNotEmpty()) {
                            Text(
                                text = "Select Family PAN from Vault:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                panProfiles.forEachIndexed { idx, profile ->
                                    FilterChip(
                                        selected = selectedPanIndex == idx,
                                        onClick = {
                                            selectedPanIndex = idx
                                            customHolderName = profile.holderName
                                            customPanNumber = profile.panNumber
                                            customUpiId = profile.upiId
                                            selectedCategory = profile.category
                                        },
                                        label = { Text("${profile.holderName} (${profile.panNumber})") }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = customHolderName,
                            onValueChange = { customHolderName = it },
                            label = { Text("Applicant Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customPanNumber,
                            onValueChange = { customPanNumber = it.uppercase().take(10) },
                            label = { Text("10-Char PAN Number (e.g. ABCDE1234F)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customUpiId,
                            onValueChange = { customUpiId = it },
                            label = { Text("ASBA UPI ID (e.g. investor@okicici)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Retail", "sHNI", "bHNI").forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val validPan = customPanNumber.trim().uppercase().padEnd(10, 'X').take(10)
                                val validName = customHolderName.trim().ifBlank { "Primary Investor" }
                                val validUpi = customUpiId.trim().ifBlank { "investor@upi" }
                                onSubmitBid(validName, validPan, selectedCategory, selectedLots, validUpi)
                                showApplyForm = false
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_submit_bid_button")
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Bid ($selectedLots Lot • ₹$totalInvestment)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(statusCode: String, statusLabel: String) {
    val (bg, fg) = when (statusCode) {
        "O", "CT" -> Color(0xFF064E3B) to Color(0xFF34D399)
        "U" -> Color(0xFF451A03) to Color(0xFFFBBF24)
        "C" -> Color(0xFF1E3A8A) to Color(0xFF60A5FA)
        "LP" -> Color(0xFF065F46) to Color(0xFF6EE7B7)
        else -> Color(0xFF334155) to Color(0xFFCBD5E1)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.border(1.dp, fg.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
    ) {
        Text(
            text = statusLabel.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun MetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ScheduleDateColumn(title: String, dateText: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dateText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SubscriptionBarRow(category: String, multiplierText: String, color: Color) {
    val numVal = multiplierText.replace("x", "", ignoreCase = true).toFloatOrNull() ?: 0f
    val progress = (numVal / 25f).coerceIn(0.03f, 1f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = multiplierText,
                style = MaterialTheme.typography.labelLarge,
                color = if (numVal >= 1f) color else MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (numVal > 0f) progress else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}

fun formatDecimal(v: Double): String {
    return if (v % 1.0 == 0.0) v.roundToInt().toString() else String.format("%.1f", v)
}
