package com.example

import com.example.data.model.ApplicationCategory
import com.example.data.model.IpoApplication
import com.example.data.model.IpoExtras
import com.example.data.model.LiveIpoItem
import com.example.data.model.SubscriptionCategory
import com.example.data.model.SubscriptionFigures
import com.example.data.model.TimelineEvent
import com.example.data.model.applicationSizes
import com.example.data.model.applicationsForIpo
import com.example.data.model.exchangeChips
import com.example.data.model.ipoTimeline
import com.example.data.model.subscriptionRows
import com.example.util.formatIndianNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class IpoDetailModelUnitTest {

    private val today = LocalDate.of(2026, 10, 19)

    private fun ipo(
        hasAnchor: Boolean = true,
        category: String = "Mainboard",
        qib: String = "12.5x",
        rii: String = "-"
    ) = LiveIpoItem(
        id = "42", name = "Acme Ltd", statusCode = "O", statusLabel = "Open Now",
        category = category, sector = "", listingExchange = "BSE, NSE",
        issuePrice = 100, gmpAmount = 10.0, gmpPercent = 10.0, gmpRangeText = "",
        estimatedListingPrice = 110.0, fireRating = 0, totalSubscription = "3.2x",
        qibSubscription = qib, niiSubscription = "-", shniSubscription = "-",
        bhniSubscription = "-", riiSubscription = rii, issueSizeCr = "₹500 Cr",
        lotSize = 150, minInvestmentInr = 15000, estProfitPerLotInr = 1500, peRatio = "-",
        openDate = "17-10", closeDate = "21-10", boaDate = "22-10", listingDate = "24-10",
        sortOpenDate = "2026-10-17", sortCloseDate = "2026-10-21", updatedOn = "Live",
        hasAnchor = hasAnchor, investorGainUrl = ""
    )

    @Test
    fun timelineWithEstimates() {
        val entries = ipoTimeline(ipo(), null, today)
        assertEquals(
            listOf(
                TimelineEvent.OPENS, TimelineEvent.CLOSES, TimelineEvent.ALLOTMENT, TimelineEvent.LISTING,
                TimelineEvent.MANDATE_END, TimelineEvent.ANCHOR_50, TimelineEvent.ANCHOR_100
            ),
            entries.map { it.event }
        )
        val mandate = entries.first { it.event == TimelineEvent.MANDATE_END }
        assertEquals(LocalDate.of(2026, 11, 4), mandate.date)
        assertTrue(mandate.estimated)
        assertEquals(LocalDate.of(2026, 11, 23), entries.first { it.event == TimelineEvent.ANCHOR_50 }.date)
        assertEquals(LocalDate.of(2027, 1, 22), entries.first { it.event == TimelineEvent.ANCHOR_100 }.date)
    }

    @Test
    fun timelineUsesExtrasAndSkipsAnchorWithoutAnchorBook() {
        val extras = IpoExtras(settlementDate = LocalDate.of(2026, 10, 23), mandateEndDate = LocalDate.of(2026, 10, 25))
        val entries = ipoTimeline(ipo(hasAnchor = false), extras, today)
        assertTrue(entries.any { it.event == TimelineEvent.SETTLEMENT && !it.estimated })
        assertFalse(entries.first { it.event == TimelineEvent.MANDATE_END }.estimated)
        assertFalse(entries.any { it.event == TimelineEvent.ANCHOR_50 })
    }

    @Test
    fun subscriptionRowsMergeFeedAndExtras() {
        val extras = IpoExtras(
            subscription = mapOf(
                SubscriptionCategory.RII to SubscriptionFigures(
                    quotaPct = 35.0, applicationsReserved = 1000, applicationsReceived = 5000, times = 4.0
                ),
                SubscriptionCategory.SHAREHOLDER to SubscriptionFigures(times = 2.0)
            )
        )
        val rows = subscriptionRows(ipo(), extras).toMap()
        assertEquals(12.5, rows[SubscriptionCategory.QIB]!!.times!!, 0.0)
        assertEquals(4.0, rows[SubscriptionCategory.RII]!!.times!!, 0.0)
        assertEquals(5.0, rows[SubscriptionCategory.RII]!!.applicationTimes!!, 0.0)
        assertNull(rows[SubscriptionCategory.QIB_FII]!!.times)
        assertTrue(rows.containsKey(SubscriptionCategory.SHAREHOLDER))
        assertFalse(subscriptionRows(ipo(), null).toMap().containsKey(SubscriptionCategory.SHAREHOLDER))
    }

    @Test
    fun mainboardApplicationSizes() {
        // Lot cost ₹15,000: RII 1..13 lots, sNII 14..66 lots, bNII from 67 lots
        val rows = applicationSizes(150, 100, isSme = false, includeEmployee = true, includeShareholder = false)
            .associateBy { it.category }
        assertEquals(1, rows[ApplicationCategory.RII]!!.min!!.lots)
        assertEquals(13, rows[ApplicationCategory.RII]!!.max!!.lots)
        assertEquals(195_000L, rows[ApplicationCategory.RII]!!.max!!.amount)
        assertEquals(14, rows[ApplicationCategory.SNII]!!.min!!.lots)
        assertEquals(66, rows[ApplicationCategory.SNII]!!.max!!.lots)
        assertEquals(67, rows[ApplicationCategory.BNII]!!.min!!.lots)
        assertNull(rows[ApplicationCategory.BNII]!!.max)
        assertEquals(33, rows[ApplicationCategory.EMPLOYEE]!!.max!!.lots)
        assertFalse(rows.containsKey(ApplicationCategory.SHAREHOLDER))
    }

    @Test
    fun smeApplicationSizes() {
        // SME lot cost ₹1,20,000: retail = 2 lots, NII from 3 lots
        val rows = applicationSizes(1200, 100, isSme = true, includeEmployee = false, includeShareholder = false)
            .associateBy { it.category }
        assertEquals(2, rows[ApplicationCategory.RII]!!.min!!.lots)
        assertEquals(2, rows[ApplicationCategory.RII]!!.max!!.lots)
        assertEquals(3, rows[ApplicationCategory.SNII]!!.min!!.lots)
        assertEquals(8, rows[ApplicationCategory.SNII]!!.max!!.lots)
        assertEquals(9, rows[ApplicationCategory.BNII]!!.min!!.lots)
        assertTrue(applicationSizes(0, 100, false, false, false).isEmpty())
    }

    @Test
    fun extrasParseNestedFields() {
        val extras = IpoExtras.fromMap(
            mapOf(
                "symbol" to "ACME",
                "leadManagers" to listOf("Axis Capital", " ", "ICICI Securities"),
                "registrar" to "KFin Technologies",
                "settlementDate" to "2026-10-23",
                "subscription" to mapOf("qibFii" to mapOf("times" to 20, "quotaPct" to "15")),
                "subscriptionUpdatedAt" to java.util.Date(1_700_000_000_000)
            )
        )
        assertEquals("ACME", extras.symbol)
        assertEquals(listOf("Axis Capital", "ICICI Securities"), extras.leadManagers)
        assertEquals(LocalDate.of(2026, 10, 23), extras.settlementDate)
        assertEquals(20.0, extras.subscription[SubscriptionCategory.QIB_FII]!!.times!!, 0.0)
        assertEquals(15.0, extras.subscription[SubscriptionCategory.QIB_FII]!!.quotaPct!!, 0.0)
        assertEquals(1_700_000_000_000, extras.subscriptionUpdatedAtMillis)
    }

    @Test
    fun exchangesAndApplications() {
        assertEquals(listOf("BSE", "NSE"), exchangeChips("BSE, NSE", isSme = false))
        assertEquals(listOf("NSE SME"), exchangeChips("NSE", isSme = true))
        val apps = listOf(
            IpoApplication(id = "1", ipoId = "42"),
            IpoApplication(id = "2", ipoId = "", ipoName = "ACME LTD"),
            IpoApplication(id = "3", ipoId = "7", ipoName = "Other")
        )
        assertEquals(listOf("1", "2"), applicationsForIpo(ipo(), apps).map { it.id })
        assertEquals("12,34,567.5", formatIndianNumber(1234567.5))
    }
}
