package com.example.data.model

import java.time.LocalDate

/** Subscription rows in display order. [key] is the field name under `ipo_extras/{id}.subscription`. */
enum class SubscriptionCategory(val key: String, val isSubRow: Boolean = false) {
    QIB("qib"),
    QIB_FII("qibFii", isSubRow = true),
    QIB_DFI("qibDfi", isSubRow = true),
    QIB_MF("qibMf", isSubRow = true),
    QIB_OTHER("qibOther", isSubRow = true),
    NII("nii"),
    BNII("bnii", isSubRow = true),
    SNII("snii", isSubRow = true),
    RII("rii"),
    EMPLOYEE("employee"),
    SHAREHOLDER("shareholder"),
    TOTAL("total")
}

data class SubscriptionFigures(
    val quotaPct: Double? = null,
    val sharesOfferedCr: Double? = null,
    val sharesAppliedCr: Double? = null,
    val applicationsReserved: Long? = null,
    val applicationsReceived: Long? = null,
    val times: Double? = null
) {
    /** Times subscribed by number of applications, when both counts are known. */
    val applicationTimes: Double?
        get() {
            val reserved = applicationsReserved ?: return null
            val received = applicationsReceived ?: return null
            return if (reserved > 0) received.toDouble() / reserved else null
        }
}

private fun parseTimes(raw: String): Double? =
    Regex("[0-9]+(?:\\.[0-9]+)?").find(raw.replace(",", ""))?.value?.toDoubleOrNull()

/**
 * Rows for the subscription table. InvestorGain supplies "times subscribed" for
 * QIB, NII, bNII, sNII, RII and total; everything else comes from [extras].
 * Shareholder is only included when the extras have it.
 */
fun subscriptionRows(
    ipo: LiveIpoItem,
    extras: IpoExtras?
): List<Pair<SubscriptionCategory, SubscriptionFigures>> {
    val fromExtras = extras?.subscription.orEmpty()
    val feedTimes = mapOf(
        SubscriptionCategory.QIB to ipo.qibSubscription,
        SubscriptionCategory.NII to ipo.niiSubscription,
        SubscriptionCategory.BNII to ipo.bhniSubscription,
        SubscriptionCategory.SNII to ipo.shniSubscription,
        SubscriptionCategory.RII to ipo.riiSubscription,
        SubscriptionCategory.TOTAL to ipo.totalSubscription
    ).mapValues { parseTimes(it.value) }

    return SubscriptionCategory.entries
        .filter { it != SubscriptionCategory.SHAREHOLDER || fromExtras.containsKey(it) }
        .map { category ->
            val base = fromExtras[category] ?: SubscriptionFigures()
            category to base.copy(times = base.times ?: feedTimes[category])
        }
}

enum class TimelineEvent {
    OPENS,
    CLOSES,
    ALLOTMENT,
    SETTLEMENT,
    LISTING,
    MANDATE_END,
    ANCHOR_50,
    ANCHOR_100
}

data class TimelineEntry(val event: TimelineEvent, val date: LocalDate, val estimated: Boolean = false)

/**
 * Timeline rows in display order; rows without a date are omitted. Mandate end
 * falls back to close + 14 days and anchor lock-ins to listing + 30 / + 90 days
 * (anchor rows only for IPOs with an anchor book), all flagged as estimated.
 */
fun ipoTimeline(ipo: LiveIpoItem, extras: IpoExtras?, today: LocalDate): List<TimelineEntry> {
    val open = ipo.openLocalDate(today)
    val close = ipo.closeLocalDate(today)
    val allotment = ipo.allotmentLocalDate(today)
    val listing = ipo.listingLocalDate(today)

    fun entry(event: TimelineEvent, actual: LocalDate?, estimate: LocalDate? = null): TimelineEntry? = when {
        actual != null -> TimelineEntry(event, actual)
        estimate != null -> TimelineEntry(event, estimate, estimated = true)
        else -> null
    }

    val hasAnchorBook = ipo.hasAnchor ||
        extras?.anchorLockIn50Date != null ||
        extras?.anchorLockIn100Date != null

    return listOfNotNull(
        entry(TimelineEvent.OPENS, open),
        entry(TimelineEvent.CLOSES, close),
        entry(TimelineEvent.ALLOTMENT, allotment),
        entry(TimelineEvent.SETTLEMENT, extras?.settlementDate),
        entry(TimelineEvent.LISTING, listing),
        entry(TimelineEvent.MANDATE_END, extras?.mandateEndDate, close?.plusDays(14)),
        if (hasAnchorBook) entry(TimelineEvent.ANCHOR_50, extras?.anchorLockIn50Date, listing?.plusDays(30)) else null,
        if (hasAnchorBook) entry(TimelineEvent.ANCHOR_100, extras?.anchorLockIn100Date, listing?.plusDays(90)) else null
    )
}

