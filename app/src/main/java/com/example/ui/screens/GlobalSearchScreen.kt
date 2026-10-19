package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.IntermediaryDirectory
import com.example.data.model.IntermediaryEntry
import com.example.data.model.IpoApplication
import com.example.data.model.IpoExtras
import com.example.data.model.ListedIpoRecord
import com.example.data.model.LiveIpoItem
import com.example.data.model.globalSearch
import com.example.data.model.lifecycleStage
import com.example.util.formatSignedPercent
import java.time.LocalDate

/** Search across IPOs (every stage), listed history, intermediaries and the user's applications. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    live: List<LiveIpoItem>,
    history: List<ListedIpoRecord>,
    directory: IntermediaryDirectory,
    applications: List<IpoApplication>,
    extras: Map<String, IpoExtras>,
    onBack: () -> Unit,
    onOpenIpo: (LiveIpoItem) -> Unit,
    onOpenListed: (ListedIpoRecord) -> Unit,
    onOpenIntermediary: (IntermediaryEntry) -> Unit,
    onOpenApplication: (IpoApplication) -> Unit
) {
    BackHandler(onBack = onBack)
    var query by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    val results = remember(query, live, history, directory, applications, extras) {
        globalSearch(query, live, history, directory, applications, extras)
    }
    val today = remember { LocalDate.now() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("search_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
                    }
                },
                title = {
                    TextField(
                        value = query,
                        onValueChange = { query = it.take(60) },
                        placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1) },
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.allotment_search_clear))
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("global_search_input")
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("global_search_results"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                query.trim().length < 2 -> item { Hint(stringResource(R.string.search_min_chars)) }
                results.isEmpty -> item { Hint(stringResource(R.string.search_no_results, query.trim())) }
                else -> {
                    if (results.ipos.isNotEmpty()) {
                        item { GroupHeader(R.string.search_group_ipos, results.ipos.size) }
                        items(results.ipos, key = { "ipo_${it.id}" }) { ipo ->
                            ResultRow(
                                title = ipo.name,
                                subtitle = listOfNotNull(
                                    extras[ipo.id]?.symbol,
                                    ipo.category,
                                    stringResource(stageTitle(ipo.lifecycleStage(today)))
                                ).joinToString(" · "),
                                onClick = { onOpenIpo(ipo) }
                            )
                        }
                    }
                    if (results.listed.isNotEmpty()) {
                        item { GroupHeader(R.string.search_group_listed, results.listed.size) }
                        items(results.listed, key = { "listed_${it.id}" }) { record ->
                            ResultRow(
                                title = record.name,
                                subtitle = listOfNotNull(
                                    record.symbol.takeIf { it.isNotBlank() },
                                    record.category,
                                    record.listingGainPct?.let { formatSignedPercent(it) }
                                ).joinToString(" · "),
                                onClick = { onOpenListed(record) }
                            )
                        }
                    }
                    if (results.intermediaries.isNotEmpty()) {
                        item { GroupHeader(R.string.search_group_intermediaries, results.intermediaries.size) }
                        items(results.intermediaries, key = { "int_${it.role}_${it.key}" }) { entry ->
                            ResultRow(
                                title = entry.name,
                                subtitle = stringResource(
                                    R.string.intermediaries_counts,
                                    entry.ipos.size,
                                    entry.mainboardCount,
                                    entry.smeCount
                                ),
                                onClick = { onOpenIntermediary(entry) }
                            )
                        }
                    }
                    if (results.applications.isNotEmpty()) {
                        item { GroupHeader(R.string.search_group_applications, results.applications.size) }
                        items(results.applications, key = { "app_${it.id}" }) { app ->
                            ResultRow(
                                title = app.ipoName,
                                subtitle = "${app.panHolderName} · ${app.status}",
                                onClick = { onOpenApplication(app) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@StringRes
private fun stageTitle(stage: com.example.data.model.IpoLifecycleStage): Int = when (stage) {
    com.example.data.model.IpoLifecycleStage.UPCOMING -> R.string.stage_upcoming
    com.example.data.model.IpoLifecycleStage.OPEN -> R.string.stage_open
    com.example.data.model.IpoLifecycleStage.CLOSED -> R.string.stage_closed
    com.example.data.model.IpoLifecycleStage.ALLOTTED -> R.string.stage_allotted
    com.example.data.model.IpoLifecycleStage.LISTED -> R.string.stage_listed
    com.example.data.model.IpoLifecycleStage.CANCELLED -> R.string.stage_cancelled
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
private fun GroupHeader(@StringRes title: Int, count: Int) {
    Row(
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ResultRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
