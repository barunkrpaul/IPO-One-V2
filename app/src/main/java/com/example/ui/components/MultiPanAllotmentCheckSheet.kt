package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.model.AllotmentOutcome
import com.example.data.model.AllotmentResult
import com.example.data.model.AllotmentTarget
import com.example.data.model.allotmentTargets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.IpoApplication
import com.example.data.model.PanProfile
import com.example.data.model.maskPanNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlin.math.abs

enum class PanAllotmentStatus(val label: String) {
    CHECKING("Checking..."),
    ALLOTTED("Allotted"),
    NOT_ALLOTTED("Not Allotted"),
    UNVERIFIED("Could not verify")
}

enum class PanResultFilter(val label: String) {
    ALL("All PANs"),
    ALLOTTED("Allotted"),
    NOT_ALLOTTED("Not Allotted")
}

data class AppliedIpoSummary(
    val ipoId: String,
    val ipoName: String,
    val category: String = "Mainboard",
    val issuePrice: Int = 100,
    val lotSize: Int = 15,
    val boaDate: String = "Live",
    val registrar: AllotmentRegistrar = detectRegistrarForIpo(ipoName, ipoId),
    // Registrar status page from the IPO's own data, when known
    val registrarUrl: String? = null
)

fun detectRegistrarForIpo(ipoName: String, ipoId: String = ""): AllotmentRegistrar {
    val combined = "$ipoName $ipoId".lowercase()
    return when {
        combined.contains("kfin") || combined.contains("hyundai") || combined.contains("swiggy") ||
            combined.contains("ntpc") || combined.contains("bajaj") -> AllotmentRegistrar.KFINTECH
        combined.contains("bigshare") || combined.contains("sme") -> AllotmentRegistrar.BIGSHARE
        else -> {
            val idx = abs(ipoName.trim().lowercase().hashCode()) % 3
            when (idx) {
                0 -> AllotmentRegistrar.LINK_INTIME
                1 -> AllotmentRegistrar.KFINTECH
                else -> AllotmentRegistrar.BIGSHARE
            }
        }
    }
}

fun shareAllotmentSummary(
    context: Context,
    appliedIpo: AppliedIpoSummary,
    panProfiles: List<PanProfile>,
    statusByPan: Map<String, String>,
    hidePanNumbers: Boolean
) {
    val total = panProfiles.size
    val allotted = panProfiles.count { statusByPan[it.panNumber.uppercase()] == "Allotted" }
    val notAllotted = panProfiles.count { statusByPan[it.panNumber.uppercase()] == "Not Allotted" }
    val sb = StringBuilder()
    sb.appendLine("📊 *IPO Allotment Status Summary — IPO One*")
    sb.appendLine("🏢 *IPO:* ${appliedIpo.ipoName} (${appliedIpo.category})")
    sb.appendLine("🏛️ *Registrar:* ${appliedIpo.registrar.fullName}")
    sb.appendLine("💰 *Issue Price:* ₹${appliedIpo.issuePrice} • *Lot:* ${appliedIpo.lotSize} Shares")
    sb.appendLine("────────────────────────")
    sb.appendLine("✅ *Allotted:* $allotted  |  ❌ *Not Allotted:* $notAllotted  |  📋 *Total PANs:* $total")
    sb.appendLine("────────────────────────")
    panProfiles.forEachIndexed { idx, pan ->
        val st = statusByPan[pan.panNumber.uppercase()] ?: "Checked"
        val icon = if (st == "Allotted") "✅" else "❌"
        val displayPan = maskPanNumber(pan.panNumber, hidePanNumbers)
        sb.appendLine("${idx + 1}. ${pan.holderName} ($displayPan • ${pan.category}): $icon *$st*")
    }
    sb.appendLine("────────────────────────")
    sb.appendLine("Checked in 1 click via IPO One")

    runCatching {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${appliedIpo.ipoName} Allotment Status")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Allotment Status"))
    }
}

private val allotmentHttpClient = OkHttpClient.Builder()
    .connectTimeout(3, TimeUnit.SECONDS)
    .readTimeout(3, TimeUnit.SECONDS)
    .build()

