package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import com.example.data.model.IpoApplication
import com.example.data.model.PanProfile
import com.example.ui.components.AllotmentRegistrar
import com.example.ui.components.RegistrarDirectCheckRequest
import com.example.ui.components.RegistrarVerificationModalSheet
import com.example.ui.components.copyPanToClipboard
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AllotmentAndVaultScreen(
    panProfilesState: UiState<List<PanProfile>>,
    applicationsState: UiState<List<IpoApplication>>,
    onAddPanProfile: (holderName: String, panNumber: String, dematId: String, upiId: String, category: String) -> Unit,
    onDeletePanProfile: (String) -> Unit,
    onCycleApplicationStatus: (IpoApplication) -> Unit,
    onDeleteApplication: (String) -> Unit,
    onUpdateApplicationStatus: ((IpoApplication, String) -> Unit)? = null
) {
    val context = LocalContext.current
    var showAddPanForm by remember { mutableStateOf(false) }
    var holderName by remember { mutableStateOf("") }
    var panNumber by remember { mutableStateOf("") }
    var dematId by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Retail") }
    var formError by remember { mutableStateOf<String?>(null) }

    val panList = (panProfilesState as? UiState.Success)?.data ?: emptyList()
    val appList = (applicationsState as? UiState.Success)?.data ?: emptyList()

    var selectedQuickPanIndex by remember { mutableStateOf(0) }
    var customQuickPan by remember(panList) {
        mutableStateOf(panList.firstOrNull()?.panNumber ?: "")
    }
    var customQuickHolder by remember(panList) {
        mutableStateOf(panList.firstOrNull()?.holderName ?: "Primary Investor")
    }
    var activeRegistrarRequest by remember { mutableStateOf<RegistrarDirectCheckRequest?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("vault_and_allotment_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 1: Encrypted Family PAN & Demat Vault
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Family PAN & Demat Vault",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Cloud-synced & protected by Biometric App Lock",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { showAddPanForm = !showAddPanForm },
                            modifier = Modifier.testTag("toggle_add_pan_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showAddPanForm) "Close" else "Add PAN")
                        }
                    }

                    AnimatedVisibility(visible = showAddPanForm) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                        ) {
                            OutlinedTextField(
                                value = holderName,
                                onValueChange = { holderName = it },
                                label = { Text("Account Holder Full Name") },
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
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pan_number_input")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = dematId,
                                onValueChange = { dematId = it.take(24) },
                                label = { Text("16-Digit CDSL / NSDL BO ID") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("demat_id_input")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = upiId,
                                onValueChange = { upiId = it.take(60) },
                                label = { Text("ASBA UPI Handle (e.g. name@okhdfcbank)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("upi_id_input")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Retail", "sHNI", "bHNI").forEach { cat ->
                                    FilterChip(
                                        selected = category == cat,
                                        onClick = { category = cat },
                                        label = { Text(cat) }
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
                                        onAddPanProfile(
                                            holderName.trim(),
                                            panNumber.trim().uppercase(),
                                            dematId.trim().ifBlank { "1208160000000001" },
                                            upiId.trim().ifBlank { "investor@upi" },
                                            category
                                        )
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
                                Text("Save to Encrypted Cloud Vault")
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
                                text = "No PAN profiles saved yet. Tap 'Add PAN' to store family Demat accounts for 1-click multi-lot IPO applications and instant allotment checks.",
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
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = pan.holderName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = pan.category,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "PAN: ${pan.panNumber} • Demat: ${pan.dematId}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = "UPI: ${pan.upiId}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { onDeletePanProfile(pan.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete PAN",
                                                tint = MaterialTheme.colorScheme.error
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

        // Section 2: Tracked IPO Applications & Allotment Status
        item {
            Text(
                text = "Your Tracked IPO Applications (${appList.size})",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (appList.isEmpty()) {
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
                            text = "No IPO Bids Logged Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap any live or upcoming IPO on the Dashboard and select 'Log / Track IPO Bid' to track your mandate and allotment status.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(appList, key = { it.id }) { app ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.ipoName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${app.panHolderName} • PAN: ${app.panNumber} (${app.category})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                color = when (app.status) {
                                    "Allotted" -> Color(0xFF064E3B)
                                    "Not Allotted" -> Color(0xFF450A0A)
                                    "Mandate Pending" -> Color(0xFF451A03)
                                    "Applied" -> Color(0xFF1E3A8A)
                                    else -> MaterialTheme.colorScheme.secondaryContainer
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clickable { onCycleApplicationStatus(app) }
                                    .testTag("cycle_app_status_${app.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Cycle status",
                                        tint = when (app.status) {
                                            "Allotted" -> Color(0xFF34D399)
                                            "Not Allotted" -> Color(0xFFF87171)
                                            "Mandate Pending" -> Color(0xFFFBBF24)
                                            else -> Color(0xFF60A5FA)
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = app.status,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${app.lotsApplied} Lot(s) (${app.sharesApplied} sh) @ ₹${app.bidPrice} = ₹${app.totalAmount}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { onDeleteApplication(app.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Application",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 1-Click Direct Registrar Check Row for this Tracked Application
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "1-Click Allotment Registrar Direct Check:",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Auto-PAN Fill",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        AllotmentRegistrar.LINK_INTIME,
                                        AllotmentRegistrar.KFINTECH,
                                        AllotmentRegistrar.BIGSHARE
                                    ).forEach { reg ->
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
                                                        panNumber = app.panNumber,
                                                        holderName = app.panHolderName,
                                                        ipoName = app.ipoName,
                                                        application = app
                                                    )
                                                }
                                                .testTag("app_check_${reg.name.lowercase()}_${app.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.VerifiedUser,
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
                }
            }
        }

        // Section 3: Allotment Registrar Direct Check Console (Link Intime, KFintech, Bigshare)
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

                    // Family PAN Selector Chips for 1-Click Check
                    if (panList.isNotEmpty()) {
                        Text(
                            text = "Select Family PAN for 1-Click Verification:",
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
                                        Text("${profile.holderName} (${profile.panNumber})")
                                    },
                                    modifier = Modifier.testTag("quick_pan_chip_${profile.panNumber}")
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Editable / Instant PAN Bar with Copy Button
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

                    // Primary 3 Registrars: Link Intime, KFintech, Bigshare
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

        // Copyright & Developer Attribution Footer
        item {
            com.example.ui.components.CopyrightAttributionCard(
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
            )
        }
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
