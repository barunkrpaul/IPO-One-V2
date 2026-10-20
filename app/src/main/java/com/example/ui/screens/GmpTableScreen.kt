package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.IpoBoardFilter
import com.example.data.model.IpoDates
import com.example.data.model.IpoLifecycleStage
import com.example.data.model.LiveIpoItem
import com.example.data.model.formatIssueSize
import com.example.data.model.simpleTableRows
import com.example.data.model.allotmentLocalDate
import com.example.data.model.closeLocalDate
import com.example.data.model.lifecycleStage
import com.example.data.model.listingLocalDate
import com.example.ui.theme.ipo
import com.example.ui.viewmodel.UiState
import com.example.util.formatInr
import com.example.util.formatSignedPercent
import java.time.LocalDate

@Composable
fun GmpTableScreen(
    allIposState: UiState<List<LiveIpoItem>>,
    boardFilter: IpoBoardFilter,
    onBoardFilterChange: (IpoBoardFilter) -> Unit,
    onRefresh: () -> Unit,
    onSelectIpo: (LiveIpoItem) -> Unit,
    onShowDetailedView: () -> Unit
) {
    val today = remember { LocalDate.now() }

    Column(modifier = Modifier.fillMaxSize().testTag("gmp_table_screen")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.gmp_table_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.gmp_table_refresh))
            }
            IconButton(onClick = onShowDetailedView, modifier = Modifier.testTag("gmp_table_detailed_toggle")) {
                Icon(Icons.Filled.ViewAgenda, contentDescription = stringResource(R.string.gmp_table_detailed_view))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BoardChip(stringResource(R.string.gmp_table_board_all), boardFilter == IpoBoardFilter.ALL) {
                onBoardFilterChange(IpoBoardFilter.ALL)
            }
            BoardChip(stringResource(R.string.detail_mainboard), boardFilter == IpoBoardFilter.MAINBOARD) {
                onBoardFilterChange(IpoBoardFilter.MAINBOARD)
            }
            BoardChip(stringResource(R.string.gmp_table_board_sme), boardFilter == IpoBoardFilter.SME) {
                onBoardFilterChange(IpoBoardFilter.SME)
            }
        }

        when (allIposState) {
            is UiState.Loading -> CenterBox { CircularProgressIndicator() }
            is UiState.Error -> CenterBox {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.gmp_table_error), textAlign = TextAlign.Center)
                    TextButton(onClick = onRefresh) { Text(stringResource(R.string.gmp_table_retry)) }
                }
            }
            is UiState.Success -> {
                val rows = remember(allIposState.data, boardFilter) {
                    simpleTableRows(allIposState.data, boardFilter, today)
                }
                if (rows.isEmpty()) {
                    CenterBox { Text(stringResource(R.string.gmp_table_empty), textAlign = TextAlign.Center) }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(rows, key = { it.first.id }) { (ipo, stage) ->
                            GmpTableRow(ipo, stage, today, onClick = { onSelectIpo(ipo) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun GmpTableRow(ipo: LiveIpoItem, stage: IpoLifecycleStage, today: LocalDate, onClick: () -> Unit) {
    val colors = MaterialTheme.ipo
    val gmpColor = when {
        ipo.gmpAmount > 0 -> colors.gain
        ipo.gmpAmount < 0 -> colors.loss
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    fun date(d: LocalDate?) = d?.let { IpoDates.formatHuman(it, today) } ?: "-"

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("gmp_table_row_${ipo.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ipo.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    StagePill(stage, ipo.category)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (ipo.gmpAmount == 0.0) "-" else formatInr(ipo.gmpAmount, 0, showPlus = true),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = gmpColor
                    )
                    Text(
                        text = if (ipo.gmpAmount == 0.0) stringResource(R.string.metric_gmp)
                        else stringResource(R.string.gmp_table_gmp_percent, formatSignedPercent(ipo.gmpPercent)),
                        style = MaterialTheme.typography.labelSmall,
                        color = gmpColor
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Cell(stringResource(R.string.gmp_table_apply_by), date(ipo.closeLocalDate(today)), Modifier.weight(1f),
                    highlight = if (stage == IpoLifecycleStage.OPEN) colors.warning else null)
                Cell(stringResource(R.string.gmp_table_size), formatIssueSize(ipo.issueSizeCr), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Cell(stringResource(R.string.gmp_table_allotment), date(ipo.allotmentLocalDate(today)), Modifier.weight(1f))
                Cell(stringResource(R.string.gmp_table_listing), date(ipo.listingLocalDate(today)), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StagePill(stage: IpoLifecycleStage, category: String) {
    val colors = MaterialTheme.ipo
    val (label, color) = when (stage) {
        IpoLifecycleStage.OPEN -> stringResource(R.string.gmp_table_stage_open) to colors.gain
        IpoLifecycleStage.UPCOMING -> stringResource(R.string.gmp_table_stage_upcoming) to colors.info
        IpoLifecycleStage.CLOSED -> stringResource(R.string.gmp_table_stage_closed) to colors.warning
        else -> stringResource(R.string.gmp_table_stage_allotted) to colors.accent
    }
    val board = if (category.equals("SME", ignoreCase = true)) stringResource(R.string.gmp_table_board_sme)
    else stringResource(R.string.detail_mainboard)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = color)
        Text(" · $board", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Cell(label: String, value: String, modifier: Modifier, highlight: Color? = null) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = highlight ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
