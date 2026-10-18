package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AllotmentStatus
import com.example.model.MandateStatus
import com.example.ui.theme.AmberPending
import com.example.ui.theme.CoralRejected
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightNavyBg
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealPrimary
import java.text.NumberFormat
import java.util.Locale

fun formatInr(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return formatter.format(amount).replace(".00", "")
}

/**
 * Bespoke vector-drawn emblem for IPO One:
 * Features a Midnight Navy & Slate hexagonal shield, an orbital Cyan/Teal ring,
 * ascending candlestick bars, and a geometric numeral '1' fused with a bullish arrow crown.
 */
@Composable
fun IpoOneLogo(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(MidnightNavyBg, SlateCard, Color(0xFF0D2B3E))
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(TealLight.copy(alpha = 0.8f), CyanAccent.copy(alpha = 0.5f))
                ),
                shape = RoundedCornerShape(size * 0.28f)
            )
            .semantics { contentDescription = "IPO One Official App Logo" }
            .testTag("ipo_one_logo"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.14f)) {
            val w = this.size.width
            val h = this.size.height

            // Outer Orbital Arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(CyanAccent.copy(alpha = 0.2f), TealLight, CyanAccent)
                ),
                startAngle = 155f,
                sweepAngle = 215f,
                useCenter = false,
                style = Stroke(width = w * 0.075f, cap = StrokeCap.Round)
            )

            // Left Ascending Candlestick Bar
            drawRoundRect(
                color = CyanAccent,
                topLeft = Offset(w * 0.17f, h * 0.48f),
                size = Size(w * 0.14f, h * 0.30f),
                cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
            )

            // Right Ascending Candlestick Bar
            drawRoundRect(
                color = TealPrimary,
                topLeft = Offset(w * 0.69f, h * 0.36f),
                size = Size(w * 0.14f, h * 0.42f),
                cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
            )

            // Central Geometric Numeral '1' Pillar
            drawRoundRect(
                color = Color(0xFFF8FAFC),
                topLeft = Offset(w * 0.41f, h * 0.24f),
                size = Size(w * 0.18f, h * 0.54f),
                cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
            )

            // Bullish Arrow Crown at Top of '1'
            val arrowPath = Path().apply {
                moveTo(w * 0.50f, h * 0.07f)
                lineTo(w * 0.72f, h * 0.30f)
                lineTo(w * 0.28f, h * 0.30f)
                close()
            }
            drawPath(
                path = arrowPath,
                brush = Brush.verticalGradient(
                    colors = listOf(TealLight, TealPrimary)
                )
            )

            // Base Pedestal Line
            drawRoundRect(
                color = TealLight,
                topLeft = Offset(w * 0.15f, h * 0.83f),
                size = Size(w * 0.70f, h * 0.07f),
                cornerRadius = CornerRadius(w * 0.035f, w * 0.035f)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("metric_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MandateStatusBadge(status: String) {
    val (bgColor, textColor, icon) = when (status) {
        MandateStatus.APPROVED.displayName -> Triple(
            EmeraldSuccess.copy(alpha = 0.15f),
            EmeraldSuccess,
            Icons.Default.CheckCircle
        )
        MandateStatus.PENDING.displayName -> Triple(
            AmberPending.copy(alpha = 0.15f),
            AmberPending,
            Icons.Default.Schedule
        )
        MandateStatus.EXPIRED.displayName, MandateStatus.REJECTED.displayName -> Triple(
            CoralRejected.copy(alpha = 0.15f),
            CoralRejected,
            Icons.Default.Info
        )
        else -> Triple(
            MaterialTheme.colorScheme.outlineVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Info
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun AllotmentStatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        AllotmentStatus.ALLOTTED.displayName -> Pair(EmeraldSuccess.copy(alpha = 0.2f), EmeraldSuccess)
        AllotmentStatus.NOT_ALLOTTED.displayName -> Pair(CoralRejected.copy(alpha = 0.15f), CoralRejected)
        AllotmentStatus.AWAITING.displayName -> Pair(CyanAccent.copy(alpha = 0.15f), CyanAccent)
        AllotmentStatus.REFUNDED.displayName -> Pair(AmberPending.copy(alpha = 0.15f), AmberPending)
        else -> Pair(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (actionText != null && onActionClick != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onActionClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
