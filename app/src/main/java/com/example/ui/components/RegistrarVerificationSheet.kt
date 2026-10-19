package com.example.ui.components

import com.example.ui.theme.ipo
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.IpoApplication

enum class AllotmentRegistrar(
    val shortName: String,
    val fullName: String,
    val subtitle: String,
    val portalUrl: String,
    val accentColor: Color,
    // Lower-case fragments that identify this registrar in a registrar name
    val nameKeys: List<String> = emptyList()
) {
    LINK_INTIME(
        shortName = "Link Intime",
        fullName = "Link Intime (MUFG Intime)",
        subtitle = "Mainboard & SME Registrar • Auto-PAN Fill",
        portalUrl = "https://in.mpms.mufg.com/Initial_Offer/public-issues.html",
        accentColor = Color(0xFF10B981),
        nameKeys = listOf("intime", "mufg")
    ),
    KFINTECH(
        shortName = "KFintech",
        fullName = "KFintech KOSMIC IPO Status",
        subtitle = "Mainboard & SME Registrar • Auto-PAN Fill",
        portalUrl = "https://kosmic.kfintech.com/ipostatus/",
        accentColor = Color(0xFFF59E0B),
        nameKeys = listOf("kfin", "karvy")
    ),
    BIGSHARE(
        shortName = "Bigshare",
        fullName = "Bigshare Services Allotment",
        subtitle = "Mainboard & SME Registrar • Auto-PAN Fill",
        portalUrl = "https://ipo.bigshareonline.com/IPO_Status.html",
        accentColor = Color(0xFF38BDF8),
        nameKeys = listOf("bigshare")
    ),
    CAMEO(
        shortName = "Cameo",
        fullName = "Cameo Corporate Services",
        subtitle = "Mainboard & SME Registrar • Auto-PAN Fill",
        portalUrl = "https://ipo.cameoindia.com/",
        accentColor = Color(0xFFF472B6),
        nameKeys = listOf("cameo")
    ),
    SKYLINE(
        shortName = "Skyline",
        fullName = "Skyline Financial Services",
        subtitle = "SME Registrar • Auto-PAN Fill",
        portalUrl = "https://www.skylinerta.com/ipo.php",
        accentColor = Color(0xFF2DD4BF),
        nameKeys = listOf("skyline")
    ),
    MAASHITLA(
        shortName = "Maashitla",
        fullName = "Maashitla Securities",
        subtitle = "SME Registrar • Auto-PAN Fill",
        portalUrl = "https://maashitla.com/allotment-status/public-issues",
        accentColor = Color(0xFFFB923C),
        nameKeys = listOf("maashitla")
    ),
    PURVA(
        shortName = "Purva",
        fullName = "Purva Sharegistry",
        subtitle = "SME Registrar • Auto-PAN Fill",
        portalUrl = "https://www.purvashare.com/investor-service/ipo-query",
        accentColor = Color(0xFFA3E635),
        nameKeys = listOf("purva")
    ),
    INTEGRATED(
        shortName = "Integrated",
        fullName = "Integrated Registry Management Services",
        subtitle = "Mainboard & SME Registrar • Auto-PAN Fill",
        portalUrl = "https://ipostatus.integratedregistry.in/",
        accentColor = Color(0xFF818CF8),
        nameKeys = listOf("integrated registry", "integrated enterprises")
    ),
    MAS_SERVICES(
        shortName = "MAS",
        fullName = "MAS Services",
        subtitle = "SME Registrar • Auto-PAN Fill",
        portalUrl = "https://www.masserv.com/opt.asp",
        accentColor = Color(0xFFFACC15),
        nameKeys = listOf("mas services", "masserv")
    ),
    BSE_DIRECT(
        shortName = "BSE India",
        fullName = "BSE India Direct Exchange Check",
        subtitle = "Exchange-wide IPO Application Verification",
        portalUrl = "https://www.bseindia.com/investors/appli_check.aspx",
        accentColor = Color(0xFFA855F7)
    ),

    // Any other registrar: opens the registrar URL stored on the IPO
    GENERIC(
        shortName = "Registrar site",
        fullName = "Registrar website",
        subtitle = "Opens the registrar link stored on the IPO",
        portalUrl = "",
        accentColor = Color(0xFF94A3B8)
    );

    companion object {
        /** The registrar named in IPO data ("Kfin Technologies Ltd.", "MUFG Intime India ..."), if known. */
        fun fromName(name: String?): AllotmentRegistrar? {
            val n = name?.lowercase() ?: return null
            return entries.firstOrNull { reg -> reg.nameKeys.any { it in n } }
        }

        /** Registrars that have their own portal, for the switcher chips. */
        val withPortal: List<AllotmentRegistrar> get() = entries.filter { it.portalUrl.isNotBlank() }
    }
}

