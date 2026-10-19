package com.example

import com.example.data.model.IpoExtras
import com.example.data.model.LiveIpoItem
import com.example.data.model.TimelineEvent
import com.example.data.model.calendarEvents
import com.example.data.model.eventsOn
import com.example.data.model.monthGrid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class IpoCalendarUnitTest {

    private val today = LocalDate.of(2026, 10, 19)

    private fun ipo(id: String, name: String, open: String, close: String, boa: String, listing: String) = LiveIpoItem(
        id = id, name = name, statusCode = "U", statusLabel = "", category = "Mainboard", sector = "",
        listingExchange = "BSE, NSE", issuePrice = 100, gmpAmount = 0.0, gmpPercent = 0.0, gmpRangeText = "",
        estimatedListingPrice = 100.0, fireRating = 0, totalSubscription = "-", qibSubscription = "-",
        niiSubscription = "-", shniSubscription = "-", bhniSubscription = "-", riiSubscription = "-",
        issueSizeCr = "TBA", lotSize = 10, minInvestmentInr = 1000, estProfitPerLotInr = 0, peRatio = "-",
        openDate = open, closeDate = close, boaDate = boa, listingDate = listing,
        sortOpenDate = "", sortCloseDate = "", updatedOn = "", hasAnchor = false, investorGainUrl = ""
    )

    @Test
    fun buildsSortedEventsAndSkipsCancelled() {
        val a = ipo("1", "Tonbo Imaging", "15-10", "19-10", "20-10", "22-10")
        val b = ipo("2", "Fusion CX", "14-10", "16-10", "19-10", "21-10")
        val c = ipo("3", "Gone Ltd", "19-10", "21-10", "22-10", "24-10")
        val events = calendarEvents(listOf(a, b, c), mapOf("3" to IpoExtras(cancelled = true)), today)

        assertTrue(events.none { it.ipoId == "3" })
        assertEquals(events.sortedBy { it.date }, events)
        val todays = eventsOn(events, today)
        assertEquals(listOf("Tonbo Imaging" to TimelineEvent.CLOSES, "Fusion CX" to TimelineEvent.ALLOTMENT),
            todays.map { it.ipoName to it.event })
        // Mandate end is estimated, so only shows when asked for
        assertTrue(eventsOn(events, LocalDate.of(2026, 11, 2)).isEmpty())
        assertEquals(1, eventsOn(events, LocalDate.of(2026, 11, 2), includeEstimated = true).size)
    }

    @Test
    fun monthGridIsMondayFirst() {
        // 1 Oct 2026 is a Thursday -> three leading blanks
        val grid = monthGrid(YearMonth.of(2026, 10))
        assertEquals(0, grid.size % 7)
        assertNull(grid[2])
        assertEquals(LocalDate.of(2026, 10, 1), grid[3])
        assertEquals(LocalDate.of(2026, 10, 31), grid.filterNotNull().last())
        // 1 Jun 2026 is a Monday -> no leading blanks
        assertEquals(LocalDate.of(2026, 6, 1), monthGrid(YearMonth.of(2026, 6))[0])
    }
}
