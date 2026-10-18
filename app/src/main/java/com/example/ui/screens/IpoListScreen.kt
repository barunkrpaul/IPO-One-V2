package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.IpoEntity
import com.example.model.IpoSeries
import com.example.model.IpoStatus
import com.example.ui.components.formatInr
import com.example.ui.theme.AmberPending
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.IpoViewModel

@Composable
fun IpoListScreen(
    ipoViewModel: IpoViewModel,
    onIpoClick: (IpoEntity) -> Unit,
    onApplyClick: (IpoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filterState by ipoViewModel.filterState.collectAsStateWithLifecycle()
    val filteredIpos by ipoViewModel.filteredIpos.collectAsStateWithLifecycle()
    val gmpSyncMessage by ipoViewModel.gmpSyncMessage.collectAsStateWithLifecycle()
    var showAddIpoDialog by remember { mutableStateOf(false) }

    gmpSyncMessage?.let { msg ->
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        ipoViewModel.clearGmpSyncMessage()
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("ipo_list_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search & Filter header
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "IPO Radar & Live GMP",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Real-time GMP tracker, crucial T+3 dates & profit calculator",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { ipoViewModel.deployLiveIpoFeed() },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("sync_all_gmp_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Deploy Live Feed",
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
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = filterState.searchQuery,
                        onValueChange = { ipoViewModel.setSearchQuery(it) },
                        placeholder = { Text("Search by company or symbol...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (filterState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { ipoViewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ipo_search_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Series Filter Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = filterState.selectedSeries == "ALL",
                                onClick = { ipoViewModel.setSelectedSeries("ALL") },
                                label = { Text("All Markets") },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterState.selectedSeries == IpoSeries.MAINBOARD.displayName,
                                onClick = { ipoViewModel.setSelectedSeries(IpoSeries.MAINBOARD.displayName) },
                                label = { Text("Mainboard") },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterState.selectedSeries == IpoSeries.SME.displayName,
                                onClick = { ipoViewModel.setSelectedSeries(IpoSeries.SME.displayName) },
                                label = { Text("SME Platform") },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Status Filter Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = filterState.selectedStatus == "ALL",
                                onClick = { ipoViewModel.setSelectedStatus("ALL") },
                                label = { Text("All Statuses") },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterState.selectedStatus == IpoStatus.OPEN.displayName,
                                onClick = { ipoViewModel.setSelectedStatus(IpoStatus.OPEN.displayName) },
                                label = { Text("Open Now") },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterState.selectedStatus == IpoStatus.UPCOMING.displayName,
                                onClick = { ipoViewModel.setSelectedStatus(IpoStatus.UPCOMING.displayName) },
                                label = { Text("Upcoming") },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterState.selectedStatus == "CLOSED",
                                onClick = { ipoViewModel.setSelectedStatus("CLOSED") },
                                label = { Text("Closed / Allotted") },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // List of filtered IPOs
            if (filteredIpos.isNotEmpty()) {
                items(filteredIpos) { ipo ->
                    IpoCatalogCard(
                        ipo = ipo,
                        onClick = { onIpoClick(ipo) },
                        onApply = { onApplyClick(ipo) },
                        onToggleFavorite = { ipoViewModel.toggleFavorite(ipo) }
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
                                text = "No IPOs match your search or filter",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try clearing filters or switching between Mainboard and SME tabs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }

        // FAB to add new IPO with GMP and Crucial Dates
        FloatingActionButton(
            onClick = { showAddIpoDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_custom_ipo_fab"),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Custom IPO")
        }
    }

    if (showAddIpoDialog) {
        AddCustomIpoDialog(
            onDismiss = { showAddIpoDialog = false },
            onConfirm = { company, symbol, series, minP, maxP, lot, issueCr, openD, closeD, allotD, refundD, listD, reg, status, gmp, qibS, niiS, retailS, totalS ->
                ipoViewModel.addCustomIpo(
                    companyName = company,
                    symbol = symbol,
                    series = series,
                    minPrice = minP,
                    maxPrice = maxP,
                    lotSize = lot,
                    issueSizeCr = issueCr,
                    openDate = openD,
                    closeDate = closeD,
                    allotmentDate = allotD,
                    refundDate = refundD,
                    listingDate = listD,
                    registrarName = reg,
                    status = status,
                    gmpInr = gmp,
                    qibSub = qibS,
                    niiSub = niiS,
                    retailSub = retailS,
                    totalSub = totalS
                )
                showAddIpoDialog = false
            }
        )
    }
}

@Composable
fun IpoCatalogCard(
    ipo: IpoEntity,
    onClick: () -> Unit,
    onApply: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (ipo.status == "Open Now") {
                    EmeraldSuccess.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                },
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .testTag("ipo_catalog_card_${ipo.symbol.lowercase()}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (ipo.status) {
                                "Open Now" -> EmeraldSuccess.copy(alpha = 0.15f)
                                "Upcoming" -> CyanAccent.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ) {
                            Text(
                                text = ipo.status,
                                color = when (ipo.status) {
                                    "Open Now" -> EmeraldSuccess
                                    "Upcoming" -> CyanAccent
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = ipo.series,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CyanAccent.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "Size: ₹${ipo.issueSizeCr.toInt()} Cr",
                                color = CyanAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = ipo.companyName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${ipo.symbol} • Issue Size: ₹${ipo.issueSizeCr} Cr • ${ipo.registrarName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("fav_btn_${ipo.symbol.lowercase()}")
                ) {
                    Icon(
                        imageVector = if (ipo.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (ipo.isFavorite) AmberPending else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Price & Lots Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Price Band",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${ipo.minPrice.toInt()} - ₹${ipo.maxPrice.toInt()}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Lot Size: ${ipo.lotSize} shares",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Minimum Bid (1 Lot)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatInr(ipo.lotSize * ipo.maxPrice),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "Total Subscribed: ${ipo.totalSub}x",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (ipo.totalSub >= 1.0) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real-Time Category-Wise Subscription Breakdown Strip (QIB, NII/HNI, Retail, Total)
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(11.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REAL-TIME CATEGORY SUBSCRIPTION",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 9.5.sp
                        )
                        Text(
                            text = if (ipo.status == "Upcoming") "Opens ${ipo.openDate.takeLast(5)}" else "Live • ${ipo.formattedGmpLastUpdated}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SubscriptionCategoryMiniPill("QIB (Inst.)", "${ipo.qibSub}x", ipo.qibSub >= 1.0)
                        SubscriptionCategoryMiniPill("NII (HNI)", "${ipo.niiSub}x", ipo.niiSub >= 1.0)
                        SubscriptionCategoryMiniPill("Retail (RII)", "${ipo.retailSub}x", ipo.retailSub >= 1.0)
                        SubscriptionCategoryMiniPill("Total Sub", "${ipo.totalSub}x", ipo.totalSub >= 1.0, isHighlight = true)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live GMP, Estimated Listing Price, Profit per Lot & Last Updated Timestamp Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (ipo.gmpInr > 0) EmeraldSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (ipo.gmpInr > 0) EmeraldSuccess.copy(alpha = 0.30f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (ipo.gmpInr > 0) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "LIVE GMP TRACKER",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (ipo.gmpInr > 0) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Updated: ${ipo.formattedGmpLastUpdated}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "GMP / Share",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Text(
                                text = if (ipo.gmpInr > 0) "+₹${ipo.gmpInr.toInt()} (+${ipo.gmpPercent}%)" else "₹0 (Neutral)",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (ipo.gmpInr > 0) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Est. Listing",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "₹${ipo.estimatedListingPrice.toInt()}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Est. Profit / Lot",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "+${formatInr(ipo.estimatedProfitPerLot)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (ipo.gmpInr > 0) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5-Stage Crucial Dates Schedule Strip
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CRUCIAL T+3 IPO DATES",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 9.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CrucialDateMiniColumn(label = "Open", date = ipo.openDate.takeLast(5))
                        CrucialDateMiniColumn(label = "Close", date = ipo.closeDate.takeLast(5))
                        CrucialDateMiniColumn(label = "Allotment", date = ipo.allotmentDate.takeLast(5))
                        CrucialDateMiniColumn(label = "Refund", date = ipo.refundDate.takeLast(5))
                        CrucialDateMiniColumn(label = "Listing", date = ipo.listingDate.takeLast(5))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap card for full schedule, subscription & GMP editor",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Button(
                    onClick = onApply,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("apply_btn_${ipo.symbol.lowercase()}")
                ) {
                    Text("Record Bid", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SubscriptionCategoryMiniPill(
    category: String,
    multiplier: String,
    isOversubscribed: Boolean,
    isHighlight: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp
        )
        Text(
            text = multiplier,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = when {
                isHighlight && isOversubscribed -> EmeraldSuccess
                isOversubscribed -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontSize = 11.5.sp
        )
    }
}

@Composable
fun CrucialDateMiniColumn(label: String, date: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.5.sp
        )
        Text(
            text = date,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.5.sp
        )
    }
}

@Composable
fun AddCustomIpoDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        company: String,
        symbol: String,
        series: String,
        minPrice: Double,
        maxPrice: Double,
        lotSize: Int,
        issueSizeCr: Double,
        openDate: String,
        closeDate: String,
        allotmentDate: String,
        refundDate: String,
        listingDate: String,
        registrarName: String,
        status: String,
        gmpInr: Double,
        qibSub: Double,
        niiSub: Double,
        retailSub: Double,
        totalSub: Double
    ) -> Unit
) {
    var companyName by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var series by remember { mutableStateOf(IpoSeries.MAINBOARD.displayName) }
    var maxPriceText by remember { mutableStateOf("500") }
    var lotSizeText by remember { mutableStateOf("30") }
    var issueSizeCrText by remember { mutableStateOf("1850") }
    var gmpText by remember { mutableStateOf("120") }
    var qibSubText by remember { mutableStateOf("12.4") }
    var niiSubText by remember { mutableStateOf("8.5") }
    var retailSubText by remember { mutableStateOf("4.2") }
    var totalSubText by remember { mutableStateOf("7.8") }
    var status by remember { mutableStateOf(IpoStatus.UPCOMING.displayName) }
    var openDate by remember { mutableStateOf("2026-10-25") }
    var closeDate by remember { mutableStateOf("2026-10-28") }
    var allotmentDate by remember { mutableStateOf("2026-10-29") }
    var refundDate by remember { mutableStateOf("2026-10-30") }
    var listingDate by remember { mutableStateOf("2026-10-31") }
    var registrarName by remember { mutableStateOf("KFin Technologies") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Current / Upcoming IPO",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name (e.g. Afcons Infrastructure)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it.uppercase() },
                        label = { Text("Symbol (e.g. AFCONS)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = issueSizeCrText,
                        onValueChange = { issueSizeCrText = it },
                        label = { Text("Issue Size (₹ Cr)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = maxPriceText,
                        onValueChange = { maxPriceText = it },
                        label = { Text("Issue Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = lotSizeText,
                        onValueChange = { lotSizeText = it },
                        label = { Text("Lot Size (Shares)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = gmpText,
                        onValueChange = { gmpText = it },
                        label = { Text("Live GMP ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Real-Time Category Subscription (Times Subscribed)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = qibSubText,
                        onValueChange = { qibSubText = it },
                        label = { Text("QIB (x)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = niiSubText,
                        onValueChange = { niiSubText = it },
                        label = { Text("NII (x)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = retailSubText,
                        onValueChange = { retailSubText = it },
                        label = { Text("Retail (x)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = totalSubText,
                        onValueChange = { totalSubText = it },
                        label = { Text("Total (x)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Crucial T+3 Schedule Dates (YYYY-MM-DD)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = openDate,
                        onValueChange = { openDate = it },
                        label = { Text("Open Date") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = closeDate,
                        onValueChange = { closeDate = it },
                        label = { Text("Close Date") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = allotmentDate,
                        onValueChange = { allotmentDate = it },
                        label = { Text("Allotment (T+1)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = refundDate,
                        onValueChange = { refundDate = it },
                        label = { Text("Refund (T+2)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = listingDate,
                        onValueChange = { listingDate = it },
                        label = { Text("Listing (T+3)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = status == IpoStatus.OPEN.displayName,
                        onClick = { status = IpoStatus.OPEN.displayName },
                        label = { Text("Open Now") }
                    )
                    FilterChip(
                        selected = status == IpoStatus.UPCOMING.displayName,
                        onClick = { status = IpoStatus.UPCOMING.displayName },
                        label = { Text("Upcoming") }
                    )
                    FilterChip(
                        selected = series == IpoSeries.SME.displayName,
                        onClick = {
                            series = if (series == IpoSeries.SME.displayName) {
                                IpoSeries.MAINBOARD.displayName
                            } else {
                                IpoSeries.SME.displayName
                            }
                        },
                        label = { Text("SME Issue") }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val maxP = maxPriceText.toDoubleOrNull() ?: 500.0
                        val minP = (maxP * 0.95).toInt().toDouble()
                        val lot = lotSizeText.toIntOrNull() ?: 30
                        val issueCr = issueSizeCrText.toDoubleOrNull() ?: 1500.0
                        val gmp = gmpText.toDoubleOrNull() ?: 0.0
                        val qib = qibSubText.toDoubleOrNull() ?: 0.0
                        val nii = niiSubText.toDoubleOrNull() ?: 0.0
                        val retail = retailSubText.toDoubleOrNull() ?: 0.0
                        val total = totalSubText.toDoubleOrNull() ?: 0.0
                        onConfirm(
                            companyName.ifBlank { "New Issue Ltd" },
                            symbol.ifBlank { "NEWIPO" },
                            series,
                            minP,
                            maxP,
                            lot,
                            issueCr,
                            openDate,
                            closeDate,
                            allotmentDate,
                            refundDate,
                            listingDate,
                            registrarName,
                            status,
                            gmp,
                            qib,
                            nii,
                            retail,
                            total
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = companyName.isNotBlank()
                ) {
                    Text("Save IPO with Size, Live Sub & GMP")
                }
            }
        }
    }
}
