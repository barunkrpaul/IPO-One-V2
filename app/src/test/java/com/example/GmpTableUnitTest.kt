package com.example

import com.example.data.model.IpoBoardFilter
import com.example.data.model.IpoLifecycleStage
import com.example.data.model.LiveIpoItem
import com.example.data.model.formatIssueSize
import com.example.data.model.simpleTableRows
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GmpTableUnitTest {

    private val today = LocalDate.of(2026, 10, 19)

    private fun ipo(id: String, statusCode: String, category: String, sortClose: String) = LiveIpoItem(
        id = id, name = "IPO $id", statusCode = statusCode, statusLabel = "", category = category,
        sector = "", listingExchange = "", issuePrice = 100, gmpAmount = 10.0, gmpPercent = 10.0,
        gmpRangeText = "", estimatedListingPrice = 110.0, fireRating = 0, totalSubscription = "-",
        qibSubscription = "-", niiSubscription = "-", shniSubscription = "-", bhniSubscription = "-",
        riiSubscription = "-", issueSizeCr = "", lotSize = 1, minInvestmentInr = 0, estProfitPerLotInr = 0,
        peRatio = "-", openDate = "-", closeDate = "-", boaDate = "-", listingDate = "-",
        sortOpenDate = "", sortCloseDate = sortClose, updatedOn = "", hasAnchor = false, investorGainUrl = ""
    )

    private val feed = listOf(
        ipo("up", "U", "IPO", "2026-10-28"),
        ipo("open-late", "O", "IPO", "2026-10-23"),
        ipo("open-soon", "O", "SME", "2026-10-20"),
        ipo("listed", "LP", "IPO", "2026-10-01")
    )

    @Test
    fun openFirstByLastDateAndListedLeftOut() {
        val rows = simpleTableRows(feed, IpoBoardFilter.ALL, today)
        assertEquals(listOf("open-soon", "open-late", "up"), rows.map { it.first.id })
        assertEquals(IpoLifecycleStage.OPEN, rows.first().second)
    }

    @Test
    fun filtersByBoard() {
        assertEquals(listOf("open-soon"), simpleTableRows(feed, IpoBoardFilter.SME, today).map { it.first.id })
        assertEquals(listOf("open-late", "up"), simpleTableRows(feed, IpoBoardFilter.MAINBOARD, today).map { it.first.id })
    }

    @Test
    fun formatsIssueSize() {
        assertEquals("₹30,213 Cr", formatIssueSize("₹30213.00 Cr"))
        assertEquals("₹45.5 Cr", formatIssueSize("45.50"))
        assertEquals("-", formatIssueSize(""))
        assertEquals("TBA", formatIssueSize("TBA"))
    }
}
