package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FactCheck
import androidx.annotation.StringRes
import com.example.data.model.CalendarEvent
import com.example.data.model.IpoBoardFilter
import com.example.data.model.IpoLifecycleStage
import com.example.data.model.IpoDates
import com.example.data.model.KeyDateKind
import com.example.data.model.groupByLifecycleStage
import com.example.data.model.lifecycleStage
import com.example.data.model.nextKeyDate
import com.example.data.model.stageFromStatusCode
import com.example.data.model.subscriptionTimes
import com.example.util.formatInr
import com.example.util.formatSignedPercent
import java.time.LocalDate
import com.example.data.model.IpoStatusFilter
import com.example.data.model.LiveIpoItem
import com.example.data.model.PanProfile
import com.example.data.model.maskPanNumber
import com.example.ui.viewmodel.IpoSortOption
import com.example.ui.viewmodel.UiState

@Composable
fun MainDashboardScreen(
    allIposState: UiState<List<LiveIpoItem>>,
    filteredIposState: UiState<List<LiveIpoItem>>,
    statusFilter: IpoStatusFilter,
    boardFilter: IpoBoardFilter,
    sortOption: IpoSortOption,
    searchQuery: String,
    onlyWatchlist: Boolean,
    watchedIds: Set<String>,
    onStatusFilterChange: (IpoStatusFilter) -> Unit,
    onBoardFilterChange: (IpoBoardFilter) -> Unit,
    onSortOptionChange: (IpoSortOption) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleOnlyWatchlist: () -> Unit,
    onToggleWatchItem: (LiveIpoItem, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onSelectIpo: (LiveIpoItem) -> Unit,
    panProfiles: List<PanProfile> = emptyList(),
    onOpenAddPanModal: (() -> Unit)? = null,
    onCheckAllotmentForIpo: ((LiveIpoItem) -> Unit)? = null,
    hidePanNumbers: Boolean = false,
    todayEvents: List<CalendarEvent> = emptyList(),
    onViewCalendar: (() -> Unit)? = null,
    onOpenIpoById: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val allList = (allIposState as? UiState.Success)?.data ?: emptyList()
    val stageCounts = remember(allList, today) {
        allList.groupingBy { it.lifecycleStage(today) }.eachCount()
    }
    val openCount = stageCounts[IpoLifecycleStage.OPEN] ?: 0
    val upcomingCount = stageCounts[IpoLifecycleStage.UPCOMING] ?: 0
    val closedCount = stageCounts[IpoLifecycleStage.CLOSED] ?: 0
    var collapsedStages by rememberSaveable {
        mutableStateOf(setOf(IpoLifecycleStage.LISTED.name, IpoLifecycleStage.CANCELLED.name))
    }
    val topGmpItem = allList.maxByOrNull { it.gmpPercent }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ipo_dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (todayEvents.isNotEmpty()) {
            item(key = "today_events") {
                TodayEventsStrip(
                    events = todayEvents,
                    onViewCalendar = onViewCalendar,
                    onOpenIpo = onOpenIpoById
                )
            }
        }

        // Live InvestorGain Header Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_ipo_hero_banner),
                        contentDescription = "Live IPO Market Dashboard",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x55060D1D), Color(0xEE060D1D))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        RoundedCornerShape(50)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "LIVE MARKET FEED",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onRefresh,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x44FFFFFF))
                                        .testTag("refresh_live_ipos_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh Live IPO Data",
                                        tint = Color.White
                                    )
                                }
                            }

                            Column {
                                if (topGmpItem != null) {
                                    Text(
                                        text = "Top GMP Today: ${topGmpItem.name} (+₹${formatDecimal(topGmpItem.gmpAmount)} / ${String.format("%.1f", topGmpItem.gmpPercent)}%)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PulseStatChip("Open Now", openCount.toString(), Color(0xFF00E676), Modifier.weight(1f))
                                    PulseStatChip("Upcoming", upcomingCount.toString(), Color(0xFFFFD54F), Modifier.weight(1f))
                                    PulseStatChip("Allotment", closedCount.toString(), Color(0xFF38BDF8), Modifier.weight(1f))
                                    PulseStatChip("Total Tracked", allList.size.toString(), Color.White, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Post-Login Saved PAN Cards & 1-Click Auto Allotment Quick Bar
        if (onOpenAddPanModal != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_pan_quick_bar"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (panProfiles.isEmpty()) {
                                        "Add PAN Cards for Auto-Allotment"
                                    } else {
                                        "${panProfiles.size} PAN Card(s) Saved for Auto-Check"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (panProfiles.isEmpty()) {
                                        "Save family PANs once to check allotment status in 1 click"
                                    } else {
                                        panProfiles.joinToString(" • ") {
                                            "${it.holderName} (${maskPanNumber(it.panNumber, hidePanNumbers)})"
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onOpenAddPanModal,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("dashboard_add_pan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add PAN",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Prominent Developer & Academic Institution Attribution Card
        item {
            com.example.ui.components.CopyrightAttributionCard()
        }

        // Search + Sort + Watchlist Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search IPO or sector...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ipo_search_input")
                )

                IconButton(
                    onClick = onToggleOnlyWatchlist,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (onlyWatchlist) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .testTag("filter_watchlist_button")
                ) {
                    Icon(
                        imageVector = if (onlyWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Filter Watchlist",
                        tint = if (onlyWatchlist) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("sort_ipos_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort IPOs",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        IpoSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSortOptionChange(option)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Status & Board Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IpoStatusFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = statusFilter == filter,
                            onClick = { onStatusFilterChange(filter) },
                            label = { Text(filter.label) },
                            modifier = Modifier.testTag("status_filter_${filter.name.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IpoBoardFilter.entries.forEach { board ->
                        FilterChip(
                            selected = boardFilter == board,
                            onClick = { onBoardFilterChange(board) },
                            label = { Text(board.label) },
                            modifier = Modifier.testTag("board_filter_${board.name.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }
            }
        }

        // Content State
        when (filteredIposState) {
            is UiState.Loading -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Syncing Live Mainboard & SME IPOs...",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Fetching real-time Grey Market Premiums & live subscription data",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            is UiState.Error -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Live Feed Connection Issue",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = filteredIposState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRefresh,
                                modifier = Modifier.testTag("retry_live_ipos_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry Sync")
                            }
                        }
                    }
                }
            }

            is UiState.Success -> {
                val ipos = filteredIposState.data
                if (ipos.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No IPOs match your active filters",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Try switching status tabs or clearing the search box.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    val sections = groupByLifecycleStage(ipos, today)
                    sections.forEach { (stage, stageIpos) ->
                        val expanded = stage.name !in collapsedStages
                        item(key = "section_${stage.name}") {
                            StageSectionHeader(
                                stage = stage,
                                count = stageIpos.size,
                                expanded = expanded,
                                onToggle = {
                                    collapsedStages = if (expanded) {
                                        collapsedStages + stage.name
                                    } else {
                                        collapsedStages - stage.name
                                    }
                                }
                            )
                        }
                        if (expanded) {
                            items(stageIpos, key = { it.id }) { ipo ->
                                val watched = watchedIds.contains(ipo.id)
                                LiveIpoCard(
                                    ipo = ipo,
                                    stage = stage,
                                    today = today,
                                    isWatched = watched,
                                    panCount = panProfiles.size,
                                    onToggleWatch = { onToggleWatchItem(ipo, watched) },
                                    onClick = { onSelectIpo(ipo) },
                                    onCheckAllotment = if (onCheckAllotmentForIpo != null) {
                                        { onCheckAllotmentForIpo(ipo) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }

        // Copyright & Developer Attribution Footer
        item {
            com.example.ui.components.CopyrightAttributionCard(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun PulseStatChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0x990D1B36),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFCBD5E1),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LiveIpoCard(
    ipo: LiveIpoItem,
    stage: IpoLifecycleStage,
    today: LocalDate,
    isWatched: Boolean,
    panCount: Int = 0,
    onToggleWatch: () -> Unit,
    onClick: () -> Unit,
    onCheckAllotment: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("ipo_card_${ipo.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Badges + Fire Rating + Watchlist
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // The raw feed status can lag the dates (e.g. still "C" after allotment);
                    // fall back to the stage name whenever the two disagree.
                    StatusBadge(
                        statusCode = stageBadgeCode(stage, ipo.statusCode),
                        statusLabel = if (stageFromStatusCode(ipo.statusCode) == stage) ipo.statusLabel
                        else stringResource(stageTitleRes(stage))
                    )
                    Surface(
                        color = if (ipo.category == "Mainboard") MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = ipo.category,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (ipo.category == "Mainboard") MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (stage == IpoLifecycleStage.UPCOMING && ipo.preApplyOpen == true) {
                        Surface(
                            color = Color(0xFF0C4A6E),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.chip_pre_apply_open),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF7DD3FC),
                                modifier = Modifier
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                    .testTag("pre_apply_chip_${ipo.id}")
                            )
                        }
                    }
                    if (ipo.hasAnchor) {
                        Surface(
                            color = Color(0xFF064E3B),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Anchor ✓",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF6EE7B7),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(ipo.fireRating.coerceIn(0, 5)) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleWatch,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isWatched) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark IPO",
                            tint = if (isWatched) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Company Name + Live GMP Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ipo.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${ipo.sector} • ${ipo.listingExchange}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // GMP Highlight Box
                Surface(
                    color = if (ipo.gmpAmount > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(
                        1.dp,
                        if (ipo.gmpAmount > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "GMP: +₹${formatDecimal(ipo.gmpAmount)} (${String.format("%.1f", ipo.gmpPercent)}%)",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (ipo.gmpAmount > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (ipo.estProfitPerLotInr > 0) "Est. Gain/Lot: +₹${ipo.estProfitPerLotInr}"
                            else "Est. Listing: ₹${formatDecimal(ipo.estimatedListingPrice)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            IpoKeyLine(ipo = ipo, stage = stage, today = today)

            if (stage == IpoLifecycleStage.LISTED) {
                ListedPerformanceLine(ipo = ipo)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics Strip: Price, Lot, Subscription, Size, Dates
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MiniMetric("Price", "₹${ipo.issuePrice}")
                    MiniMetric("Lot Size", "${ipo.lotSize} sh")
                    MiniMetric("Live Sub", ipo.totalSubscription, highlight = ipo.totalSubscription != "-")
                    MiniMetric("Issue Size", ipo.issueSizeCr)
                    MiniMetric("Open - Close", "${ipo.openDate} - ${ipo.closeDate}")
                }
            }

            // Show live QIB / NII / RII strip if subscription is active
            if (ipo.qibSubscription != "-" || ipo.riiSubscription != "-" || ipo.niiSubscription != "-") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QIB: ${ipo.qibSubscription}  •  NII: ${ipo.niiSubscription}  •  Retail: ${ipo.riiSubscription}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "BoA: ${ipo.boaDate} • List: ${ipo.listingDate}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onCheckAllotment != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFF064E3B).copy(alpha = 0.55f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCheckAllotment() }
                        .testTag("card_check_allotment_${ipo.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (panCount > 0) {
                                "Click to Check Allotment Status (Auto-Check All $panCount Saved PANs)"
                            } else {
                                "Click to Check Allotment Status (Auto-Check Saved PANs)"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF6EE7B7),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMetric(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private val GainGreen = Color(0xFF34D399)
private val LossRed = Color(0xFFF87171)

@StringRes
private fun stageTitleRes(stage: IpoLifecycleStage): Int = when (stage) {
    IpoLifecycleStage.UPCOMING -> R.string.stage_upcoming
    IpoLifecycleStage.OPEN -> R.string.stage_open
    IpoLifecycleStage.CLOSED -> R.string.stage_closed
    IpoLifecycleStage.ALLOTTED -> R.string.stage_allotted
    IpoLifecycleStage.LISTED -> R.string.stage_listed
    IpoLifecycleStage.CANCELLED -> R.string.stage_cancelled
}

private fun stageBadgeCode(stage: IpoLifecycleStage, statusCode: String): String = when (stage) {
    IpoLifecycleStage.UPCOMING -> "U"
    IpoLifecycleStage.OPEN -> if (statusCode == "CT") "CT" else "O"
    IpoLifecycleStage.CLOSED -> "C"
    IpoLifecycleStage.ALLOTTED -> "A"
    IpoLifecycleStage.LISTED -> if (statusCode.startsWith("L")) statusCode else "LN"
    IpoLifecycleStage.CANCELLED -> "X"
}

private fun stageAccent(stage: IpoLifecycleStage): Color = when (stage) {
    IpoLifecycleStage.UPCOMING -> Color(0xFFFBBF24)
    IpoLifecycleStage.OPEN -> Color(0xFF34D399)
    IpoLifecycleStage.CLOSED -> Color(0xFF60A5FA)
    IpoLifecycleStage.ALLOTTED -> Color(0xFFC084FC)
    IpoLifecycleStage.LISTED -> Color(0xFF6EE7B7)
    IpoLifecycleStage.CANCELLED -> Color(0xFFF87171)
}

@Composable
private fun StageSectionHeader(
    stage: IpoLifecycleStage,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val title = stringResource(stageTitleRes(stage))
    val accent = stageAccent(stage)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("stage_section_${stage.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = accent.copy(alpha = 0.18f),
            shape = RoundedCornerShape(50),
            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.45f))
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = accent,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = stringResource(
                if (expanded) R.string.section_collapse else R.string.section_expand,
                title
            ),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Price band · GMP · next key date · subscription, separated by middle dots. */
@Composable
private fun IpoKeyLine(
    ipo: LiveIpoItem,
    stage: IpoLifecycleStage,
    today: LocalDate
) {
    val keyDate = ipo.nextKeyDate(stage, today)
    val keyDateText = keyDate?.let {
        val human = IpoDates.formatHuman(it.date, today)
        when (it.kind) {
            KeyDateKind.OPENS -> stringResource(R.string.key_date_opens, human)
            KeyDateKind.CLOSES -> stringResource(R.string.key_date_closes, human)
            KeyDateKind.ALLOTMENT -> stringResource(R.string.key_date_allotment, human)
            KeyDateKind.LISTING -> stringResource(R.string.key_date_listing, human)
            KeyDateKind.LISTED -> stringResource(R.string.key_date_listed, human)
        }
    }
    val showGmp = stage != IpoLifecycleStage.LISTED && stage != IpoLifecycleStage.CANCELLED
    val gmpText = if (showGmp) {
        stringResource(
            R.string.card_gmp,
            formatInr(ipo.gmpAmount, showPlus = true),
            formatSignedPercent(ipo.gmpPercent)
        )
    } else null
    val subscriptionText = ipo.subscriptionTimes()?.let {
        stringResource(R.string.card_subscribed, String.format(java.util.Locale.ENGLISH, "%.2f", it))
    }
    val priceText = when {
        ipo.issuePrice <= 0 -> null
        ipo.priceBandLow != null && ipo.priceBandLow in 1 until ipo.issuePrice ->
            "${formatInr(ipo.priceBandLow)}–${formatInr(ipo.issuePrice)}"
        else -> formatInr(ipo.issuePrice)
    }

    val gmpColor = when {
        ipo.gmpAmount > 0 -> GainGreen
        ipo.gmpAmount < 0 -> LossRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val separator = "  ·  "
    val parts = listOfNotNull(priceText, gmpText, keyDateText, subscriptionText)
    if (parts.isEmpty()) return

    Text(
        text = buildAnnotatedString {
            parts.forEachIndexed { index, part ->
                if (index > 0) append(separator)
                when (part) {
                    gmpText -> withStyle(SpanStyle(color = gmpColor, fontWeight = FontWeight.Bold)) { append(part) }
                    priceText -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) }
                    else -> append(part)
                }
            }
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("ipo_key_line_${ipo.id}")
    )
}

/** "Issued ₹A → Listed ₹B (+x%) → Now ₹C (+y%)"; hidden until a listing price is known. */
@Composable
private fun ListedPerformanceLine(ipo: LiveIpoItem) {
    val listingPrice = ipo.listingPrice ?: return
    if (ipo.issuePrice <= 0) return
    val issue = ipo.issuePrice.toDouble()
    val listingGain = (listingPrice - issue) / issue * 100.0
    val currentPrice = ipo.currentPrice
    val currentGain = currentPrice?.let { (it - issue) / issue * 100.0 }

    val issuedText = stringResource(R.string.listed_issued, formatInr(ipo.issuePrice))
    val listedText = stringResource(
        R.string.listed_listed,
        formatInr(listingPrice),
        formatSignedPercent(listingGain)
    )
    val nowText = if (currentPrice != null && currentGain != null) {
        stringResource(R.string.listed_now, formatInr(currentPrice), formatSignedPercent(currentGain))
    } else null
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant
    fun gainColor(pct: Double) = when {
        pct > 0 -> GainGreen
        pct < 0 -> LossRed
        else -> neutral
    }

    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = neutral)) { append(issuedText) }
            append("  →  ")
            withStyle(SpanStyle(color = gainColor(listingGain), fontWeight = FontWeight.Bold)) { append(listedText) }
            if (nowText != null && currentGain != null) {
                append("  →  ")
                withStyle(SpanStyle(color = gainColor(currentGain), fontWeight = FontWeight.Bold)) { append(nowText) }
            }
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("listed_performance_${ipo.id}")
    )
}

/** Horizontally scrolling chips for today's confirmed IPO milestones. */
@Composable
private fun TodayEventsStrip(
    events: List<CalendarEvent>,
    onViewCalendar: (() -> Unit)?,
    onOpenIpo: ((String) -> Unit)?
) {
    Column(modifier = Modifier.testTag("today_events_strip")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.today_events_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (onViewCalendar != null) {
                TextButton(
                    onClick = onViewCalendar,
                    modifier = Modifier.testTag("today_events_view_calendar")
                ) {
                    Text(stringResource(R.string.today_events_view_calendar))
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            events.forEach { event ->
                val color = calendarEventColor(event.event)
                Surface(
                    color = color.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .then(
                            if (onOpenIpo != null) Modifier.clickable { onOpenIpo(event.ipoId) } else Modifier
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(
                                R.string.today_event_chip,
                                event.ipoName,
                                stringResource(calendarEventLabelRes(event.event))
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
