package com.example.data.model

import java.time.LocalDate
import java.time.MonthDay
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Six-stage IPO lifecycle used to group the dashboard. Declaration order is the
 * order in which dashboard sections are shown.
 */
enum class IpoLifecycleStage {
    UPCOMING,
    OPEN,
    CLOSED, // bidding over, allotment pending
    ALLOTTED, // basis of allotment out, listing pending
    LISTED,
    CANCELLED // withdrawn or postponed
}

enum class KeyDateKind { OPENS, CLOSES, ALLOTMENT, LISTING, LISTED }

data class IpoKeyDate(val kind: KeyDateKind, val date: LocalDate)

/**
 * Hand-maintained values from Firestore `ipo_extras/{ipoId}` for data that
 * InvestorGain does not provide. Every field is optional.
 */
data class IpoExtras(
    val priceBandLow: Int? = null,
    val preApplyOpen: Boolean? = null,
    val listingPrice: Double? = null,
    val currentPrice: Double? = null,
    val cancelled: Boolean? = null
) {
    companion object {
        fun fromMap(data: Map<String, Any?>): IpoExtras = IpoExtras(
            priceBandLow = data["priceBandLow"].asDouble()?.toInt(),
            preApplyOpen = data["preApplyOpen"].asBoolean(),
            listingPrice = data["listingPrice"].asDouble(),
            currentPrice = data["currentPrice"].asDouble(),
            cancelled = data["cancelled"].asBoolean()
        )

        private fun Any?.asDouble(): Double? = when (this) {
            is Number -> toDouble()
            is String -> trim().replace(",", "").toDoubleOrNull()
            else -> null
        }

        private fun Any?.asBoolean(): Boolean? = when (this) {
            is Boolean -> this
            is String -> when (trim().lowercase()) {
                "true", "yes", "1" -> true
                "false", "no", "0" -> false
                else -> null
            }
            else -> null
        }
    }
}

fun LiveIpoItem.withExtras(extras: IpoExtras?): LiveIpoItem {
    if (extras == null) return this
    return copy(
        priceBandLow = extras.priceBandLow ?: priceBandLow,
        preApplyOpen = extras.preApplyOpen ?: preApplyOpen,
        listingPrice = extras.listingPrice ?: listingPrice,
        currentPrice = extras.currentPrice ?: currentPrice,
        cancelled = extras.cancelled ?: cancelled
    )
}

object IpoDates {
    private fun formatter(pattern: String): DateTimeFormatter =
        DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern(pattern)
            .toFormatter(Locale.ENGLISH)

    private val fullDateFormats = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.BASIC_ISO_DATE,
        formatter("d-M-yyyy"),
        formatter("d/M/yyyy"),
        formatter("d-MMM-yyyy"),
        formatter("d MMM yyyy"),
        formatter("d MMM, yyyy"),
        formatter("MMM d, yyyy"),
        formatter("EEE, MMM d, yyyy"),
        formatter("EEE, d MMM yyyy")
    )

    private val yearlessFormats = listOf(
        formatter("d-M"),
        formatter("d/M"),
        formatter("d-MMM"),
        formatter("d MMM"),
        formatter("MMM d"),
        formatter("EEE, d MMM"),
        formatter("EEE, MMM d")
    )

    /**
     * Parses the date strings seen in the InvestorGain feed ("2026-10-21",
     * "21-10", "21-Oct", "21 Oct 2026", ...). Strings without a year are placed
     * in the year that puts them closest to [reference].
     */
    fun parse(raw: String?, reference: LocalDate): LocalDate? {
        val clean = raw
            ?.substringBefore("(")
            ?.substringBefore("T")
            ?.trim()
            ?.trimEnd(',', '.')
            .orEmpty()
        if (clean.isEmpty() || clean == "-" || clean.equals("TBA", ignoreCase = true)) return null

        for (f in fullDateFormats) {
            runCatching { return LocalDate.parse(clean, f) }
        }
        for (f in yearlessFormats) {
            val monthDay = runCatching { MonthDay.from(f.parse(clean)) }.getOrNull() ?: continue
            return (reference.year - 1..reference.year + 1)
                .map { monthDay.atYear(it) }
                .minByOrNull { kotlin.math.abs(ChronoUnit.DAYS.between(reference, it)) }
        }
        return null
    }

    private val shortFormat = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
    private val longFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

    /** "Wed, 21 Oct" in the current year, "Wed, 21 Oct 2027" otherwise. */
    fun formatHuman(date: LocalDate, today: LocalDate): String =
        if (date.year == today.year) date.format(shortFormat) else date.format(longFormat)
}

fun LiveIpoItem.openLocalDate(today: LocalDate): LocalDate? =
    IpoDates.parse(sortOpenDate, today) ?: IpoDates.parse(openDate, today)

