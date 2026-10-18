package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.round

@Entity(tableName = "ipos")
data class IpoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyName: String,
    val symbol: String,
    val series: String, // Mainboard, SME
    val minPrice: Double,
    val maxPrice: Double,
    val lotSize: Int,
    val issueSizeCr: Double,
    val openDate: String, // YYYY-MM-DD
    val closeDate: String, // YYYY-MM-DD
    val allotmentDate: String, // YYYY-MM-DD
    val refundDate: String, // YYYY-MM-DD
    val listingDate: String, // YYYY-MM-DD
    val registrarName: String, // Link Intime, KFin Technologies, Bigshare Services, Cameo, etc.
    val registrarUrl: String,
    val bseVerificationUrl: String = "https://www.bseindia.com/investors/appli_check.aspx",
    val nseVerificationUrl: String = "https://www.nseindia.com/products/content/equities/ipos/ipo_bid_verification.htm",
    val status: String, // Upcoming, Open, Closed, Allotted, Listed
    val gmpInr: Double = 0.0,
    val gmpPercent: Double = 0.0,
    val subscriptionTimes: Double = 0.0,
    val qibSub: Double = 0.0,
    val niiSub: Double = 0.0,
    val retailSub: Double = 0.0,
    val totalSub: Double = subscriptionTimes,
    val aboutCompany: String = "",
    val isFavorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val qibSubscription: Double
        get() = qibSub

    val niiSubscription: Double
        get() = niiSub

    val retailSubscription: Double
        get() = retailSub

    val estimatedListingPrice: Double
        get() = maxPrice + gmpInr

    val estimatedProfitPerLot: Double
        get() = gmpInr * lotSize

    val subjectToSaudaInr: Double
        get() = if (gmpInr > 0) round(estimatedProfitPerLot * 0.72) else 0.0

    val formattedGmpLastUpdated: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
            return "${sdf.format(Date(updatedAt))} IST"
        }

    val anchorBidDate: String
        get() = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val parsed = sdf.parse(openDate)
            if (parsed != null) {
                sdf.format(Date(parsed.time - 86_400_000L))
            } else openDate
        } catch (e: Exception) {
            openDate
        }

    val dematCreditDate: String
        get() = refundDate

    val retailMinAmount: Double
        get() = lotSize * maxPrice

    val shniMinLots: Int
        get() = if (retailMinAmount > 0) ceil(200001.0 / retailMinAmount).toInt().coerceAtLeast(2) else 14

    val shniMinAmount: Double
        get() = shniMinLots * retailMinAmount

    val currentStageStep: Int
        get() = when (status) {
            "Upcoming" -> 1
            "Open Now" -> 2
            "Closed / Awaiting Allotment" -> 3
            "Allotment Finalized" -> 4
            "Listed" -> 6
            else -> 2
        }
}
