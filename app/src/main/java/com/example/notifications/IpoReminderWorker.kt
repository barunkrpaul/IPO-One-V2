package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppSettingsStore
import com.example.data.local.NotificationPrefs
import com.example.data.model.DueReminder
import com.example.data.model.IpoLifecycleStage
import com.example.data.model.ReminderType
import com.example.data.model.allotmentWatchCandidates
import com.example.data.model.dueDateReminders
import com.example.data.model.lifecycleStage
import com.example.data.model.pruneReminderKeys
import com.example.data.remote.InvestorGainClient
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

private val IST: ZoneId = ZoneId.of("Asia/Kolkata")

/**
 * Runs every 30 minutes (when online) and posts reminders for watchlisted IPOs
 * and IPOs with a logged application: opens today, closes today and listing
 * today in the morning, and "allotment out" once the IPO's page shows the
 * basis of allotment as finalised on allotment day. It never checks any PAN.
 */
class IpoReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = AppSettingsStore(applicationContext)
        val prefs = store.current().notifications
        val tracked = store.trackedIpoIds()
        if (!prefs.anyEnabled || tracked.isEmpty()) return Result.success()

        val now = ZonedDateTime.now(IST)
        val today = now.toLocalDate()
        val client = InvestorGainClient()
        val ipos = client.fetchLiveIpos().getOrElse { return Result.retry() }

        var sent = pruneReminderKeys(store.sentReminderKeys(), today)
        val due = dueDateReminders(ipos, tracked, prefs.enabledTypes(), today, now.hour, sent).toMutableList()

        if (prefs.allotmentOut) {
            for ((ipo, allotmentDate) in allotmentWatchCandidates(ipos, tracked, today, sent)) {
                val stage = ipo.lifecycleStage(today)
                val announced = stage == IpoLifecycleStage.ALLOTTED || stage == IpoLifecycleStage.LISTED ||
                    client.isAllotmentFinalized(ipo.investorGainUrl).getOrDefault(false)
                if (announced) due += DueReminder(ReminderType.ALLOTMENT_OUT, ipo.id, ipo.name, allotmentDate)
            }
        }

        if (due.isNotEmpty() && canNotify(applicationContext)) {
            ensureChannel(applicationContext)
            due.forEach { post(applicationContext, it) }
            sent = sent + due.map { it.key }
        }
        store.setSentReminderKeys(sent)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK = "ipo_reminders"
        const val CHANNEL_ID = "ipo_reminders"

        /** Starts (or keeps) the 30-minute reminder job. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<IpoReminderWorker>(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
        }

        fun canNotify(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        fun ensureChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.notif_channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply { description = context.getString(R.string.notif_channel_description) }
                )
            }
        }

        private fun post(context: Context, reminder: DueReminder) {
            val text = when (reminder.type) {
                ReminderType.OPENS_TODAY -> context.getString(R.string.notif_opens_today, reminder.ipoName)
                ReminderType.CLOSES_TODAY -> context.getString(R.string.notif_closes_today, reminder.ipoName)
                ReminderType.ALLOTMENT_OUT -> context.getString(R.string.notif_allotment_out, reminder.ipoName)
                ReminderType.LISTING_TODAY -> context.getString(R.string.notif_listing_today, reminder.ipoName)
            }
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_ipo)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            runCatching { NotificationManagerCompat.from(context).notify(reminder.key.hashCode(), notification) }
        }
    }
}

fun NotificationPrefs.enabledTypes(): Set<ReminderType> = buildSet {
    if (opensToday) add(ReminderType.OPENS_TODAY)
    if (closesToday) add(ReminderType.CLOSES_TODAY)
    if (allotmentOut) add(ReminderType.ALLOTMENT_OUT)
    if (listingToday) add(ReminderType.LISTING_TODAY)
}
