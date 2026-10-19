package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.IpoApplication
import com.example.data.model.LiveIpoItem
import com.example.data.model.PanProfile
import com.example.data.model.maskPanNumber
import com.example.ui.components.AllotmentOverviewBannerCard
import com.example.ui.components.AllotmentRegistrar
import com.example.ui.components.AppliedIpoSummary
import com.example.ui.components.MultiPanAllotmentCheckModalSheet
import com.example.ui.components.NaradaIpoFaqCard
import com.example.ui.components.RegistrarDirectCheckRequest
import com.example.ui.components.RegistrarVerificationModalSheet
import com.example.ui.components.RequestNewMandateDialog
import com.example.ui.components.copyPanToClipboard
import com.example.ui.components.detectRegistrarForIpo
import com.example.ui.components.shareAllotmentSummary
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AllotmentAndVaultScreen(
    panProfilesState: UiState<List<PanProfile>>,
    applicationsState: UiState<List<IpoApplication>>,
    onAddPanProfile: (holderName: String, panNumber: String, dematId: String, upiId: String, category: String) -> Unit,
    onUpdatePanProfile: ((panId: String, holderName: String, panNumber: String, dematId: String, upiId: String, category: String) -> Unit)? = null,
    onDeletePanProfile: (String) -> Unit,
    onCycleApplicationStatus: (IpoApplication) -> Unit,
    onDeleteApplication: (String) -> Unit,
    onUpdateApplicationStatus: ((IpoApplication, String) -> Unit)? = null,
    onRequestNewMandate: ((IpoApplication, String) -> Unit)? = null,
    liveIposState: UiState<List<LiveIpoItem>> = UiState.Success(emptyList()),
    onApplyIpoForAllPans: ((LiveIpoItem, List<PanProfile>) -> Unit)? = null,
    onSaveBatchAllotmentResults: ((AppliedIpoSummary, Map<PanProfile, String>) -> Unit)? = null,
    hidePanNumbers: Boolean = false,
    onToggleHidePanNumbers: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showAddPanForm by remember { mutableStateOf(false) }
    var editingPanId by remember { mutableStateOf<String?>(null) }
    var showOptionalDematFields by remember { mutableStateOf(false) }
    var holderName by remember { mutableStateOf("") }
    var panNumber by remember { mutableStateOf("") }
    var dematId by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Retail") }
    var formError by remember { mutableStateOf<String?>(null) }

    val panList = (panProfilesState as? UiState.Success)?.data ?: emptyList()
    val appList = (applicationsState as? UiState.Success)?.data ?: emptyList()
    val liveIpos = (liveIposState as? UiState.Success)?.data ?: emptyList()

    var selectedQuickPanIndex by remember { mutableIntStateOf(0) }
    var customQuickPan by remember(panList) {
        mutableStateOf(panList.firstOrNull()?.panNumber ?: "")
    }
    var customQuickHolder by remember(panList) {
        mutableStateOf(panList.firstOrNull()?.holderName ?: "Primary Investor")
    }
    var activeRegistrarRequest by remember { mutableStateOf<RegistrarDirectCheckRequest?>(null) }
    var activeMultiPanIpoCheck by remember { mutableStateOf<AppliedIpoSummary?>(null) }
    var activeNewMandateApp by remember { mutableStateOf<IpoApplication?>(null) }
    var showSelectAppliedIpoPicker by remember { mutableStateOf(false) }
    var appliedIpoFilter by remember { mutableStateOf("ALL") } // "ALL", "ALLOTTED", "PENDING"

    // Group user's applied IPOs into unique AppliedIpoSummary items so 1 Applied IPO shows all saved PANs
    val groupedAppliedIpos = remember(appList, liveIpos) {
        val byIpo = appList.groupBy { it.ipoId.ifBlank { it.ipoName.lowercase() } }
        byIpo.map { (_, appsForIpo) ->
            val first = appsForIpo.first()
            val liveMatch = liveIpos.firstOrNull {
                it.id == first.ipoId || it.name.equals(first.ipoName, ignoreCase = true)
            }
            val lotSize = liveMatch?.lotSize?.takeIf { it > 0 }
                ?: (first.sharesApplied / first.lotsApplied.coerceAtLeast(1L)).toInt().coerceAtLeast(1)
            val issuePrice = liveMatch?.issuePrice?.takeIf { it > 0 }
                ?: first.bidPrice.toInt().coerceAtLeast(100)
            AppliedIpoSummary(
                ipoId = first.ipoId.ifBlank { liveMatch?.id ?: "ipo_${first.ipoName.hashCode()}" },
                ipoName = first.ipoName,
                category = liveMatch?.category ?: first.category,
                issuePrice = issuePrice,
                lotSize = lotSize,
                boaDate = liveMatch?.boaDate ?: "Allotment Live",
                registrar = detectRegistrarForIpo(first.ipoName, first.ipoId)
            )
        }
    }

    val filteredAppliedIpos = remember(groupedAppliedIpos, appList, appliedIpoFilter) {
        groupedAppliedIpos.filter { summary ->
            val apps = appList.filter {
                it.ipoId == summary.ipoId || it.ipoName.equals(summary.ipoName, ignoreCase = true)
            }
            when (appliedIpoFilter) {
                "ALLOTTED" -> apps.any { it.status == "Allotted" }
                "PENDING" -> apps.any { it.status == "Mandate Pending" || it.status == "Applied" }
                else -> true
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("vault_and_allotment_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 0: Narada-Style Allotment & Mandate Overview Banner + Hide PANs Privacy Toggle
        item {
            AllotmentOverviewBannerCard(
                totalSavedPans = panList.size,
                totalAppliedIpos = groupedAppliedIpos.size,
                allApplications = appList,
                hidePanNumbers = hidePanNumbers,
                onToggleHidePanNumbers = { onToggleHidePanNumbers?.invoke() }
            )
        }

        // Section 1: Post-Login Saved PAN Cards Vault (Add, Edit, Copy, Delete & Mask PANs)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Saved PAN Cards (${panList.size})",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Save family PANs once • Auto-checked across every applied IPO",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (showAddPanForm) {
                                    showAddPanForm = false
                                    editingPanId = null
                                } else {
                                    editingPanId = null
                                    holderName = ""
                                    panNumber = ""
                                    dematId = ""
                                    upiId = ""
                                    showAddPanForm = true
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("toggle_add_pan_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showAddPanForm) "Close" else "Add PAN",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // Prominent Full-Width "+ Add New PAN Card" CTA when form is closed
                    if (!showAddPanForm) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                editingPanId = null
                                holderName = ""
                                panNumber = ""
                                dematId = ""
                                upiId = ""
                                showAddPanForm = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("full_width_add_pan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+ Add Family / Applicant PAN Card",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    AnimatedVisibility(visible = showAddPanForm) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                        ) {
                            if (editingPanId != null) {
                                Text(
                                    text = "Editing Saved PAN Account",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            OutlinedTextField(
                                value = holderName,
                                onValueChange = { holderName = it },
                                label = { Text("Account Holder Full Name") },
                                placeholder = { Text("e.g. Ujjal Kumar Das") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pan_holder_name_input")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = panNumber,
                                onValueChange = { panNumber = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(10) },
                                label = { Text("10-Char PAN (e.g. ABCDE1234F)") },
                                placeholder = { Text("ABCDE1234F") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pan_number_input")
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Retail", "sHNI", "bHNI").forEach { cat ->
                                        FilterChip(
                                            selected = category == cat,
                                            onClick = { category = cat },
                                            label = { Text(cat) }
                                        )
                                    }
                                }

                                Text(
                                    text = if (showOptionalDematFields) "Hide Demat/UPI" else "+ Demat/UPI (Optional)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        showOptionalDematFields = !showOptionalDematFields
                                    }
                                )
                            }

                            AnimatedVisibility(visible = showOptionalDematFields) {
                                Column(modifier = Modifier.padding(top = 6.dp)) {
                                    OutlinedTextField(
                                        value = dematId,
                                        onValueChange = { dematId = it.take(24) },
                                        label = { Text("16-Digit CDSL / NSDL BO ID (Optional)") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("demat_id_input")
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = upiId,
                                        onValueChange = { upiId = it.take(60) },
                                        label = { Text("ASBA UPI Handle (Optional)") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("upi_id_input")
                                    )
                                }
                            }

                            if (formError != null) {
                                Text(
                                    text = formError!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (holderName.trim().length < 2) {
                                        formError = "Enter holder name (at least 2 characters)."
                                    } else if (panNumber.trim().length != 10) {
                                        formError = "PAN number must be exactly 10 alphanumeric characters."
                                    } else {
                                        formError = null
                                        val editId = editingPanId
                                        if (editId != null && onUpdatePanProfile != null) {
                                            onUpdatePanProfile(
                                                editId,
                                                holderName.trim(),
                                                panNumber.trim().uppercase(),
                                                dematId.trim().ifBlank { "1208160000000001" },
                                                upiId.trim().ifBlank { "investor@upi" },
                                                category
                                            )
                                        } else {
                                            onAddPanProfile(
                                                holderName.trim(),
                                                panNumber.trim().uppercase(),
                                                dematId.trim().ifBlank { "1208160000000001" },
                                                upiId.trim().ifBlank { "investor@upi" },
                                                category
                                            )
                                        }
                                        editingPanId = null
                                        holderName = ""
                                        panNumber = ""
                                        dematId = ""
                                        upiId = ""
                                        showAddPanForm = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_pan_profile_button")
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (editingPanId != null) "Update PAN Account" else "Save PAN Card to Vault")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (panList.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No PAN cards saved yet. Tap '+ Add Family / Applicant PAN Card' above to save your PAN cards once — then click 'Check Allotment Status' on any Applied IPO to auto-check all saved PANs at once!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            panList.forEach { pan ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
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
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = pan.holderName.trim().take(1).uppercase().ifEmpty { "P" },
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = pan.holderName,
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = pan.category,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "PAN: ${maskPanNumber(pan.panNumber, hidePanNumbers)} • UPI: ${pan.upiId.ifBlank { "investor@upi" }}",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    editingPanId = pan.id
                                                    holderName = pan.holderName
                                                    panNumber = pan.panNumber
                                                    dematId = pan.dematId
                                                    upiId = pan.upiId
                                                    category = pan.category
                                                    showOptionalDematFields = true
                                                    showAddPanForm = true
                                                },
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .testTag("edit_pan_${pan.panNumber}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit PAN",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    copyPanToClipboard(context, pan.panNumber, "PAN Vault")
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy PAN",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeletePanProfile(pan.id) },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete PAN",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Applied IPOs — 1-Click Auto Allotment Check for ALL Saved PAN Cards (Narada Style)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Applied IPOs (${groupedAppliedIpos.size})",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "1 click auto-checks allotment status for all ${panList.size} saved PAN card(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (liveIpos.isNotEmpty() && onApplyIpoForAllPans != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { showSelectAppliedIpoPicker = !showSelectAppliedIpoPicker },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("toggle_select_applied_ipo_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showSelectAppliedIpoPicker) "Done" else "+ Add Applied IPO",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                if (groupedAppliedIpos.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = appliedIpoFilter == "ALL",
                            onClick = { appliedIpoFilter = "ALL" },
                            label = { Text("All (${groupedAppliedIpos.size})") }
                        )
                        FilterChip(
                            selected = appliedIpoFilter == "ALLOTTED",
                            onClick = { appliedIpoFilter = "ALLOTTED" },
                            label = { Text("Allotted") }
                        )
                        FilterChip(
                            selected = appliedIpoFilter == "PENDING",
                            onClick = { appliedIpoFilter = "PENDING" },
                            label = { Text("Active / Mandate Pending") }
                        )
                    }
                }
            }
        }

        // Quick Picker to add any Live IPO as an Applied IPO (or check its allotment directly)
        if ((showSelectAppliedIpoPicker || groupedAppliedIpos.isEmpty()) && liveIpos.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Select an IPO you applied for to Auto-Check All Saved PANs:",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            liveIpos.take(12).forEach { ipo ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .clickable {
                                            onApplyIpoForAllPans?.invoke(ipo, panList)
                                            showSelectAppliedIpoPicker = false
                                            activeMultiPanIpoCheck = AppliedIpoSummary(
                                                ipoId = ipo.id,
                                                ipoName = ipo.name,
                                                category = ipo.category,
                                                issuePrice = if (ipo.issuePrice > 0) ipo.issuePrice else 100,
                                                lotSize = if (ipo.lotSize > 0) ipo.lotSize else 15,
                                                boaDate = ipo.boaDate,
                                                registrar = detectRegistrarForIpo(ipo.name, ipo.id)
                                            )
                                        }
                                        .testTag("quick_apply_ipo_chip_${ipo.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FactCheck,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = ipo.name,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${ipo.category} • Tap to Check All PANs",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (filteredAppliedIpos.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (groupedAppliedIpos.isEmpty()) "No Applied IPOs Selected Yet" else "No Applied IPOs Match Filter",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap any IPO chip above or select 'Apply / Pre-Apply' on the Dashboard. Once an IPO is added here, click 'Click to Check Allotment Status' to auto-check all your saved PAN cards at once!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredAppliedIpos, key = { it.ipoId }) { appliedIpo ->
                val appsForThisIpo = appList.filter {
                    it.ipoId == appliedIpo.ipoId || it.ipoName.equals(appliedIpo.ipoName, ignoreCase = true)
                }

                // Combine all saved PAN cards with any PANs logged on this IPO so EVERY saved PAN card is displayed
                val effectivePanList: List<PanProfile> = remember(panList, appsForThisIpo) {
                    if (panList.isNotEmpty()) {
                        panList
                    } else {
                        appsForThisIpo.map { app ->
                            PanProfile(
                                id = app.id,
                                userId = app.userId,
                                holderName = app.panHolderName,
                                panNumber = app.panNumber,
                                category = app.category,
                                upiId = app.upiId
                            )
                        }
                    }
                }

                val allottedCount = effectivePanList.count { pan ->
                    appsForThisIpo.any {
                        it.panNumber.equals(pan.panNumber, ignoreCase = true) && it.status == "Allotted"
                    }
                }
                val notAllottedCount = effectivePanList.count { pan ->
                    appsForThisIpo.any {
                        it.panNumber.equals(pan.panNumber, ignoreCase = true) && it.status == "Not Allotted"
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("applied_ipo_card_${appliedIpo.ipoId}"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Top Row: IPO Title + Registrar Badge + Share + Remove Button
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
                                    Surface(
                                        color = appliedIpo.registrar.accentColor.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, appliedIpo.registrar.accentColor.copy(alpha = 0.45f))
                                    ) {
                                        Text(
                                            text = appliedIpo.registrar.shortName,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = appliedIpo.registrar.accentColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${appliedIpo.category} • ₹${appliedIpo.issuePrice} (${appliedIpo.lotSize} Sh/Lot)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = appliedIpo.ipoName,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = "Saved PANs: ${effectivePanList.size} • Allotted: $allottedCount • Not Allotted: $notAllottedCount",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val statusMap = effectivePanList.associate { pan ->
                                            val st = appsForThisIpo.firstOrNull {
                                                it.panNumber.equals(pan.panNumber, ignoreCase = true)
                                            }?.status ?: "Checked"
                                            pan.panNumber.uppercase() to st
                                        }
                                        shareAllotmentSummary(
                                            context = context,
                                            appliedIpo = appliedIpo,
                                            panProfiles = effectivePanList,
                                            statusByPan = statusMap,
                                            hidePanNumbers = hidePanNumbers
                                        )
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("share_applied_ipo_${appliedIpo.ipoId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share Allotment Results",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        appsForThisIpo.forEach { onDeleteApplication(it.id) }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Applied IPO",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Prominent Narada-Style "Click to Check Allotment Status" Button
                        Button(
                            onClick = {
                                activeMultiPanIpoCheck = appliedIpo
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color(0xFF031B14)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("check_allotment_all_pans_${appliedIpo.ipoId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Click to Check Allotment Status (${effectivePanList.size} PANs)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Per-PAN Allotment Status List right inside the Applied IPO Card (Narada Style)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SAVED PAN CARDS ALLOTMENT STATUS",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Tap badge or New Mandate",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                effectivePanList.forEach { pan ->
                                    val matchedApp = appsForThisIpo.firstOrNull {
                                        it.panNumber.equals(pan.panNumber, ignoreCase = true)
                                    }
                                    val statusText = matchedApp?.status ?: "Tap to Check"
                                    val isAllotted = statusText == "Allotted"
                                    val isNotAllotted = statusText == "Not Allotted"
                                    val isMandatePending = statusText == "Mandate Pending"

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when {
                                                    isAllotted -> Color(0xFF064E3B).copy(alpha = 0.35f)
                                                    isNotAllotted -> Color(0xFF450A0A).copy(alpha = 0.35f)
                                                    isMandatePending -> Color(0xFF451A03).copy(alpha = 0.35f)
                                                    else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                                }
                                            )
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = pan.holderName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "PAN: ${maskPanNumber(pan.panNumber, hidePanNumbers)}",
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                    Text(
                                                        text = "• ${pan.category}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Status Badge against each PAN Card
                                            Surface(
                                                color = when {
                                                    isAllotted -> Color(0xFF059669)
                                                    isNotAllotted -> Color(0xFF991B1B)
                                                    isMandatePending -> Color(0xFFD97706)
                                                    else -> Color(0xFF1E3A8A)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .clickable {
                                                        if (matchedApp != null) {
                                                            val next = if (matchedApp.status == "Allotted") "Not Allotted" else "Allotted"
                                                            if (onUpdateApplicationStatus != null) {
                                                                onUpdateApplicationStatus(matchedApp, next)
                                                            } else {
                                                                onCycleApplicationStatus(matchedApp)
                                                            }
                                                        } else {
                                                            activeMultiPanIpoCheck = appliedIpo
                                                        }
                                                    }
                                                    .testTag("inline_pan_status_${appliedIpo.ipoId}_${pan.panNumber}")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = when {
                                                            isAllotted -> Icons.Default.CheckCircle
                                                            isNotAllotted -> Icons.Default.Cancel
                                                            else -> Icons.Default.Sync
                                                        },
                                                        contentDescription = statusText,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = when {
                                                            isAllotted -> "ALLOTTED"
                                                            isNotAllotted -> "NOT ALLOTTED"
                                                            isMandatePending -> "MANDATE PENDING"
                                                            else -> "CHECK STATUS"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }

                                        // Secondary action row for Mandate Management (Request New Mandate - Narada Feature)
                                        if (matchedApp != null && !isAllotted && !isNotAllotted && onRequestNewMandate != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "UPI: ${matchedApp.upiId.ifBlank { pan.upiId.ifBlank { "investor@upi" } }}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier
                                                        .clickable { activeNewMandateApp = matchedApp }
                                                        .testTag("request_new_mandate_${matchedApp.id}")
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Refresh,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Request New Mandate",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Direct Registrar Portal Console (Link Intime, KFintech, Bigshare)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("registrar_direct_check_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Allotment Registrar Direct Check",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "One-click allotment status verification against Link Intime, KFintech, and Bigshare",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (panList.isNotEmpty()) {
                        Text(
                            text = "Select Family PAN for 1-Click Registrar Portal:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            panList.forEachIndexed { index, profile ->
                                FilterChip(
                                    selected = selectedQuickPanIndex == index && customQuickPan == profile.panNumber,
                                    onClick = {
                                        selectedQuickPanIndex = index
                                        customQuickPan = profile.panNumber
                                        customQuickHolder = profile.holderName
                                    },
                                    label = {
                                        Text("${profile.holderName} (${maskPanNumber(profile.panNumber, hidePanNumbers)})")
                                    },
                                    modifier = Modifier.testTag("quick_pan_chip_${profile.panNumber}")
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = customQuickPan,
                        onValueChange = { customQuickPan = it.uppercase().take(10) },
                        label = { Text("PAN for Instant Registrar Check (Auto-Copied & Injected)") },
                        placeholder = { Text("ABCDE1234F") },
                        singleLine = true,
                        trailingIcon = {
                            if (customQuickPan.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        copyPanToClipboard(context, customQuickPan, "Registrar Check")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy PAN",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("direct_check_pan_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AllotmentRegistrar.entries.forEach { registrar ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        registrar.accentColor.copy(alpha = 0.4f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        activeRegistrarRequest = RegistrarDirectCheckRequest(
                                            registrar = registrar,
                                            panNumber = customQuickPan.trim().uppercase(),
                                            holderName = customQuickHolder.ifBlank { "Primary Investor" }
                                        )
                                    }
                                    .testTag("direct_check_${registrar.name}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    registrar.accentColor.copy(alpha = 0.16f),
                                                    RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FactCheck,
                                                contentDescription = null,
                                                tint = registrar.accentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = registrar.fullName,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = registrar.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Button(
                                            onClick = {
                                                activeRegistrarRequest = RegistrarDirectCheckRequest(
                                                    registrar = registrar,
                                                    panNumber = customQuickPan.trim().uppercase(),
                                                    holderName = customQuickHolder.ifBlank { "Primary Investor" }
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = registrar.accentColor,
                                                contentColor = Color(0xFF04131A)
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text(
                                                text = "Verify Now",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = {
                                                copyPanToClipboard(context, customQuickPan, registrar.shortName)
                                                runCatching {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_VIEW, Uri.parse(registrar.portalUrl))
                                                    )
                                                }
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = "Open ${registrar.fullName} in Browser",
                                                tint = registrar.accentColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Narada-Style Interactive PAN & IPO Allotment FAQs
        item {
            NaradaIpoFaqCard()
        }

        // Copyright & Developer Attribution Footer
        item {
            com.example.ui.components.CopyrightAttributionCard(
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
            )
        }
    }

    // Request New UPI Mandate Dialog
    activeNewMandateApp?.let { targetApp ->
        RequestNewMandateDialog(
            application = targetApp,
            onConfirmNewMandate = { updatedUpi ->
                onRequestNewMandate?.invoke(targetApp, updatedUpi)
            },
            onDismiss = { activeNewMandateApp = null }
        )
    }

    // Narada-Style Multi-PAN Auto Allotment Status Modal Sheet
    activeMultiPanIpoCheck?.let { targetIpo ->
        val effectivePansForSheet = if (panList.isNotEmpty()) {
            panList
        } else {
            appList.filter {
                it.ipoId == targetIpo.ipoId || it.ipoName.equals(targetIpo.ipoName, ignoreCase = true)
            }.map { app ->
                PanProfile(
                    id = app.id,
                    userId = app.userId,
                    holderName = app.panHolderName,
                    panNumber = app.panNumber,
                    category = app.category,
                    upiId = app.upiId
                )
            }
        }

        MultiPanAllotmentCheckModalSheet(
            appliedIpo = targetIpo,
            panProfiles = effectivePansForSheet,
            existingApplications = appList,
            onSaveBatchResults = { results ->
                onSaveBatchAllotmentResults?.invoke(targetIpo, results)
            },
            onToggleSinglePanStatus = { pan, newStatus ->
                onSaveBatchAllotmentResults?.invoke(targetIpo, mapOf(pan to newStatus))
            },
            onAddPanProfile = onAddPanProfile,
            onOpenOfficialRegistrar = { req ->
                activeRegistrarRequest = req
            },
            onDismiss = { activeMultiPanIpoCheck = null },
            hidePanNumbers = hidePanNumbers,
            onToggleHidePanNumbers = onToggleHidePanNumbers
        )
    }

    activeRegistrarRequest?.let { req ->
        RegistrarVerificationModalSheet(
            request = req,
            onDismiss = { activeRegistrarRequest = null },
            onUpdateApplicationStatus = onUpdateApplicationStatus ?: { app, _ ->
                onCycleApplicationStatus(app)
            }
        )
    }
}
