package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.IpoEntity
import com.example.data.local.entity.UpiAccountEntity
import com.example.model.DematWithMember
import com.example.model.IpoCategory
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.BatchDematSelection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewApplicationDialog(
    ipos: List<IpoEntity>,
    dematAccounts: List<DematWithMember>,
    upiAccounts: List<UpiAccountEntity>,
    initialIpo: IpoEntity? = null,
    initialLots: Int = 1,
    initialBatchMode: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (
        ipoId: Long,
        dematAccountId: Long,
        familyMemberId: Long,
        upiVpa: String,
        upiAccountId: Long?,
        category: String,
        lots: Int,
        bidPrice: Double,
        isCutoff: Boolean,
        totalAmount: Double,
        appNumber: String,
        notes: String?
    ) -> Unit,
    onBatchSubmit: ((
        ipoId: Long,
        selections: List<BatchDematSelection>,
        category: String,
        lots: Int,
        bidPrice: Double,
        isCutoff: Boolean,
        totalAmountPerBid: Double,
        notes: String?
    ) -> Unit)? = null
) {
    var isBatchMode by remember { mutableStateOf(initialBatchMode) }
    var selectedIpo by remember { mutableStateOf(initialIpo ?: ipos.firstOrNull()) }
    var selectedDemat by remember { mutableStateOf(dematAccounts.firstOrNull()) }

    // For 1-Click All Family Demats batch mode: pre-select all Demat accounts
    var selectedBatchDematIds by remember(dematAccounts) {
        mutableStateOf(dematAccounts.map { it.dematAccount.id }.toSet())
    }

    var selectedLots by remember { mutableIntStateOf(initialLots) }
    var isCutoff by remember { mutableStateOf(true) }
    var customPrice by remember { mutableStateOf(selectedIpo?.maxPrice?.toString() ?: "500") }
    var applicationNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // UPI ID Selection: user can pick any UPI ID or enter a custom one
    var selectedUpiAccount by remember { mutableStateOf(upiAccounts.firstOrNull()) }
    var isCustomUpi by remember { mutableStateOf(false) }
    var customUpiVpa by remember { mutableStateOf("") }

    // Dropdown expanded states
    var ipoDropdownExpanded by remember { mutableStateOf(false) }
    var dematDropdownExpanded by remember { mutableStateOf(false) }
    var upiDropdownExpanded by remember { mutableStateOf(false) }

    val bidPrice = if (isCutoff) {
        selectedIpo?.maxPrice ?: 0.0
    } else {
        customPrice.toDoubleOrNull() ?: (selectedIpo?.maxPrice ?: 0.0)
    }

    val lotSize = selectedIpo?.lotSize ?: 1
    val totalShares = selectedLots * lotSize
    val totalAmount = totalShares * bidPrice

    val activeUpiVpa = if (isCustomUpi) customUpiVpa.trim() else (selectedUpiAccount?.vpa ?: "")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("new_application_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBatchMode) "1-Click Multi-Demat Apply" else "Record IPO Application",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isBatchMode) {
                                "Generate bids across all family Demat accounts in 1 click"
                            } else {
                                "Track Demat bid, UPI mandate & allotment"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_new_app_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mode Switcher: Single Demat vs ⚡ 1-Click All Family Demats
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (!isBatchMode) TealPrimary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { isBatchMode = false }
                                .testTag("single_demat_mode_btn")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Single Demat Bid",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isBatchMode) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (isBatchMode) TealPrimary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { isBatchMode = true }
                                .testTag("batch_all_demats_mode_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = if (isBatchMode) Color(0xFF042F2E) else TealLight,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "1-Click All Demats (${dematAccounts.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isBatchMode) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Select IPO Dropdown
                Text(
                    text = "Select Target IPO",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = ipoDropdownExpanded,
                    onExpandedChange = { ipoDropdownExpanded = !ipoDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedIpo?.companyName ?: "Select IPO",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ipoDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("select_ipo_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = ipoDropdownExpanded,
                        onDismissRequest = { ipoDropdownExpanded = false }
                    ) {
                        ipos.forEach { ipo ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(ipo.companyName, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${ipo.series} • Size ₹${ipo.issueSizeCr.toInt()} Cr • Sub ${ipo.totalSub}x • GMP +₹${ipo.gmpInr.toInt()}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                },
                                onClick = {
                                    selectedIpo = ipo
                                    customPrice = ipo.maxPrice.toString()
                                    ipoDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isBatchMode) {
                    // Single Demat Account Dropdown
                    Text(
                        text = "Applicant Demat Account",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded = dematDropdownExpanded,
                        onExpandedChange = { dematDropdownExpanded = !dematDropdownExpanded }
                    ) {
                        val label = selectedDemat?.let {
                            "${it.familyMember.fullName} - ${it.dematAccount.accountAlias} (${it.dematAccount.depository})"
                        } ?: "Select Demat Account"

                        OutlinedTextField(
                            value = label,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dematDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("select_demat_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = dematDropdownExpanded,
                            onDismissRequest = { dematDropdownExpanded = false }
                        ) {
                            dematAccounts.forEach { dm ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                "${dm.familyMember.fullName} (${dm.familyMember.relationship})",
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                "${dm.dematAccount.brokerName} • Client: ${dm.dematAccount.clientId} • PAN: ${dm.familyMember.maskedPan}",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedDemat = dm
                                        dematDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // 1-Click Multi-Demat Checklist
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Family Demat Accounts (${selectedBatchDematIds.size}/${dematAccounts.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedBatchDematIds.size == dematAccounts.size) "Deselect All" else "Select All",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TealLight,
                            modifier = Modifier.clickable {
                                selectedBatchDematIds = if (selectedBatchDematIds.size == dematAccounts.size) {
                                    emptySet()
                                } else {
                                    dematAccounts.map { it.dematAccount.id }.toSet()
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        dematAccounts.forEach { dm ->
                            val isChecked = selectedBatchDematIds.contains(dm.dematAccount.id)
                            val matchedUpi = upiAccounts.firstOrNull { it.familyMemberId == dm.familyMember.id }
                                ?: upiAccounts.firstOrNull()

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChecked) TealPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = if (isChecked) TealPrimary.copy(alpha = 0.5f) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedBatchDematIds = if (isChecked) {
                                            selectedBatchDematIds - dm.dematAccount.id
                                        } else {
                                            selectedBatchDematIds + dm.dematAccount.id
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedBatchDematIds = if (checked) {
                                                selectedBatchDematIds + dm.dematAccount.id
                                            } else {
                                                selectedBatchDematIds - dm.dematAccount.id
                                            }
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${dm.familyMember.fullName} • ${dm.dematAccount.accountAlias}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "PAN: ${dm.familyMember.maskedPan} • Auto UPI: ${matchedUpi?.vpa ?: activeUpiVpa.ifBlank { "user@upi" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(
                                        text = formatInr(totalAmount),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = TealLight
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // UPI ID Selection (Freedom for ANY UPI ID as requested!)
                Text(
                    text = if (isBatchMode) "Fallback / Override UPI ID for Mandate" else "UPI ID for ASBA Mandate",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (!isCustomUpi) {
                    ExposedDropdownMenuBox(
                        expanded = upiDropdownExpanded,
                        onExpandedChange = { upiDropdownExpanded = !upiDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedUpiAccount?.let { "${it.vpa} (${it.bankName})" } ?: "Select UPI ID",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = upiDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("select_upi_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = upiDropdownExpanded,
                            onDismissRequest = { upiDropdownExpanded = false }
                        ) {
                            upiAccounts.forEach { upi ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(upi.vpa, fontWeight = FontWeight.SemiBold)
                                            Text(upi.bankName, style = MaterialTheme.typography.labelSmall)
                                        }
                                    },
                                    onClick = {
                                        selectedUpiAccount = upi
                                        upiDropdownExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Enter Custom / Other UPI ID", color = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    isCustomUpi = true
                                    upiDropdownExpanded = false
                                }
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customUpiVpa,
                            onValueChange = { customUpiVpa = it },
                            placeholder = { Text("e.g. mobile@okaxis or user@upi") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_upi_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { isCustomUpi = false }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Use saved UPI")
                        }
                    }
                }

                // UPI & 1-Click workflow note
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBatchMode) {
                            "1-Click Batch Mode logs all selected Demat bids at once. Per SEBI/NPCI rules, approve each UPI mandate PIN in your UPI app."
                        } else {
                            "You can link any UPI ID from any family member or bank account."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Lots and Cutoff Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Number of Lots (per Demat)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "${selectedLots * lotSize} shares per bid",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (selectedLots > 1) selectedLots-- },
                            enabled = selectedLots > 1,
                            modifier = Modifier.testTag("decrease_lots_button")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(
                            text = "$selectedLots",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        IconButton(
                            onClick = { if (selectedLots < 14) selectedLots++ },
                            modifier = Modifier.testTag("increase_lots_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cutoff price checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isCutoff,
                        onCheckedChange = { isCutoff = it },
                        modifier = Modifier.testTag("cutoff_checkbox")
                    )
                    Text(
                        text = "Apply at Cutoff Price (₹${selectedIpo?.maxPrice?.toInt() ?: 0})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (!isCutoff) {
                    OutlinedTextField(
                        value = customPrice,
                        onValueChange = { customPrice = it },
                        label = { Text("Bid Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (!isBatchMode) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Application Reference Number (Optional)
                    OutlinedTextField(
                        value = applicationNumber,
                        onValueChange = { applicationNumber = it },
                        label = { Text("Application Reference No. (Optional)") },
                        placeholder = { Text("Auto-generated if blank") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("application_number_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Calculated Summary Card
                val effectiveCount = if (isBatchMode) selectedBatchDematIds.size else 1
                val aggregateBlocked = totalAmount * effectiveCount

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isBatchMode) "Total Aggregate Blocked ($effectiveCount Demats)" else "Total Blocked Amount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatInr(aggregateBlocked),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (totalAmount <= 200000.0) "Retail Quota" else "sHNI Quota",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (selectedIpo != null && selectedIpo!!.gmpInr > 0) {
                                Text(
                                    text = "Est. Gain: +${formatInr(selectedIpo!!.estimatedProfitPerLot * selectedLots * effectiveCount)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                Button(
                    onClick = {
                        val ipo = selectedIpo ?: return@Button
                        val category = if (totalAmount <= 200000.0) {
                            IpoCategory.RETAIL.displayName
                        } else {
                            IpoCategory.SHNI.displayName
                        }
                        val fallbackUpi = if (activeUpiVpa.isNotBlank()) activeUpiVpa else "user@upi"

                        if (isBatchMode) {
                            val chosenDemats = dematAccounts.filter { selectedBatchDematIds.contains(it.dematAccount.id) }
                            if (chosenDemats.isEmpty()) return@Button

                            if (onBatchSubmit != null) {
                                val selections = chosenDemats.map { dm ->
                                    val memberUpi = if (!isCustomUpi) {
                                        upiAccounts.firstOrNull { it.familyMemberId == dm.familyMember.id }
                                            ?: selectedUpiAccount
                                    } else null
                                    BatchDematSelection(
                                        dematAccountId = dm.dematAccount.id,
                                        familyMemberId = dm.familyMember.id,
                                        upiVpa = memberUpi?.vpa ?: fallbackUpi,
                                        upiAccountId = memberUpi?.id
                                    )
                                }
                                onBatchSubmit(
                                    ipo.id,
                                    selections,
                                    category,
                                    selectedLots,
                                    bidPrice,
                                    isCutoff,
                                    totalAmount,
                                    notes.ifBlank { "1-Click Multi-Demat Batch Bid" }
                                )
                            } else {
                                chosenDemats.forEach { dm ->
                                    onSubmit(
                                        ipo.id,
                                        dm.dematAccount.id,
                                        dm.familyMember.id,
                                        fallbackUpi,
                                        if (isCustomUpi) null else selectedUpiAccount?.id,
                                        category,
                                        selectedLots,
                                        bidPrice,
                                        isCutoff,
                                        totalAmount,
                                        "",
                                        notes.ifBlank { "1-Click Multi-Demat Batch Bid" }
                                    )
                                }
                            }
                        } else {
                            val demat = selectedDemat ?: return@Button
                            onSubmit(
                                ipo.id,
                                demat.dematAccount.id,
                                demat.familyMember.id,
                                fallbackUpi,
                                if (isCustomUpi) null else selectedUpiAccount?.id,
                                category,
                                selectedLots,
                                bidPrice,
                                isCutoff,
                                totalAmount,
                                applicationNumber,
                                notes.ifBlank { null }
                            )
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_application_button"),
                    shape = RoundedCornerShape(14.dp),
                    enabled = selectedIpo != null && (if (isBatchMode) selectedBatchDematIds.isNotEmpty() else selectedDemat != null)
                ) {
                    Icon(
                        imageVector = if (isBatchMode) Icons.Default.Bolt else Icons.Default.Check,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBatchMode) {
                            "1-Click Apply Across ${selectedBatchDematIds.size} Demats (${formatInr(aggregateBlocked)})"
                        } else {
                            "Save Application (${formatInr(totalAmount)})"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
