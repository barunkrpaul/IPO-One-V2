package com.example

import com.example.data.model.IpoDates
import com.example.data.model.IpoExtras
import com.example.data.model.IpoLifecycleStage
import com.example.data.model.KeyDateKind
import com.example.data.model.LiveIpoItem
import com.example.data.model.groupByLifecycleStage
import com.example.data.model.lifecycleStage
import com.example.data.model.nextKeyDate
import com.example.data.model.subscriptionTimes
import com.example.data.model.withExtras
import com.example.util.formatInr
import com.example.util.formatSignedPercent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class IpoLifecycleUnitTest {

    private val today = LocalDate.of(2026, 10, 19)

    private fun ipo(
        id: String = "1",
        statusCode: String = "U",
        open: String = "-",
        close: String = "-",
        boa: String = "-",
        listing: String = "-",
        sortOpen: String = "",
        sortClose: String = "",
        totalSub: String = "-"
    ) = LiveIpoItem(
        id = id,
        name = "Test IPO $id",
        statusCode = statusCode,
        statusLabel = "",
        category = "Mainboard",
        sector = "",
        listingExchange = "BSE, NSE",
        issuePrice = 100,
        gmpAmount = 10.0,
        gmpPercent = 10.0,
        gmpRangeText = "",
        estimatedListingPrice = 110.0,
        fireRating = 0,
        totalSubscription = totalSub,
        qibSubscription = "-",
        niiSubscription = "-",
        shniSubscription = "-",
        bhniSubscription = "-",
        riiSubscription = "-",
        issueSizeCr = "TBA",
        lotSize = 150,
        minInvestmentInr = 15000,
        estProfitPerLotInr = 1500,
        peRatio = "-",
        openDate = open,
        closeDate = close,
        boaDate = boa,
        listingDate = listing,
        sortOpenDate = sortOpen,
        sortCloseDate = sortClose,
        updatedOn = "Live",
        hasAnchor = false,
        investorGainUrl = ""
    )

    @Test
    fun parsesFeedDateFormats() {
        assertEquals(LocalDate.of(2026, 10, 21), IpoDates.parse("2026-10-21", today))
        assertEquals(LocalDate.of(2026, 10, 21), IpoDates.parse("21-10", today))
        assertEquals(LocalDate.of(2026, 10, 21), IpoDates.parse("21-Oct", today))
        assertEquals(LocalDate.of(2026, 10, 21), IpoDates.parse("21 Oct 2026", today))
        assertEquals(LocalDate.of(2026, 10, 21), IpoDates.parse("Oct 21, 2026", today))
        assertNull(IpoDates.parse("-", today))
        assertNull(IpoDates.parse("TBA", today))
        assertNull(IpoDates.parse("soon", today))
    }

    @Test
    fun yearlessDatesSnapToNearestYear() {
        val december = LocalDate.of(2026, 12, 30)
        assertEquals(LocalDate.of(2027, 1, 5), IpoDates.parse("05-01", december))
        val january = LocalDate.of(2027, 1, 3)
        assertEquals(LocalDate.of(2026, 12, 28), IpoDates.parse("28-Dec", january))
    }

    @Test
    fun formatsHumanDates() {
        assertEquals("Wed, 21 Oct", IpoDates.formatHuman(LocalDate.of(2026, 10, 21), today))
        assertEquals("Tue, 5 Jan 2027", IpoDates.formatHuman(LocalDate.of(2027, 1, 5), today))
    }

    @Test
    fun derivesStagesFromStatusAndDates() {
        assertEquals(IpoLifecycleStage.UPCOMING, ipo(statusCode = "U").lifecycleStage(today))
        assertEquals(IpoLifecycleStage.OPEN, ipo(statusCode = "CT").lifecycleStage(today))
        // Closed, allotment tomorrow
        assertEquals(
            IpoLifecycleStage.CLOSED,
            ipo(statusCode = "C", close = "16-10", boa = "20-10", listing = "22-10").lifecycleStage(today)
        )
        // Closed in the feed but allotment date has passed
        assertEquals(
            IpoLifecycleStage.ALLOTTED,
            ipo(statusCode = "C", close = "14-10", boa = "15-10", listing = "21-10").lifecycleStage(today)
        )
        // Closed in the feed but listing date reached
        assertEquals(
            IpoLifecycleStage.LISTED,
            ipo(statusCode = "C", close = "10-10", boa = "14-10", listing = "19-10").lifecycleStage(today)
        )
        assertEquals(IpoLifecycleStage.LISTED, ipo(statusCode = "LP").lifecycleStage(today))
        assertEquals(
            IpoLifecycleStage.CANCELLED,
            ipo(statusCode = "U").withExtras(IpoExtras(cancelled = true)).lifecycleStage(today)
        )
        // Unknown code falls back to dates
        assertEquals(
            IpoLifecycleStage.OPEN,
            ipo(statusCode = "?", sortOpen = "2026-10-17", sortClose = "2026-10-21").lifecycleStage(today)
        )
    }

    @Test
    fun picksNextKeyDate() {
        val upcoming = ipo(statusCode = "U", sortOpen = "2026-10-21")
        val key = upcoming.nextKeyDate(IpoLifecycleStage.UPCOMING, today)
        assertEquals(KeyDateKind.OPENS, key?.kind)
        assertEquals(LocalDate.of(2026, 10, 21), key?.date)

        val closed = ipo(statusCode = "C", close = "16-10", boa = "20-10")
        assertEquals(KeyDateKind.ALLOTMENT, closed.nextKeyDate(IpoLifecycleStage.CLOSED, today)?.kind)
        assertNull(ipo().nextKeyDate(IpoLifecycleStage.CANCELLED, today))
    }

    @Test
    fun groupsInDashboardOrder() {
        val list = listOf(
            ipo(id = "a", statusCode = "LP"),
            ipo(id = "b", statusCode = "U"),
            ipo(id = "c", statusCode = "O"),
            ipo(id = "d", statusCode = "U")
        )
        val groups = groupByLifecycleStage(list, today)
        assertEquals(
            listOf(IpoLifecycleStage.UPCOMING, IpoLifecycleStage.OPEN, IpoLifecycleStage.LISTED),
            groups.map { it.first }
        )
        assertEquals(listOf("b", "d"), groups.first().second.map { it.id })
    }

    @Test
    fun parsesSubscriptionTimes() {
        assertEquals(7.78, ipo(totalSub = "7.78x").subscriptionTimes()!!, 0.0001)
        assertEquals(1234.5, ipo(totalSub = "1,234.5").subscriptionTimes()!!, 0.0001)
        assertNull(ipo(totalSub = "-").subscriptionTimes())
    }

    @Test
    fun extrasFromFirestoreMap() {
        val extras = IpoExtras.fromMap(
            mapOf("preApplyOpen" to true, "listingPrice" to 120L, "currentPrice" to "135.5", "cancelled" to null)
        )
        assertEquals(true, extras.preApplyOpen)
        assertEquals(120.0, extras.listingPrice!!, 0.0)
        assertEquals(135.5, extras.currentPrice!!, 0.0)
        assertNull(extras.cancelled)
        assertNull(ipo().withExtras(null).listingPrice)
    }

    @Test
    fun formatsIndianCurrency() {
        assertEquals("₹1,91,100", formatInr(191100))
        assertEquals("₹999", formatInr(999))
        assertEquals("₹1,00,00,000", formatInr(10000000))
        assertEquals("+₹12.5", formatInr(12.5, showPlus = true))
        assertEquals("-₹4", formatInr(-4.0))
        assertEquals("+8.3%", formatSignedPercent(8.333))
        assertEquals("-2.0%", formatSignedPercent(-2.0))
    }
}
