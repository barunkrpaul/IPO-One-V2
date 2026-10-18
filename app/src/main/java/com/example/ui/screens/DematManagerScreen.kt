package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.DematAccountEntity
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.UpiAccountEntity
import com.example.model.FamilyMemberWithAccounts
import com.example.ui.components.SectionHeader
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.DematViewModel

@Composable
fun DematManagerScreen(
    dematViewModel: DematViewModel,
    modifier: Modifier = Modifier
) {
    val familyMembersWithAccounts by dematViewModel.familyMembersWithAccounts.collectAsStateWithLifecycle()

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var selectedMemberForDemat by remember { mutableStateOf<FamilyMemberEntity?>(null) }
    var selectedMemberForUpi by remember { mutableStateOf<FamilyMemberEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("demat_manager_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "Family & Demat Vault",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Identifiers masked by default • Protected by local encryption",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Privacy & Masking Banner
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PAN and Demat BOIDs are masked by default. Tap the eye icon on any profile to temporarily reveal identifiers.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick add member header
            item {
                SectionHeader(
                    title = "Investor Profiles (${familyMembersWithAccounts.size})",
                    actionText = "+ Add Member",
                    onActionClick = { showAddMemberDialog = true }
                )
            }

            // Family Member Cards
            items(familyMembersWithAccounts) { memberWithAccounts ->
                FamilyMemberProfileCard(
                    data = memberWithAccounts,
                    onAddDemat = { selectedMemberForDemat = memberWithAccounts.member },
                    onAddUpi = { selectedMemberForUpi = memberWithAccounts.member },
                    onDeleteDemat = { dematViewModel.deleteDematAccount(it) },
                    onDeleteUpi = { dematViewModel.deleteUpiAccount(it) },
                    onDeleteMember = { dematViewModel.deleteFamilyMember(memberWithAccounts.member) }
                )
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }

        // FAB to add member
        FloatingActionButton(
            onClick = { showAddMemberDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_family_member_fab"),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Member")
        }
    }

    // Add Family Member Dialog
    if (showAddMemberDialog) {
        AddFamilyMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { name, relationship, pan ->
                dematViewModel.addFamilyMember(name, relationship, pan)
                showAddMemberDialog = false
            }
        )
    }

    // Add Demat Dialog
    selectedMemberForDemat?.let { member ->
        AddDematDialog(
            member = member,
            onDismiss = { selectedMemberForDemat = null },
            onConfirm = { broker, depository, dpId, clientId, alias ->
                dematViewModel.addDematAccount(
                    member.id,
                    broker,
                    depository,
                    dpId,
                    clientId,
                    alias
                )
                selectedMemberForDemat = null
            }
        )
    }

    // Add UPI Dialog
    selectedMemberForUpi?.let { member ->
        AddUpiDialog(
            member = member,
            onDismiss = { selectedMemberForUpi = null },
            onConfirm = { vpa, bank ->
                dematViewModel.addUpiAccount(member.id, vpa, bank)
                selectedMemberForUpi = null
            }
        )
    }
}

