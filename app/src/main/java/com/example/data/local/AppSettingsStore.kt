package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.IpoBoardFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK, BLACK }

/** Which IPO reminders the user wants. */
data class NotificationPrefs(
    val opensToday: Boolean = true,
    val closesToday: Boolean = true,
    val allotmentOut: Boolean = true,
    val listingToday: Boolean = true
) {
    val anyEnabled: Boolean get() = opensToday || closesToday || allotmentOut || listingToday
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val defaultBoard: IpoBoardFilter = IpoBoardFilter.ALL,
    /** Automatic refresh of the live feed while the app is open; 0 = off. */
    val refreshIntervalMinutes: Int = 0,
    val notifications: NotificationPrefs = NotificationPrefs()
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ipo_one_settings")

/**
 * App preferences plus the small amount of state the reminder worker needs
 * while the app is closed (which IPOs to watch, which reminders were sent).
 */
class AppSettingsStore(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val board = stringPreferencesKey("default_board")
        val refresh = intPreferencesKey("refresh_interval_minutes")
        val notifyOpens = booleanPreferencesKey("notify_opens")
        val notifyCloses = booleanPreferencesKey("notify_closes")
        val notifyAllotment = booleanPreferencesKey("notify_allotment")
        val notifyListing = booleanPreferencesKey("notify_listing")
        val trackedIpos = stringSetPreferencesKey("tracked_ipo_ids")
        val sentReminders = stringSetPreferencesKey("sent_reminder_keys")
    }

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            themeMode = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            defaultBoard = p[Keys.board]?.let { runCatching { IpoBoardFilter.valueOf(it) }.getOrNull() }
                ?: IpoBoardFilter.ALL,
            refreshIntervalMinutes = p[Keys.refresh] ?: 0,
            notifications = NotificationPrefs(
                opensToday = p[Keys.notifyOpens] ?: true,
                closesToday = p[Keys.notifyCloses] ?: true,
                allotmentOut = p[Keys.notifyAllotment] ?: true,
                listingToday = p[Keys.notifyListing] ?: true
            )
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[Keys.theme] = mode.name }

    suspend fun setDefaultBoard(board: IpoBoardFilter) = store.edit { it[Keys.board] = board.name }

    suspend fun setRefreshInterval(minutes: Int) = store.edit { it[Keys.refresh] = minutes }

    suspend fun setNotifications(prefs: NotificationPrefs) = store.edit {
        it[Keys.notifyOpens] = prefs.opensToday
        it[Keys.notifyCloses] = prefs.closesToday
        it[Keys.notifyAllotment] = prefs.allotmentOut
        it[Keys.notifyListing] = prefs.listingToday
    }

    /** Watchlisted IPOs plus IPOs with a logged application; written by the app, read by the worker. */
    suspend fun setTrackedIpoIds(ids: Set<String>) = store.edit { it[Keys.trackedIpos] = ids }

    suspend fun trackedIpoIds(): Set<String> = store.data.first()[Keys.trackedIpos].orEmpty()

    suspend fun current(): AppSettings = settings.first()

    suspend fun sentReminderKeys(): Set<String> = store.data.first()[Keys.sentReminders].orEmpty()

    /** Remembers sent reminders; keys older than a few days are dropped by the caller. */
    suspend fun setSentReminderKeys(keys: Set<String>) = store.edit { it[Keys.sentReminders] = keys }
}
