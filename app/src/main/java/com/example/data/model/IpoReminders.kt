package com.example.data.model

import java.time.LocalDate

enum class ReminderType { OPENS_TODAY, CLOSES_TODAY, ALLOTMENT_OUT, LISTING_TODAY }

data class DueReminder(
    val type: ReminderType,
    val ipoId: String,
    val ipoName: String,
    val date: LocalDate
) {
    /** Identifies a reminder so it is sent only once. */
    val key: String get() = "${type.name}:$ipoId:$date"
}

/** Morning reminders go out from this hour (IST) onwards. */
const val REMINDER_START_HOUR_IST = 8

/**
 * Date-based reminders due now for the tracked IPOs: opens / closes / listing
 * today, once it is past [REMINDER_START_HOUR_IST]. Already-sent keys are skipped.
 */
fun dueDateReminders(
    ipos: List<LiveIpoItem>,
    trackedIds: Set<String>,
    enabled: Set<ReminderType>,
    today: LocalDate,
    hourIst: Int,
    sentKeys: Set<String>
): List<DueReminder> {
    if (hourIst < REMINDER_START_HOUR_IST) return emptyList()
    return ipos.filter { it.id in trackedIds && it.cancelled != true }.flatMap { ipo ->
        listOfNotNull(
            ipo.openLocalDate(today)?.takeIf { it == today && ReminderType.OPENS_TODAY in enabled }
                ?.let { DueReminder(ReminderType.OPENS_TODAY, ipo.id, ipo.name, it) },
            ipo.closeLocalDate(today)?.takeIf { it == today && ReminderType.CLOSES_TODAY in enabled }
                ?.let { DueReminder(ReminderType.CLOSES_TODAY, ipo.id, ipo.name, it) },
            ipo.listingLocalDate(today)?.takeIf { it == today && ReminderType.LISTING_TODAY in enabled }
                ?.let { DueReminder(ReminderType.LISTING_TODAY, ipo.id, ipo.name, it) }
        )
    }.filter { it.key !in sentKeys }
}

/**
 * Tracked IPOs whose allotment is due today (or was due yesterday and has not
 * been announced yet); the worker checks each one's page for the basis of
 * allotment being finalised.
 */
fun allotmentWatchCandidates(
    ipos: List<LiveIpoItem>,
    trackedIds: Set<String>,
    today: LocalDate,
    sentKeys: Set<String>
): List<Pair<LiveIpoItem, LocalDate>> =
    ipos.filter { it.id in trackedIds && it.cancelled != true }.mapNotNull { ipo ->
        val allotment = ipo.allotmentLocalDate(today) ?: return@mapNotNull null
        val due = allotment == today || allotment == today.minusDays(1)
        val key = DueReminder(ReminderType.ALLOTMENT_OUT, ipo.id, ipo.name, allotment).key
        if (due && key !in sentKeys) ipo to allotment else null
    }

/** Drops sent-reminder keys older than [keepDays] so the stored set stays small. */
fun pruneReminderKeys(keys: Set<String>, today: LocalDate, keepDays: Long = 3): Set<String> =
    keys.filterTo(mutableSetOf()) { key ->
        val date = runCatching { LocalDate.parse(key.substringAfterLast(':')) }.getOrNull()
        date != null && !date.isBefore(today.minusDays(keepDays))
    }
