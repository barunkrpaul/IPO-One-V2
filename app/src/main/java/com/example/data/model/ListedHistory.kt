package com.example.data.model

import java.time.LocalDate
import java.time.YearMonth

/** One row of InvestorGain's listed-IPO performance report (report 377). */
data class ListedIpoRecord(
    val id: String,
    val name: String,
    val symbol: String,
    val category: String, // "Mainboard" or "SME"
    val issuePrice: Double,
    val listingPrice: Double?,
    val listingGainPct: Double?,
    val currentPrice: Double?,
    val currentGainPct: Double?,
    val listingDate: LocalDate?,
    val url: String,
    // Set from ipo_extras when a split/bonus is known and an adjusted price is supplied
    val corporateAction: String? = null,
    val adjusted: Boolean = false
)

/**
 * Applies a known corporate action from ipo_extras: the adjusted current price
 * replaces the raw LTP and the gain is recomputed against the issue price.
 */
fun ListedIpoRecord.withExtras(extras: IpoExtras?): ListedIpoRecord {
    val adjustedPrice = extras?.adjustedCurrentPrice
    if (extras?.corporateAction == null || adjustedPrice == null) {
        return if (extras?.corporateAction != null) copy(corporateAction = extras.corporateAction) else this
    }
    return copy(
        currentPrice = adjustedPrice,
        currentGainPct = if (issuePrice > 0) (adjustedPrice - issuePrice) / issuePrice * 100.0 else currentGainPct,
        corporateAction = extras.corporateAction,
        adjusted = true
    )
}

private val PRICE_WITH_PCT = Regex("([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*(?:\\(\\s*([+-]?[0-9]+(?:\\.[0-9]+)?)\\s*%\\s*\\))?")

/** "<span>&#8377;80.00 (-2.44%)</span>" -> (80.0, -2.44). Either part may be null. */
fun parsePriceAndPercent(html: String?): Pair<Double?, Double?> {
    val text = html.orEmpty()
        .replace(Regex("<[^>]+>"), " ")
        .replace("&#8377;", " ")
        .replace("₹", " ")
        .trim()
    val match = PRICE_WITH_PCT.find(text) ?: return null to null
    val price = match.groupValues[1].replace(",", "").toDoubleOrNull()
    val pct = match.groupValues[2].takeIf { it.isNotEmpty() }?.toDoubleOrNull()
    return price to pct
}

/** "L@221.00 (12.76%)" inside the live report's name cell -> listing price 221.0. */
fun parseListingPriceFromNameCell(html: String?): Double? =
    Regex("L@\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)").find(html.orEmpty())
        ?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

data class ListingPerformance(
    val count: Int,
    val averageListingGainPct: Double?,
    val best: ListedIpoRecord?,
    val worst: ListedIpoRecord?
)

/** Listing stats for IPOs that listed in [month]. */
fun listingPerformance(records: List<ListedIpoRecord>, month: YearMonth): ListingPerformance {
    val inMonth = records.filter { it.listingDate != null && YearMonth.from(it.listingDate) == month }
    val withGain = inMonth.filter { it.listingGainPct != null }
    return ListingPerformance(
        count = inMonth.size,
        averageListingGainPct = withGain.takeIf { it.isNotEmpty() }?.map { it.listingGainPct!! }?.average(),
        best = withGain.maxByOrNull { it.listingGainPct!! },
        worst = withGain.minByOrNull { it.listingGainPct!! }
    )
}

/** Years present in the history, newest first, for the year filter chips. */
fun listingYears(records: List<ListedIpoRecord>): List<Int> =
    records.mapNotNull { it.listingDate?.year }.distinct().sortedDescending()

/** Newest listing first; records without a date go last. */
fun sortListedHistory(records: List<ListedIpoRecord>): List<ListedIpoRecord> =
    records.sortedWith(compareByDescending<ListedIpoRecord, LocalDate?>(nullsFirst()) { it.listingDate })
