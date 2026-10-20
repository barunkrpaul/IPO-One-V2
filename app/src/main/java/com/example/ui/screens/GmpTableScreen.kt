package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
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
import com.example.data.model.allotmentLocalDate
import com.example.data.model.closeLocalDate
import com.example.data.model.formatIssueSize
import com.example.data.model.listingLocalDate
import com.example.data.model.simpleTableRows
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
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            IconButton(onClick = onRefresh) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.gmp_table_refresh),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onShowDetailedView, modifier = Modifier.testTag("gmp_table_detailed_toggle")) {
                Icon(
                    Icons.Filled.ViewAgenda,
                    contentDescription = stringResource(R.string.gmp_table_detailed_view),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when (allIposState) {
            is UiState.Loading -> CenterBox { CircularProgressIndicator() }
            is UiState.Error -> CenterBox {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.gmp_table_error),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onRefresh) { Text(stringResource(R.string.gmp_table_retry)) }
                }
            }
            is UiState.Success -> {
                val rows = remember(allIposState.data, boardFilter) {
                    simpleTableRows(allIposState.data, boardFilter, today)
                }
                if (rows.isEmpty()) {
                    CenterBox {
                        Text(
                            stringResource(R.string.gmp_table_empty),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val sections = remember(rows) { rows.groupBy({ it.second }, { it.first }) }
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sections.forEach { (stage, ipos) ->
                            item(key = "header_${stage.name}") { SectionHeader(stage, ipos.size) }
                            items(ipos, key = { it.id }) { ipo ->
                                GmpTableRow(
                                    ipo = ipo,
                                    stage = stage,
                                    today = today,
                                    showBoard = boardFilter == IpoBoardFilter.ALL,
                                    onClick = { onSelectIpo(ipo) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun stageColor(stage: IpoLifecycleStage): Color = when (stage) {
    IpoLifecycleStage.OPEN -> MaterialTheme.ipo.gain
    IpoLifecycleStage.UPCOMING -> MaterialTheme.ipo.info
    IpoLifecycleStage.CLOSED -> MaterialTheme.ipo.warning
    else -> MaterialTheme.ipo.accent
}

@Composable
private fun SectionHeader(stage: IpoLifecycleStage, count: Int) {
    val label = when (stage) {
        IpoLifecycleStage.OPEN -> stringResource(R.string.gmp_table_stage_open)
        IpoLifecycleStage.UPCOMING -> stringResource(R.string.gmp_table_stage_upcoming)
        IpoLifecycleStage.CLOSED -> stringResource(R.string.gmp_table_section_closed)
        else -> stringResource(R.string.gmp_table_section_allotted)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(stageColor(stage)))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun GmpTableRow(
    ipo: LiveIpoItem,
    stage: IpoLifecycleStage,
    today: LocalDate,
    showBoard: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.ipo
    val hasGmp = ipo.gmpAmount != 0.0
    val gmpColor = when {
        ipo.gmpAmount > 0 -> colors.gain
        ipo.gmpAmount < 0 -> colors.loss
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    fun date(d: LocalDate?) = d?.let { IpoDates.formatHuman(it, today) } ?: "-"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("gmp_table_row_${ipo.id}")
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = ipo.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (showBoard) {
                        Text(
                            text = if (ipo.category.equals("SME", ignoreCase = true)) stringResource(R.string.gmp_table_board_sme)
                            else stringResource(R.string.detail_mainboard),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (hasGmp) formatInr(ipo.gmpAmount, 0, showPlus = true) else "-",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = gmpColor
                    )
                    Text(
                        text = if (hasGmp) stringResource(R.string.gmp_table_gmp_percent, formatSignedPercent(ipo.gmpPercent))
                        else stringResource(R.string.metric_gmp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasGmp) gmpColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Cell(
                    stringResource(R.string.gmp_table_apply_by), date(ipo.closeLocalDate(today)), Modifier.weight(1f),
                    highlight = if (stage == IpoLifecycleStage.OPEN) colors.warning else null
                )
                Cell(stringResource(R.string.gmp_table_size), formatIssueSize(ipo.issueSizeCr), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Cell(stringResource(R.string.gmp_table_allotment), date(ipo.allotmentLocalDate(today)), Modifier.weight(1f))
                Cell(stringResource(R.string.gmp_table_listing), date(ipo.listingLocalDate(today)), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Cell(label: String, value: String, modifier: Modifier, highlight: Color? = null) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = highlight ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
