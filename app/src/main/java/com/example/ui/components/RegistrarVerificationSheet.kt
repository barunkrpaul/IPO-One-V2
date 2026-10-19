package com.example.ui.components

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
    val accentColor: Color
) {
    LINK_INTIME(
        shortName = "Link Intime",
        fullName = "Link Intime (MUFG Intime)",
        subtitle = "Mainboard & SME Registrar • Auto-PAN Fill",
        portalUrl = "https://in.mpms.mufg.com/Initial_Offer/public-issues.html",
        accentColor = Color(0xFF10B981)
    ),
    KFINTECH(
        shortName = "KFintech",
        fullName = "KFintech KOSMIC IPO Status",
        subtitle = "Mainboard & SME Registrar • Auto-PAN Fill",
        portalUrl = "https://kosmic.kfintech.com/ipostatus/",
        accentColor = Color(0xFFF59E0B)
    ),
    BIGSHARE(
        shortName = "Bigshare",
        fullName = "Bigshare Services Allotment",
        subtitle = "Mainboard & SME Registrar • Auto-PAN Fill",
        portalUrl = "https://ipo.bigshareonline.com/IPO_Status.html",
        accentColor = Color(0xFF38BDF8)
    ),
    BSE_DIRECT(
        shortName = "BSE India",
        fullName = "BSE India Direct Exchange Check",
        subtitle = "Exchange-wide IPO Application Verification",
        portalUrl = "https://www.bseindia.com/investors/appli_check.aspx",
        accentColor = Color(0xFFA855F7)
    )
}

data class RegistrarDirectCheckRequest(
    val registrar: AllotmentRegistrar,
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

private fun buildAutoFillPanJs(pan: String): String {
    val safePan = pan.trim().uppercase().replace("'", "").replace("\"", "")
    return """
        (function() {
            try {
                var panVal = '$safePan';
                if (!panVal) return 'EMPTY';
                
                // Select PAN radio option if present on Link Intime / KFintech / Bigshare
                var radios = document.querySelectorAll('input[type="radio"]');
                for (var i = 0; i < radios.length; i++) {
                    var r = radios[i];
                    var idOrVal = ((r.id || '') + ' ' + (r.value || '') + ' ' + (r.name || '')).toLowerCase();
                    if (idOrVal.indexOf('pan') !== -1 || idOrVal.indexOf('342') !== -1) {
                        r.click();
                        r.checked = true;
                    }
                }
                
                // Find text inputs likely corresponding to PAN
                var inputs = document.querySelectorAll('input[type="text"], input:not([type])');
                var filled = 0;
                for (var j = 0; j < inputs.length; j++) {
                    var inp = inputs[j];
                    var meta = ((inp.id || '') + ' ' + (inp.name || '') + ' ' + (inp.placeholder || '') + ' ' + (inp.className || '')).toLowerCase();
                    if (meta.indexOf('captcha') === -1 && meta.indexOf('search') === -1) {
                        inp.focus();
                        inp.value = panVal;
                        inp.dispatchEvent(new Event('input', { bubbles: true }));
                        inp.dispatchEvent(new Event('change', { bubbles: true }));
                        filled++;
                    }
                }
                return 'FILLED_' + filled;
            } catch (e) {
                return 'ERR';
            }
        })();
    """.trimIndent()
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
                color = Color(0xFF071122),
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
                                    color = Color.White
                                )
                                Text(
                                    text = if (request.ipoName.isNotBlank()) {
                                        "${request.ipoName} • ${activeRegistrar.fullName}"
                                    } else {
                                        activeRegistrar.fullName
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
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
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(activeRegistrar.portalUrl))
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
                                    tint = Color.White,
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
                        AllotmentRegistrar.entries.forEach { reg ->
                            val selected = activeRegistrar == reg
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    activeRegistrar = reg
                                    webViewRef?.loadUrl(reg.portalUrl)
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
                            color = Color(0xFF0F1E36),
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
                                        color = Color.White
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
                                                buildAutoFillPanJs(request.panNumber)
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
                                            buildAutoFillPanJs(request.panNumber)
                                        ) {
                                            autoFillStatus =
                                                "PAN ${request.panNumber.uppercase()} auto-filled & ready on ${activeRegistrar.shortName}"
                                        }
                                    }
                                }
                            }

                            loadUrl(activeRegistrar.portalUrl)
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
                    color = Color(0xFF071122),
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
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Current: ${trackedApp.status}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onUpdateApplicationStatus(trackedApp, "Allotted")
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF065F46),
                                    contentColor = Color(0xFF6EE7B7)
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
                                Text("Not Allotted", color = Color(0xFFF87171))
                            }
                        }
                    }
                }
            }
        }
    }
}
