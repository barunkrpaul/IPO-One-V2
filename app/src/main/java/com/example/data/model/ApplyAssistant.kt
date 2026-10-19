package com.example.data.model

/**
 * Brokers the Apply assistant can hand off to. IPO bids can only be placed by
 * a SEBI-registered broker or bank, so the app opens the broker's app (or its
 * website when the app is not installed) with the details ready to paste.
 */
enum class Broker(val label: String, val packageName: String?, val webUrl: String) {
    ZERODHA("Zerodha Kite", "com.zerodha.kite3", "https://kite.zerodha.com/"),
    GROWW("Groww", "com.nextbillion.groww", "https://groww.in/ipo"),
    UPSTOX("Upstox", "in.upstox.app", "https://upstox.com/ipo/"),
    ANGEL_ONE("Angel One", "com.msf.angelmobile", "https://www.angelone.in/ipo"),
    DHAN("Dhan", "com.dhan.live", "https://dhan.co/ipo/"),
    PAYTM_MONEY("Paytm Money", "com.paytmmoney", "https://www.paytmmoney.com/ipo"),
    FIVE_PAISA("5paisa", "com.fivepaisa.trade", "https://www.5paisa.com/ipo"),
    ICICI_DIRECT("ICICI Direct", null, "https://www.icicidirect.com/ipo"),
    KOTAK_NEO("Kotak Neo", null, "https://www.kotaksecurities.com/ipo/"),
    HDFC_SKY("HDFC Sky", null, "https://hdfcsky.com/ipo"),
    OTHER("Other / bank ASBA", null, "");

    companion object {
        fun fromStored(value: String?): Broker? = entries.firstOrNull { it.name == value }
    }
}

/** Application categories the assistant offers, matching the vault's PAN categories. */
enum class BidCategory(val stored: String, val sizeCategory: ApplicationCategory) {
    RETAIL("Retail", ApplicationCategory.RII),
    SHNI("sHNI", ApplicationCategory.SNII),
    BHNI("bHNI", ApplicationCategory.BNII);

    companion object {
        fun fromStored(value: String?): BidCategory = entries.firstOrNull { it.stored == value } ?: RETAIL
    }
}

/** Allowed lots for a category: (min, max); max is null when only the NII quota limits it. */
fun lotRange(category: BidCategory, sizes: List<ApplicationSizeRow>): Pair<Int, Int?>? {
    val row = sizes.firstOrNull { it.category == category.sizeCategory } ?: return null
    val min = row.min?.lots ?: return null
    return min to row.max?.lots
}

/** Text the user can paste into a broker or bank form for one PAN. */
fun applyDetailsText(
    ipoName: String,
    pan: PanProfile,
    category: BidCategory,
    lots: Int,
    lotSize: Int,
    price: Int
): String = buildString {
    appendLine("$ipoName IPO")
    appendLine("Applicant: ${pan.holderName}")
    appendLine("PAN: ${pan.panNumber}")
    if (pan.dematId.isNotBlank()) appendLine("Demat (BO) ID: ${pan.dematId}")
    if (pan.brokerClientId.isNotBlank()) appendLine("Client ID: ${pan.brokerClientId}")
    if (pan.upiId.isNotBlank()) appendLine("UPI ID: ${pan.upiId}")
    appendLine("Category: ${category.stored}")
    append("Bid: $lots lot(s) = ${lots * lotSize} shares at cut-off ₹$price")
}
