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
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material3.ButtonDefaults
import com.example.data.model.IpoApplication
import com.example.data.model.LiveIpoItem
import com.example.data.model.PanProfile
import com.example.data.model.maskPanNumber
import com.example.ui.components.AllotmentRegistrar
import com.example.ui.components.AppliedIpoSummary
import com.example.ui.components.MultiPanAllotmentCheckModalSheet
import com.example.ui.components.RegistrarDirectCheckRequest
import com.example.ui.components.RegistrarVerificationModalSheet
import com.example.ui.components.SwipeToConfirmApplyBar
import com.example.ui.components.detectRegistrarForIpo
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IpoDetailSheet(
    ipo: LiveIpoItem,
    isWatched: Boolean,
    panProfiles: List<PanProfile>,
    onToggleWatch: () -> Unit,
    onSubmitBid: (panHolderName: String, panNumber: String, category: String, lots: Int, upiId: String) -> Unit,
    onDismiss: () -> Unit,
    existingApplications: List<IpoApplication> = emptyList(),
    onApplyForAllPans: ((LiveIpoItem, List<PanProfile>) -> Unit)? = null,
    onSaveBatchAllotmentResults: ((AppliedIpoSummary, Map<PanProfile, String>) -> Unit)? = null,
    onAddPanProfile: ((holderName: String, panNumber: String, dematId: String, upiId: String, category: String) -> Unit)? = null,
    hidePanNumbers: Boolean = false,
    onToggleHidePanNumbers: (() -> Unit)? = null
) {
    BackHandler { onDismiss() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                                text = "LIVE GREY MARKET PREMIUM",
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

            Text(
                text = "Live Subscription Breakdown & Price Demand",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                val riiMult = ipo.riiSubscription.replace("x", "", ignoreCase = true).toFloatOrNull() ?: 0f
                val oddsText = when {
                    riiMult <= 0f -> "Subscription yet to open / Pre-Apply Active"
                    riiMult <= 1f -> "100% Guaranteed 1 Lot per Valid Retail PAN"
                    else -> "Approx. 1 in ${riiMult.roundToInt().coerceAtLeast(1)} Retail PANs (${String.format("%.1f", (100f / riiMult).coerceIn(0.5f, 100f))}% chance per PAN)"
                }

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

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Subscription Breakdown by Price & Allotment Odds (Narada Feature)
                    Text(
                        text = "Subscription by Price: Cut-Off (₹${ipo.issuePrice}): ~88% Bids • Price Band Bids: ~12%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Retail Allotment Odds: $oddsText",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            IpoApplyAndAllotmentSection(
                ipo = ipo,
                panProfiles = panProfiles,
                onSubmitBid = onSubmitBid,
                onBidSaved = onDismiss,
                existingApplications = existingApplications,
                onApplyForAllPans = onApplyForAllPans,
                onSaveBatchAllotmentResults = onSaveBatchAllotmentResults,
                onAddPanProfile = onAddPanProfile,
                hidePanNumbers = hidePanNumbers,
                onToggleHidePanNumbers = onToggleHidePanNumbers
            )
        }
    }
}

/**
 * Lot calculator, apply / pre-apply actions, single-bid form and registrar
 * allotment checks. Shared by the bottom sheet and the full-screen detail page.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IpoApplyAndAllotmentSection(
    ipo: LiveIpoItem,
    panProfiles: List<PanProfile>,
    onSubmitBid: (panHolderName: String, panNumber: String, category: String, lots: Int, upiId: String) -> Unit,
    onBidSaved: () -> Unit,
    existingApplications: List<IpoApplication> = emptyList(),
    onApplyForAllPans: ((LiveIpoItem, List<PanProfile>) -> Unit)? = null,
    onSaveBatchAllotmentResults: ((AppliedIpoSummary, Map<PanProfile, String>) -> Unit)? = null,
    onAddPanProfile: ((holderName: String, panNumber: String, dematId: String, upiId: String, category: String) -> Unit)? = null,
    hidePanNumbers: Boolean = false,
    onToggleHidePanNumbers: (() -> Unit)? = null,
    registrar: AllotmentRegistrar = detectRegistrarForIpo(ipo.name, ipo.id),
    registrarUrl: String? = null,
    onSaveAllotmentOutcome: ((com.example.data.model.AllotmentTarget, com.example.data.model.AllotmentOutcome) -> Unit)? = null
) {
    var selectedLots by remember { mutableIntStateOf(1) }
    var selectedCategory by remember { mutableStateOf("Retail") }
    var selectedPanIndex by remember { mutableIntStateOf(0) }
    var customHolderName by remember { mutableStateOf(panProfiles.firstOrNull()?.holderName ?: "") }
    var customPanNumber by remember { mutableStateOf(panProfiles.firstOrNull()?.panNumber ?: "") }
    var customUpiId by remember { mutableStateOf(panProfiles.firstOrNull()?.upiId ?: "") }
    var showApplyForm by remember { mutableStateOf(false) }
    var activeRegistrarRequest by remember { mutableStateOf<RegistrarDirectCheckRequest?>(null) }
    var showMultiPanCheckSheet by remember { mutableStateOf(false) }
    val isUpcomingPreApply = ipo.statusCode == "U"

    val lotShares = if (ipo.lotSize > 0) ipo.lotSize else 1
    val totalShares = lotShares * selectedLots
    val totalInvestment = totalShares * ipo.issuePrice
    val estTotalProfit = (ipo.gmpAmount * totalShares).roundToInt()

    Column(modifier = Modifier.fillMaxWidth()) {
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

        // Narada-Style 1-Click Check Allotment Status for All Saved PAN Cards
        Button(
            onClick = { showMultiPanCheckSheet = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF10B981),
                contentColor = Color(0xFF031B14)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("detail_check_all_pans_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.FactCheck, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Click to Check Allotment Status (${panProfiles.size} Saved PANs)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (onApplyForAllPans != null) {
            SwipeToConfirmApplyBar(
                label = if (isUpcomingPreApply) {
                    "Swipe to Pre-Apply (${panProfiles.size.coerceAtLeast(1)} PANs)"
                } else {
                    "Swipe to Apply (${panProfiles.size.coerceAtLeast(1)} Saved PANs)"
                },
                isPreApply = isUpcomingPreApply,
                onConfirmed = {
                    onApplyForAllPans(ipo, panProfiles)
                    showMultiPanCheckSheet = true
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (!showApplyForm) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showApplyForm = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("open_apply_form_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isUpcomingPreApply) "Pre-Apply Single Bid" else "Log Single Bid")
                }

                if (onApplyForAllPans != null && panProfiles.isNotEmpty()) {
                    Button(
                        onClick = {
                            onApplyForAllPans(ipo, panProfiles)
                            showMultiPanCheckSheet = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("apply_all_saved_pans_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isUpcomingPreApply) "Pre-Apply (${panProfiles.size}) PANs"
                            else "Apply All (${panProfiles.size}) PANs"
                        )
                    }
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
                        text = if (isUpcomingPreApply) "Pre-Apply IPO Bid in Cloud Vault" else "Track IPO Application in Cloud Vault",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (panProfiles.isNotEmpty()) {
                        Text(
                            text = "Select Family PAN from Vault (Edit details below if needed):",
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
                                    label = {
                                        Text("${profile.holderName} (${maskPanNumber(profile.panNumber, hidePanNumbers)})")
                                    }
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
                            onBidSaved()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_submit_bid_button")
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isUpcomingPreApply) "Confirm Pre-Apply ($selectedLots Lot • ₹$totalInvestment)"
                            else "Save Bid ($selectedLots Lot • ₹$totalInvestment)"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1-Click Allotment Registrar Direct Check Card inside IPO Detail Sheet
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("detail_sheet_registrar_check_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Allotment Registrar Direct Check",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "One-click allotment status verification (BoA: ${ipo.boaDate.ifBlank { "TBA" }})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // This IPO's registrar first, then the three largest
                    (listOf(registrar) + listOf(
                        AllotmentRegistrar.LINK_INTIME,
                        AllotmentRegistrar.KFINTECH,
                        AllotmentRegistrar.BIGSHARE
                    )).distinct().take(3).forEach { reg ->
                        Surface(
                            color = reg.accentColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    1.dp,
                                    reg.accentColor.copy(alpha = 0.45f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    activeRegistrarRequest = RegistrarDirectCheckRequest(
                                        registrar = reg,
                                        portalUrlOverride = registrarUrl.takeIf { reg == registrar },
                                        panNumber = customPanNumber.trim().uppercase(),
                                        holderName = customHolderName.ifBlank { "Primary Investor" },
                                        ipoName = ipo.name
                                    )
                                }
                                .testTag("detail_check_${reg.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = reg.accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = reg.shortName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMultiPanCheckSheet) {
        val summary = AppliedIpoSummary(
            ipoId = ipo.id,
            ipoName = ipo.name,
            category = ipo.category,
            issuePrice = if (ipo.issuePrice > 0) ipo.issuePrice else 100,
            lotSize = if (ipo.lotSize > 0) ipo.lotSize else 15,
            boaDate = ipo.boaDate,
            registrar = registrar,
            registrarUrl = registrarUrl
        )
        MultiPanAllotmentCheckModalSheet(
            appliedIpo = summary,
            panProfiles = panProfiles,
            existingApplications = existingApplications,
            onSaveBatchResults = { results ->
                onSaveBatchAllotmentResults?.invoke(summary, results)
            },
            onToggleSinglePanStatus = { pan, newStatus ->
                onSaveBatchAllotmentResults?.invoke(summary, mapOf(pan to newStatus))
            },
            onAddPanProfile = { h, p, d, u, c ->
                onAddPanProfile?.invoke(h, p, d, u, c)
            },
            onOpenOfficialRegistrar = { req ->
                activeRegistrarRequest = req
            },
            onDismiss = { showMultiPanCheckSheet = false },
            hidePanNumbers = hidePanNumbers,
            onToggleHidePanNumbers = onToggleHidePanNumbers,
            onSaveAllotmentOutcome = onSaveAllotmentOutcome
        )
    }

    activeRegistrarRequest?.let { req ->
        RegistrarVerificationModalSheet(
            request = req,
            onDismiss = { activeRegistrarRequest = null }
        )
    }
}

@Composable
fun StatusBadge(statusCode: String, statusLabel: String) {
    val (bg, fg) = when (statusCode) {
        "O", "CT" -> Color(0xFF064E3B) to Color(0xFF34D399)
        "U" -> Color(0xFF451A03) to Color(0xFFFBBF24)
        "C" -> Color(0xFF1E3A8A) to Color(0xFF60A5FA)
        "LP" -> Color(0xFF065F46) to Color(0xFF6EE7B7)
        "A" -> Color(0xFF3B0764) to Color(0xFFC084FC)
        "X" -> Color(0xFF450A0A) to Color(0xFFF87171)
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
internal fun MetricBox(label: String, value: String, modifier: Modifier = Modifier) {
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
