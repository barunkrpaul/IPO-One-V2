package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.local.BrokerSessionStore
import com.example.data.model.BidCategory
import com.example.data.model.Broker
import com.example.data.model.LiveIpoItem
import com.example.data.model.PanProfile
import com.example.data.model.applicationSizes
import com.example.data.model.isBidConfirmation
import com.example.data.model.lotRange
import com.example.data.model.maskPanNumber
import com.example.data.model.newPageLines
import com.example.data.model.pageLineSet
import com.example.ui.theme.ipo
import com.example.util.formatInr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One family member's bid, set up before the run. */
private class FamilyBid(
    val pan: PanProfile,
    selected: Boolean,
    broker: Broker?,
    category: BidCategory,
    lots: Int,
    upi: String
) {
    var selected by mutableStateOf(selected)
    var broker by mutableStateOf(broker)
    var category by mutableStateOf(category)
    var lots by mutableIntStateOf(lots)
    var upi by mutableStateOf(upi)
    var outcome by mutableStateOf<String?>(null)
}

private enum class FamilyPhase { SETUP, RUNNING, DONE }

/**
 * Applies for one IPO for several family members without switching apps. Each
 * person's broker site opens inside IPO One with their own saved login, the
 * bid (lots, cut-off price, UPI ID) is filled in, the person confirms the bid
 * on the broker page, and the app logs it and moves to the next person. Each
 * person then approves the UPI mandate in their UPI app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FamilyApplySheet(
    ipo: LiveIpoItem,
    panProfiles: List<PanProfile>,
    alreadyApplied: Set<String>,
    hidePanNumbers: Boolean,
    onSetBroker: (panId: String, broker: String, clientId: String) -> Unit,
    onApplied: (pan: PanProfile, category: String, lots: Int, upi: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessions = remember { BrokerSessionStore(context) }
    val sizes = remember(ipo) {
        applicationSizes(ipo.lotSize, ipo.issuePrice, ipo.category.equals("SME", ignoreCase = true), false, false)
    }
    val bids = remember(panProfiles) {
        mutableStateListOf<FamilyBid>().apply {
            panProfiles.forEach { pan ->
                val category = BidCategory.fromStored(pan.category)
                add(
                    FamilyBid(
                        pan = pan,
                        selected = pan.panNumber.uppercase() !in alreadyApplied,
                        broker = Broker.fromStored(pan.broker)?.takeIf { it.supportsInApp },
                        category = category,
                        lots = lotRange(category, sizes)?.first ?: 1,
                        upi = pan.upiId
                    )
                )
            }
        }
    }
    var phase by remember { mutableStateOf(FamilyPhase.SETUP) }
    val queue = remember { mutableStateListOf<FamilyBid>() }
    var index by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .testTag("family_apply_sheet")
        ) {
            Surface(color = MaterialTheme.ipo.panel, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.family_title, ipo.name),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.ipo.onPanel
                        )
                        Text(
                            text = when (phase) {
                                FamilyPhase.SETUP -> stringResource(R.string.family_subtitle_setup)
                                FamilyPhase.RUNNING -> stringResource(R.string.family_subtitle_running, index + 1, queue.size)
                                FamilyPhase.DONE -> stringResource(R.string.family_subtitle_done)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.ipo.onPanelMuted
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.runner_close), tint = MaterialTheme.ipo.onPanel)
                    }
                }
            }

            when (phase) {
                FamilyPhase.SETUP -> FamilySetup(
                    ipo = ipo,
                    bids = bids,
                    sizes = sizes,
                    hidePanNumbers = hidePanNumbers,
                    hasLogin = { bid -> bid.broker?.let { sessions.hasSession(BrokerSessionStore.personKey(bid.pan.panNumber, it.name)) } == true },
                    onBrokerChosen = { bid, broker ->
                        bid.broker = broker
                        onSetBroker(bid.pan.id, broker.name, bid.pan.brokerClientId)
                    },
                    onStart = {
                        queue.clear()
                        queue.addAll(bids.filter { it.selected && it.broker != null })
                        index = 0
                        phase = if (queue.isEmpty()) FamilyPhase.DONE else FamilyPhase.RUNNING
                    }
                )

                FamilyPhase.RUNNING -> {
                    val bid = queue.getOrNull(index)
                    if (bid != null) {
                        FamilyRunStep(
                            ipo = ipo,
                            bid = bid,
                            sessions = sessions,
                            hidePanNumbers = hidePanNumbers,
                            onConfirmed = {
                                bid.outcome = context.getString(R.string.family_outcome_applied)
                                onApplied(bid.pan, bid.category.stored, bid.lots, bid.upi.trim())
                            },
                            onNext = {
                                if (index + 1 < queue.size) index++ else phase = FamilyPhase.DONE
                            },
                            onSkip = {
                                bid.outcome = context.getString(R.string.family_outcome_skipped)
                                if (index + 1 < queue.size) index++ else phase = FamilyPhase.DONE
                            }
                        )
                    }
                }

                FamilyPhase.DONE -> FamilyDone(
                    bids = queue,
                    hidePanNumbers = hidePanNumbers,
                    onDone = {
                        scope.launch { sessions.clearWebSession() }
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun FamilySetup(
    ipo: LiveIpoItem,
    bids: List<FamilyBid>,
    sizes: List<com.example.data.model.ApplicationSizeRow>,
    hidePanNumbers: Boolean,
    hasLogin: (FamilyBid) -> Boolean,
    onBrokerChosen: (FamilyBid, Broker) -> Unit,
    onStart: () -> Unit
) {
    val inAppBrokers = Broker.entries.filter { it.supportsInApp }
    val ready = bids.count { it.selected && it.broker != null }
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(color = MaterialTheme.ipo.warningContainer, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = stringResource(R.string.family_how_it_works),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.ipo.onWarningContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            if (bids.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.apply_no_pans),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(bids, key = { it.pan.id.ifBlank { it.pan.panNumber } }) { bid ->
                val range = lotRange(bid.category, sizes)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("family_member_${bid.pan.panNumber}")
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = bid.selected, onCheckedChange = { bid.selected = it })
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bid.pan.holderName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = maskPanNumber(bid.pan.panNumber, hidePanNumbers),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (hasLogin(bid)) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.ipo.gain, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.family_login_saved),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.ipo.gain
                                )
                            }
                        }
                        if (bid.selected) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                inAppBrokers.forEach { b ->
                                    FilterChip(
                                        selected = bid.broker == b,
                                        onClick = { onBrokerChosen(bid, b) },
                                        label = { Text(b.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                BidCategory.entries.forEach { c ->
                                    FilterChip(
                                        selected = bid.category == c,
                                        onClick = {
                                            bid.category = c
                                            bid.lots = lotRange(c, sizes)?.first ?: 1
                                        },
                                        label = { Text(c.stored) }
                                    )
                                }
                            }
                            if (range != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (bid.lots > range.first) bid.lots-- }) {
                                        Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.apply_fewer_lots))
                                    }
                                    Text(
                                        text = stringResource(
                                            R.string.apply_lots,
                                            bid.lots,
                                            bid.lots * ipo.lotSize,
                                            formatInr((bid.lots.toLong() * ipo.lotSize * ipo.issuePrice).toDouble())
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { if (range.second == null || bid.lots < range.second!!) bid.lots++ }) {
                                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.apply_more_lots))
                                    }
                                }
                            }
                            UpiField(bid)
                        }
                    }
                }
            }
        }
        HorizontalDivider()
        Button(
            onClick = onStart,
            enabled = ready > 0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(48.dp)
                .testTag("family_start"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(stringResource(R.string.family_start, ready))
        }
    }
}

/** The UPI ID is always typeable: prefilled from the vault, editable for this bid. */
@Composable
private fun UpiField(bid: FamilyBid) {
    OutlinedTextField(
        value = bid.upi,
        onValueChange = { bid.upi = it.trim().take(60) },
        label = { Text(stringResource(R.string.family_upi_label)) },
        placeholder = { Text("name@okhdfcbank") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("family_upi_${bid.pan.panNumber}")
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun FamilyRunStep(
    ipo: LiveIpoItem,
    bid: FamilyBid,
    sessions: BrokerSessionStore,
    hidePanNumbers: Boolean,
    onConfirmed: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val broker = bid.broker ?: return
    val stepScope = rememberCoroutineScope()
    val personKey = BrokerSessionStore.personKey(bid.pan.panNumber, broker.name)
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loading by remember(bid) { mutableStateOf(true) }
    var pageToken by remember(bid) { mutableIntStateOf(0) }
    var pendingStorage by remember(bid) { mutableStateOf<String?>(null) }
    var storageApplied by remember(bid) { mutableStateOf(false) }
    var confirmed by remember(bid) { mutableStateOf(false) }
    var status by remember(bid) { mutableStateOf("") }
    val fillingText = stringResource(R.string.family_status_filling)
    val loginText = stringResource(R.string.family_status_login)
    val confirmText = stringResource(R.string.family_status_confirm)
    val doneText = stringResource(R.string.family_status_done)

    suspend fun saveLogin(view: WebView) {
        val storage = view.evaluateJs(READ_SITE_STORAGE_JS)
        sessions.save(personKey, broker.sessionUrls, storage)
    }

    // Switch the shared WebView to this person's saved login, then open their broker
    LaunchedEffect(bid) {
        while (webView == null) delay(50)
        loading = true
        pendingStorage = sessions.restore(personKey)
        storageApplied = false
        webView?.loadUrl(broker.inAppUrl ?: broker.webUrl)
    }

    // Each loaded page: fill the bid form, then watch for the broker's confirmation
    LaunchedEffect(pageToken) {
        val view = webView ?: return@LaunchedEffect
        if (pageToken == 0 || confirmed) return@LaunchedEffect
        val storage = pendingStorage
        if (storage != null && !storageApplied) {
            storageApplied = true
            if (view.evaluateJs(buildRestoreSiteStorageJs(storage)) == "true") {
                view.reload()
                return@LaunchedEffect
            }
        }
        loading = false
        delay(1_200)
        val baseline = pageLineSet(view.evaluateJs(READ_PAGE_TEXT_JS))
        var filled = false
        while (!confirmed) {
            if (!filled) {
                val report = view.evaluateJs(
                    buildBrokerFillJs(ipo.name, bid.lots, bid.lots * ipo.lotSize, ipo.issuePrice, bid.upi.trim())
                )
                filled = report.contains("\"upi\":true") || report.contains("\"qty\":true")
                status = when {
                    filled -> confirmText
                    report.contains("\"openedApply\":true") -> fillingText
                    else -> loginText
                }
            }
            delay(2_000)
            val text = view.evaluateJs(READ_PAGE_TEXT_JS)
            if (filled && isBidConfirmation(newPageLines(baseline, text))) {
                confirmed = true
                status = doneText
                saveLogin(view)
                onConfirmed()
                delay(1_500)
                onNext()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(
                        R.string.family_current,
                        bid.pan.holderName,
                        maskPanNumber(bid.pan.panNumber, hidePanNumbers),
                        broker.label,
                        bid.lots
                    ),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = status.ifBlank { loginText },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (confirmed) MaterialTheme.ipo.gain else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("family_status")
                )
                Spacer(modifier = Modifier.height(4.dp))
                UpiField(bid)
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                if (url != null && url != "about:blank") pageToken += 1
                            }
                        }
                        webView = this
                    }
                }
            )
            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                webView?.let { view ->
                    view.evaluateJavascript(
                        buildBrokerFillJs(ipo.name, bid.lots, bid.lots * ipo.lotSize, ipo.issuePrice, bid.upi.trim()),
                        null
                    )
                }
            }) { Text(stringResource(R.string.family_fill_again)) }
            TextButton(onClick = {
                // Keep this person's login before the browser is switched to the next person
                stepScope.launch {
                    webView?.let { saveLogin(it) }
                    onSkip()
                }
            }) { Text(stringResource(R.string.runner_skip)) }
            Spacer(modifier = Modifier.weight(1f))
            if (!confirmed) {
                Button(
                    onClick = {
                        confirmed = true
                        stepScope.launch {
                            webView?.let { saveLogin(it) }
                            onConfirmed()
                            onNext()
                        }
                    },
                    modifier = Modifier.testTag("family_mark_applied")
                ) {
                    Text(stringResource(R.string.family_mark_applied))
                }
            }
        }
    }
}

@Composable
private fun FamilyDone(bids: List<FamilyBid>, hidePanNumbers: Boolean, onDone: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Surface(color = MaterialTheme.ipo.infoContainer, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = stringResource(R.string.family_mandate_reminder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            items(bids, key = { it.pan.id.ifBlank { it.pan.panNumber } }) { bid ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(bid.pan.holderName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "${maskPanNumber(bid.pan.panNumber, hidePanNumbers)} · ${bid.broker?.label.orEmpty()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val applied = bid.outcome == stringResource(R.string.family_outcome_applied)
                    if (applied) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.ipo.gain, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = bid.outcome ?: stringResource(R.string.family_outcome_skipped),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (applied) MaterialTheme.ipo.gain else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        OutlinedButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(stringResource(R.string.runner_done))
        }
    }
}