/**
 * The registrar to use for an IPO: the one named in its data when the app knows
 * it, the generic "registrar site" when only a URL is known, and otherwise the
 * older name-based guess.
 */
fun resolveRegistrar(ipoName: String, ipoId: String, extras: com.example.data.model.IpoExtras?): AllotmentRegistrar =
    AllotmentRegistrar.fromName(extras?.registrar)
        ?: if (!extras?.registrarWebsite.isNullOrBlank()) AllotmentRegistrar.GENERIC
        else detectRegistrarForIpo(ipoName, ipoId)

data class RegistrarDirectCheckRequest(
    val registrar: AllotmentRegistrar,
    // Registrar URL from the IPO's own data; used for GENERIC and preferred when present
    val portalUrlOverride: String? = null,
    val panNumber: String,
    val holderName: String = "Investor",
    val dematId: String = "",
    val ipoName: String = "",
    val application: IpoApplication? = null
)

fun copyPanToClipboard(context: Context, panNumber: String, registrarName: String) {
    if (panNumber.isBlank()) return
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("IPO PAN", panNumber.uppercase()))
        Toast.makeText(
            context,
            "PAN ${panNumber.uppercase()} copied & ready for $registrarName!",
            Toast.LENGTH_SHORT
        ).show()
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RegistrarVerificationModalSheet(
    request: RegistrarDirectCheckRequest,
    onDismiss: () -> Unit,
    onUpdateApplicationStatus: ((IpoApplication, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeRegistrar by remember(request) { mutableStateOf(request.registrar) }
    var pageProgress by remember { mutableIntStateOf(15) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var autoFillStatus by remember { mutableStateOf("PAN copied & auto-fill armed") }
    // The IPO's own registrar link wins for its registrar; GENERIC has no other URL
    fun urlFor(reg: AllotmentRegistrar): String =
        request.portalUrlOverride?.takeIf { it.isNotBlank() && reg == request.registrar } ?: reg.portalUrl
    val switcherRegistrars = remember(request) {
        AllotmentRegistrar.withPortal +
            listOfNotNull(AllotmentRegistrar.GENERIC.takeIf { request.registrar == it && !request.portalUrlOverride.isNullOrBlank() })
    }

    LaunchedEffect(activeRegistrar, request.panNumber) {
        if (request.panNumber.isNotBlank()) {
            copyPanToClipboard(context, request.panNumber, activeRegistrar.shortName)
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
                .fillMaxHeight(0.93f)
                .testTag("registrar_verification_sheet")
        ) {
            // Header Bar
            Surface(
                color = MaterialTheme.ipo.panel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(activeRegistrar.accentColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = activeRegistrar.accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Direct Registrar Allotment Check",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.ipo.onPanel
                                )
                                Text(
                                    text = if (request.ipoName.isNotBlank()) {
                                        "${request.ipoName} • ${activeRegistrar.fullName}"
                                    } else {
                                        activeRegistrar.fullName
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.ipo.onPanelMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { webViewRef?.reload() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload Portal",
                                    tint = MaterialTheme.ipo.onPanel,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(urlFor(activeRegistrar)))
                                        )
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open in External Browser",
                                    tint = activeRegistrar.accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("close_registrar_sheet_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.ipo.onPanel,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // One-Click Switcher between Link Intime, KFintech, Bigshare, BSE
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        switcherRegistrars.forEach { reg ->
                            val selected = activeRegistrar == reg
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    activeRegistrar = reg
                                    webViewRef?.loadUrl(urlFor(reg))
                                },
                                label = {
                                    Text(
                                        text = reg.shortName,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = reg.accentColor.copy(alpha = 0.22f),
                                    selectedLabelColor = reg.accentColor
                                ),
                                modifier = Modifier.testTag("switch_registrar_${reg.name}")
                            )
                        }
                    }

                    // Active PAN Auto-Fill Control Bar
                    if (request.panNumber.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.ipo.panelVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    activeRegistrar.accentColor.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${request.holderName} • PAN: ${request.panNumber.uppercase()}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.ipo.onPanel
                                    )
                                    Text(
                                        text = autoFillStatus,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = activeRegistrar.accentColor
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            copyPanToClipboard(
                                                context,
                                                request.panNumber,
                                                activeRegistrar.shortName
                                            )
                                            autoFillStatus = "PAN copied to clipboard"
                                        },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                            horizontal = 10.dp,
                                            vertical = 4.dp
                                        ),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("copy_pan_in_verifier_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy PAN", style = MaterialTheme.typography.labelMedium)
                                    }

                                    Button(
                                        onClick = {
                                            webViewRef?.evaluateJavascript(
                                                buildRegistrarFillJs(request.panNumber, request.ipoName, autoSubmit = true)
                                            ) {
                                                autoFillStatus =
                                                    "PAN ${request.panNumber.uppercase()} injected into ${activeRegistrar.shortName} form!"
                                            }
                                            copyPanToClipboard(
                                                context,
                                                request.panNumber,
                                                activeRegistrar.shortName
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = activeRegistrar.accentColor,
                                            contentColor = Color(0xFF04131A)
                                        ),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                            horizontal = 12.dp,
                                            vertical = 4.dp
                                        ),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("autofill_pan_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FactCheck,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "1-Click Fill PAN",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (pageProgress in 1..99) {
                LinearProgressIndicator(
                    progress = { pageProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = activeRegistrar.accentColor
                )
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // Embedded Live Registrar WebView
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadsImagesAutomatically = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    pageProgress = newProgress
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    return false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    pageProgress = 100
                                    if (request.panNumber.isNotBlank()) {
                                        view?.evaluateJavascript(
                                            buildRegistrarFillJs(request.panNumber, request.ipoName, autoSubmit = true)
                                        ) {
                                            autoFillStatus =
                                                "PAN ${request.panNumber.uppercase()} auto-filled & ready on ${activeRegistrar.shortName}"
                                        }
                                    }
                                }
                            }

                            loadUrl(urlFor(activeRegistrar))
                            webViewRef = this
                        }
                    },
                    update = { view ->
                        webViewRef = view
                    }
                )
            }

            // Bottom Quick Status Bar if verifying a specific IPO Application
            val trackedApp = request.application
            if (trackedApp != null && onUpdateApplicationStatus != null) {
                Surface(
                    color = MaterialTheme.ipo.panel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Update ${trackedApp.ipoName} Status",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.ipo.onPanel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Current: ${trackedApp.status}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.ipo.onPanelMuted
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onUpdateApplicationStatus(trackedApp, "Allotted")
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.ipo.gainContainer,
                                    contentColor = MaterialTheme.ipo.onGainContainer
                                ),
                                modifier = Modifier.testTag("verifier_mark_allotted_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Allotted", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    onUpdateApplicationStatus(trackedApp, "Not Allotted")
                                    onDismiss()
                                },
                                modifier = Modifier.testTag("verifier_mark_not_allotted_button")
                            ) {
                                Text("Not Allotted", color = MaterialTheme.ipo.loss)
                            }
                        }
                    }
                }
            }
        }
    }
}
