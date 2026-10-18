package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.CloudSyncRepository
import com.example.security.BiometricAuthHelper
import com.example.security.onGoogleSignInClicked
import com.example.security.signOutGoogle
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.SettingsViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@Composable
fun ComplianceSettingsScreen(
    settingsViewModel: SettingsViewModel,
    currentTheme: AppThemeOption,
    onOpenThemeSelector: () -> Unit,
    onLockAppNow: () -> Unit = {},
    currentUser: FirebaseUser? = null,
    investorName: String = "Barun Kr Paul",
    mobileNumber: String = "+91 98765 43210",
    investorCategory: String = "RETAIL",
    onUpdateInvestorProfile: (name: String, mobile: String, category: String) -> Unit = { _, _, _ -> },
    onOpenAuthScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val exportJson by settingsViewModel.exportJson.collectAsStateWithLifecycle()
    val actionMessage by settingsViewModel.actionMessage.collectAsStateWithLifecycle()
    val privacyCounts by settingsViewModel.privacyCounts.collectAsStateWithLifecycle()

    var showWipeConfirmDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    var biometricLockEnabled by remember { mutableStateOf(true) }
    var selectedTimeout by remember { mutableStateOf("5m") }
    var selectedRetention by remember { mutableStateOf("Manual") }

    val biometricCapability = remember { BiometricAuthHelper.checkCapability(context) }
    var lastBiometricResult by remember {
        mutableStateOf<String?>(null)
    }

    var editableName by remember(investorName, currentUser) {
        mutableStateOf(currentUser?.displayName?.takeIf { it.isNotBlank() } ?: investorName)
    }
    var editableMobile by remember(mobileNumber) {
        mutableStateOf(mobileNumber)
    }
    var cloudSyncStatus by remember { mutableStateOf<String?>(null) }
    var isSyncingCloud by remember { mutableStateOf(false) }

    actionMessage?.let { msg ->
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        settingsViewModel.clearActionMessage()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("compliance_settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Header
        item {
            Column {
                Text(
                    text = "Privacy, Security & Account",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "BiometricPrompt API lock, Google & Mobile (+91) Sign-Up, Android Keystore & themes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Google & Mobile (+91) Account & Cloud Sync Card
        item {
            SectionHeader(title = "Investor Sign-Up (Google & Mobile +91)")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_signup_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (currentUser != null) EmeraldSuccess.copy(alpha = 0.18f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = if (currentUser != null) EmeraldSuccess else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (currentUser != null) {
                                        currentUser.displayName?.takeIf { it.isNotBlank() } ?: editableName
                                    } else {
                                        "$editableName (Local Mobile Vault)"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (currentUser != null) {
                                        "Google Connected: ${currentUser.email ?: currentUser.uid.take(10)} • $investorCategory"
                                    } else {
                                        "Mobile Vault: $editableMobile • Sign in with Google for Cloud Sync"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (currentUser != null) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editableName,
                            onValueChange = { editableName = it },
                            label = { Text("Investor Name") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_investor_name_input")
                        )
                        OutlinedTextField(
                            value = editableMobile,
                            onValueChange = { editableMobile = it.take(16) },
                            label = { Text("Mobile (+91)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_mobile_input")
                        )
                    }

                    if (cloudSyncStatus != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldSuccess.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = cloudSyncStatus!!,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentUser == null) {
                            Button(
                                onClick = {
                                    onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            onUpdateInvestorProfile(editableName, editableMobile, investorCategory)
                                            cloudSyncStatus = "Signed in with Google & linked mobile $editableMobile!"
                                        },
                                        onAuthError = { err ->
                                            cloudSyncStatus = "Google Sign-In note: $err"
                                        },
                                        scope = coroutineScope
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_google_sign_in_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign in with Google", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    isSyncingCloud = true
                                    onUpdateInvestorProfile(editableName, editableMobile, investorCategory)
                                    coroutineScope.launch {
                                        val repo = CloudSyncRepository(context)
                                        val res = repo.saveUserProfile(
                                            displayName = editableName,
                                            email = currentUser.email ?: "",
                                            mobileNumber = editableMobile,
                                            investorCategory = investorCategory,
                                            syncedDematCount = privacyCounts.dematCount,
                                            syncedApplicationCount = privacyCounts.applicationsCount
                                        )
                                        isSyncingCloud = false
                                        cloudSyncStatus = if (res.isSuccess) {
                                            "Synced profile & mobile ($editableMobile) to Firestore Cloud!"
                                        } else {
                                            "Saved locally (${res.exceptionOrNull()?.localizedMessage ?: "Cloud sync error"})"
                                        }
                                    }
                                },
                                enabled = !isSyncingCloud,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sync_cloud_profile_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isSyncingCloud) "Syncing..." else "Sync Mobile & Cloud",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (currentUser != null) {
                                    signOutGoogle(
                                        credentialManager = credentialManager,
                                        onSignOutComplete = {
                                            cloudSyncStatus = "Signed out of Google Cloud session."
                                        },
                                        scope = coroutineScope
                                    )
                                } else {
                                    onUpdateInvestorProfile(editableName, editableMobile, investorCategory)
                                    cloudSyncStatus = "Saved mobile ($editableMobile) to local Keystore vault!"
                                    onOpenAuthScreen()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("switch_or_signout_account_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (currentUser != null) "Sign Out Google" else "Full Sign-Up Screen",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Color Theme Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenThemeSelector() }
                    .testTag("appearance_theme_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(currentTheme.primaryColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = currentTheme.primaryColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Color Combination",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${currentTheme.title} (${currentTheme.subtitle})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenThemeSelector,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Change", fontSize = 12.sp)
                    }
                }
            }
        }

        // BiometricPrompt API Lock, Verification & Session Timeout Card
        item {
            SectionHeader(title = "BiometricPrompt API & App Lock Security")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "BiometricPrompt App Unlock",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = biometricCapability.statusTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (biometricCapability.isHardwareReady) EmeraldSuccess else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Switch(
                            checked = biometricLockEnabled,
                            onCheckedChange = { targetState ->
                                val activity = context as? FragmentActivity
                                if (activity != null) {
                                    BiometricAuthHelper.authenticate(
                                        activity = activity,
                                        title = if (targetState) "Enable Biometric App Lock" else "Disable Biometric App Lock",
                                        subtitle = "Confirm with Fingerprint, Face, or Device PIN",
                                        onSuccess = { msg ->
                                            biometricLockEnabled = targetState
                                            lastBiometricResult = msg
                                        },
                                        onError = { err ->
                                            lastBiometricResult = err
                                        }
                                    )
                                } else {
                                    biometricLockEnabled = targetState
                                }
                            },
                            modifier = Modifier.testTag("biometric_lock_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = biometricCapability.statusDetail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (lastBiometricResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldSuccess.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("biometric_verification_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = lastBiometricResult!!,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Auto-Lock Session Timeout:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Immediate", "5m", "15m", "30m").forEach { option ->
                            FilterChip(
                                selected = selectedTimeout == option,
                                onClick = { selectedTimeout = option },
                                label = { Text(option, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val activity = context as? FragmentActivity
                                if (activity != null) {
                                    BiometricAuthHelper.authenticate(
                                        activity = activity,
                                        title = "Verify BiometricPrompt Vault Access",
                                        subtitle = "Authenticate to unlock sensitive PAN & Demat data",
                                        onSuccess = { msg ->
                                            lastBiometricResult = msg
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { err ->
                                            lastBiometricResult = err
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("verify_biometric_prompt_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify Biometric", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onLockAppNow,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lock_session_now_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock App Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Privacy Dashboard Card
        item {
            SectionHeader(title = "Privacy Dashboard & Data Inventory")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "On-Device Privacy & Encryption Vault",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Android Keystore AES-256-GCM • Zero PII in Analytics",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Saved Data Counts Grid
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PrivacyCountCol("Profiles", "${privacyCounts.membersCount}")
                            PrivacyCountCol("Demats", "${privacyCounts.dematCount}")
                            PrivacyCountCol("UPI VPAs", "${privacyCounts.upiCount}")
                            PrivacyCountCol("IPO Bids", "${privacyCounts.applicationsCount}")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Data Retention Policy:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Manual", "90 Days", "1 Year").forEach { policy ->
                            FilterChip(
                                selected = selectedRetention == policy,
                                onClick = { selectedRetention = policy },
                                label = { Text(policy, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SafeguardItem(
                        title = "Zero Sensitive Credential Storage",
                        description = "IPO One NEVER collects or stores UPI PINs, passwords, net-banking logins, or OTPs."
                    )
                    SafeguardItem(
                        title = "Masked Identifiers by Default",
                        description = "PANs and Demat BOIDs are masked across all screens and exports by default until explicitly revealed."
                    )
                    SafeguardItem(
                        title = "Zero Page Scraping & Zero PII Telemetry",
                        description = "No background scraping or CAPTCHA bypassing. Analytics are 100% free of PANs, account numbers, UPI IDs, and application references."
                    )
                }
            }
        }

        // SEBI & NPCI Regulatory Citations Card
        item {
            SectionHeader(title = "Regulatory Citations & Guidelines")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    RegulatoryItem(
                        tag = "SEBI Circular SEBI/HO/CFD/DIL2/CIR/P/2022/45",
                        title = "UPI as Payment Mechanism in Public Issues",
                        body = "Requires eligible investor identity matching in ASBA/UPI public issue bids to maintain auditability."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    RegulatoryItem(
                        tag = "NPCI / RBI Mandate Limit",
                        title = "₹5,00,000 UPI IPO Mandate Ceiling",
                        body = "Retail & sHNI investors can block up to ₹5,00,000 per IPO application via UPI. Applications exceeding ₹5 Lakh must use Bank ASBA net banking."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    RegulatoryItem(
                        tag = "SEBI Master Circular (T+3 Mandate)",
                        title = "Listing within T+3 Business Days",
                        body = "SEBI made T+3 timeline mandatory from Dec 1, 2023. Allotment is finalized on T+1, funds unblocked on T+2, and shares list on T+3."
                    )
                }
            }
        }

        // Stage 1 vs Stage 2 Feature-by-Feature Matrix
        item {
            SectionHeader(title = "Stage 1 (Tracker) vs Stage 2 (API Matrix)")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Authorized Application Integrations Matrix",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Status of manual tracker features vs written partner API authorization:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    MatrixRow(
                        feature = "Personal Multi-Demat & Live GMP Tracker",
                        stage = "Stage 1 (Active)",
                        status = "Working Manually (Room DB)"
                    )
                    MatrixRow(
                        feature = "Masked PAN & UPI ID Vault",
                        stage = "Stage 1 (Active)",
                        status = "Working Locally (Keystore)"
                    )
                    MatrixRow(
                        feature = "Official Registrar & BSE/NSE Lookup",
                        stage = "Stage 1 (Active)",
                        status = "Documented Deep Link (No scrape)"
                    )
                    MatrixRow(
                        feature = "Broker Bid Push (Zerodha, Angel, Groww)",
                        stage = "Stage 2 (Contract)",
                        status = "Requires Broker Partner API"
                    )
                    MatrixRow(
                        feature = "Live Registrar Allotment Webhook",
                        stage = "Stage 2 (Contract)",
                        status = "Requires Registrar B2B Agreement"
                    )
                }
            }
        }

        // Data Export & Backup
        item {
            SectionHeader(title = "Data Portability & Backup (DPDP Act)")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Export On-Device Data",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Generate a complete JSON backup of all family profiles (PANs masked), Demat accounts, and IPO application histories.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { settingsViewModel.generateDataExport() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_data_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Data as JSON")
                    }

                    if (exportJson != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "JSON Payload Ready (${exportJson!!.length} bytes)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldSuccess
                                    )
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("IPO One Backup", exportJson)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Backup JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy JSON",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = exportJson!!.take(280) + if (exportJson!!.length > 280) "..." else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Free Deployment & Public Distribution Guide
        item {
            SectionHeader(title = "Free GitHub Release Guide (github.com/barunkrpaul)")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Publish IPO One on GitHub Releases (100% Free)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Your project now includes a pre-configured GitHub Actions workflow (.github/workflows/release-apk.yml) and README badge for github.com/barunkrpaul:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    RegulatoryItem(
                        tag = "STEP 1 • PUSH CODE & EXPORT APK FROM AI STUDIO",
                        title = "Push to github.com/barunkrpaul/ipo-one",
                        body = "Click the Settings (gear) or GitHub icon in the top-right corner of AI Studio → select 'Push to GitHub' → authorize your account (barunkrpaul) and create repository 'ipo-one'. Also click 'Download / Build APK' to save the .apk file to your computer."
                    )
                    RegulatoryItem(
                        tag = "STEP 2 • CREATE A PUBLIC RELEASE ON GITHUB",
                        title = "Open github.com/barunkrpaul/ipo-one/releases/new",
                        body = "In your GitHub repository, click 'Releases' on the right sidebar → click 'Create a new release'. Type 'v1.0.0' in 'Choose a tag' (click '+ Create new tag: v1.0.0 on publish') and set the Release title to 'IPO One v1.0.0 (Official Android APK)'."
                    )
                    RegulatoryItem(
                        tag = "STEP 3 • UPLOAD APK & SHARE PERMANENT LINK",
                        title = "Attach APK & Click 'Publish release'",
                        body = "Drag and drop your downloaded .apk file into the 'Attach binaries by dropping them here' box, then click 'Publish release'. Anyone can now download your app for free at: https://github.com/barunkrpaul/ipo-one/releases/latest"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/barunkrpaul"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_github_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open GitHub Profile",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open My GitHub", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val releaseUrl = "https://github.com/barunkrpaul/ipo-one/releases/latest"
                                clipboard.setPrimaryClip(ClipData.newPlainText("IPO One Public Download URL", releaseUrl))
                                Toast.makeText(context, "Public Release Link copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_github_release_url_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Release Link",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Share URL", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Data Reset & Erasure (Right to be Forgotten)
        item {
            SectionHeader(title = "Account & Database Deletion")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Reset Sample Data or Delete All Records",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Restore realistic demonstration IPOs and profiles, or permanently erase all local personal data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showResetConfirmDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reset_sample_data_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Sample", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { showWipeConfirmDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wipe_all_data_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete All Data", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }

    // Wipe Confirm Dialog
    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmDialog = false },
            title = { Text("Erase All Personal Data?") },
            text = { Text("This will permanently delete all Demat accounts, family members, and application history from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.wipeAllUserData()
                        showWipeConfirmDialog = false
                    }
                ) {
                    Text("Delete Everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Confirm Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset to Realistic Sample Data?") },
            text = { Text("This will replace current applications with realistic active, upcoming, and closed IPO sample data.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.resetToSampleData()
                        showResetConfirmDialog = false
                    }
                ) {
                    Text("Reset Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PrivacyCountCol(label: String, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SafeguardItem(title: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = EmeraldSuccess,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp)
        )
    }
}

@Composable
fun RegulatoryItem(tag: String, title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = tag,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MatrixRow(feature: String, stage: String, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = feature,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (stage.contains("Stage 1")) EmeraldSuccess else MaterialTheme.colorScheme.secondary
        )
    }
}
