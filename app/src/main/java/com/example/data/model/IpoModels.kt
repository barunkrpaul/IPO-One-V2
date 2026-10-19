package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class IpoStatusFilter(val label: String, val code: String) {
    ALL("All Live", "ALL"),
    OPEN("Open", "O"),
    UPCOMING("Upcoming", "U"),
    CLOSED("Closed", "C"),
    ALLOTTED("Allotted", "A"),
    LISTED("Listed", "L"),
    CANCELLED("Cancelled", "X")
}

fun maskPanNumber(pan: String, hidePan: Boolean): String {
    val clean = pan.trim().uppercase()
    if (!hidePan || clean.length < 6) return clean
    return "${clean.take(3)}****${clean.takeLast(2)}"
}

enum class IpoBoardFilter(val label: String) {
    ALL("All Boards"),
    MAINBOARD("Mainboard"),
    SME("SME")
}

data class LiveIpoItem(
    val id: String,
    val name: String,
    val statusCode: String, // "O", "CT", "U", "C", "LP", "LN"
    val statusLabel: String,
    val category: String, // "IPO" (Mainboard) or "SME"
    val sector: String,
    val listingExchange: String,
    val issuePrice: Int,
    val gmpAmount: Double,
    val gmpPercent: Double,
    val gmpRangeText: String, // e.g., "148 ↓ / 235 ↑"
    val estimatedListingPrice: Double,
    val fireRating: Int, // 0..5
    val totalSubscription: String, // e.g. "7.78x"
    val qibSubscription: String,
    val niiSubscription: String,
    val shniSubscription: String,
    val bhniSubscription: String,
    val riiSubscription: String,
    val issueSizeCr: String, // e.g. "₹30213.00 Cr"
    val lotSize: Int,
    val minInvestmentInr: Int,
    val estProfitPerLotInr: Int,
    val peRatio: String,
    val openDate: String,
    val closeDate: String,
    val boaDate: String,
    val listingDate: String,
    val sortOpenDate: String,
    val sortCloseDate: String,
    val updatedOn: String,
    val hasAnchor: Boolean,
    val investorGainUrl: String,
    // Fields not provided by InvestorGain; filled from Firestore ipo_extras/{ipoId} when present
    val priceBandLow: Int? = null,
    val preApplyOpen: Boolean? = null,
    val listingPrice: Double? = null,
    val currentPrice: Double? = null,
    val cancelled: Boolean? = null,
    // ISO dates from the feed's ~Srt_BoA_Dt / ~Str_Listing columns
    val sortBoaDate: String = "",
    val sortListingDate: String = ""
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val phoneNumber: String? = null,
    val authProvider: String? = null,
    val biometricEnabled: Boolean = false,
    val priceAlertsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PanProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val holderName: String = "",
    val panNumber: String = "",
    val dematId: String = "",
    val upiId: String = "",
    val category: String = "Retail",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class IpoApplication(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ipoId: String = "",
    val ipoName: String = "",
    val panHolderName: String = "",
    val panNumber: String = "",
    val category: String = "Retail",
    val lotsApplied: Long = 1L,
    val sharesApplied: Long = 1L,
    val bidPrice: Long = 100L,
    val totalAmount: Long = 14000L,
    val upiId: String = "",
    val status: String = "Applied",
    // Set by the registrar allotment check: ALLOTTED / NOT_ALLOTTED / UNVERIFIED
    val allotmentResult: String? = null,
    val sharesAllotted: Long? = null,
    val checkedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WatchlistItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ipoId: String = "",
    val ipoName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
