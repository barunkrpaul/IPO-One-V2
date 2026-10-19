package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BidCategory
import com.example.data.model.Broker
import com.example.data.model.IpoApplication
import com.example.data.model.IpoLifecycleStage
import com.example.data.model.LiveIpoItem
import com.example.data.model.PanProfile
import com.example.data.model.applicationSizes
import com.example.data.model.applyDetailsText
import com.example.data.model.lifecycleStage
import com.example.data.model.lotRange
import com.example.data.model.maskPanNumber
import com.example.data.model.maskUpiId
import com.example.ui.theme.ipo
import com.example.util.formatInr
import java.time.LocalDate

/**
 * Helps apply for an IPO with every saved PAN. IPO bids can only be placed by a
 * SEBI-registered broker or bank, so for each PAN this shows the bid, the
 * details to paste (PAN, demat BO ID, client ID, UPI) with copy buttons, opens
 * that PAN's broker, and logs the application once the user marks it done.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyAssistantSheet(
    ipo: LiveIpoItem,
    panProfiles: List<PanProfile>,
    applications: List<IpoApplication>,
    hidePanNumbers: Boolean,
    onSetBroker: (panId: String, broker: String, clientId: String) -> Unit,
    onMarkApplied: (pan: PanProfile, category: String, lots: Int) -> Unit,
    onAddPan: () -> Unit,
    onDismiss: () -> Unit
) {
    val sizes = remember(ipo) {
        applicationSizes(
            lotSize = ipo.lotSize,
            upperPrice = ipo.issuePrice,
            isSme = ipo.category.equals("SME", ignoreCase = true),
            includeEmployee = false,
            includeShareholder = false
        )
    }
    val stage = remember(ipo) { ipo.lifecycleStage(LocalDate.now()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .testTag("apply_assistant_sheet"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                Text(
                    text = stringResource(R.string.apply_title, ipo.name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        R.string.apply_summary,
                        formatInr(ipo.issuePrice),
                        ipo.lotSize
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.ipo.warningContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (stage) {
                            IpoLifecycleStage.OPEN -> stringResource(R.string.apply_note_open)
                            IpoLifecycleStage.UPCOMING -> stringResource(R.string.apply_note_upcoming)
                            else -> stringResource(R.string.apply_note_closed)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.ipo.onWarningContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                }
            }

            if (panProfiles.isEmpty()) {
                item {
                    Column {
                    Text(
                        text = stringResource(R.string.apply_no_pans),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onAddPan, modifier = Modifier.padding(top = 8.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.apply_add_pan))
                    }
                    }
                }
            }

            items(panProfiles, key = { it.id.ifBlank { it.panNumber } }) { pan ->
                val existing = applications.firstOrNull {
                    (it.ipoId == ipo.id || it.ipoName.equals(ipo.name, ignoreCase = true)) &&
                        it.panNumber.equals(pan.panNumber, ignoreCase = true)
                }
                PanApplyCard(
                    ipo = ipo,
                    pan = pan,
                    existing = existing,
                    sizes = sizes,
                    hidePanNumbers = hidePanNumbers,
                    canApply = stage == IpoLifecycleStage.OPEN || stage == IpoLifecycleStage.UPCOMING,
                    onSetBroker = onSetBroker,
                    onMarkApplied = onMarkApplied
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PanApplyCard(
    ipo: LiveIpoItem,
    pan: PanProfile,
    existing: IpoApplication?,
    sizes: List<com.example.data.model.ApplicationSizeRow>,
    hidePanNumbers: Boolean,
    canApply: Boolean,
    onSetBroker: (panId: String, broker: String, clientId: String) -> Unit,
    onMarkApplied: (pan: PanProfile, category: String, lots: Int) -> Unit
) {
    val context = LocalContext.current
    var category by remember(pan.id) { mutableStateOf(BidCategory.fromStored(pan.category)) }
    val range = lotRange(category, sizes)
    var lots by remember(pan.id, category) { mutableIntStateOf(range?.first ?: 1) }
    var broker by remember(pan.id) { mutableStateOf(Broker.fromStored(pan.broker)) }
    var clientId by remember(pan.id) { mutableStateOf(pan.brokerClientId) }
    val lotSize = ipo.lotSize.coerceAtLeast(1)
    val amount = lots.toLong() * lotSize * ipo.issuePrice.coerceAtLeast(0)
    val details = applyDetailsText(ipo.name, pan, category, lots, lotSize, ipo.issuePrice)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("apply_card_${pan.panNumber}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pan.holderName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = maskPanNumber(pan.panNumber, hidePanNumbers),
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (existing != null) {
                    Surface(color = MaterialTheme.ipo.gainContainer, shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.ipo.onGainContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = existing.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.ipo.onGainContainer
                            )
                        }
                    }
                }
            }

            // Category and lots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BidCategory.entries.forEach { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = c },
                        label = { Text(c.stored) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
            if (range == null) {
                Text(
                    text = stringResource(R.string.apply_category_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (lots > range.first) lots-- }, modifier = Modifier.testTag("apply_lots_minus_${pan.panNumber}")) {
                        Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.apply_fewer_lots))
                    }
                    Text(
                        text = stringResource(R.string.apply_lots, lots, lots * lotSize, formatInr(amount.toDouble())),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { if (range.second == null || lots < range.second!!) lots++ },
                        modifier = Modifier.testTag("apply_lots_plus_${pan.panNumber}")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.apply_more_lots))
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Details to paste into the broker or bank form
            CopyRow(stringResource(R.string.apply_field_pan), pan.panNumber, maskPanNumber(pan.panNumber, hidePanNumbers))
            if (pan.dematId.isNotBlank()) CopyRow(stringResource(R.string.apply_field_demat), pan.dematId, pan.dematId)
            if (pan.upiId.isNotBlank()) CopyRow(stringResource(R.string.apply_field_upi), pan.upiId, maskUpiId(pan.upiId))

            // Broker for this PAN
            Text(
                text = stringResource(R.string.apply_broker),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Broker.entries.forEach { b ->
                    FilterChip(
                        selected = broker == b,
                        onClick = {
                            broker = b
                            onSetBroker(pan.id, b.name, clientId)
                        },
                        label = { Text(b.label) }
                    )
                }
            }
            OutlinedTextField(
                value = clientId,
                onValueChange = { clientId = it.take(40) },
                label = { Text(stringResource(R.string.apply_client_id)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apply_client_id_${pan.panNumber}"),
                trailingIcon = {
                    if (clientId != pan.brokerClientId) {
                        IconButton(onClick = { onSetBroker(pan.id, broker?.name.orEmpty(), clientId) }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.apply_save_client_id))
                        }
                    }
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { copyText(context, details, context.getString(R.string.apply_copied_details)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.apply_copy_all))
                }
                Button(
                    onClick = {
                        // UPI is what brokers ask for last, so it is on the clipboard when their form opens
                        if (pan.upiId.isNotBlank()) copyText(context, pan.upiId, context.getString(R.string.apply_copied_upi))
                        broker?.let { openBroker(context, it) }
                    },
                    enabled = broker != null && broker != Broker.OTHER,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("apply_open_broker_${pan.panNumber}")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = broker?.takeIf { it != Broker.OTHER }?.label ?: stringResource(R.string.apply_pick_broker),
                        maxLines = 1
                    )
                }
            }
            if (existing == null && range != null) {
                Button(
                    onClick = { onMarkApplied(pan, category.stored, lots) },
                    enabled = canApply,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apply_mark_applied_${pan.panNumber}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.apply_mark_applied))
                }
            }
        }
    }
}

@Composable
private fun CopyRow(label: String, value: String, shown: String) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = shown,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = { copyText(context, value, context.getString(R.string.apply_copied_field, label)) }) {
            Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.apply_copy_field, label))
        }
    }
}

private fun copyText(context: Context, text: String, toast: String) {
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("IPO One", text))
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
    }
}

/** Opens the broker's app when installed, otherwise its IPO page in the browser. */
private fun openBroker(context: Context, broker: Broker) {
    val launch = broker.packageName?.let { context.packageManager.getLaunchIntentForPackage(it) }
    val intent = launch ?: broker.webUrl.takeIf { it.isNotBlank() }?.let { Intent(Intent.ACTION_VIEW, Uri.parse(it)) }
    if (intent != null) runCatching { context.startActivity(intent) }
}