fun LiveIpoItem.closeLocalDate(today: LocalDate): LocalDate? =
    IpoDates.parse(sortCloseDate, today) ?: IpoDates.parse(closeDate, openLocalDate(today) ?: today)

fun LiveIpoItem.allotmentLocalDate(today: LocalDate): LocalDate? =
    IpoDates.parse(boaDate, closeLocalDate(today) ?: today)

fun LiveIpoItem.listingLocalDate(today: LocalDate): LocalDate? =
    IpoDates.parse(listingDate, closeLocalDate(today) ?: today)

/** Stage implied by the InvestorGain status code alone, or null for unknown codes. */
fun stageFromStatusCode(statusCode: String): IpoLifecycleStage? {
    val code = statusCode.trim().uppercase()
    return when {
        code == "U" -> IpoLifecycleStage.UPCOMING
        code == "O" || code == "CT" -> IpoLifecycleStage.OPEN
        code == "C" -> IpoLifecycleStage.CLOSED
        code == "A" -> IpoLifecycleStage.ALLOTTED
        code.startsWith("L") -> IpoLifecycleStage.LISTED
        else -> null
    }
}

fun LiveIpoItem.lifecycleStage(today: LocalDate = LocalDate.now()): IpoLifecycleStage {
    if (cancelled == true) return IpoLifecycleStage.CANCELLED

    val listing = listingLocalDate(today)
    val allotment = allotmentLocalDate(today)
    val listedByDate = listing != null && !today.isBefore(listing)
    val allottedByDate = allotment != null && today.isAfter(allotment)

    return when (stageFromStatusCode(statusCode)) {
        IpoLifecycleStage.UPCOMING -> IpoLifecycleStage.UPCOMING
        IpoLifecycleStage.OPEN -> IpoLifecycleStage.OPEN
        IpoLifecycleStage.LISTED -> IpoLifecycleStage.LISTED
        IpoLifecycleStage.CLOSED, IpoLifecycleStage.ALLOTTED -> when {
            listedByDate -> IpoLifecycleStage.LISTED
            allottedByDate || statusCode.equals("A", ignoreCase = true) -> IpoLifecycleStage.ALLOTTED
            else -> IpoLifecycleStage.CLOSED
        }
        else -> {
            val open = openLocalDate(today)
            val close = closeLocalDate(today)
            when {
                listedByDate -> IpoLifecycleStage.LISTED
                allottedByDate -> IpoLifecycleStage.ALLOTTED
                close != null && today.isAfter(close) -> IpoLifecycleStage.CLOSED
                open != null && !today.isBefore(open) -> IpoLifecycleStage.OPEN
                else -> IpoLifecycleStage.UPCOMING
            }
        }
    }
}

/** The single most relevant upcoming (or, for listed IPOs, past) date for the card. */
fun LiveIpoItem.nextKeyDate(stage: IpoLifecycleStage, today: LocalDate): IpoKeyDate? = when (stage) {
    IpoLifecycleStage.UPCOMING -> openLocalDate(today)?.let { IpoKeyDate(KeyDateKind.OPENS, it) }
    IpoLifecycleStage.OPEN -> closeLocalDate(today)?.let { IpoKeyDate(KeyDateKind.CLOSES, it) }
    IpoLifecycleStage.CLOSED -> allotmentLocalDate(today)?.let { IpoKeyDate(KeyDateKind.ALLOTMENT, it) }
        ?: listingLocalDate(today)?.let { IpoKeyDate(KeyDateKind.LISTING, it) }
    IpoLifecycleStage.ALLOTTED -> listingLocalDate(today)?.let { IpoKeyDate(KeyDateKind.LISTING, it) }
    IpoLifecycleStage.LISTED -> listingLocalDate(today)?.let { IpoKeyDate(KeyDateKind.LISTED, it) }
    IpoLifecycleStage.CANCELLED -> null
}

/** Total subscription as a number ("7.78x" -> 7.78), or null when not yet available. */
fun LiveIpoItem.subscriptionTimes(): Double? =
    Regex("[0-9]+(?:\\.[0-9]+)?").find(totalSubscription.replace(",", ""))
        ?.value
        ?.toDoubleOrNull()
        ?.takeIf { it > 0.0 }

/** Groups IPOs by stage in dashboard order, keeping the incoming order inside each group. */
fun groupByLifecycleStage(
    ipos: List<LiveIpoItem>,
    today: LocalDate
): List<Pair<IpoLifecycleStage, List<LiveIpoItem>>> {
    val grouped = ipos.groupBy { it.lifecycleStage(today) }
    return IpoLifecycleStage.entries.mapNotNull { stage ->
        grouped[stage]?.takeIf { it.isNotEmpty() }?.let { stage to it }
    }
}
