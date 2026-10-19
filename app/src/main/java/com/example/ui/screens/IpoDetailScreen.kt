package com.example.ui.screens

import com.example.ui.theme.ipo
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ApplicationCategory
import com.example.data.model.ApplicationSize
import com.example.data.model.IntermediaryRole
import com.example.data.model.IpoApplication
import com.example.data.model.IpoExtras
import com.example.data.model.IpoLifecycleStage
import com.example.data.model.LiveIpoItem
import com.example.data.model.PanProfile
import com.example.data.model.SubscriptionCategory
import com.example.data.model.SubscriptionFigures
import com.example.data.model.TimelineEntry
import com.example.data.model.TimelineEvent
import com.example.data.model.applicationSizes
import com.example.data.model.exchangeChips
import com.example.data.model.ipoTimeline
import com.example.data.model.lifecycleStage
import com.example.data.model.maskPanNumber
import com.example.data.model.stageFromStatusCode
import com.example.data.model.subscriptionRows
import com.example.ui.components.AllotmentRegistrar
import com.example.ui.components.AppliedIpoSummary
import com.example.ui.components.detectRegistrarForIpo
import com.example.util.formatIndianNumber
import com.example.util.formatInr
import com.example.util.formatSignedPercent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DetailDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

/** Maps a registrar name from ipo_extras to the registrars the app can auto-check. */
fun registrarFromName(name: String?): AllotmentRegistrar? = AllotmentRegistrar.fromName(name)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpoDetailScreen(
    ipo: LiveIpoItem,
    extras: IpoExtras?,
    isWatched: Boolean,
    lastUpdatedMillis: Long?,
    applications: List<IpoApplication>,
    panProfiles: List<PanProfile>,
    hidePanNumbers: Boolean,
    onBack: () -> Unit,
    onToggleWatch: () -> Unit,
    onRefresh: () -> Unit,
    onSubmitBid: (panHolderName: String, panNumber: String, category: String, lots: Int, upiId: String) -> Unit,
    onApplyForAllPans: (LiveIpoItem, List<PanProfile>) -> Unit,
    onSaveBatchAllotmentResults: (AppliedIpoSummary, Map<PanProfile, String>) -> Unit,
    onAddPanProfile: (holderName: String, panNumber: String, dematId: String, upiId: String, category: String) -> Unit,
    onToggleHidePanNumbers: () -> Unit,
    onCheckAllotmentForAll: (AllotmentRegistrar) -> Unit,
    onSaveAllotmentOutcome: ((com.example.data.model.AllotmentTarget, com.example.data.model.AllotmentOutcome) -> Unit)? = null,
    onTimelineDateClick: ((LocalDate) -> Unit)? = null,
    onIntermediaryClick: ((IntermediaryRole, String) -> Unit)? = null,
    onSetPanBroker: ((panId: String, broker: String, clientId: String) -> Unit)? = null,
    onMarkApplied: ((pan: PanProfile, category: String, lots: Int) -> Unit)? = null,
    onAddPan: () -> Unit = {},
    onFamilyApplied: ((pan: PanProfile, category: String, lots: Int, upi: String) -> Unit)? = null
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val stage = ipo.lifecycleStage(today)
    val isSme = ipo.category.equals("SME", ignoreCase = true)
    val registrar = com.example.ui.components.resolveRegistrar(ipo.name, ipo.id, extras)
    val myApplications = remember(ipo, applications) {
        com.example.data.model.applicationsForIpo(ipo, applications)
    }
    var showMyApplications by remember { mutableStateOf(false) }
    var showApplyAssistant by remember { mutableStateOf(false) }
    var showFamilyApply by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.detail_back)
                        )
                    }
                },
                title = {
                    Text(
                        text = ipo.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    if (ipo.investorGainUrl.isNotBlank()) {
                        IconButton(
                            onClick = { openUrl(context, ipo.investorGainUrl) },
                            modifier = Modifier.testTag("detail_open_browser_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = stringResource(R.string.detail_open_browser)
                            )
                        }
                    }
                    IconButton(
                        onClick = onToggleWatch,
                        modifier = Modifier.testTag("detail_watchlist_button")
                    ) {
                        Icon(
                            imageVector = if (isWatched) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = stringResource(
                                if (isWatched) R.string.detail_watch_remove else R.string.detail_watch_add
                            ),
                            tint = if (isWatched) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("ipo_detail_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DetailHeader(ipo = ipo, extras = extras, stage = stage, isSme = isSme)
            DetailMetricGrid(ipo = ipo, extras = extras, isSme = isSme)
            if (onMarkApplied != null && onSetPanBroker != null &&
                (stage == IpoLifecycleStage.OPEN || stage == IpoLifecycleStage.UPCOMING)
            ) {
                if (onFamilyApplied != null) {
                    ApplyAssistantEntry(
                        title = stringResource(R.string.family_open),
                        note = stringResource(R.string.family_open_note),
                        tag = "detail_family_apply",
                        onClick = { showFamilyApply = true }
                    )
                }
                ApplyAssistantEntry(
                    title = stringResource(R.string.apply_open),
                    note = stringResource(R.string.apply_open_note),
                    tag = "detail_apply_assistant",
                    onClick = { showApplyAssistant = true }
                )
            }
            TimelineCard(
                entries = ipoTimeline(ipo, extras, today),
                today = today,
                onDateClick = onTimelineDateClick
            )
            SubscriptionCard(
                rows = subscriptionRows(ipo, extras),
                lastUpdatedMillis = lastUpdatedMillis ?: extras?.subscriptionUpdatedAtMillis,
                onRefresh = onRefresh
            )
            ApplicationSizeCard(ipo = ipo, extras = extras, isSme = isSme)
            IntermediariesCard(extras = extras, isSme = isSme, onClick = onIntermediaryClick)
            MyApplicationsRow(
                count = myApplications.size,
                onClick = { showMyApplications = true }
            )
            IpoApplyAndAllotmentSection(
                ipo = ipo,
                panProfiles = panProfiles,
                onSubmitBid = onSubmitBid,
                onBidSaved = {},
                existingApplications = applications,
                onApplyForAllPans = onApplyForAllPans,
                onSaveBatchAllotmentResults = onSaveBatchAllotmentResults,
                onAddPanProfile = onAddPanProfile,
                hidePanNumbers = hidePanNumbers,
                onToggleHidePanNumbers = onToggleHidePanNumbers,
                registrar = registrar,
                registrarUrl = extras?.registrarWebsite,
                onSaveAllotmentOutcome = onSaveAllotmentOutcome
            )
            com.example.ui.components.CopyrightAttributionCard(
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
        }
    }

    if (showFamilyApply && onFamilyApplied != null && onSetPanBroker != null) {
        com.example.ui.components.FamilyApplySheet(
            ipo = ipo,
            panProfiles = panProfiles,
            alreadyApplied = myApplications.map { it.panNumber.uppercase() }.toSet(),
            hidePanNumbers = hidePanNumbers,
            onSetBroker = onSetPanBroker,
            onApplied = onFamilyApplied,
            onDismiss = { showFamilyApply = false }
        )
    }

    if (showApplyAssistant && onMarkApplied != null && onSetPanBroker != null) {
        com.example.ui.components.ApplyAssistantSheet(
            ipo = ipo,
            panProfiles = panProfiles,
            applications = applications,
            hidePanNumbers = hidePanNumbers,
            onSetBroker = onSetPanBroker,
            onMarkApplied = onMarkApplied,
            onAddPan = {
                showApplyAssistant = false
                onAddPan()
            },
            onDismiss = { showApplyAssistant = false }
        )
    }

    if (showMyApplications) {
        MyApplicationsSheet(
            applications = myApplications,
            hidePanNumbers = hidePanNumbers,
            onCheckAll = {
                showMyApplications = false
                onCheckAllotmentForAll(registrar)
            },
            onDismiss = { showMyApplications = false }
        )
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    val fullUrl = if (url.startsWith("http")) url else "https://$url"
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)))
    }
}

