package com.example

import com.example.data.model.IpoDates
import com.example.data.model.IpoExtras
import com.example.data.model.ListedIpoRecord
import com.example.data.model.listingPerformance
import com.example.data.model.listingYears
import com.example.data.model.parseListingPriceFromNameCell
import com.example.data.model.parsePriceAndPercent
import com.example.data.model.sortListedHistory
import com.example.data.model.withExtras
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ListedHistoryUnitTest {

    private fun record(id: String, date: LocalDate?, listingGain: Double?, issue: Double = 100.0) = ListedIpoRecord(
        id = id, name = "IPO $id", symbol = "SYM$id", category = "Mainboard", issuePrice = issue,
        listingPrice = listingGain?.let { issue * (1 + it / 100) }, listingGainPct = listingGain,
        currentPrice = 150.0, currentGainPct = 50.0, listingDate = date, url = ""
    )

    @Test
    fun parsesFeedCells() {
        // Shapes taken from InvestorGain report 377 / 331
        assertEquals(80.0 to -2.44, parsePriceAndPercent("<span class='text-danger'>&#8377;80.00 (-2.44%)</span>"))
        assertEquals(72.2 to -11.95, parsePriceAndPercent("<span class='text-danger'>&#8377;72.2 (-11.95%)</span>"))
        assertEquals(82.0 to null, parsePriceAndPercent("&#8377;82.00"))
        assertEquals(1234.5 to 0.0, parsePriceAndPercent("₹1,234.50 (0%)"))
        assertEquals(null to null, parsePriceAndPercent("-"))
        assertEquals(221.0, parseListingPriceFromNameCell(
            "<a href=\"/gmp/x/2006/\">Acme</a> <span><small><b>L@221.00 (12.76%)</b></small></span>"
        )!!, 0.0)
        assertNull(parseListingPriceFromNameCell("<a>Tonbo</a><span>C</span>"))
        assertEquals(LocalDate.of(2026, 10, 12), IpoDates.parse("12-Oct-26", LocalDate.of(2026, 10, 19)))
    }

    @Test
    fun monthPerformanceAndYears() {
        val records = listOf(
            record("1", LocalDate.of(2026, 10, 3), 12.0),
            record("2", LocalDate.of(2026, 10, 9), -4.0),
            record("3", LocalDate.of(2026, 10, 12), null),
            record("4", LocalDate.of(2026, 9, 30), 90.0),
            record("5", LocalDate.of(2025, 12, 1), 5.0)
        )
        val perf = listingPerformance(records, YearMonth.of(2026, 10))
        assertEquals(3, perf.count)
        assertEquals(4.0, perf.averageListingGainPct!!, 1e-9)
        assertEquals("1", perf.best!!.id)
        assertEquals("2", perf.worst!!.id)
        assertEquals(listOf(2026, 2025), listingYears(records))
        assertNull(listingPerformance(records, YearMonth.of(2024, 1)).averageListingGainPct)
    }

    @Test
    fun sortsNewestFirstWithUndatedLast() {
        val sorted = sortListedHistory(
            listOf(record("a", null, 1.0), record("b", LocalDate.of(2026, 1, 1), 1.0), record("c", LocalDate.of(2026, 5, 1), 1.0))
        )
        assertEquals(listOf("c", "b", "a"), sorted.map { it.id })
    }

    @Test
    fun corporateActionAdjustsCurrentPrice() {
        val base = record("1", LocalDate.of(2026, 1, 1), 10.0, issue = 200.0)
        val adjusted = base.withExtras(IpoExtras(corporateAction = "1:2 split", adjustedCurrentPrice = 300.0))
        assertTrue(adjusted.adjusted)
        assertEquals(50.0, adjusted.currentGainPct!!, 1e-9)
        // Action known but no adjusted price: keep the raw price, not flagged as adjusted
        val raw = base.withExtras(IpoExtras(corporateAction = "1:1 bonus"))
        assertFalse(raw.adjusted)
        assertEquals(150.0, raw.currentPrice!!, 0.0)
        assertEquals(base, base.withExtras(null))
    }
}
