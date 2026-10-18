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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AllotmentStatus
import com.example.model.ApplicationWithDetails
import com.example.model.MandateStatus
import com.example.ui.components.AllotmentStatusBadge
import com.example.ui.components.MandateStatusBadge
import com.example.ui.components.formatInr
import com.example.ui.theme.AmberPending
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.ApplicationFilter
import com.example.ui.viewmodel.ApplicationViewModel

@Composable
fun ApplicationTrackerScreen(
    applicationViewModel: ApplicationViewModel,
    onOpenNewApplicationDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedFilter by applicationViewModel.selectedFilter.collectAsStateWithLifecycle()
    val filteredApplications by applicationViewModel.filteredApplications.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    var statusUpdateApp by remember { mutableStateOf<ApplicationWithDetails?>(null) }

    val displayedApplications = remember(filteredApplications, searchQuery) {
        if (searchQuery.isBlank()) {
            filteredApplications
        } else {
            filteredApplications.filter {
                it.ipo.companyName.contains(searchQuery, ignoreCase = true) ||
                        it.ipo.symbol.contains(searchQuery, ignoreCase = true) ||
                        it.familyMember.fullName.contains(searchQuery, ignoreCase = true) ||
                        it.dematAccount.brokerName.contains(searchQuery, ignoreCase = true) ||
                        it.application.applicationNumber.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("application_tracker_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "Application & Bid Tracker",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Personal tracker • Status history, source & last-updated timestamps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by IPO, family member, broker or App #...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("application_search_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filters
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ApplicationFilter.values()) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { applicationViewModel.setFilter(filter) },
                                label = { Text(filter.displayName) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // Applications List
            if (displayedApplications.isNotEmpty()) {
                items(displayedApplications) { appWithDetails ->
                    DetailedApplicationCard(
                        item = appWithDetails,
                        onToggleBroker = {
                            applicationViewModel.toggleChecklist(
                                appWithDetails.application,
                                brokerSubmitted = !appWithDetails.application.checklistBrokerSubmitted
                            )
                        },
                        onToggleMandate = {
                            val newAccepted = !appWithDetails.application.checklistMandateAccepted
                            applicationViewModel.toggleChecklist(
                                appWithDetails.application,
                                mandateAccepted = newAccepted
                            )
                            if (newAccepted) {
                                applicationViewModel.updateMandateStatus(
                                    appWithDetails.application,
                                    MandateStatus.APPROVED
                                )
                            }
                        },
                        onToggleAllotment = {
                            applicationViewModel.toggleChecklist(
                                appWithDetails.application,
                                allotmentChecked = !appWithDetails.application.checklistAllotmentChecked
                            )
                        },
                        onOpenStatusModal = { statusUpdateApp = appWithDetails },
                        onDelete = { applicationViewModel.deleteApplication(appWithDetails.application) }
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Applications Found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try changing the filter or record your first application.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onOpenNewApplicationDialog,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Application")
                            }
                        }
                    }
                }
            }

            // Bottom space for FAB
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onOpenNewApplicationDialog,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_application_fab"),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Application")
        }
    }

    // Status Update Dialog
    statusUpdateApp?.let { app ->
        ApplicationStatusUpdateDialog(
            item = app,
            onDismiss = { statusUpdateApp = null },
            onUpdateMandate = { newMandate ->
                applicationViewModel.updateMandateStatus(app.application, newMandate)
                statusUpdateApp = null
            },
            onUpdateAllotment = { newAllotment, shares, refund ->
                applicationViewModel.updateAllotmentStatus(app.application, newAllotment, shares, refund)
                statusUpdateApp = null
            }
        )
    }
}

