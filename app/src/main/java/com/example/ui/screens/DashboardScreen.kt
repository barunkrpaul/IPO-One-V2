package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.IpoEntity
import com.example.data.local.entity.StatusEventEntity
import com.example.model.ApplicationWithDetails
import com.example.ui.components.AllotmentStatusBadge
import com.example.ui.components.IpoOneLogo
import com.example.ui.components.MandateStatusBadge
import com.example.ui.components.MetricCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatInr
import com.example.ui.theme.AmberPending
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.DashboardViewModel

enum class IpoDisplayMode {
    GRID,
    LIST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    onNavigateToIpos: () -> Unit,
    onNavigateToApplications: () -> Unit,
    onNavigateToAllotment: () -> Unit,
    onOpenNewApplicationDialog: (IpoEntity?) -> Unit,
    onIpoClick: (IpoEntity) -> Unit,
    onNavigateToDemat: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val allIpos by dashboardViewModel.allIpos.collectAsStateWithLifecycle()
    val applications by dashboardViewModel.applications.collectAsStateWithLifecycle()
    val recentStatusEvents by dashboardViewModel.recentStatusEvents.collectAsStateWithLifecycle()
    val syncMessage by dashboardViewModel.syncMessage.collectAsStateWithLifecycle()

    syncMessage?.let { msg ->
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        dashboardViewModel.clearSyncMessage()
    }

    var ipoDisplayMode by remember { mutableStateOf(IpoDisplayMode.GRID) }
    var selectedIpoFilter by remember { mutableStateOf("All") }

    var notifyCloseDeadline by remember { mutableStateOf(true) }
    var notifyMandateCutoff by remember { mutableStateOf(true) }
    var notifyAllotmentOut by remember { mutableStateOf(true) }

    val activeAndUpcomingIpos = remember(allIpos, selectedIpoFilter) {
        val base = allIpos.filter { it.status == "Open Now" || it.status == "Upcoming" }
        when (selectedIpoFilter) {
            "Open Now" -> base.filter { it.status == "Open Now" }
            "Upcoming" -> base.filter { it.status == "Upcoming" }
            else -> base
        }
    }

    val openIpos = remember(allIpos) {
        allIpos.filter { it.status == "Open Now" || it.status == "Open" }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Quick-Access Navigation Bar (Navy / Slate / Teal)
        item {
            DashboardQuickAccessBar(
                onNewBid = { onOpenNewApplicationDialog(null) },
                onIpoRadar = onNavigateToIpos,
                onDematVault = onNavigateToDemat,
                onAllotmentHub = onNavigateToAllotment
            )
        }

        // 2. Active Applications Summary Card (Navy / Slate / Teal Centerpiece)
        item {
            ActiveApplicationsSummaryCard(
                uiState = uiState,
                applications = applications,
                onRecordBid = { onOpenNewApplicationDialog(null) },
                onManageBids = onNavigateToApplications
            )
        }

        // 3. Urgent UPI Mandate Alert Banner
        if (uiState.urgentMandates.isNotEmpty()) {
            item {
                val urgent = uiState.urgentMandates.first()
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = AmberPending.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("urgent_mandate_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = AmberPending.copy(alpha = 0.12f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AmberPending.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = AmberPending,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "UPI Mandate Action Required!",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = AmberPending
                            )
                            Text(
                                text = "Approve ₹${urgent.application.totalAmount.toInt()} for ${urgent.ipo.companyName} on ${urgent.application.upiVpa} before 5:00 PM cutoff.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { dashboardViewModel.quickApproveMandate(urgent) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPending),
                            modifier = Modifier.testTag("quick_approve_mandate_button")
                        ) {
                            Text("Approved", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. Upcoming & Open IPOs Section Header + Grid/List Display Toggle
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upcoming_ipos_section"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Upcoming & Live IPOs (${activeAndUpcomingIpos.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Live GMP, T+3 dates & 1-tap bid logging",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { dashboardViewModel.deployLiveIpos() },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("deploy_live_ipos_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Deploy Live IPO Feed",
                                modifier = Modifier.size(14.dp),
                                tint = EmeraldSuccess
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Deploy Live Feed",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        }

                        // Grid / List Toggle Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("ipo_view_mode_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                ViewModeToggleChip(
                                    label = "Grid",
                                    icon = Icons.Default.GridView,
                                    selected = ipoDisplayMode == IpoDisplayMode.GRID,
                                    onClick = { ipoDisplayMode = IpoDisplayMode.GRID },
                                    testTag = "ipo_grid_view_button"
                                )
                                ViewModeToggleChip(
                                    label = "List",
                                    icon = Icons.Default.ViewList,
                                    selected = ipoDisplayMode == IpoDisplayMode.LIST,
                                    onClick = { ipoDisplayMode = IpoDisplayMode.LIST },
                                    testTag = "ipo_list_view_button"
                                )
                            }
                        }
                    }
                }

                // Filter Chips Row + Directory Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Open Now", "Upcoming").forEach { filter ->
                            val selected = selectedIpoFilter == filter
                            FilterChip(
                                selected = selected,
                                onClick = { selectedIpoFilter = filter },
                                label = {
                                    Text(
                                        text = filter,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = TealLight
                                ),
                                modifier = Modifier.testTag("dashboard_ipo_filter_${filter.lowercase().replace(" ", "_")}")
                            )
                        }
                    }

