package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.model.AllotmentOutcome
import com.example.data.model.AllotmentResult
import com.example.data.model.AllotmentTarget
import com.example.data.model.RegistrarRetry
import com.example.data.model.allotmentResultLabel
import com.example.data.model.classifyAllotmentText
import com.example.data.model.maskPanNumber
import com.example.data.model.newPageLines
import com.example.data.model.pageLineSet
import kotlinx.coroutines.delay

private enum class RunnerPhase {
    LOADING, // page requested
    RETRY_WAIT, // registrar errored; waiting before the next attempt
    FILLING, // filling company / PAN
    READING, // submitted automatically; watching for the result
    WAITING_FOR_USER, // CAPTCHA or manual submit needed; still watching for the result
    FINISHED // every PAN done; summary shown
}

private const val PAGE_LOAD_TIMEOUT_MS = 30_000L
private const val AUTO_READ_TIMEOUT_MS = 45_000L
private const val POLL_INTERVAL_MS = 1_500L

/**
 * Checks allotment for each target in turn on the registrar's site: loads the
 * portal, fills the company and PAN, submits when there is no CAPTCHA (or lets
 * the user solve it), reads the result from the page and moves on. Slow sites
 * are retried with backoff. Each outcome is reported through [onResult] as soon
 * as it is known; a summary is shown at the end.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AllotmentCheckRunnerSheet(
    ipoName: String,
    registrar: AllotmentRegistrar,
    portalUrl: String,
    targets: List<AllotmentTarget>,
    hidePanNumbers: Boolean,
    onResult: (AllotmentTarget, AllotmentOutcome) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var index by remember { mutableIntStateOf(0) }
    var attempt by remember { mutableIntStateOf(1) }
    var loadToken by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(if (targets.isEmpty()) RunnerPhase.FINISHED else RunnerPhase.LOADING) }
    var pageReadyToken by remember { mutableIntStateOf(0) }
    var filledForToken by remember { mutableIntStateOf(-1) }
    var progress by remember { mutableIntStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val outcomes = remember { mutableStateMapOf<Int, AllotmentOutcome>() }
    val currentOnResult by rememberUpdatedState(onResult)

    val current = targets.getOrNull(index)

    fun record(outcome: AllotmentOutcome) {
        val target = targets.getOrNull(index) ?: return
        if (outcomes.containsKey(index)) return
        outcomes[index] = outcome
        currentOnResult(target, outcome)
    }

    fun next() {
        if (index + 1 >= targets.size) {
            phase = RunnerPhase.FINISHED
        } else {
            index += 1
            attempt = 1
            phase = RunnerPhase.LOADING
            loadToken += 1
        }
    }

    fun onLoadFailed() {
        if (phase == RunnerPhase.FINISHED || outcomes.containsKey(index)) return
        if (attempt < RegistrarRetry.MAX_ATTEMPTS) {
            attempt += 1
            phase = RunnerPhase.RETRY_WAIT
            loadToken += 1
        } else {
            record(AllotmentOutcome(AllotmentResult.UNVERIFIED, note = "Registrar did not respond"))
            next()
        }
    }

    // Load (or reload after a backoff) the registrar page for the current PAN
    LaunchedEffect(index, loadToken) {
        if (phase == RunnerPhase.FINISHED || current == null) return@LaunchedEffect
        while (webView == null) delay(50)
        delay(RegistrarRetry.delayBeforeAttempt(attempt))
        phase = RunnerPhase.LOADING
        filledForToken = -1
        webView?.loadUrl(portalUrl)
        delay(PAGE_LOAD_TIMEOUT_MS)
        if (phase == RunnerPhase.LOADING) onLoadFailed()
    }

    // Once the page is ready: fill it, then keep reading until a result appears
    LaunchedEffect(pageReadyToken) {
        val view = webView ?: return@LaunchedEffect
        val target = targets.getOrNull(index) ?: return@LaunchedEffect
        if (pageReadyToken == 0 || phase == RunnerPhase.FINISHED || outcomes.containsKey(index)) return@LaunchedEffect

        if (filledForToken != loadToken) {
            filledForToken = loadToken
            phase = RunnerPhase.FILLING
            delay(800) // let script-built forms render
            val baseline = pageLineSet(view.evaluateJs(READ_PAGE_TEXT_JS))
            var report = RegistrarFillReport()
            // Choosing "PAN" can reveal the PAN field, so try a few times
            repeat(3) {
                if (!report.panFilled) {
                    report = parseFillReport(view.evaluateJs(buildRegistrarFillJs(target.panNumber, ipoName, autoSubmit = true)))
                    if (!report.panFilled) delay(1_000)
                }
            }
            phase = if (report.submitted) RunnerPhase.READING else RunnerPhase.WAITING_FOR_USER
            pollForResult(view, baseline, phaseProvider = { phase }) { outcome ->
                record(outcome)
            }
        }
    }

    // Move on shortly after a result so the user can see it
    LaunchedEffect(outcomes.size) {
        if (outcomes.containsKey(index) && phase != RunnerPhase.FINISHED) {
            delay(1_200)
            next()
        }
    }

    // A submitted form that never shows a readable result hands over to the user
    LaunchedEffect(index, phase) {
        if (phase == RunnerPhase.READING) {
            delay(AUTO_READ_TIMEOUT_MS)
            if (phase == RunnerPhase.READING && !outcomes.containsKey(index)) phase = RunnerPhase.WAITING_FOR_USER
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
                .testTag("allotment_runner_sheet")
        ) {
            RunnerHeader(
                ipoName = ipoName,
                registrar = registrar,
                done = outcomes.size,
                total = targets.size,
                onClose = onDismiss
            )

            if (phase == RunnerPhase.FINISHED) {
                RunnerSummary(targets = targets, outcomes = outcomes, hidePanNumbers = hidePanNumbers, onDone = onDismiss)
                return@Column
            }

            current?.let { target ->
                RunnerStatusBar(
                    holderName = target.holderName,
                    pan = maskPanNumber(target.panNumber, hidePanNumbers),
                    position = index + 1,
                    total = targets.size,
                    phase = phase,
                    attempt = attempt,
                    outcome = outcomes[index]
                )
            }
            if (phase == RunnerPhase.LOADING && progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = registrar.accentColor
                )
            }

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
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                private var failedThisLoad = false

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    failedThisLoad = false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    if (!failedThisLoad && url != null && url != "about:blank") {
                                        // A late page from an attempt we already gave up on is ignored
                                        if (phase == RunnerPhase.LOADING) pageReadyToken += 1
                                    }
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        failedThisLoad = true
                                        onLoadFailed()
                                    }
                                }

                                override fun onReceivedHttpError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    errorResponse: WebResourceResponse?
                                ) {
                                    // 5xx and 429 mean the registrar is overloaded (common in the result hour)
                                    val code = errorResponse?.statusCode ?: return
                                    if (request?.isForMainFrame == true && (code >= 500 || code == 429)) {
                                        failedThisLoad = true
                                        onLoadFailed()
                                    }
                                }
                            }
                            webView = this
                        }
                    }
                )

                // Never leave a blank screen while the registrar is loading or busy
                if (phase == RunnerPhase.LOADING || phase == RunnerPhase.RETRY_WAIT || phase == RunnerPhase.FILLING) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = registrar.accentColor)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = when (phase) {
                                    RunnerPhase.RETRY_WAIT -> stringResource(
                                        R.string.runner_busy_retrying,
                                        attempt,
                                        RegistrarRetry.MAX_ATTEMPTS
                                    )
                                    RunnerPhase.FILLING -> stringResource(R.string.runner_filling)
                                    else -> stringResource(R.string.runner_loading, registrar.fullName)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            RunnerActions(
                phase = phase,
                onAllotted = { record(AllotmentOutcome(AllotmentResult.ALLOTTED, note = "Marked by you")) },
                onNotAllotted = { record(AllotmentOutcome(AllotmentResult.NOT_ALLOTTED, sharesAllotted = 0, note = "Marked by you")) },
                onSkip = { record(AllotmentOutcome(AllotmentResult.UNVERIFIED, note = "Skipped")) },
                onRetry = {
                    attempt = 1
                    phase = RunnerPhase.LOADING
                    loadToken += 1
                }
            )
        }
    }
}

/** Reads the page every [POLL_INTERVAL_MS] until a result shows or the step ends. */
private suspend fun pollForResult(
    view: WebView,
    baseline: Set<String>,
    phaseProvider: () -> RunnerPhase,
    onOutcome: (AllotmentOutcome) -> Unit
) {
    while (phaseProvider() == RunnerPhase.READING || phaseProvider() == RunnerPhase.WAITING_FOR_USER) {
        delay(POLL_INTERVAL_MS)
        val text = view.evaluateJs(READ_PAGE_TEXT_JS)
        val outcome = classifyAllotmentText(newPageLines(baseline, text))
        if (outcome != null) {
            onOutcome(outcome)
            return
        }
    }
}

