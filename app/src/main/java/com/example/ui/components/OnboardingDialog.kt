package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("onboarding_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IpoOneLogo(size = 64.dp)

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Welcome to IPO One",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Futuristic Multi-Demat & Live GMP Suite",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(18.dp))

                OnboardingFeatureRow(
                    icon = Icons.Default.TrendingUp,
                    title = "Live GMP, Issue Size & Category Subscriptions",
                    desc = "Monitor Live Grey Market Premium, Issue Size (₹ Cr), and QIB, NII/HNI, Retail & Total subscription multipliers."
                )

                Spacer(modifier = Modifier.height(12.dp))

                OnboardingFeatureRow(
                    icon = Icons.Default.CalendarMonth,
                    title = "Full 6-Stage T+3 Crucial Dates",
                    desc = "Track Anchor Bid, Open, Close (5 PM Cutoff), Allotment (T+1), Refund (T+2) & Listing (T+3)."
                )

                Spacer(modifier = Modifier.height(12.dp))

                OnboardingFeatureRow(
                    icon = Icons.Default.Groups,
                    title = "⚡ 1-Click All Family Demats Batch Apply",
                    desc = "Prepare and log IPO bids across all configured family Demat & UPI accounts in a single click."
                )

                Spacer(modifier = Modifier.height(12.dp))

                OnboardingFeatureRow(
                    icon = Icons.Default.AccountBalance,
                    title = "1-Tap Official Allotment Check",
                    desc = "Copy masked PANs in 1 tap and open official KFin, Link Intime, BSE & NSE portals."
                )

                Spacer(modifier = Modifier.height(12.dp))

                OnboardingFeatureRow(
                    icon = Icons.Default.Lock,
                    title = "Android Keystore & Biometric Vault",
                    desc = "Zero UPI PIN/password storage, masked PANs by default, and local encrypted SQLite."
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("get_started_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Enter IPO One",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingFeatureRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}