@Composable
fun FamilyMemberProfileCard(
    data: FamilyMemberWithAccounts,
    onAddDemat: () -> Unit,
    onAddUpi: () -> Unit,
    onDeleteDemat: (DematAccountEntity) -> Unit,
    onDeleteUpi: (UpiAccountEntity) -> Unit,
    onDeleteMember: () -> Unit
) {
    var revealSensitive by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("family_card_${data.member.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Member Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = data.member.fullName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (data.member.isPrimary) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 6.dp)
                                ) {
                                    Text(
                                        text = "Primary",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${data.member.relationship} • PAN: ${if (revealSensitive) data.member.pan else data.member.maskedPan}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (revealSensitive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { revealSensitive = !revealSensitive },
                        modifier = Modifier.testTag("toggle_reveal_pan_${data.member.id}")
                    ) {
                        Icon(
                            imageVector = if (revealSensitive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (revealSensitive) "Hide PAN & Account IDs" else "Reveal PAN & Account IDs",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (!data.member.isPrimary) {
                        IconButton(onClick = onDeleteMember) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Demat Accounts Sub-Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Demat Accounts (${data.dematAccounts.size})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "+ Add Demat",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onAddDemat() }
                        .padding(4.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            if (data.dematAccounts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    data.dematAccounts.forEach { demat ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = demat.accountAlias,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val dpDisplay = if (revealSensitive) demat.dpId else demat.maskedDpId
                                        val clientDisplay = if (revealSensitive) demat.clientId else demat.maskedClientId
                                        Text(
                                            text = "${demat.brokerName} • ${demat.depository} DP: $dpDisplay • Client: $clientDisplay",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteDemat(demat) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "No Demat account linked. Tap + Add Demat to link Zerodha, Groww, etc.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // UPI Accounts Sub-Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UPI Accounts (${data.upiAccounts.size})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "+ Add UPI",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onAddUpi() }
                        .padding(4.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            if (data.upiAccounts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    data.upiAccounts.forEach { upi ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = upi.vpa,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = upi.bankName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteUpi(upi) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "No UPI ID added. Tap + Add UPI to store bank VPA for mandate requests.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AddFamilyMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Self") }
    var pan by remember { mutableStateOf("") }
    var consentChecked by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Add Family Member / Investor",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name (as per PAN)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship (e.g. Self, Spouse, Father)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = pan,
                    onValueChange = { if (it.length <= 10) pan = it.uppercase() },
                    label = { Text("10-Digit PAN (e.g. ABCPS1234F)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Informed Consent Notice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = consentChecked,
                        onCheckedChange = { consentChecked = it }
                    )
                    Text(
                        text = "I consent to storing this PAN locally on-device solely for IPO allotment lookup & multi-account tracking.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (fullName.isNotBlank() && pan.length == 10 && consentChecked) {
                            onConfirm(fullName, relationship, pan)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    enabled = fullName.isNotBlank() && pan.length == 10 && consentChecked
                ) {
                    Text("Save Investor Profile")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDematDialog(
    member: FamilyMemberEntity,
    onDismiss: () -> Unit,
    onConfirm: (broker: String, depository: String, dpId: String, clientId: String, alias: String) -> Unit
) {
    var brokerName by remember { mutableStateOf("Zerodha") }
    var depository by remember { mutableStateOf("CDSL") }
    var dpId by remember { mutableStateOf("12081600") }
    var clientId by remember { mutableStateOf("") }
    var accountAlias by remember { mutableStateOf("") }
    var consentChecked by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Link Demat for ${member.fullName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = brokerName,
                    onValueChange = { brokerName = it },
                    label = { Text("Broker (Zerodha, Groww, AngelOne...)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = depository,
                        onValueChange = { depository = it.uppercase() },
                        label = { Text("Depository (CDSL/NSDL)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dpId,
                        onValueChange = { dpId = it },
                        label = { Text("DP ID (8 Digits)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = it },
                    label = { Text("Client ID / BOID (8 Digits)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = accountAlias,
                    onValueChange = { accountAlias = it },
                    label = { Text("Account Alias (e.g. Rahul Main Zerodha)") },
                    placeholder = { Text("${member.fullName} $brokerName") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = consentChecked,
                        onCheckedChange = { consentChecked = it }
                    )
                    Text(
                        text = "Save DP/Client ID locally (masked by default). Never enter broker passwords or PINs.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (clientId.isNotBlank() && consentChecked) {
                            val alias = accountAlias.ifBlank { "${member.fullName} $brokerName" }
                            onConfirm(brokerName, depository, dpId, clientId, alias)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    enabled = clientId.isNotBlank() && consentChecked
                ) {
                    Text("Save Demat Account")
                }
            }
        }
    }
}

@Composable
fun AddUpiDialog(
    member: FamilyMemberEntity,
    onDismiss: () -> Unit,
    onConfirm: (vpa: String, bank: String) -> Unit
) {
    var vpa by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("HDFC Bank") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Add UPI ID for ${member.fullName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Only VPA handle is stored for mandate tracking. Never enter UPI PIN.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = vpa,
                    onValueChange = { vpa = it.trim().lowercase() },
                    label = { Text("UPI VPA (e.g. mobile@okhdfcbank)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Linked Bank (HDFC, SBI, ICICI...)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (vpa.contains("@")) {
                            onConfirm(vpa, bankName)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    enabled = vpa.contains("@")
                ) {
                    Text("Save UPI ID")
                }
            }
        }
    }
}