/**
 * Queries the registrar endpoint on IO thread if available, and resolves the Allotment Status
 * ("Allotted" or "Not Allotted") for a specific saved PAN card against the target IPO.
 */
suspend fun autoVerifySinglePanAllotment(
    ipoId: String,
    ipoName: String,
    registrar: AllotmentRegistrar,
    panProfile: PanProfile,
    panIndex: Int,
    existingStatus: String?,
    forceRecheck: Boolean = false
): PanAllotmentStatus = withContext(Dispatchers.IO) {
    if (!forceRecheck && existingStatus == "Allotted") {
        return@withContext PanAllotmentStatus.ALLOTTED
    }
    if (!forceRecheck && existingStatus == "Not Allotted") {
        return@withContext PanAllotmentStatus.NOT_ALLOTTED
    }

    // Attempt live registrar verification request
    val liveResult: PanAllotmentStatus? = runCatching {
        val jsonMedia = "application/json; charset=utf-8".toMediaType()
        when (registrar) {
            AllotmentRegistrar.LINK_INTIME -> {
                val payload = """{"clientid":"0","PAN":"${panProfile.panNumber.uppercase()}","key_word":"PAN"}"""
                val req = Request.Builder()
                    .url("https://in.mpms.mufg.com/Initial_Offer/IPO.aspx/SearchOnPan")
                    .post(payload.toRequestBody(jsonMedia))
                    .header("Accept", "application/json")
                    .build()
                allotmentHttpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (body.contains("<ALLOT>", ignoreCase = true)) {
                        val notAllotted = body.contains("<ALLOT>0</ALLOT>", ignoreCase = true)
                        if (notAllotted) PanAllotmentStatus.NOT_ALLOTTED else PanAllotmentStatus.ALLOTTED
                    } else null
                }
            }
            AllotmentRegistrar.BIGSHARE -> {
                val payload =
                    """{"Applicationno":"","company":"0","SelectionType":"PN","PanNo":"${panProfile.panNumber.uppercase()}","txtcsdl":"","txtDPID":"","txtClId":"","ddlType":""}"""
                val req = Request.Builder()
                    .url("https://ipo.bigshareonline.com/Data.aspx/FetchIpoDetails")
                    .post(payload.toRequestBody(jsonMedia))
                    .header("Accept", "application/json")
                    .build()
                allotmentHttpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (body.contains("ALLOTED", ignoreCase = true) && !body.contains("NON ALLOTED", ignoreCase = true)) {
                        PanAllotmentStatus.ALLOTTED
                    } else if (body.contains("NON ALLOTED", ignoreCase = true)) {
                        PanAllotmentStatus.NOT_ALLOTTED
                    } else null
                }
            }
            else -> null
        }
    }.getOrNull()

    // Without a definite answer from the registrar the result is unknown; never guess
    liveResult ?: PanAllotmentStatus.UNVERIFIED
}

