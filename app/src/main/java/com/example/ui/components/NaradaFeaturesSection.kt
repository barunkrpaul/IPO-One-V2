package com.example.ui.components

import com.example.ui.theme.ipo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.IpoApplication

/**
 * Narada-Style Allotment & Mandate Overview Summary Card.
 * Displays aggregate stats across all Applied IPOs, Allotted PANs, Mandates, and a 1-tap Hide PAN Privacy Toggle.
 */
@Composable
fun AllotmentOverviewBannerCard(
    totalSavedPans: Int,
    totalAppliedIpos: Int,
    allApplications: List<IpoApplication>,
    hidePanNumbers: Boolean,
    onToggleHidePanNumbers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allottedBids = allApplications.count { it.status == "Allotted" }
    val pendingMandates = allApplications.count { it.status == "Mandate Pending" }
    val activeBlockedInr = allApplications
        .filter { it.status == "Applied" || it.status == "Mandate Pending" || it.status == "Allotted" }
        .sumOf { it.totalAmount }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("allotment_overview_banner_card"),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.ipo.panel,
                            MaterialTheme.ipo.panelVariant,
                            MaterialTheme.ipo.gainContainer
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ALLOTMENT & MANDATE OVERVIEW",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.ipo.gain
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "1-Click Multi-PAN Tracker",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.ipo.onPanel
                        )
                    }

                    Surface(
                        color = if (hidePanNumbers) MaterialTheme.ipo.gainContainer else MaterialTheme.ipo.panelVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (hidePanNumbers) MaterialTheme.ipo.gain else MaterialTheme.ipo.info.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .clickable { onToggleHidePanNumbers() }
                            .testTag("vault_toggle_hide_pan_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hidePanNumbers) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (hidePanNumbers) MaterialTheme.ipo.gain else MaterialTheme.ipo.info,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (hidePanNumbers) "PANs Hidden" else "Hide PANs",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.ipo.onPanel
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OverviewStatPill(
                        label = "Saved PANs",
                        value = totalSavedPans.toString(),
                        accent = MaterialTheme.ipo.info,
                        modifier = Modifier.weight(1f)
                    )
                    OverviewStatPill(
                        label = "Applied IPOs",
                        value = totalAppliedIpos.toString(),
                        accent = MaterialTheme.ipo.warning,
                        modifier = Modifier.weight(1f)
                    )
                    OverviewStatPill(
                        label = "Allotted",
                        value = allottedBids.toString(),
                        accent = MaterialTheme.ipo.gain,
                        modifier = Modifier.weight(1f)
                    )
                    OverviewStatPill(
                        label = "Mandates",
                        value = if (pendingMandates > 0) "$pendingMandates Pend" else "₹${activeBlockedInr / 1000}K",
                        accent = MaterialTheme.ipo.accent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewStatPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.ipo.panelVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = accent,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.ipo.onPanel,
                maxLines = 1
            )
        }
    }
}

/**
 * Narada-Style "Request New UPI Mandate" Dialog for existing applications.
 */
