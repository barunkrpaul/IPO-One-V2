package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.IntermediaryDirectory
import com.example.data.model.IntermediaryEntry
import com.example.data.model.IntermediaryRole
import com.example.data.model.IpoRef
import com.example.data.model.intermediaryKey
import com.example.ui.viewmodel.PageDetailsSyncState
import com.example.util.formatSignedPercent
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RefDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

@StringRes
private fun tabLabelRes(role: IntermediaryRole): Int = when (role) {
    IntermediaryRole.LEAD_MANAGER -> R.string.intermediaries_tab_lead_managers
    IntermediaryRole.REGISTRAR -> R.string.intermediaries_tab_registrars
    IntermediaryRole.MARKET_MAKER -> R.string.intermediaries_tab_market_makers
}

@StringRes
private fun roleLabelRes(role: IntermediaryRole): Int = when (role) {
    IntermediaryRole.LEAD_MANAGER -> R.string.intermediaries_role_lead_manager
    IntermediaryRole.REGISTRAR -> R.string.intermediaries_role_registrar
    IntermediaryRole.MARKET_MAKER -> R.string.intermediaries_role_market_maker
}

/**
 * Lead manager / registrar / market maker directories. [focus] opens one
 * intermediary directly (from a chip on the IPO detail page).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntermediariesScreen(
    directory: IntermediaryDirectory,
    sync: PageDetailsSyncState,
    focus: Pair<IntermediaryRole, String>?,
    onBack: () -> Unit,
    onSyncAll: () -> Unit,
    onCancelSync: () -> Unit,
    onOpenIpo: (IpoRef) -> Unit
) {
    val roles = IntermediaryRole.entries
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var focusMissingName by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(focus) {
        if (focus != null) {
            tabIndex = roles.indexOf(focus.first)
            selectedKey = intermediaryKey(focus.second)
            focusMissingName = focus.second
        }
    }

    val role = roles[tabIndex]
    val selected = selectedKey?.let { key -> directory.entries[role].orEmpty().firstOrNull { it.key == key } }

    BackHandler {
        if (selectedKey != null) selectedKey = null else onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { if (selectedKey != null) selectedKey = null else onBack() },
                        modifier = Modifier.testTag("intermediaries_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
                    }
                },
                title = {
                    Text(
                        text = selected?.name ?: stringResource(R.string.intermediaries_screen_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("intermediaries_screen")
        ) {
            when {
                selected != null -> IntermediaryDetail(entry = selected, onOpenIpo = onOpenIpo)

                selectedKey != null -> {
                    // Focused from a chip but not in the directory yet
                    MissingIntermediary(
                        name = focusMissingName.orEmpty(),
                        directory = directory,
                        sync = sync,
                        onSyncAll = onSyncAll,
                        onCancelSync = onCancelSync
                    )
                }

                else -> {
                    TabRow(selectedTabIndex = tabIndex) {
                        roles.forEachIndexed { index, r ->
                            Tab(
                                selected = tabIndex == index,
                                onClick = { tabIndex = index },
                                text = { Text(stringResource(tabLabelRes(r)), maxLines = 1) },
                                modifier = Modifier.testTag("intermediaries_tab_${r.name.lowercase()}")
                            )
                        }
                    }
                    DirectoryList(
                        entries = directory.entries[role].orEmpty(),
                        directory = directory,
                        sync = sync,
                        onSyncAll = onSyncAll,
                        onCancelSync = onCancelSync,
                        onSelect = { selectedKey = it.key }
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverageCard(
    directory: IntermediaryDirectory,
    sync: PageDetailsSyncState,
    onSyncAll: () -> Unit,
    onCancelSync: () -> Unit
) {
    val missing = directory.totalIpoCount - directory.coveredIpoCount
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("intermediaries_coverage"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.intermediaries_coverage, directory.coveredIpoCount, directory.totalIpoCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (sync.isRunning) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { if (sync.total > 0) sync.done.toFloat() / sync.total else 0f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.intermediaries_fetching, sync.done, sync.total),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onCancelSync, modifier = Modifier.testTag("intermediaries_cancel_sync")) {
                        Text(stringResource(R.string.intermediaries_cancel))
                    }
                }
            } else {
                if (sync.failed > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.intermediaries_fetch_failed, sync.failed),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (missing > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onSyncAll,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("intermediaries_sync_all"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.intermediaries_fetch, missing))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.intermediaries_fetch_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectoryList(
    entries: List<IntermediaryEntry>,
    directory: IntermediaryDirectory,
    sync: PageDetailsSyncState,
    onSyncAll: () -> Unit,
    onCancelSync: () -> Unit,
    onSelect: (IntermediaryEntry) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (directory.totalIpoCount == 0) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 10.dp))
                    Text(
                        text = stringResource(R.string.intermediaries_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@LazyColumn
        }
        item { CoverageCard(directory, sync, onSyncAll, onCancelSync) }
        if (entries.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.intermediaries_tab_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
        items(entries, key = { it.key }) { entry ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(entry) }
                    .testTag("intermediary_${entry.key}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(
                                R.string.intermediaries_counts,
                                entry.ipos.size,
                                entry.mainboardCount,
                                entry.smeCount
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        entry.averageListingGainPct?.let { avg ->
                            Text(
                                text = stringResource(R.string.intermediaries_avg_gain, formatSignedPercent(avg)),
                                style = MaterialTheme.typography.labelMedium,
                                color = gainColor(avg)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun gainColor(pct: Double): Color = when {
    pct > 0 -> Color(0xFF34D399)
    pct < 0 -> Color(0xFFF87171)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun IntermediaryDetail(entry: IntermediaryEntry, onOpenIpo: (IpoRef) -> Unit) {
    val context = LocalContext.current
    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("intermediary_detail"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(roleLabelRes(entry.role)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.intermediaries_counts, entry.ipos.size, entry.mainboardCount, entry.smeCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    entry.averageListingGainPct?.let { avg ->
                        Text(
                            text = stringResource(R.string.intermediaries_avg_gain, formatSignedPercent(avg)),
                            style = MaterialTheme.typography.labelLarge,
                            color = gainColor(avg)
                        )
                    }
                    entry.website?.let { website ->
                        Spacer(modifier = Modifier.height(12.dp))
                        if (entry.role == IntermediaryRole.REGISTRAR) {
                            Button(
                                onClick = { open(website) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("intermediary_check_allotment"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.FactCheck, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.intermediaries_check_allotment))
                            }
                        } else {
                            OutlinedButton(onClick = { open(website) }, shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.intermediaries_website))
                            }
                        }
                    }
                }
            }
        }
        items(entry.ipos, key = { it.id }) { ref ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenIpo(ref) }
                    .testTag("intermediary_ipo_${ref.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ref.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = listOfNotNull(ref.category, ref.listingDate?.format(RefDateFormat)).joinToString("  ·  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val gain = ref.listingGainPct
                    Text(
                        text = gain?.let { formatSignedPercent(it) } ?: stringResource(R.string.intermediaries_not_listed),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = gain?.let { gainColor(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MissingIntermediary(
    name: String,
    directory: IntermediaryDirectory,
    sync: PageDetailsSyncState,
    onSyncAll: () -> Unit,
    onCancelSync: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.intermediaries_not_found, name),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CoverageCard(directory, sync, onSyncAll, onCancelSync)
    }
}