                    Text(
                        text = "Full Directory →",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNavigateToIpos() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 5. Upcoming & Live IPOs Grid / List Content
        item {
            if (activeAndUpcomingIpos.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "No IPOs matching '$selectedIpoFilter' right now. Check the full IPO directory.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            } else {
                AnimatedContent(
                    targetState = ipoDisplayMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ipo_grid_list_transition"
                ) { mode ->
                    when (mode) {
                        IpoDisplayMode.GRID -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("upcoming_ipos_grid_container"),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                activeAndUpcomingIpos.chunked(2).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowItems.forEach { ipo ->
                                            DashboardIpoGridCard(
                                                ipo = ipo,
                                                onClick = { onIpoClick(ipo) },
                                                onApply = { onOpenNewApplicationDialog(ipo) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (rowItems.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        IpoDisplayMode.LIST -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("upcoming_ipos_list_container"),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                activeAndUpcomingIpos.forEach { ipo ->
                                    DashboardIpoCard(
                                        ipo = ipo,
                                        onClick = { onIpoClick(ipo) },
                                        onApply = { onOpenNewApplicationDialog(ipo) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. 4-Grid Portfolio Metrics
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Blocked Capital",
                        value = formatInr(uiState.totalBlockedCapital),
                        subtitle = "Active in ASBA/UPI",
                        icon = Icons.Default.Lock,
                        iconColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Pending Mandates",
                        value = "${uiState.pendingMandatesCount}",
                        subtitle = "Needs UPI approval",
                        icon = Icons.Default.Schedule,
                        iconColor = if (uiState.pendingMandatesCount > 0) AmberPending else EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Total Bids Placed",
                        value = "${uiState.totalApplications}",
                        subtitle = "Across all Demats",
                        icon = Icons.Default.Assignment,
                        iconColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Allotments Won",
                        value = "${uiState.allottedCount}",
                        subtitle = if (uiState.totalApplications > 0) "${(uiState.allottedCount * 100 / uiState.totalApplications)}% success rate" else "Track wins",
                        icon = Icons.Default.CheckCircle,
                        iconColor = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 7. Recent Active Applications List
        item {
            SectionHeader(
                title = "Active Applications (${applications.size})",
                actionText = "Manage Bids",
                onActionClick = onNavigateToApplications
            )
        }

        if (applications.isNotEmpty()) {
            items(applications.take(4)) { appWithDetails ->
                DashboardApplicationCard(
                    item = appWithDetails,
                    onClick = onNavigateToApplications
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
                            text = "No IPO Applications Logged Yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Apply across family Demat accounts and track UPI approval.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { onOpenNewApplicationDialog(null) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Record Application")
                        }
                    }
                }
            }
        }

        // 8. Concise Activity Timeline with Source & Last-Updated Time
        if (recentStatusEvents.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Activity & Status Timeline",
                    actionText = "Full History",
                    onActionClick = onNavigateToApplications
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        recentStatusEvents.take(4).forEach { event ->
                            ActivityTimelineRow(event = event)
                        }
                    }
                }
            }
        }

        // 8b. Super Feature: Smart Allotment Probability & Family Capital Optimizer
        if (openIpos.isNotEmpty()) {
            item {
                SmartAllotmentOptimizerCard(
                    openIpos = openIpos,
                    activeDematCount = 4,
                    onApplyAllFamily = { ipo -> onOpenNewApplicationDialog(ipo) }
                )
            }
        }

        // 9. Deadline Reminders & Notification Controls Card
        item {
            SectionHeader(title = "Smart IPO Reminders & Alerts")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    ReminderToggleRow(
                        title = "Closing Day 3:00 PM Bid Deadline",
                        subtitle = "Alert before exchange ASBA window closes",
                        checked = notifyCloseDeadline,
                        onCheckedChange = { notifyCloseDeadline = it }
                    )
                    ReminderToggleRow(
                        title = "5:00 PM UPI Mandate Cutoff Alert",
                        subtitle = "Remind for pending UPI block requests",
                        checked = notifyMandateCutoff,
                        onCheckedChange = { notifyMandateCutoff = it }
                    )
                    ReminderToggleRow(
                        title = "T+1 Registrar Allotment Out Alert",
                        subtitle = "Notify when KFin / Link Intime publishes basis",
                        checked = notifyAllotmentOut,
                        onCheckedChange = { notifyAllotmentOut = it }
                    )
                }
            }
        }

        // 10. Quick Allotment Checker Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAllotment() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Official Allotment Status Hub",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "1-click PAN copy & official registrar portals",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onNavigateToAllotment,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Check", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardQuickAccessBar(
    onNewBid: () -> Unit,
    onIpoRadar: () -> Unit,
    onDematVault: () -> Unit,
    onAllotmentHub: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("dashboard_quick_access_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickAccessNavPill(
                label = "Record Bid",
                icon = Icons.Default.Add,
                accentColor = TealPrimary,
                onClick = onNewBid,
                testTag = "quick_nav_record_bid"
            )
            QuickAccessNavPill(
                label = "IPO Radar",
                icon = Icons.Default.TrendingUp,
                accentColor = CyanAccent,
                onClick = onIpoRadar,
                testTag = "quick_nav_ipo_radar"
            )
            QuickAccessNavPill(
                label = "Demat Vault",
                icon = Icons.Default.Groups,
                accentColor = MaterialTheme.colorScheme.secondary,
                onClick = onDematVault,
                testTag = "quick_nav_demat_vault"
            )
            QuickAccessNavPill(
                label = "Allotment",
                icon = Icons.Default.AccountBalance,
                accentColor = EmeraldSuccess,
                onClick = onAllotmentHub,
                testTag = "quick_nav_allotment"
            )
        }
    }
}

@Composable
private fun QuickAccessNavPill(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.16f))
                .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp
        )
    }
}