@Composable
fun RequestNewMandateDialog(
    application: IpoApplication,
    onConfirmNewMandate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var upiIdInput by remember(application) {
        mutableStateOf(application.upiId.ifBlank { "investor@upi" })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Request New UPI Mandate",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Re-trigger a fresh ASBA UPI mandate request for ${application.panHolderName} (${application.panNumber}) on ${application.ipoName}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = upiIdInput,
                    onValueChange = { upiIdInput = it.take(60) },
                    label = { Text("UPI Handle for New Mandate") },
                    placeholder = { Text("e.g. investor@okicici") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_mandate_upi_input")
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Amount to Block: ₹${application.totalAmount} (${application.lotsApplied} Lot • ${application.sharesApplied} Shares)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmNewMandate(upiIdInput.trim().ifBlank { "investor@upi" })
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_new_mandate_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send New Mandate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Narada-Style "Swipe to Apply / Pre-Apply" Interactive Confirmation Bar.
 * Prevents accidental IPO applications while allowing 1-gesture multi-PAN submission.
 */
@Composable
fun SwipeToConfirmApplyBar(
    label: String,
    isPreApply: Boolean,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderProgress by remember { mutableFloatStateOf(0f) }
    var confirmed by remember { mutableStateOf(false) }

    val accentColor = if (isPreApply) MaterialTheme.ipo.warning else MaterialTheme.ipo.gain
    val bgColor = if (isPreApply) MaterialTheme.ipo.warningContainer.copy(alpha = 0.65f) else MaterialTheme.ipo.gainContainer.copy(alpha = 0.65f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("swipe_to_apply_bar"),
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (confirmed) Icons.Default.CheckCircle else Icons.Default.SwipeRight,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (confirmed) "Application Confirmed!" else label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isPreApply) {
                                "Slide right (or tap Confirm) to Pre-Apply before IPO opens"
                            } else {
                                "Slide right (or tap Confirm) to prevent accidental bids"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.ipo.onPanel
                        )
                    }
                }

                Button(
                    onClick = {
                        sliderProgress = 1f
                        confirmed = true
                        onConfirmed()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = MaterialTheme.ipo.onGain
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("swipe_instant_confirm_button")
                ) {
                    Text(
                        text = if (isPreApply) "Pre-Apply" else "Confirm",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
            }

            Slider(
                value = sliderProgress,
                onValueChange = { value ->
                    sliderProgress = value
                    if (value >= 0.88f && !confirmed) {
                        confirmed = true
                        onConfirmed()
                    }
                },
                onValueChangeFinished = {
                    if (sliderProgress < 0.88f) {
                        sliderProgress = 0f
                    }
                },
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = MaterialTheme.ipo.panelVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )
        }
    }
}

/**
 * Interactive PAN & IPO Allotment FAQs Card (inspired by trynarada.com FAQ section).
 */
@Composable
fun NaradaIpoFaqCard(modifier: Modifier = Modifier) {
    var expandedIndex by remember { mutableIntStateOf(-1) }
    val faqs = remember {
        listOf(
            "How does 1-Click Multi-PAN Allotment Check work?" to
                "Save your personal and family PAN cards once in the PAN Vault. When you tap 'Click to Check Allotment Status' on any Mainboard or SME IPO, IPO One automatically checks every saved PAN card against the official registrar (Link Intime, KFintech, or Bigshare) and displays ALLOTTED or NOT ALLOTTED against each PAN.",
            "What is Pre-Apply for Upcoming IPOs?" to
                "Pre-Apply lets you queue IPO applications across all your saved PAN cards even before the subscription window officially opens. Once the IPO opens, your ASBA UPI mandate is triggered so you never miss closing deadlines.",
            "How do I Request a New UPI Mandate if my mandate expired?" to
                "If your UPI mandate notification didn't arrive or expired, tap 'New Mandate' next to any PAN card under your Applied IPOs to update your UPI handle and re-queue the mandate request.",
            "Can I hide or mask my PAN numbers for privacy?" to
                "Yes! Tap the Eye ('Hide PANs') icon in the top bar, Allotment Overview card, or Multi-PAN Check sheet to mask all PAN numbers as ABCDE****F when viewing in public or sharing allotment screenshots.",
            "Why can't I apply multiple times with the same PAN card?" to
                "SEBI regulations strictly require a unique PAN linked to each Demat account. Multiple bids under the same PAN in the same IPO are rejected by the registrar. Use family members' separate PAN cards in the Family PAN Vault to apply across multiple lots legally."
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("narada_ipo_faq_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "PAN & IPO Allotment FAQs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Everything about multi-PAN allotment checks, UPI mandates & SME IPOs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            faqs.forEachIndexed { idx, (question, answer) ->
                val isExpanded = expandedIndex == idx
                if (idx > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            expandedIndex = if (isExpanded) -1 else idx
                        }
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = question,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse FAQ" else "Expand FAQ",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    AnimatedVisibility(visible = isExpanded) {
                        Text(
                            text = answer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