/**
 * Narada-Style Multi-PAN Auto Allotment Status Modal Sheet.
 * Automatically checks ALL saved PAN cards for a selected Applied IPO and shows
 * ALLOTTED or NOT ALLOTTED against each PAN card, with Result Filtering, Share Summary, and Hide PAN privacy toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiPanAllotmentCheckModalSheet(
    appliedIpo: AppliedIpoSummary,
    panProfiles: List<PanProfile>,
    existingApplications: List<IpoApplication>,
    onSaveBatchResults: (Map<PanProfile, String>) -> Unit,
    onToggleSinglePanStatus: (PanProfile, String) -> Unit,
    onAddPanProfile: (holderName: String, panNumber: String, dematId: String, upiId: String, category: String) -> Unit,
    onOpenOfficialRegistrar: (RegistrarDirectCheckRequest) -> Unit,
    onDismiss: () -> Unit,
    hidePanNumbers: Boolean = false,
    onToggleHidePanNumbers: (() -> Unit)? = null,
    onSaveAllotmentOutcome: ((AllotmentTarget, AllotmentOutcome) -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRunner by remember { mutableStateOf(false) }
    val panStatusMap = remember { mutableStateMapOf<String, PanAllotmentStatus>() }
    var isAutoChecking by remember { mutableStateOf(false) }
    var checkedCount by remember { mutableIntStateOf(0) }
    var recheckTrigger by remember { mutableIntStateOf(0) }
    var resultFilter by remember { mutableStateOf(PanResultFilter.ALL) }
    var localHidePan by remember(hidePanNumbers) { mutableStateOf(hidePanNumbers) }

    var showInlineAddPan by remember { mutableStateOf(panProfiles.isEmpty()) }
    var newHolderName by remember { mutableStateOf("") }
    var newPanNumber by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Retail") }
    var inlineError by remember { mutableStateOf<String?>(null) }

    // Auto-run allotment check across ALL saved PAN cards when opened or when PAN list / trigger updates
    LaunchedEffect(appliedIpo.ipoId, panProfiles.map { it.panNumber }, recheckTrigger) {
        if (panProfiles.isEmpty()) {
            isAutoChecking = false
            checkedCount = 0
            return@LaunchedEffect
        }
        isAutoChecking = true
        checkedCount = 0
        panProfiles.forEach { pan ->
            panStatusMap[pan.panNumber.uppercase()] = PanAllotmentStatus.CHECKING
        }

        val resolvedBatch = LinkedHashMap<PanProfile, String>()
        panProfiles.forEachIndexed { index, pan ->
            val key = pan.panNumber.uppercase()
            val matchingApp = existingApplications.firstOrNull {
                (it.ipoId == appliedIpo.ipoId || it.ipoName.equals(appliedIpo.ipoName, ignoreCase = true)) &&
                    it.panNumber.equals(key, ignoreCase = true)
            }
            val resultStatus = autoVerifySinglePanAllotment(
                ipoId = appliedIpo.ipoId,
                ipoName = appliedIpo.ipoName,
                registrar = appliedIpo.registrar,
                panProfile = pan,
                panIndex = index,
                existingStatus = matchingApp?.status,
                forceRecheck = recheckTrigger > 0
            )
            panStatusMap[key] = resultStatus
            checkedCount = index + 1
            if (resultStatus == PanAllotmentStatus.ALLOTTED || resultStatus == PanAllotmentStatus.NOT_ALLOTTED) {
                resolvedBatch[pan] = resultStatus.label
            }
        }
        isAutoChecking = false
        if (resolvedBatch.isNotEmpty()) {
            onSaveBatchResults(resolvedBatch)
        }
    }

    val totalPans = panProfiles.size
    val allottedCount = panProfiles.count { panStatusMap[it.panNumber.uppercase()] == PanAllotmentStatus.ALLOTTED }
    val notAllottedCount = panProfiles.count { panStatusMap[it.panNumber.uppercase()] == PanAllotmentStatus.NOT_ALLOTTED }
    val lotShares = if (appliedIpo.lotSize > 0) appliedIpo.lotSize else 15
    val lotAmount = lotShares * (if (appliedIpo.issuePrice > 0) appliedIpo.issuePrice else 100)

    val filteredPanList = remember(panProfiles, panStatusMap.toMap(), resultFilter) {
        panProfiles.filter { pan ->
            val st = panStatusMap[pan.panNumber.uppercase()]
            when (resultFilter) {
                PanResultFilter.ALL -> true
                PanResultFilter.ALLOTTED -> st == PanAllotmentStatus.ALLOTTED
                PanResultFilter.NOT_ALLOTTED -> st == PanAllotmentStatus.NOT_ALLOTTED
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .testTag("multi_pan_allotment_sheet")
        ) {
            // Top Gradient Header (Narada-style IPO Allotment Header)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF071326),
                                Color(0xFF0D1F3C)
                            )
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = appliedIpo.registrar.accentColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, appliedIpo.registrar.accentColor.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = appliedIpo.registrar.accentColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Registrar: ${appliedIpo.registrar.shortName}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = appliedIpo.registrar.accentColor
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${appliedIpo.category} • ₹${appliedIpo.issuePrice} • $lotShares Sh/Lot",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFCBD5E1),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = appliedIpo.ipoName,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = if (isAutoChecking) {
                                    "Auto-checking saved PAN cards ($checkedCount of $totalPans) with ${appliedIpo.registrar.fullName}..."
                                } else if (totalPans == 0) {
                                    "Add your PAN cards below to auto-check allotment status"
                                } else {
                                    "All $totalPans saved PAN card(s) auto-checked • Filter, Share or Verify below"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isAutoChecking) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Hide / Mask PAN Privacy Toggle (Narada Feature)
                            IconButton(
                                onClick = {
                                    localHidePan = !localHidePan
                                    onToggleHidePanNumbers?.invoke()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .testTag("sheet_toggle_hide_pan_button")
                            ) {
                                Icon(
                                    imageVector = if (localHidePan) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (localHidePan) "Show PAN Numbers" else "Hide PAN Numbers",
                                    tint = if (localHidePan) Color(0xFF38BDF8) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // 1-Tap Share Allotment Summary (Narada Feature)
                            if (panProfiles.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        val statusStrings = panProfiles.associate {
                                            it.panNumber.uppercase() to (panStatusMap[it.panNumber.uppercase()]?.label ?: "Checked")
                                        }
                                        shareAllotmentSummary(
                                            context = context,
                                            appliedIpo = appliedIpo,
                                            panProfiles = panProfiles,
                                            statusByPan = statusStrings,
                                            hidePanNumbers = localHidePan
                                        )
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF064E3B))
                                        .testTag("sheet_share_allotment_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share Allotment Summary",
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .testTag("close_multi_pan_sheet_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Allotment Check",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (isAutoChecking && totalPans > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (checkedCount.toFloat() / totalPans.toFloat()).coerceIn(0.1f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF10B981),
                            trackColor = Color(0xFF1E293B)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Narada-Style 3-Column Summary Strip: Total PANs | Allotted | Not Allotted (Clickable to filter!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AllotmentSummaryStatBox(
                            title = "ALL PANS",
                            count = totalPans.toString(),
                            accentColor = Color(0xFF38BDF8),
                            containerColor = Color(0xFF0F243C),
                            isSelected = resultFilter == PanResultFilter.ALL,
                            onClick = { resultFilter = PanResultFilter.ALL },
                            modifier = Modifier.weight(1f)
                        )
                        AllotmentSummaryStatBox(
                            title = "ALLOTTED",
                            count = allottedCount.toString(),
                            accentColor = Color(0xFF10B981),
                            containerColor = Color(0xFF063229),
                            isSelected = resultFilter == PanResultFilter.ALLOTTED,
                            onClick = { resultFilter = PanResultFilter.ALLOTTED },
                            modifier = Modifier.weight(1f)
                        )
                        AllotmentSummaryStatBox(
                            title = "NOT ALLOTTED",
                            count = notAllottedCount.toString(),
                            accentColor = Color(0xFFF87171),
                            containerColor = Color(0xFF3B1219),
                            isSelected = resultFilter == PanResultFilter.NOT_ALLOTTED,
                            onClick = { resultFilter = PanResultFilter.NOT_ALLOTTED },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Guided check on the registrar's own site: fills each PAN in turn and reads the result
                    if (onSaveAllotmentOutcome != null) {
                        Button(
                            onClick = { showRunner = true },
                            enabled = !isAutoChecking,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = appliedIpo.registrar.accentColor,
                                contentColor = Color(0xFF04131A)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("start_registrar_runner_button")
                        ) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.runner_start),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Action Row: Re-Check All PANs + Share + Add New PAN Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { recheckTrigger++ },
                            enabled = !isAutoChecking && totalPans > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color(0xFF031B14)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("recheck_all_pans_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isAutoChecking) "Checking..." else "Re-Check All",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (panProfiles.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    val statusStrings = panProfiles.associate {
                                        it.panNumber.uppercase() to (panStatusMap[it.panNumber.uppercase()]?.label ?: "Checked")
                                    }
                                    shareAllotmentSummary(
                                        context = context,
                                        appliedIpo = appliedIpo,
                                        panProfiles = panProfiles,
                                        statusByPan = statusStrings,
                                        hidePanNumbers = localHidePan
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF34D399)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("sheet_share_summary_cta")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Share",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF34D399)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showInlineAddPan = !showInlineAddPan },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("sheet_toggle_add_pan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showInlineAddPan) "Hide" else "+ Add PAN",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                }
            }

            // Inline Quick Add PAN Form inside Allotment Checker
            AnimatedVisibility(visible = showInlineAddPan) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Add Family / Applicant PAN Card (Auto-Checks Immediately)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newHolderName,
                                onValueChange = { newHolderName = it },
                                label = { Text("Applicant Name") },
                                placeholder = { Text("e.g. Barun Kr Paul") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sheet_pan_holder_input")
                            )
                            OutlinedTextField(
                                value = newPanNumber,
                                onValueChange = {
                                    newPanNumber = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(10)
                                },
                                label = { Text("10-Char PAN") },
                                placeholder = { Text("ABCDE1234F") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sheet_pan_number_input")
                            )
                        }
                        if (inlineError != null) {
                            Text(
                                text = inlineError!!,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Retail", "sHNI", "bHNI").forEach { cat ->
                                    FilterChip(
                                        selected = newCategory == cat,
                                        onClick = { newCategory = cat },
                                        label = { Text(cat) }
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    if (newHolderName.trim().length < 2) {
                                        inlineError = "Enter applicant name"
                                    } else if (newPanNumber.trim().length != 10) {
                                        inlineError = "PAN must be 10 characters (e.g. ABCDE1234F)"
                                    } else {
                                        inlineError = null
                                        onAddPanProfile(
                                            newHolderName.trim(),
                                            newPanNumber.trim().uppercase(),
                                            "1208160000000001",
                                            "investor@upi",
                                            newCategory
                                        )
                                        newHolderName = ""
                                        newPanNumber = ""
                                        showInlineAddPan = false
                                    }
                                },
                                modifier = Modifier.testTag("sheet_save_pan_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save & Auto-Check")
                            }
                        }
                    }
                }
            }

            // Narada Result Filter Strip + Privacy Status Bar
            if (panProfiles.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = resultFilter == PanResultFilter.ALL,
                                onClick = { resultFilter = PanResultFilter.ALL },
                                label = { Text("All ($totalPans)") },
                                modifier = Modifier.testTag("filter_pan_all")
                            )
                            FilterChip(
                                selected = resultFilter == PanResultFilter.ALLOTTED,
                                onClick = { resultFilter = PanResultFilter.ALLOTTED },
                                label = { Text("Allotted ($allottedCount)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF064E3B),
                                    selectedLabelColor = Color(0xFF34D399)
                                ),
                                modifier = Modifier.testTag("filter_pan_allotted")
                            )
                            FilterChip(
                                selected = resultFilter == PanResultFilter.NOT_ALLOTTED,
                                onClick = { resultFilter = PanResultFilter.NOT_ALLOTTED },
                                label = { Text("Not Allotted ($notAllottedCount)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF450A0A),
                                    selectedLabelColor = Color(0xFFFCA5A5)
                                ),
                                modifier = Modifier.testTag("filter_pan_not_allotted")
                            )
                        }

                        Text(
                            text = if (localHidePan) "PAN Masked" else "Hide PAN",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                localHidePan = !localHidePan
                                onToggleHidePanNumbers?.invoke()
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Main List: Every Saved PAN Card with ALLOTTED / NOT ALLOTTED Status
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("multi_pan_results_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (panProfiles.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No Saved PAN Cards Yet",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Add your family PAN cards above once, and IPO One will automatically check allotment status across all saved PAN cards in one click!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else if (filteredPanList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No PAN cards match '${resultFilter.label}'",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(onClick = { resultFilter = PanResultFilter.ALL }) {
                                    Text("Show All ($totalPans) PAN Cards")
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(filteredPanList, key = { _, p -> p.id.ifBlank { p.panNumber } }) { index, pan ->
                        val status = panStatusMap[pan.panNumber.uppercase()] ?: PanAllotmentStatus.CHECKING
                        NaradaStylePanResultCard(
                            index = index + 1,
                            panProfile = pan,
                            status = status,
                            lotShares = lotShares,
                            lotAmount = lotAmount,
                            registrar = appliedIpo.registrar,
                            hidePan = localHidePan,
                            onToggleStatus = {
                                val nextStatus = if (status == PanAllotmentStatus.ALLOTTED) {
                                    PanAllotmentStatus.NOT_ALLOTTED
                                } else {
                                    PanAllotmentStatus.ALLOTTED
                                }
                                panStatusMap[pan.panNumber.uppercase()] = nextStatus
                                onToggleSinglePanStatus(pan, nextStatus.label)
                            },
                            onOpenRegistrarPortal = {
                                onOpenOfficialRegistrar(
                                    RegistrarDirectCheckRequest(
                                        registrar = appliedIpo.registrar,
                                        portalUrlOverride = appliedIpo.registrarUrl,
                                        panNumber = pan.panNumber.uppercase(),
                                        holderName = pan.holderName,
                                        dematId = pan.dematId,
                                        ipoName = appliedIpo.ipoName
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (showRunner && onSaveAllotmentOutcome != null) {
        val targets = remember(appliedIpo, panProfiles, existingApplications) {
            allotmentTargets(appliedIpo.ipoId, appliedIpo.ipoName, panProfiles, existingApplications)
        }
        AllotmentCheckRunnerSheet(
            ipoName = appliedIpo.ipoName,
            registrar = appliedIpo.registrar,
            portalUrl = appliedIpo.registrarUrl?.takeIf { it.isNotBlank() } ?: appliedIpo.registrar.portalUrl,
            targets = targets,
            hidePanNumbers = localHidePan,
            onResult = { target, outcome ->
                // Mirror the result in this sheet's per-PAN cards
                panStatusMap[target.panNumber] = when (outcome.result) {
                    AllotmentResult.ALLOTTED -> PanAllotmentStatus.ALLOTTED
                    AllotmentResult.NOT_ALLOTTED -> PanAllotmentStatus.NOT_ALLOTTED
                    AllotmentResult.UNVERIFIED -> PanAllotmentStatus.UNVERIFIED
                }
                onSaveAllotmentOutcome(target, outcome)
            },
            onDismiss = { showRunner = false }
        )
    }
}

@Composable
private fun AllotmentSummaryStatBox(
    title: String,
    count: String,
    accentColor: Color,
    containerColor: Color,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) accentColor else accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = accentColor
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFCBD5E1),
                maxLines = 1
            )
        }
    }
}

@Composable
fun NaradaStylePanResultCard(
    index: Int,
    panProfile: PanProfile,
    status: PanAllotmentStatus,
    lotShares: Int,
    lotAmount: Int,
    registrar: AllotmentRegistrar,
    hidePan: Boolean = false,
    onToggleStatus: () -> Unit,
    onOpenRegistrarPortal: () -> Unit
) {
    val borderColor = when (status) {
        PanAllotmentStatus.ALLOTTED -> Color(0xFF10B981).copy(alpha = 0.55f)
        PanAllotmentStatus.NOT_ALLOTTED -> Color(0xFFF87171).copy(alpha = 0.45f)
        PanAllotmentStatus.CHECKING -> Color(0xFF38BDF8).copy(alpha = 0.4f)
        PanAllotmentStatus.UNVERIFIED -> Color(0xFF94A3B8).copy(alpha = 0.4f)
    }
    val cardBg = when (status) {
        PanAllotmentStatus.ALLOTTED -> Color(0xFF062E24)
        PanAllotmentStatus.NOT_ALLOTTED -> Color(0xFF1F1218)
        PanAllotmentStatus.CHECKING -> Color(0xFF0F1E36)
        PanAllotmentStatus.UNVERIFIED -> Color(0xFF151C28)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .testTag("pan_allotment_result_card_${panProfile.panNumber}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
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
                // Left: Holder Name + PAN Number
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when (status) {
                                    PanAllotmentStatus.ALLOTTED -> Color(0xFF10B981).copy(alpha = 0.2f)
                                    PanAllotmentStatus.NOT_ALLOTTED -> Color(0xFFF87171).copy(alpha = 0.2f)
                                    PanAllotmentStatus.CHECKING -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                                    PanAllotmentStatus.UNVERIFIED -> Color(0xFF94A3B8).copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = panProfile.holderName.trim().take(1).uppercase().ifEmpty { "$index" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = when (status) {
                                PanAllotmentStatus.ALLOTTED -> Color(0xFF34D399)
                                PanAllotmentStatus.NOT_ALLOTTED -> Color(0xFFF87171)
                                PanAllotmentStatus.CHECKING -> Color(0xFF38BDF8)
                                PanAllotmentStatus.UNVERIFIED -> Color(0xFF94A3B8)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = panProfile.holderName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = Color(0xFF0A1426),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Text(
                                    text = maskPanNumber(panProfile.panNumber, hidePan),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = panProfile.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right: Prominent ALLOTTED / NOT ALLOTTED Status Badge against each PAN
                when (status) {
                    PanAllotmentStatus.CHECKING -> {
                        Surface(
                            color = Color(0xFF0C2744),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF38BDF8)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Checking...",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }

                    PanAllotmentStatus.ALLOTTED -> {
                        Surface(
                            color = Color(0xFF059669),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .clickable { onToggleStatus() }
                                .testTag("pan_status_badge_${panProfile.panNumber}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Allotted",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "ALLOTTED",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    PanAllotmentStatus.UNVERIFIED -> {
                        Surface(
                            color = Color(0xFF334155),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .clickable { onToggleStatus() }
                                .testTag("pan_status_badge_${panProfile.panNumber}")
                        ) {
                            Text(
                                text = "COULD NOT VERIFY",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    PanAllotmentStatus.NOT_ALLOTTED -> {
                        Surface(
                            color = Color(0xFF991B1B),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .clickable { onToggleStatus() }
                                .testTag("pan_status_badge_${panProfile.panNumber}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Not Allotted",
                                    tint = Color(0xFFFECACA),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "NOT ALLOTTED",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Detail Row: Shares Allotted + Live Registrar Portal Verification Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (status) {
                        PanAllotmentStatus.ALLOTTED ->
                            "Allotted: $lotShares Shares (1 Lot • ₹$lotAmount)"
                        PanAllotmentStatus.NOT_ALLOTTED ->
                            "Allotted: 0 Shares (Applied: $lotShares Sh • Mandate Unblocked)"
                        PanAllotmentStatus.CHECKING ->
                            "Querying ${registrar.shortName} allotment server..."
                        PanAllotmentStatus.UNVERIFIED ->
                            "No answer from ${registrar.shortName} yet. Check on the registrar site."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = when (status) {
                        PanAllotmentStatus.ALLOTTED -> Color(0xFF6EE7B7)
                        PanAllotmentStatus.NOT_ALLOTTED -> Color(0xFFFCA5A5)
                        PanAllotmentStatus.CHECKING -> Color(0xFF94A3B8)
                        PanAllotmentStatus.UNVERIFIED -> Color(0xFFCBD5E1)
                    },
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onToggleStatus() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Toggle status",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Switch",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    Surface(
                        color = registrar.accentColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, registrar.accentColor.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clickable { onOpenRegistrarPortal() }
                            .testTag("verify_portal_${panProfile.panNumber}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = registrar.shortName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = registrar.accentColor
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Verify on Registrar",
                                tint = registrar.accentColor,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Global Post-Login Quick Add / Edit / Manage PAN Card Modal Sheet.
 * Supports adding new PAN cards, editing existing PAN cards, and masking PAN numbers for privacy.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddPanModalSheet(
    panProfiles: List<PanProfile>,
    onAddPanProfile: (holderName: String, panNumber: String, dematId: String, upiId: String, category: String) -> Unit,
    onUpdatePanProfile: ((panId: String, holderName: String, panNumber: String, dematId: String, upiId: String, category: String) -> Unit)? = null,
    onDeletePanProfile: (String) -> Unit,
    hidePanNumbers: Boolean = false,
    onToggleHidePanNumbers: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingPanId by remember { mutableStateOf<String?>(null) }
    var holderName by remember { mutableStateOf("") }
    var panNumber by remember { mutableStateOf("") }
    var dematId by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Retail") }
    var showAdvancedFields by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("quick_add_pan_modal_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (editingPanId != null) "Edit Saved PAN Card" else "Add & Manage PAN Cards",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "All saved PAN cards are auto-checked for IPO allotment in 1 click",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onToggleHidePanNumbers != null) {
                        IconButton(
                            onClick = onToggleHidePanNumbers,
                            modifier = Modifier.testTag("modal_toggle_hide_pan_button")
                        ) {
                            Icon(
                                imageVector = if (hidePanNumbers) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (hidePanNumbers) "Show PAN Numbers" else "Hide PAN Numbers",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = holderName,
                        onValueChange = { holderName = it },
                        label = { Text("PAN Holder Full Name") },
                        placeholder = { Text("e.g. Ujjal Kumar Das") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modal_pan_holder_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = panNumber,
                        onValueChange = {
                            panNumber = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(10)
                        },
                        label = { Text("10-Character PAN Number") },
                        placeholder = { Text("ABCDE1234F") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modal_pan_number_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Retail", "sHNI", "bHNI").forEach { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }

                        Text(
                            text = if (showAdvancedFields) "Hide Demat/UPI" else "+ Demat/UPI (Optional)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showAdvancedFields = !showAdvancedFields }
                        )
                    }

                    AnimatedVisibility(visible = showAdvancedFields) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedTextField(
                                value = dematId,
                                onValueChange = { dematId = it.take(24) },
                                label = { Text("16-Digit CDSL / NSDL BO ID (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = upiId,
                                onValueChange = { upiId = it.take(60) },
                                label = { Text(stringResource(R.string.upi_optional_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (errorText != null) {
                        Text(
                            text = errorText!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (editingPanId != null) {
                            OutlinedButton(
                                onClick = {
                                    editingPanId = null
                                    holderName = ""
                                    panNumber = ""
                                    dematId = ""
                                    upiId = ""
                                    category = "Retail"
                                    errorText = null
                                },
                                modifier = Modifier.height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel Edit")
                            }
                        }

                        Button(
                            onClick = {
                                if (holderName.trim().length < 2) {
                                    errorText = "Please enter the PAN holder's name (at least 2 characters)."
                                } else if (panNumber.trim().length != 10) {
                                    errorText = "Please enter a valid 10-character PAN number (e.g. ABCDE1234F)."
                                } else {
                                    errorText = null
                                    val targetId = editingPanId
                                    if (targetId != null && onUpdatePanProfile != null) {
                                        onUpdatePanProfile(
                                            targetId,
                                            holderName.trim(),
                                            panNumber.trim().uppercase(),
                                            dematId.trim().ifBlank { "1208160000000001" },
                                            upiId.trim(),
                                            category
                                        )
                                    } else {
                                        onAddPanProfile(
                                            holderName.trim(),
                                            panNumber.trim().uppercase(),
                                            dematId.trim().ifBlank { "1208160000000001" },
                                            upiId.trim(),
                                            category
                                        )
                                    }
                                    editingPanId = null
                                    holderName = ""
                                    panNumber = ""
                                    dematId = ""
                                    upiId = ""
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("modal_save_pan_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (editingPanId != null) "Update Saved PAN Card" else "Save PAN Card for Auto-Allotment",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (panProfiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved PAN Cards (${panProfiles.size}) • Tap to Edit",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (onToggleHidePanNumbers != null) {
                        Text(
                            text = if (hidePanNumbers) "Show Full PANs" else "Mask PANs",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onToggleHidePanNumbers() }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    panProfiles.forEach { pan ->
                        Surface(
                            color = if (editingPanId == pan.id) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                editingPanId = pan.id
                                holderName = pan.holderName
                                panNumber = pan.panNumber
                                dematId = pan.dematId
                                upiId = pan.upiId
                                category = pan.category
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit PAN",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${pan.holderName} • ${maskPanNumber(pan.panNumber, hidePanNumbers)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove PAN",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onDeletePanProfile(pan.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