@Composable
fun ActiveApplicationsSummaryCard(
    uiState: DashboardUiState,
    applications: List<ApplicationWithDetails>,
    onRecordBid: () -> Unit,
    onManageBids: () -> Unit
) {
    val approvedMandatesCount = applications.count { it.application.mandateStatus == "Approved" }
    val activeProgress = if (uiState.totalApplications > 0) {
        (approvedMandatesCount.toFloat() / uiState.totalApplications.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        TealPrimary.copy(alpha = 0.65f),
                        CyanAccent.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("active_applications_summary_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TealPrimary.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = TealLight,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ACTIVE APPLICATIONS SUMMARY • MULTI-DEMAT",
                                color = TealLight,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "IPO One Command Hub",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Active ASBA Capital, Mandate Progress & Live GMP Gain",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IpoOneLogo(size = 48.dp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Metrics Strip inside Summary Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Active Bids",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${uiState.totalApplications} Applications",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ASBA Blocked",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatInr(uiState.totalBlockedCapital),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = TealLight
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Est. GMP Gain",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "+${formatInr(uiState.totalExpectedListingGain)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = EmeraldSuccess
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mandate Approval Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UPI Mandate Approval Progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$approvedMandatesCount/${uiState.totalApplications} Approved",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TealLight
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { activeProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(99.dp)),
                        color = TealPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status Breakdown Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryStatusPill(
                            label = "Approved",
                            count = approvedMandatesCount,
                            color = EmeraldSuccess
                        )
                        SummaryStatusPill(
                            label = "Pending UPI",
                            count = uiState.pendingMandatesCount,
                            color = AmberPending
                        )
                        SummaryStatusPill(
                            label = "Allotted",
                            count = uiState.allottedCount,
                            color = CyanAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary Card Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRecordBid,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("summary_record_bid_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF042F2E),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "1-Click / Record Bid",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF042F2E)
                    )
                }
                OutlinedButton(
                    onClick = onManageBids,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("summary_view_bids_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Manage Active Bids",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStatusPill(
    label: String,
    count: Int,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "$label: $count",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = color,
                fontSize = 10.5.sp
            )
        }
    }
}

@Composable
private fun ViewModeToggleChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = if (selected) TealPrimary else Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (selected) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DashboardIpoGridCard(
    ipo: IpoEntity,
    onClick: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOpen = ipo.status == "Open Now"
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (isOpen) TealPrimary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .testTag("dashboard_ipo_card_${ipo.symbol.lowercase()}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isOpen) TealPrimary.copy(alpha = 0.18f) else CyanAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = ipo.status.uppercase(),
                        color = if (isOpen) TealLight else CyanAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CyanAccent.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "₹${ipo.issueSizeCr.toInt()} Cr",
                        color = CyanAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = ipo.companyName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "₹${ipo.minPrice.toInt()}–₹${ipo.maxPrice.toInt()} • ${ipo.lotSize} sh",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Real-Time Category Subscription Mini Pill
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "QIB ${ipo.qibSubscription}x",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ret ${ipo.retailSubscription}x",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealLight
                    )
                    Text(
                        text = "Tot ${ipo.subscriptionTimes}x",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (ipo.subscriptionTimes >= 1.0) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Live GMP Badge inside Grid Card
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE GMP",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (ipo.gmpInr > 0) "+₹${ipo.gmpInr.toInt()} (${ipo.gmpPercent}%)" else "₹0",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (ipo.gmpInr > 0) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Est. +${formatInr(ipo.estimatedProfitPerLot)}/lot",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Close: ${ipo.closeDate.takeLast(5)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.5.sp
                )
                Text(
                    text = "List: ${ipo.listingDate.takeLast(5)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TealLight,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text(
                    text = "1-Click / Record Bid",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF042F2E)
                )
            }
        }
    }
}

@Composable
fun DashboardIpoCard(
    ipo: IpoEntity,
    onClick: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier.width(295.dp)
) {
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (ipo.status == "Open Now") TealPrimary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .testTag("dashboard_ipo_card_${ipo.symbol.lowercase()}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${ipo.status} • ${ipo.series}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TealLight
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CyanAccent.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "Size: ₹${ipo.issueSizeCr} Cr",
                            color = CyanAccent,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (ipo.gmpInr > 0) EmeraldSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = if (ipo.gmpInr > 0) "LIVE GMP +₹${ipo.gmpInr.toInt()} (+${ipo.gmpPercent}%)" else "GMP ₹0",
                        color = if (ipo.gmpInr > 0) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = ipo.companyName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Updated: ${ipo.formattedGmpLastUpdated}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Min Bid: ${formatInr(ipo.lotSize * ipo.maxPrice)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (ipo.gmpInr > 0) {
                    Text(
                        text = "Est. +${formatInr(ipo.estimatedProfitPerLot)}/lot",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = EmeraldSuccess
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Real-Time Category Subscription Row inside List Card
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CrucialDateMiniColumn("QIB Sub", "${ipo.qibSubscription}x")
                    CrucialDateMiniColumn("NII/HNI", "${ipo.niiSubscription}x")
                    CrucialDateMiniColumn("Retail", "${ipo.retailSubscription}x")
                    CrucialDateMiniColumn("Total Sub", "${ipo.subscriptionTimes}x")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Crucial Dates mini strip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CrucialDateMiniColumn("Open", ipo.openDate.takeLast(5))
                    CrucialDateMiniColumn("Close", ipo.closeDate.takeLast(5))
                    CrucialDateMiniColumn("Allot", ipo.allotmentDate.takeLast(5))
                    CrucialDateMiniColumn("List", ipo.listingDate.takeLast(5))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text(
                    text = "⚡ 1-Click All Demats / Record Bid",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF042F2E)
                )
            }
        }
    }
}

@Composable
fun ActivityTimelineRow(event: StatusEventEntity) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${event.sourceLabel} • ${event.formattedTimestamp}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun ReminderToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun DashboardApplicationCard(
    item: ApplicationWithDetails,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("app_card_${item.application.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.ipo.companyName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.familyMember.fullName} (${item.familyMember.maskedPan}) • ${item.dematAccount.accountAlias}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MandateStatusBadge(status = item.application.mandateStatus)
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UPI: ${item.application.upiVpa}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${formatInr(item.application.totalAmount)} (${item.application.lotCount} Lot)",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = TealLight
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App No: ${item.application.applicationNumber} • Manual Tracker",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AllotmentStatusBadge(status = item.application.allotmentStatus)
            }
        }
    }
}

