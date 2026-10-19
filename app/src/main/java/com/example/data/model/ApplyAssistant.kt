package com.example.data.model

/**
 * Brokers the Apply assistant can hand off to. IPO bids can only be placed by
 * a SEBI-registered broker or bank, so the app opens the broker's app (or its
 * website when the app is not installed) with the details ready to paste.
 */
enum class Broker(
    val label: String,
    val packageName: String?,
    val webUrl: String,
    /** Page opened inside IPO One for Family Apply; null when only the external app/site is supported. */
    val inAppUrl: String? = null,
    /** Sites whose login cookies are kept per family member. */
    val sessionUrls: List<String> = emptyList()
) {
    ZERODHA(
        "Zerodha Kite", "com.zerodha.kite3", "https://kite.zerodha.com/",
        inAppUrl = "https://kite.zerodha.com/bids/ipo",
        sessionUrls = listOf("https://kite.zerodha.com", "https://zerodha.com", "https://console.zerodha.com")
    ),
    GROWW(
        "Groww", "com.nextbillion.groww", "https://groww.in/ipo",
        inAppUrl = "https://groww.in/ipo",
        sessionUrls = listOf("https://groww.in")
    ),
    UPSTOX("Upstox", "in.upstox.app", "https://upstox.com/ipo/"),
    ANGEL_ONE(
        "Angel One", "com.msf.angelmobile", "https://www.angelone.in/ipo",
        inAppUrl = "https://www.angelone.in/ipo",
        sessionUrls = listOf("https://www.angelone.in", "https://trade.angelone.in", "https://angelone.in")
    ),
    DHAN("Dhan", "com.dhan.live", "https://dhan.co/ipo/"),
    PAYTM_MONEY("Paytm Money", "com.paytmmoney", "https://www.paytmmoney.com/ipo"),
    FIVE_PAISA("5paisa", "com.fivepaisa.trade", "https://www.5paisa.com/ipo"),
    ICICI_DIRECT("ICICI Direct", null, "https://www.icicidirect.com/ipo"),
    KOTAK_NEO(
        "Kotak Neo", null, "https://www.kotaksecurities.com/ipo/",
        inAppUrl = "https://neo.kotaksecurities.com/",
        sessionUrls = listOf("https://neo.kotaksecurities.com", "https://www.kotaksecurities.com")
    ),
    HDFC_SKY("HDFC Sky", null, "https://hdfcsky.com/ipo"),
    OTHER("Other / bank ASBA", null, "");

    val supportsInApp: Boolean get() = inAppUrl != null

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

/**
 * True when newly shown page text confirms a bid was placed, so Family Apply
 * can log it and move to the next person. Only text that appeared after the
 * form was filled is checked.
 */
fun isBidConfirmation(newText: String): Boolean {
    val text = newText.lowercase().replace(Regex("\\s+"), " ")
    val phrases = listOf(
        "bid placed", "bid submitted", "bid has been placed", "bid successfully",
        "application submitted", "application placed", "applied successfully", "successfully applied",
        "order placed successfully", "ipo application is submitted", "mandate request has been sent",
        "mandate request sent", "accept the mandate", "approve the mandate"
    )
    return phrases.any { it in text }
}