@StringRes
private fun stageLabelRes(stage: IpoLifecycleStage): Int = when (stage) {
    IpoLifecycleStage.UPCOMING -> R.string.stage_upcoming
    IpoLifecycleStage.OPEN -> R.string.stage_open
    IpoLifecycleStage.CLOSED -> R.string.stage_closed
    IpoLifecycleStage.ALLOTTED -> R.string.stage_allotted
    IpoLifecycleStage.LISTED -> R.string.stage_listed
    IpoLifecycleStage.CANCELLED -> R.string.stage_cancelled
}

private fun stageBadge(stage: IpoLifecycleStage, statusCode: String): String = when (stage) {
    IpoLifecycleStage.UPCOMING -> "U"
    IpoLifecycleStage.OPEN -> if (statusCode == "CT") "CT" else "O"
    IpoLifecycleStage.CLOSED -> "C"
    IpoLifecycleStage.ALLOTTED -> "A"
    IpoLifecycleStage.LISTED -> if (statusCode.startsWith("L")) statusCode else "LN"
    IpoLifecycleStage.CANCELLED -> "X"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHeader(
    ipo: LiveIpoItem,
    extras: IpoExtras?,
    stage: IpoLifecycleStage,
    isSme: Boolean
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            // Initial-letter placeholder sits behind the logo, so it shows while loading or on error
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ipo.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!extras?.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = extras?.logoUrl,
                        contentDescription = stringResource(R.string.detail_logo, ipo.name),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White)
                            .padding(4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ipo.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                extras?.symbol?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusBadge(
                        statusCode = stageBadge(stage, ipo.statusCode),
                        statusLabel = if (stageFromStatusCode(ipo.statusCode) == stage) ipo.statusLabel
                        else stringResource(stageLabelRes(stage))
                    )
                    HeaderTag(
                        text = stringResource(if (isSme) R.string.detail_sme else R.string.detail_mainboard),
                        container = if (isSme) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.primaryContainer,
                        content = if (isSme) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary
                    )
                    exchangeChips(ipo.listingExchange, isSme).forEach { exchange ->
                        HeaderTag(
                            text = exchange,
                            container = MaterialTheme.colorScheme.surfaceVariant,
                            content = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                extras?.website?.let { website ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { openUrl(context, website) }
                            .padding(vertical = 4.dp)
                            .testTag("detail_website_link"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.detail_website),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderTag(text: String, container: Color, content: Color) {
    Surface(color = container, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun DetailMetricGrid(ipo: LiveIpoItem, extras: IpoExtras?, isSme: Boolean) {
    val unavailable = stringResource(R.string.value_unavailable)
    val priceRange = when {
        ipo.issuePrice <= 0 -> unavailable
        ipo.priceBandLow != null && ipo.priceBandLow in 1 until ipo.issuePrice ->
            "${formatInr(ipo.priceBandLow)}–${formatInr(ipo.issuePrice)}"
        else -> formatInr(ipo.issuePrice)
    }
    val gmp = if (ipo.issuePrice > 0) {
        "${formatInr(ipo.gmpAmount, showPlus = true)} (${formatSignedPercent(ipo.gmpAmount / ipo.issuePrice * 100.0)})"
    } else {
        formatInr(ipo.gmpAmount, showPlus = true)
    }
    val lotSize = if (ipo.lotSize > 0) stringResource(R.string.metric_lot_size_value, ipo.lotSize) else unavailable
    val issueSize = ipo.issueSizeCr.takeUnless { it.isBlank() || it == "TBA" || it == "-" } ?: unavailable
    val minInvestment = applicationSizes(
        lotSize = ipo.lotSize,
        upperPrice = ipo.issuePrice,
        isSme = isSme,
        includeEmployee = false,
        includeShareholder = false
    ).firstOrNull { it.category == ApplicationCategory.RII }?.min?.let { formatInr(it.amount.toDouble()) }
        ?: unavailable

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricBox(stringResource(R.string.metric_price_range), priceRange, Modifier.weight(1f))
            MetricBox(stringResource(R.string.metric_gmp), gmp, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricBox(stringResource(R.string.metric_lot_size), lotSize, Modifier.weight(1f))
            MetricBox(stringResource(R.string.metric_issue_size), issueSize, Modifier.weight(1f))
        }
        MetricBox(stringResource(R.string.metric_min_investment), minInvestment, Modifier.fillMaxWidth())
    }
}

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                trailing?.invoke()
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@StringRes
private fun timelineLabelRes(event: TimelineEvent): Int = when (event) {
    TimelineEvent.OPENS -> R.string.timeline_opens
    TimelineEvent.CLOSES -> R.string.timeline_closes
    TimelineEvent.ALLOTMENT -> R.string.timeline_allotment
    TimelineEvent.SETTLEMENT -> R.string.timeline_settlement
    TimelineEvent.LISTING -> R.string.timeline_listing
    TimelineEvent.MANDATE_END -> R.string.timeline_mandate_end
    TimelineEvent.ANCHOR_50 -> R.string.timeline_anchor_50
    TimelineEvent.ANCHOR_100 -> R.string.timeline_anchor_100
}

@Composable
private fun TimelineCard(
    entries: List<TimelineEntry>,
    today: LocalDate,
    onDateClick: ((LocalDate) -> Unit)?
) {
    SectionCard(title = stringResource(R.string.timeline_title), modifier = Modifier.testTag("detail_timeline")) {
        if (entries.isEmpty()) {
            Text(
                text = stringResource(R.string.timeline_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        entries.forEach { entry ->
            val isPast = entry.date.isBefore(today)
            val isToday = entry.date == today
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (onDateClick != null) Modifier.clickable { onDateClick(entry.date) } else Modifier
                    )
                    .padding(vertical = 7.dp, horizontal = 4.dp)
                    .testTag("timeline_${entry.event.name.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isToday -> MaterialTheme.colorScheme.primary
                                isPast -> MaterialTheme.colorScheme.outline
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(timelineLabelRes(entry.event)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (entry.estimated) {
                        Text(
                            text = stringResource(R.string.timeline_estimated),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Text(
                    text = entry.date.format(DetailDateFormat),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.timeline_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@StringRes
private fun subscriptionLabelRes(category: SubscriptionCategory): Int = when (category) {
    SubscriptionCategory.QIB -> R.string.sub_qib
    SubscriptionCategory.QIB_FII -> R.string.sub_qib_fii
    SubscriptionCategory.QIB_DFI -> R.string.sub_qib_dfi
    SubscriptionCategory.QIB_MF -> R.string.sub_qib_mf
    SubscriptionCategory.QIB_OTHER -> R.string.sub_qib_other
    SubscriptionCategory.NII -> R.string.sub_nii
    SubscriptionCategory.BNII -> R.string.sub_bnii
    SubscriptionCategory.SNII -> R.string.sub_snii
    SubscriptionCategory.RII -> R.string.sub_rii
    SubscriptionCategory.EMPLOYEE -> R.string.sub_employee
    SubscriptionCategory.SHAREHOLDER -> R.string.sub_shareholder
    SubscriptionCategory.TOTAL -> R.string.sub_total
}

private fun formatTimes(value: Double?): String? =
    value?.let { String.format(Locale.ENGLISH, "%.2fx", it) }

@Composable
private fun SubscriptionCard(
    rows: List<Pair<SubscriptionCategory, SubscriptionFigures>>,
    lastUpdatedMillis: Long?,
    onRefresh: () -> Unit
) {
    var showApplications by rememberSaveable { mutableStateOf(false) }
    val dash = stringResource(R.string.value_unavailable)

    SectionCard(
        title = stringResource(R.string.subscription_title),
        modifier = Modifier.testTag("detail_subscription"),
        trailing = {
            IconButton(onClick = onRefresh, modifier = Modifier.testTag("detail_subscription_refresh")) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.subscription_refresh),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showApplications,
                onClick = { showApplications = false },
                label = { Text(stringResource(R.string.subscription_shares)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("subscription_toggle_shares")
            )
            FilterChip(
                selected = showApplications,
                onClick = { showApplications = true },
                label = { Text(stringResource(R.string.subscription_applications)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("subscription_toggle_applications")
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        TableRow(
            cells = if (showApplications) {
                listOf(
                    "",
                    stringResource(R.string.subscription_col_quota),
                    stringResource(R.string.subscription_col_apps_reserved),
                    stringResource(R.string.subscription_col_apps_received),
                    stringResource(R.string.subscription_col_times)
                )
            } else {
                listOf(
                    "",
                    stringResource(R.string.subscription_col_quota),
                    stringResource(R.string.subscription_col_shares_offered),
                    stringResource(R.string.subscription_col_shares_applied),
                    stringResource(R.string.subscription_col_times)
                )
            },
            header = true
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        rows.forEach { (category, figures) ->
            val quota = figures.quotaPct?.let { "${formatIndianNumber(it)}%" } ?: dash
            val cells = if (showApplications) {
                listOf(
                    stringResource(subscriptionLabelRes(category)),
                    quota,
                    figures.applicationsReserved?.let { formatIndianNumber(it) } ?: dash,
                    figures.applicationsReceived?.let { formatIndianNumber(it) } ?: dash,
                    formatTimes(figures.applicationTimes) ?: dash
                )
            } else {
                listOf(
                    stringResource(subscriptionLabelRes(category)),
                    quota,
                    figures.sharesOfferedCr?.let { formatIndianNumber(it) } ?: dash,
                    figures.sharesAppliedCr?.let { formatIndianNumber(it) } ?: dash,
                    formatTimes(figures.times) ?: dash
                )
            }
            TableRow(
                cells = cells,
                indent = category.isSubRow,
                bold = category == SubscriptionCategory.TOTAL,
                modifier = Modifier.testTag("subscription_row_${category.key}")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = lastUpdatedMillis?.let {
                stringResource(
                    R.string.subscription_last_updated,
                    DateUtils.getRelativeTimeSpanString(
                        it,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    ).toString()
                )
            } ?: stringResource(R.string.subscription_never_updated),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** First cell is the row label and gets more room; the rest are right-aligned figures. */
@Composable
private fun TableRow(
    cells: List<String>,
    modifier: Modifier = Modifier,
    header: Boolean = false,
    indent: Boolean = false,
    bold: Boolean = false
) {
    val style = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
    val weight = if (bold || header) FontWeight.Bold else FontWeight.Normal
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        cells.forEachIndexed { index, cell ->
            Text(
                text = cell,
                style = style.copy(fontWeight = weight),
                color = when {
                    header -> MaterialTheme.colorScheme.onSurfaceVariant
                    indent -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                },
                textAlign = if (index == 0) androidx.compose.ui.text.style.TextAlign.Start
                else androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier
                    .weight(if (index == 0) 1.6f else 1f)
                    .padding(start = if (index == 0 && indent) 12.dp else 0.dp)
            )
        }
    }
}

@StringRes
private fun applicationLabelRes(category: ApplicationCategory): Int = when (category) {
    ApplicationCategory.RII -> R.string.sub_rii
    ApplicationCategory.SNII -> R.string.sub_snii
    ApplicationCategory.BNII -> R.string.sub_bnii
    ApplicationCategory.EMPLOYEE -> R.string.sub_employee
    ApplicationCategory.SHAREHOLDER -> R.string.sub_shareholder
}

@Composable
private fun applicationSizeText(size: ApplicationSize?): String =
    size?.let {
        stringResource(
            R.string.app_size_cell,
            formatIndianNumber(it.shares.toLong()),
            formatIndianNumber(it.lots.toLong()),
            formatInr(it.amount.toDouble())
        )
    } ?: stringResource(R.string.value_unavailable)

@Composable
private fun ApplicationSizeCard(ipo: LiveIpoItem, extras: IpoExtras?, isSme: Boolean) {
    val subscription = extras?.subscription.orEmpty()
    val rows = applicationSizes(
        lotSize = ipo.lotSize,
        upperPrice = ipo.issuePrice,
        isSme = isSme,
        includeEmployee = subscription.containsKey(SubscriptionCategory.EMPLOYEE),
        includeShareholder = subscription.containsKey(SubscriptionCategory.SHAREHOLDER)
    )
    SectionCard(title = stringResource(R.string.app_size_title), modifier = Modifier.testTag("detail_application_size")) {
        if (rows.isEmpty()) {
            Text(
                text = stringResource(R.string.app_size_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@SectionCard
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            listOf(
                stringResource(R.string.app_size_col_category) to 0.8f,
                stringResource(R.string.app_size_col_min) to 1.5f,
                stringResource(R.string.app_size_col_max) to 1.5f
            ).forEach { (label, w) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(w)
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("app_size_${row.category.name.lowercase()}"),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = stringResource(applicationLabelRes(row.category)).substringBefore(" ("),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(0.8f)
                )
                Text(
                    text = applicationSizeText(row.min),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1.5f).padding(end = 6.dp)
                )
                Text(
                    text = applicationSizeText(row.max),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1.5f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.app_size_footer, formatInr(ipo.issuePrice)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntermediariesCard(
    extras: IpoExtras?,
    isSme: Boolean,
    onClick: ((IntermediaryRole, String) -> Unit)?
) {
    val groups = listOfNotNull(
        extras?.leadManagers?.takeIf { it.isNotEmpty() }?.let {
            Triple(R.string.intermediaries_lead_managers, IntermediaryRole.LEAD_MANAGER, it)
        },
        extras?.registrar?.let {
            Triple(R.string.intermediaries_registrar, IntermediaryRole.REGISTRAR, listOf(it))
        },
        extras?.marketMaker?.takeIf { isSme }?.let {
            Triple(R.string.intermediaries_market_maker, IntermediaryRole.MARKET_MAKER, listOf(it))
        }
    )
    SectionCard(title = stringResource(R.string.intermediaries_title), modifier = Modifier.testTag("detail_intermediaries")) {
        if (groups.isEmpty()) {
            Text(
                text = stringResource(R.string.intermediaries_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        groups.forEachIndexed { index, (labelRes, role, names) ->
            if (index > 0) Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                names.forEach { name ->
                    AssistChip(
                        onClick = { onClick?.invoke(role, name) },
                        enabled = onClick != null,
                        label = { Text(name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MyApplicationsRow(count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("detail_my_applications"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FactCheck,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.my_applications_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (count > 0) stringResource(R.string.my_applications_count, count)
                    else stringResource(R.string.my_applications_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyApplicationsSheet(
    applications: List<IpoApplication>,
    hidePanNumbers: Boolean,
    onCheckAll: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.my_applications_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (applications.isEmpty()) {
                Text(
                    text = stringResource(R.string.my_applications_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            applications.forEach { app ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                R.string.my_applications_row,
                                app.panHolderName,
                                maskPanNumber(app.panNumber, hidePanNumbers),
                                app.lotsApplied.toInt(),
                                formatInr(app.totalAmount.toDouble())
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = app.status,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = when (app.status) {
                                "Allotted" -> MaterialTheme.ipo.gain
                                "Not Allotted" -> MaterialTheme.ipo.loss
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onCheckAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("my_applications_check_all"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.FactCheck, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.my_applications_check_all))
            }
        }
    }
}

@Composable
private fun ApplyAssistantEntry(title: String, note: String, tag: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