@Composable
fun DetailedApplicationCard(
    item: ApplicationWithDetails,
    onToggleBroker: () -> Unit,
    onToggleMandate: () -> Unit,
    onToggleAllotment: () -> Unit,
    onOpenStatusModal: () -> Unit,
    onDelete: () -> Unit
) {
    var revealIdentifiers by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("detailed_app_card_${item.application.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Company & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.ipo.companyName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "App Ref: ${item.application.applicationNumber} • Personal Tracker Entry",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MandateStatusBadge(status = item.application.mandateStatus)
                    AllotmentStatusBadge(status = item.application.allotmentStatus)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Applicant, Broker, and UPI Details with Masking by Default
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val panDisplay = if (revealIdentifiers) item.familyMember.pan else item.familyMember.maskedPan
                        Text(
                            text = "Applicant: ${item.familyMember.fullName} ($panDisplay)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.dematAccount.brokerName,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { revealIdentifiers = !revealIdentifiers },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (revealIdentifiers) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (revealIdentifiers) "Mask IDs" else "Reveal IDs",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "UPI VPA: ${item.application.upiVpa}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val dpDisplay = if (revealIdentifiers) item.dematAccount.dpId else item.dematAccount.maskedDpId
                        val clientDisplay = if (revealIdentifiers) item.dematAccount.clientId else item.dematAccount.maskedClientId
                        Text(
                            text = "DP: $dpDisplay • BOID: $clientDisplay",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bid Financials & Live GMP Expected Gain
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Lots & Category",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${item.application.lotCount} Lot (${item.application.lotCount * item.ipo.lotSize} shares) • ${item.application.category}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (item.ipo.gmpInr > 0) {
                        val estGain = item.application.lotCount * item.ipo.estimatedProfitPerLot
                        Text(
                            text = "Est. GMP Gain: +${formatInr(estGain)} (Updated: ${item.ipo.formattedGmpLastUpdated})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldSuccess,
                            fontSize = 10.sp
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Blocked Amount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatInr(item.application.totalAmount),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            if (item.application.sharesAllotted > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldSuccess.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Allotted: ${item.application.sharesAllotted} shares! Listing on ${item.ipo.listingDate}.",
                            color = EmeraldSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Checklists Row
            Text(
                text = "Application Milestones Checklist",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            ChecklistItem(
                label = "1. Broker Bid Submitted (${item.dematAccount.brokerName})",
                isChecked = item.application.checklistBrokerSubmitted,
                onToggle = onToggleBroker
            )
            ChecklistItem(
                label = "2. UPI Mandate Approved (Before 5:00 PM on ${item.ipo.closeDate})",
                isChecked = item.application.checklistMandateAccepted,
                onToggle = onToggleMandate
            )
            ChecklistItem(
                label = "3. Allotment Result Verified (${item.ipo.allotmentDate})",
                isChecked = item.application.checklistAllotmentChecked,
                onToggle = onToggleAllotment
            )

            // Status History & Source Audit Log
            if (item.statusEvents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Status History & Source Log",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        item.statusEvents.sortedByDescending { it.timestamp }.take(3).forEach { ev ->
                            Column {
                                Text(
                                    text = "• ${ev.description}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "  ${ev.sourceLabel} • Last Updated: ${ev.formattedTimestamp}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.5.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Actions row: Update Status, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_app_${item.application.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }

                OutlinedButton(
                    onClick = onOpenStatusModal,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("update_status_btn_${item.application.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Update Status", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ChecklistItem(
    label: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onToggle() }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isChecked) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isChecked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ApplicationStatusUpdateDialog(
    item: ApplicationWithDetails,
    onDismiss: () -> Unit,
    onUpdateMandate: (MandateStatus) -> Unit,
    onUpdateAllotment: (AllotmentStatus, shares: Int, refund: Double) -> Unit
) {
    var sharesAllottedText by remember {
        mutableStateOf(if (item.application.sharesAllotted > 0) item.application.sharesAllotted.toString() else item.ipo.lotSize.toString())
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Update Status: ${item.ipo.companyName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Applicant: ${item.familyMember.fullName} • App #${item.application.applicationNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mandate Status Selection
                Text(
                    text = "UPI Mandate Status",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onUpdateMandate(MandateStatus.APPROVED) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Approved", fontSize = 11.sp, color = EmeraldSuccess)
                    }
                    OutlinedButton(
                        onClick = { onUpdateMandate(MandateStatus.PENDING) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Pending", fontSize = 11.sp, color = AmberPending)
                    }
                    OutlinedButton(
                        onClick = { onUpdateMandate(MandateStatus.REJECTED) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Rejected", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Allotment Status Selection
                Text(
                    text = "Allotment Result",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = sharesAllottedText,
                    onValueChange = { sharesAllottedText = it },
                    label = { Text("Shares Allotted (If won)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val shares = sharesAllottedText.toIntOrNull() ?: item.ipo.lotSize
                            onUpdateAllotment(AllotmentStatus.ALLOTTED, shares, 0.0)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Allotted!", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            onUpdateAllotment(AllotmentStatus.NOT_ALLOTTED, 0, item.application.totalAmount)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Not Allotted", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