enum class ApplicationCategory { RII, SNII, BNII, EMPLOYEE, SHAREHOLDER }

data class ApplicationSize(val lots: Int, val shares: Int, val amount: Long)

data class ApplicationSizeRow(
    val category: ApplicationCategory,
    val min: ApplicationSize?,
    val max: ApplicationSize?
)

private const val TWO_LAKH = 200_000L
private const val FIVE_LAKH = 500_000L
private const val TEN_LAKH = 1_000_000L

/**
 * Min/max bid per category at the upper price band under SEBI limits:
 * RII up to ₹2 lakh, sNII above ₹2 lakh up to ₹10 lakh, bNII above ₹10 lakh
 * (max depends on the NII quota, so it is left empty), employee up to ₹5 lakh,
 * shareholder up to ₹2 lakh. SME issues follow the 2025 SME rules: retail bids
 * exactly two lots and NII bids start at three lots.
 */
fun applicationSizes(
    lotSize: Int,
    upperPrice: Int,
    isSme: Boolean,
    includeEmployee: Boolean,
    includeShareholder: Boolean
): List<ApplicationSizeRow> {
    if (lotSize <= 0 || upperPrice <= 0) return emptyList()
    val lotCost = lotSize.toLong() * upperPrice

    fun size(lots: Int): ApplicationSize = ApplicationSize(lots, lots * lotSize, lots * lotCost)
    fun lotsWithin(limit: Long): Int = (limit / lotCost).toInt()
    fun lotsAbove(limit: Long): Int = (limit / lotCost).toInt() + 1
    fun range(category: ApplicationCategory, minLots: Int, maxLots: Int?): ApplicationSizeRow =
        if (minLots < 1 || (maxLots != null && maxLots < minLots)) {
            ApplicationSizeRow(category, null, null)
        } else {
            ApplicationSizeRow(category, size(minLots), maxLots?.let { size(it) })
        }

    val rows = mutableListOf<ApplicationSizeRow>()
    if (isSme) {
        rows += range(ApplicationCategory.RII, 2, 2)
        rows += range(ApplicationCategory.SNII, maxOf(3, lotsAbove(TWO_LAKH)), lotsWithin(TEN_LAKH))
        rows += range(ApplicationCategory.BNII, maxOf(3, lotsAbove(TEN_LAKH)), null)
    } else {
        rows += range(ApplicationCategory.RII, 1, lotsWithin(TWO_LAKH))
        rows += range(ApplicationCategory.SNII, lotsAbove(TWO_LAKH), lotsWithin(TEN_LAKH))
        rows += range(ApplicationCategory.BNII, lotsAbove(TEN_LAKH), null)
    }
    if (includeEmployee) rows += range(ApplicationCategory.EMPLOYEE, 1, lotsWithin(FIVE_LAKH))
    if (includeShareholder) rows += range(ApplicationCategory.SHAREHOLDER, 1, lotsWithin(TWO_LAKH))
    return rows
}

/** "BSE, NSE" / "NSE SME" / "BSE SME" -> display chips. */
fun exchangeChips(listingExchange: String, isSme: Boolean): List<String> {
    val text = listingExchange.uppercase()
    val bse = text.contains("BSE")
    val nse = text.contains("NSE")
    val suffix = if (isSme) " SME" else ""
    return listOfNotNull(
        if (bse) "BSE$suffix" else null,
        if (nse) "NSE$suffix" else null
    )
}

/** Applications logged for this IPO (matched by id, or by name for older entries). */
fun applicationsForIpo(ipo: LiveIpoItem, applications: List<IpoApplication>): List<IpoApplication> =
    applications.filter { it.ipoId == ipo.id || it.ipoName.equals(ipo.name, ignoreCase = true) }

enum class IntermediaryRole { LEAD_MANAGER, REGISTRAR, MARKET_MAKER }
