package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AppSettings
import com.example.data.local.NotificationPrefs
import com.example.data.local.ThemeMode
import com.example.data.model.IpoBoardFilter
import com.example.notifications.IpoReminderWorker

private val RefreshChoices = listOf(0, 5, 15, 30)

/**
 * App-wide preferences: theme, launch board, auto-refresh, reminders and cache.
 * Account security lives one level down ([onOpenSecurity]).
 */
@Composable
fun AppSettingsScreen(
    settings: AppSettings,
    isGuest: Boolean,
    onThemeChange: (ThemeMode) -> Unit,
    onDefaultBoardChange: (IpoBoardFilter) -> Unit,
    onRefreshIntervalChange: (Int) -> Unit,
    onNotificationsChange: (NotificationPrefs) -> Unit,
    onClearCache: () -> Unit,
    onOpenSecurity: () -> Unit,
    onSignInRequested: () -> Unit
) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(IpoReminderWorker.canNotify(context)) }
    var pendingPrefs by remember { mutableStateOf<NotificationPrefs?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        pendingPrefs?.let { if (granted) onNotificationsChange(it) }
        pendingPrefs = null
    }

    // Turning a reminder on asks for the Android 13+ notification permission first
    fun updateNotifications(prefs: NotificationPrefs) {
        val needsPermission = prefs.anyEnabled && !IpoReminderWorker.canNotify(context) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        if (needsPermission) {
            pendingPrefs = prefs
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onNotificationsChange(prefs)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SettingsCard(title = stringResource(R.string.settings_appearance)) {
                listOf(
                    ThemeMode.SYSTEM to R.string.settings_theme_system,
                    ThemeMode.LIGHT to R.string.settings_theme_light,
                    ThemeMode.DARK to R.string.settings_theme_dark,
                    ThemeMode.BLACK to R.string.settings_theme_black
                ).forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = settings.themeMode == mode,
                                onClick = { onThemeChange(mode) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 2.dp)
                            .testTag("theme_${mode.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = settings.themeMode == mode, onClick = null)
                        Text(
                            text = stringResource(label),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        item {
            SettingsCard(title = stringResource(R.string.settings_dashboard)) {
                Text(
                    text = stringResource(R.string.settings_default_board),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IpoBoardFilter.entries.forEach { board ->
                        FilterChip(
                            selected = settings.defaultBoard == board,
                            onClick = { onDefaultBoardChange(board) },
                            label = { Text(board.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("default_board_${board.name.lowercase()}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.settings_refresh_interval),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RefreshChoices.forEach { minutes ->
                        FilterChip(
                            selected = settings.refreshIntervalMinutes == minutes,
                            onClick = { onRefreshIntervalChange(minutes) },
                            label = {
                                Text(
                                    if (minutes == 0) stringResource(R.string.settings_refresh_off)
                                    else stringResource(R.string.settings_refresh_minutes, minutes)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("refresh_interval_$minutes")
                        )
                    }
                }
            }
        }

        item {
            SettingsCard(title = stringResource(R.string.settings_notifications)) {
                if (isGuest) {
                    Text(
                        text = stringResource(R.string.settings_notifications_sign_in),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onSignInRequested) {
                        Text(stringResource(R.string.nav_sign_in))
                    }
                    return@SettingsCard
                }
                Text(
                    text = stringResource(R.string.settings_notifications_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val prefs = settings.notifications
                NotificationSwitch(R.string.settings_notify_opens, prefs.opensToday, "notify_opens") {
                    updateNotifications(prefs.copy(opensToday = it))
                }
                NotificationSwitch(R.string.settings_notify_closes, prefs.closesToday, "notify_closes") {
                    updateNotifications(prefs.copy(closesToday = it))
                }
                NotificationSwitch(R.string.settings_notify_allotment, prefs.allotmentOut, "notify_allotment") {
                    updateNotifications(prefs.copy(allotmentOut = it))
                }
                NotificationSwitch(R.string.settings_notify_listing, prefs.listingToday, "notify_listing") {
                    updateNotifications(prefs.copy(listingToday = it))
                }
                if (prefs.anyEnabled && !permissionGranted) {
                    Text(
                        text = stringResource(R.string.settings_notifications_denied),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                pendingPrefs = prefs
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag("allow_notifications")
                    ) {
                        Text(stringResource(R.string.settings_notifications_allow))
                    }
                }
            }
        }

        item {
            SettingsCard(title = stringResource(R.string.settings_storage)) {
                Text(
                    text = stringResource(R.string.settings_clear_cache_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onClearCache,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("clear_cache_button")
                ) {
                    Text(stringResource(R.string.settings_clear_cache))
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSecurity)
                    .testTag("open_security_settings"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_security),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_security_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            com.example.ui.components.CopyrightAttributionCard(modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun NotificationSwitch(label: Int, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
    }
}