@Composable
private fun RunnerHeader(
    ipoName: String,
    registrar: AllotmentRegistrar,
    done: Int,
    total: Int,
    onClose: () -> Unit
) {
    Surface(color = Color(0xFF071122), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.runner_title, ipoName),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.runner_subtitle, registrar.fullName, done, total),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.testTag("runner_close")) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.runner_close), tint = Color.White)
                }
            }
            if (total > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { done.toFloat() / total },
                    modifier = Modifier.fillMaxWidth(),
                    color = registrar.accentColor
                )
            }
        }
    }
}

@Composable
private fun RunnerStatusBar(
    holderName: String,
    pan: String,
    position: Int,
    total: Int,
    phase: RunnerPhase,
    attempt: Int,
    outcome: AllotmentOutcome?
) {
    val message = when {
        outcome != null -> allotmentResultLabel(outcome.result, outcome.sharesAllotted)
        phase == RunnerPhase.RETRY_WAIT -> stringResource(R.string.runner_busy_retrying, attempt, RegistrarRetry.MAX_ATTEMPTS)
        phase == RunnerPhase.READING -> stringResource(R.string.runner_reading)
        phase == RunnerPhase.WAITING_FOR_USER -> stringResource(R.string.runner_waiting_for_user)
        phase == RunnerPhase.FILLING -> stringResource(R.string.runner_filling)
        else -> stringResource(R.string.runner_loading_short)
    }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = stringResource(R.string.runner_current, position, total, holderName, pan),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = when (outcome?.result) {
                    AllotmentResult.ALLOTTED -> Color(0xFF34D399)
                    AllotmentResult.NOT_ALLOTTED -> Color(0xFFF87171)
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.testTag("runner_status")
            )
        }
    }
}

