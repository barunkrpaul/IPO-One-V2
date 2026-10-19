package com.example.data.model

import java.time.LocalDate

/**
 * Helpers for the data InvestorGain embeds in each IPO page (Next.js flight
 * payload). The payload is a JS string, so quotes arrive as \" and are
 * unescaped once before searching.
 */
object IpoPageParser {

    /**
     * Undoes one level of JS string escaping for quotes and backslashes only, so
     * `\"` becomes `"` and `\\\"` becomes `\"` (still a valid JSON escape).
     */
    fun unescape(html: String): String {
        val out = StringBuilder(html.length)
        var i = 0
        while (i < html.length) {
            val c = html[i]
            if (c == '\\' && i + 1 < html.length && (html[i + 1] == '"' || html[i + 1] == '\\')) {
                out.append(html[i + 1])
                i += 2
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    /**
     * Subscription rows from the latest `ipoBiddingData` entry. Offered share
     * counts become ₹ Cr at [capPrice]; quota is each category's share of the
     * total offered. Employee and shareholder rows appear only when offered.
     */
    fun subscriptionFromBidding(
        values: Map<String, String>,
        capPrice: Double?
    ): Map<SubscriptionCategory, SubscriptionFigures> {
        fun num(key: String): Double? = values[key]?.replace(",", "")?.trim()?.toDoubleOrNull()
        val totalOffered = num("total_offered")
        fun row(offeredKey: String, bidKey: String, timesKey: String): SubscriptionFigures? {
            val offered = num(offeredKey)
            val figures = SubscriptionFigures(
                quotaPct = if (offered != null && totalOffered != null && totalOffered > 0) offered / totalOffered * 100 else null,
                sharesOfferedCr = if (offered != null && capPrice != null) offered * capPrice / 1e7 else null,
                sharesAppliedCr = num(bidKey),
                times = num(timesKey)
            )
            return figures.takeIf { it.times != null || it.sharesOfferedCr != null || it.sharesAppliedCr != null }
        }
        val rows = mutableMapOf<SubscriptionCategory, SubscriptionFigures>()
        row("qib_offered", "qib_bid_amt", "qib")?.let { rows[SubscriptionCategory.QIB] = it }
        row("nii_offered", "nii_bid_amt", "nii")?.let { rows[SubscriptionCategory.NII] = it }
        row("nii_offered_big", "nii_bid_amt_big", "nii_big")?.let { rows[SubscriptionCategory.BNII] = it }
        row("nii_offered_small", "nii_bid_amt_small", "nii_small")?.let { rows[SubscriptionCategory.SNII] = it }
        row("rii_offered", "rii_bid_amt", "rii")?.let { rows[SubscriptionCategory.RII] = it }
        if ((num("emp_offered") ?: 0.0) > 0) {
            row("emp_offered", "emp_bid_amt", "emp")?.let { rows[SubscriptionCategory.EMPLOYEE] = it }
        }
        if ((num("shareholder_offered") ?: 0.0) > 0) {
            row("shareholder_offered", "shareholder_bid_amt", "shareholder")?.let { rows[SubscriptionCategory.SHAREHOLDER] = it }
        }
        row("total_offered", "total_bid_amt", "total")?.let { rows[SubscriptionCategory.TOTAL] = it }
        return rows
    }

    /** The JSON array text that follows `"key":[`, matched bracket for bracket. */
    fun extractJsonArray(text: String, key: String): String? {
        val start = text.indexOf("\"$key\":[")
        if (start < 0) return null
        val open = text.indexOf('[', start)
        var depth = 0
        var inString = false
        var i = open
        while (i < text.length) {
            val c = text[i]
            if (inString) {
                if (c == '\\') i++ else if (c == '"') inString = false
            } else {
                when (c) {
                    '"' -> inString = true
                    '[' -> depth++
                    ']' -> {
                        depth--
                        if (depth == 0) return text.substring(open, i + 1)
                    }
                }
            }
            i++
        }
        return null
    }

    /** First string or number value of `"key":...`, or null when empty. */
    fun extractValue(text: String, key: String): String? =
        Regex("\"${Regex.escape(key)}\":\"?([^\",}]*)\"?").find(text)
            ?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

    /** `"key":"2026-10-22T00:00:00.000Z"` -> 2026-10-22. */
    fun extractIsoDate(text: String, key: String): LocalDate? =
        extractValue(text, key)?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** Market maker from the "Market Maker of the company is <a ... title="X Review">" prose. */
    fun extractMarketMaker(text: String): NamedIntermediary? {
        val match = Regex("ipo-market-maker-vs-listing-gain/\\d+/(\\d+)/?\"[^>]*?title=\"([^\"]+?) Review\"")
            .find(text) ?: return null
        return NamedIntermediary(match.groupValues[1], match.groupValues[2].trim())
    }

    /** First http(s) link in an HTML fragment (the registrar's website). */
    fun firstLink(html: String?): String? =
        html?.let { Regex("href=\\\\?\"(https?://[^\"\\\\]+)").find(it)?.groupValues?.get(1) }
}

data class NamedIntermediary(val id: String, val name: String)

/** Everything the app reads from one IPO page. */
data class IpoPageDetails(
    val leadManagers: List<NamedIntermediary> = emptyList(),
    val registrar: NamedIntermediary? = null,
    val registrarWebsite: String? = null,
    val marketMaker: NamedIntermediary? = null,
    val symbol: String? = null,
    val logoUrl: String? = null,
    val floorPrice: Double? = null,
    val settlementDate: LocalDate? = null,
    val anchorLockIn50Date: LocalDate? = null,
    val anchorLockIn100Date: LocalDate? = null,
    val subscription: Map<SubscriptionCategory, SubscriptionFigures> = emptyMap(),
    val subscriptionUpdatedAtMillis: Long? = null,
    val fetchedAtMillis: Long = 0L
) {
    fun toExtras(): IpoExtras = IpoExtras(
        priceBandLow = floorPrice?.toInt(),
        symbol = symbol,
        logoUrl = logoUrl,
        settlementDate = settlementDate,
        anchorLockIn50Date = anchorLockIn50Date,
        anchorLockIn100Date = anchorLockIn100Date,
        leadManagers = leadManagers.map { it.name },
        registrar = registrar?.name,
        registrarWebsite = registrarWebsite,
        marketMaker = marketMaker?.name,
        subscription = subscription,
        subscriptionUpdatedAtMillis = subscriptionUpdatedAtMillis
    )
}

/** Hand-maintained values (this) win; anything they leave empty falls back to [base]. */
fun IpoExtras.mergedOver(base: IpoExtras?): IpoExtras {
    if (base == null) return this
    return IpoExtras(
        priceBandLow = priceBandLow ?: base.priceBandLow,
        preApplyOpen = preApplyOpen ?: base.preApplyOpen,
        listingPrice = listingPrice ?: base.listingPrice,
        currentPrice = currentPrice ?: base.currentPrice,
        cancelled = cancelled ?: base.cancelled,
        symbol = symbol ?: base.symbol,
        logoUrl = logoUrl ?: base.logoUrl,
        website = website ?: base.website,
        settlementDate = settlementDate ?: base.settlementDate,
        mandateEndDate = mandateEndDate ?: base.mandateEndDate,
        anchorLockIn50Date = anchorLockIn50Date ?: base.anchorLockIn50Date,
        anchorLockIn100Date = anchorLockIn100Date ?: base.anchorLockIn100Date,
        leadManagers = leadManagers.ifEmpty { base.leadManagers },
        registrar = registrar ?: base.registrar,
        registrarWebsite = registrarWebsite ?: base.registrarWebsite,
        marketMaker = marketMaker ?: base.marketMaker,
        subscription = base.subscription + subscription,
        subscriptionUpdatedAtMillis = subscriptionUpdatedAtMillis ?: base.subscriptionUpdatedAtMillis,
        corporateAction = corporateAction ?: base.corporateAction,
        adjustedCurrentPrice = adjustedCurrentPrice ?: base.adjustedCurrentPrice
    )
}