@Composable
fun SmartAllotmentOptimizerCard(
    openIpos: List<IpoEntity>,
    activeDematCount: Int,
    onApplyAllFamily: (IpoEntity) -> Unit
) {
    var selectedIndex by remember { mutableStateOf(0) }
    val safeIndex = selectedIndex.coerceIn(0, (openIpos.size - 1).coerceAtLeast(0))
    val ipo = openIpos[safeIndex]

    val retailSub = ipo.retailSubscription.coerceAtLeast(0.1)
    val singleAccountWinProb = if (retailSub <= 1.0) 1.0 else (1.0 / retailSub)
    val singlePct = kotlin.math.round(singleAccountWinProb * 1000.0) / 10.0

    val familyCombinedProb = if (retailSub <= 1.0) {
        1.0
    } else {
        1.0 - Math.pow(1.0 - singleAccountWinProb, activeDematCount.toDouble())
    }
    val familyPct = kotlin.math.round(familyCombinedProb * 1000.0) / 10.0
    val expectedFamilyGain = kotlin.math.round(ipo.estimatedProfitPerLot * activeDematCount * singleAccountWinProb)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = "Smart Allotment Probability & Capital Optimizer")
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = TealPrimary.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(18.dp)
                )
                .testTag("smart_allotment_optimizer_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SEBI Retail Lottery Probability Engine",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Based on live Retail Subscription (${ipo.retailSubscription}x) & $activeDematCount Family Demats",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldSuccess.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "${activeDematCount}x Strategy",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldSuccess,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // IPO Selector Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(openIpos) { item ->
                        val isSelected = openIpos.indexOf(item) == safeIndex
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val foundIdx = openIpos.indexOf(item)
                                if (foundIdx >= 0) selectedIndex = foundIdx
                            },
                            label = {
                                Text(
                                    text = "${item.symbol} (${item.retailSubscription}x RII)",
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }

                // Probability Comparison Strip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "1 Demat Odds",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$singlePct%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$activeDematCount Family Demats Odds",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$familyPct%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = EmeraldSuccess
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Expected Family Gain",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatInr(expectedFamilyGain),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = TealPrimary
                            )
                        }
                    }
                }

                Text(
                    text = if (retailSub > 1.0) {
                        "💡 Capital Tip: ${ipo.symbol} is oversubscribed (${retailSub}x in Retail). Under SEBI rules, applying >1 lot in a single Demat account gives zero extra lottery weight. Apply 1 lot (${formatInr(ipo.retailMinAmount)}) across each of your $activeDematCount family Demat accounts instead!"
                    } else {
                        "💡 Capital Tip: ${ipo.symbol} Retail quota is currently undersubscribed (${retailSub}x). All valid Retail bids have ~100% allotment probability."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { onApplyAllFamily(ipo) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("optimizer_apply_family_btn_${ipo.symbol}")
                ) {
                    Text(
                        text = "⚡ Apply 1 Lot Across All $activeDematCount Family Demats (${ipo.symbol})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

