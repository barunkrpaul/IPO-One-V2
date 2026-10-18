package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.security.BiometricAuthHelper
import com.example.security.attemptAutoSignIn
import com.example.security.onGoogleSignInClicked
import com.example.ui.components.IpoOneLogo
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TealPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    initialMobileNumber: String = "",
    initialInvestorName: String = "",
    onAuthSuccess: (investorName: String, mobileNumber: String, investorCategory: String) -> Unit,
    onContinueLocalVaultOnly: (investorName: String, mobileNumber: String, investorCategory: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var investorName by remember { mutableStateOf(initialInvestorName) }
    var mobileNumber by remember { mutableStateOf(initialMobileNumber) }
    var selectedCategory by remember { mutableStateOf("RETAIL") }
    var isSigningIn by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var mobileValidationMessage by remember { mutableStateOf<String?>(null) }

    val biometricCapability = remember { BiometricAuthHelper.checkCapability(context) }

    // Attempt silent Google Sign-In on initial launch
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                onAuthSuccess(
                    investorName.ifBlank { "IPO Investor" },
                    formatIndianMobile(mobileNumber),
                    selectedCategory
                )
            },
            onUnauthenticated = {},
            scope = coroutineScope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF060B16),
                        Color(0xFF0E182C),
                        Color(0xFF080E1B)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Brand Logo & Futuristic Header
            IpoOneLogo(size = 72.dp)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "IPO One",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color(0xFFF8FAFC)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Multi-Demat IPO Terminal • Live GMP • Biometric Vault",
                    style = MaterialTheme.typography.bodySmall,
                    color = TealPrimary,
                    textAlign = TextAlign.Center
                )
            }

            // Security Architecture Pill
            Surface(
                shape = RoundedCornerShape(50),
                color = TealPrimary.copy(alpha = 0.12f),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = TealPrimary.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(50)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (biometricCapability.isHardwareReady) {
                            "BiometricPrompt Hardware Active • AES-256-GCM"
                        } else {
                            "Android Keystore AES-256-GCM • Biometric Vault Ready"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFE2E8F0)
                    )
                }
            }

            // Primary Sign-Up / Sign-In Card (Google + Mobile Number)
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131E33)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color(0xFF233554),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TealPrimary.copy(alpha = 0.16f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Investor Sign-Up & Cloud Sync",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFF8FAFC)
                            )
                            Text(
                                text = "Sign in with Google and link your +91 mobile number for UPI mandate tracking",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Investor Full Name
                    OutlinedTextField(
                        value = investorName,
                        onValueChange = { investorName = it },
                        label = { Text("Investor Full Name (Optional — auto-filled by Google)") },
                        placeholder = { Text("e.g., Barun Kr Paul") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = TealPrimary
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_name_input")
                    )

                    // Mobile Number (+91) Input
                    OutlinedTextField(
                        value = mobileNumber,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '+' || it == ' ' }.take(15)
                            mobileNumber = filtered
                            mobileValidationMessage = null
                        },
                        label = { Text("Mobile Number (+91 for UPI Mandate Alerts)") },
                        placeholder = { Text("e.g., 9876543210") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = TealPrimary
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_mobile_input")
                    )

                    if (mobileValidationMessage != null) {
                        Text(
                            text = mobileValidationMessage!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF59E0B)
                        )
                    }

                    // Default Investor Category Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Default Investor Category",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFCBD5E1)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "RETAIL" to "Retail (≤ ₹2L)",
                                "sNII" to "sNII (₹2L–₹10L)",
                                "bNII" to "bNII (> ₹10L)",
                                "EMPLOYEE" to "Employee"
                            ).forEach { (code, label) ->
                                FilterChip(
                                    selected = selectedCategory == code,
                                    onClick = { selectedCategory = code },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Error banner if Google Sign-In encounters an issue
                    if (authErrorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = authErrorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFCA5A5),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Primary Button: Sign in with Google (Credential Manager)
                    Button(
                        onClick = {
                            authErrorMessage = null
                            isSigningIn = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isSigningIn = false
                                    onAuthSuccess(
                                        investorName.trim(),
                                        formatIndianMobile(mobileNumber),
                                        selectedCategory
                                    )
                                },
                                onAuthError = { errorMsg ->
                                    isSigningIn = false
                                    authErrorMessage = errorMsg
                                },
                                scope = coroutineScope,
                                onAuthCancelled = {
                                    isSigningIn = false
                                }
                            )
                        },
                        enabled = !isSigningIn,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealPrimary,
                            contentColor = Color(0xFF04131A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color(0xFF04131A),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Connecting Google Credential...",
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign in with Google",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }

                    // Secondary Button: Mobile Number + On-Device Encrypted Vault Registration
                    OutlinedButton(
                        onClick = {
                            val digitsOnly = mobileNumber.filter { it.isDigit() }
                            val cleanDigits = if (digitsOnly.startsWith("91") && digitsOnly.length == 12) {
                                digitsOnly.drop(2)
                            } else {
                                digitsOnly
                            }
                            if (cleanDigits.length < 10) {
                                mobileValidationMessage = "Enter a valid 10-digit Indian mobile number to register via Mobile Vault, or tap 'Sign in with Google' above."
                                return@OutlinedButton
                            }
                            onContinueLocalVaultOnly(
                                investorName.trim().ifBlank { "Investor (+91 ${cleanDigits.takeLast(4)})" },
                                "+91 $cleanDigits",
                                selectedCategory
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("mobile_signup_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign Up with Mobile (+91 Local Encrypted Vault)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFE2E8F0)
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    // Privacy & Security Guarantees
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "PAN, Demat BO IDs & UPI IDs remain AES-256-GCM encrypted in Android Keystore.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Google Sign-In syncs your Investor Profile & IPO Watchlist to Firebase Firestore.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatIndianMobile(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    val tenDigits = if (digits.startsWith("91") && digits.length == 12) digits.drop(2) else digits.takeLast(10)
    return "+91 $tenDigits"
}