@Composable
private fun RunnerActions(
    phase: RunnerPhase,
    onAllotted: () -> Unit,
    onNotAllotted: () -> Unit,
    onSkip: () -> Unit,
    onRetry: () -> Unit
) {
    HorizontalDivider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onRetry, modifier = Modifier.testTag("runner_retry")) {
            Text(stringResource(R.string.runner_retry))
        }
        TextButton(onClick = onSkip, modifier = Modifier.testTag("runner_skip")) {
            Text(stringResource(R.string.runner_skip))
        }
        Spacer(modifier = Modifier.weight(1f))
        if (phase == RunnerPhase.WAITING_FOR_USER || phase == RunnerPhase.READING) {
            OutlinedButton(onClick = onNotAllotted, modifier = Modifier.testTag("runner_mark_not_allotted")) {
                Text(stringResource(R.string.runner_mark_not_allotted), color = Color(0xFFF87171))
            }
            Button(onClick = onAllotted, modifier = Modifier.testTag("runner_mark_allotted")) {
                Text(stringResource(R.string.runner_mark_allotted))
            }
        }
    }
}

@Composable
private fun RunnerSummary(
    targets: List<AllotmentTarget>,
    outcomes: Map<Int, AllotmentOutcome>,
    hidePanNumbers: Boolean,
    onDone: () -> Unit
) {
    val allotted = outcomes.values.count { it.result == AllotmentResult.ALLOTTED }
    val notAllotted = outcomes.values.count { it.result == AllotmentResult.NOT_ALLOTTED }
    val unverified = targets.size - allotted - notAllotted
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.runner_summary, allotted, notAllotted, unverified),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp)
        )
        if (targets.isEmpty()) {
            Text(
                text = stringResource(R.string.runner_no_targets),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .testTag("runner_summary"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
        ) {
            itemsIndexed(targets) { i, target ->
                val outcome = outcomes[i]
                val result = outcome?.result ?: AllotmentResult.UNVERIFIED
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = target.holderName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = listOfNotNull(
                                    maskPanNumber(target.panNumber, hidePanNumbers),
                                    outcome?.note
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = allotmentResultLabel(result, outcome?.sharesAllotted),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = when (result) {
                                AllotmentResult.ALLOTTED -> Color(0xFF34D399)
                                AllotmentResult.NOT_ALLOTTED -> Color(0xFFF87171)
                                AllotmentResult.UNVERIFIED -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(48.dp)
                .testTag("runner_done"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.runner_done))
        }
    }
}
