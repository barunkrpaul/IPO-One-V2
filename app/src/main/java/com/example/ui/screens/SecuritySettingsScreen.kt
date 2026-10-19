package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.data.model.UserProfile
import com.example.util.BiometricAuthHelper

@Composable
fun SecuritySettingsScreen(
    userProfile: UserProfile?,
    userEmail: String?,
    userDisplayName: String?,
    onToggleBiometric: (Boolean) -> Unit,
    onLockAppNow: () -> Unit,
    onUpdateMobilePhone: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val capability = remember(context) { BiometricAuthHelper.checkCapability(context) }

    val biometricEnabled = userProfile?.biometricEnabled ?: false
    var biometricStatusMessage by remember { mutableStateOf<String?>(null) }
    var mobileInput by remember(userProfile?.phoneNumber) {
        mutableStateOf(userProfile?.phoneNumber ?: "")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("security_settings_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // BiometricPrompt Hardware Security Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric Security",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BiometricPrompt Vault Lock",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = capability.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (capability.canAuthenticate) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Require Biometric Unlock on App Launch",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Protects PAN numbers, Demat BO IDs, and ASBA UPI mandates using Android BiometricPrompt API.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { wantEnabled ->
                                if (wantEnabled && activity != null && capability.canAuthenticate) {
                                    BiometricAuthHelper.authenticate(
                                        activity = activity,
                                        title = "Confirm Biometric Lock Setup",
                                        subtitle = "Authenticate with Fingerprint or Face to enable IPO One Vault Lock",
                                        onSuccess = {
                                            biometricStatusMessage = "BiometricPrompt verified! App Lock is now ACTIVE."
                                            onToggleBiometric(true)
                                        },
                                        onError = { err ->
                                            biometricStatusMessage = "Biometric verification failed: $err"
                                        }
                                    )
                                } else {
                                    onToggleBiometric(wantEnabled)
                                    biometricStatusMessage = if (wantEnabled) {
                                        "Biometric Lock preference enabled in Cloud Profile."
                                    } else {
                                        "Biometric Lock disabled."
                                    }
                                }
                            },
                            modifier = Modifier.testTag("biometric_lock_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (activity != null) {
                                    BiometricAuthHelper.authenticate(
                                        activity = activity,
                                        title = "Verify Financial Vault Access",
                                        subtitle = "Testing AndroidX BiometricPrompt hardware authentication",
                                        onSuccess = {
                                            biometricStatusMessage = "BiometricPrompt Authentication Succeeded!"
                                        },
                                        onError = { err ->
                                            biometricStatusMessage = err
                                        }
                                    )
                                } else {
                                    biometricStatusMessage = "BiometricPrompt requires FragmentActivity host."
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_biometric_prompt_button")
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test BiometricPrompt")
                        }

                        OutlinedButton(
                            onClick = onLockAppNow,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lock_app_now_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock Vault Now")
                        }
                    }

                    if (biometricStatusMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = biometricStatusMessage!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Account & Mobile Linking Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Authenticated Investor Profile",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${userProfile?.displayName ?: userDisplayName ?: "Investor"} • ${userProfile?.email ?: userEmail ?: "Google Verified"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = mobileInput,
                        onValueChange = { mobileInput = it.filter { c -> c.isDigit() || c == '+' || c == '-' }.take(15) },
                        label = { Text("Linked Mobile Number for IPO Alerts (+91)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_mobile_input"),
                        trailingIcon = {
                            if (!userProfile?.phoneNumber.isNullOrBlank()) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (mobileInput.isNotBlank()) {
                                onUpdateMobilePhone(mobileInput.trim())
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_mobile_number_button")
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Verified Mobile Number")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out of IPO One", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Copyright & Developer Attribution Card
        item {
            com.example.ui.components.CopyrightAttributionCard(
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }
    }
}
