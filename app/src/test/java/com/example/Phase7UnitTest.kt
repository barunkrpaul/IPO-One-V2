package com.example

import com.example.data.model.IntermediaryDirectory
import com.example.data.model.IntermediaryEntry
import com.example.data.model.IntermediaryRole
import com.example.data.model.IpoApplication
import com.example.data.model.IpoExtras
import com.example.data.model.IpoRef
import com.example.data.model.ListedIpoRecord
import com.example.data.model.LiveIpoItem
import com.example.data.model.ReminderType
import com.example.data.model.allotmentWatchCandidates
import com.example.data.model.dueDateReminders
import com.example.data.model.globalSearch
import com.example.data.model.pruneReminderKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class Phase7UnitTest {

    private val today = LocalDate.of(2026, 10, 19)

    private fun ipo(id: String, name: String, open: String, close: String, boa: String, listing: String) = LiveIpoItem(
        id = id, name = name, statusCode = "U", statusLabel = "", category = "Mainboard", sector = "Defence",
        listingExchange = "BSE, NSE", issuePrice = 100, gmpAmount = 0.0, gmpPercent = 0.0, gmpRangeText = "",
        estimatedListingPrice = 100.0, fireRating = 0, totalSubscription = "-", qibSubscription = "-",
        niiSubscription = "-", shniSubscription = "-", bhniSubscription = "-", riiSubscription = "-",
        issueSizeCr = "TBA", lotSize = 10, minInvestmentInr = 1000, estProfitPerLotInr = 0, peRatio = "-",
        openDate = open, closeDate = close, boaDate = boa, listingDate = listing,
        sortOpenDate = "", sortCloseDate = "", updatedOn = "", hasAnchor = false, investorGainUrl = ""
    )

    private val ipos = listOf(
        ipo("1", "Tonbo Imaging", "15-Oct", "19-Oct", "21-Oct", "23-Oct"),
        ipo("2", "Fusion CX", "19-Oct", "21-Oct", "22-Oct", "24-Oct"),
        ipo("3", "Acme Rail", "13-Oct", "15-Oct", "18-Oct", "19-Oct")
    )

    @Test
    fun dateRemindersRespectTrackingTogglesTimeAndSentKeys() {
        val all = ReminderType.entries.toSet()
        val due = dueDateReminders(ipos, setOf("1", "2", "3"), all, today, hourIst = 9, sentKeys = emptySet())
        assertEquals(
            setOf("CLOSES_TODAY:1", "OPENS_TODAY:2", "LISTING_TODAY:3"),
            due.map { "${it.type}:${it.ipoId}" }.toSet()
        )
        assertTrue(dueDateReminders(ipos, setOf("1", "2", "3"), all, today, hourIst = 6, sentKeys = emptySet()).isEmpty())
        assertEquals(1, dueDateReminders(ipos, setOf("2"), all, today, 9, emptySet()).size)
        assertTrue(dueDateReminders(ipos, setOf("2"), setOf(ReminderType.CLOSES_TODAY), today, 9, emptySet()).isEmpty())
        val sent = due.map { it.key }.toSet()
        assertTrue(dueDateReminders(ipos, setOf("1", "2", "3"), all, today, 9, sent).isEmpty())
    }

    @Test
    fun allotmentWatchCoversTodayAndYesterday() {
        val candidates = allotmentWatchCandidates(ipos, setOf("1", "2", "3"), LocalDate.of(2026, 10, 22), emptySet())
        // Fusion's allotment is today, Tonbo's was yesterday, Acme's was days ago
        assertEquals(setOf("1", "2"), candidates.map { it.first.id }.toSet())
    }

    @Test
    fun prunesOldReminderKeys() {
        val keys = setOf("OPENS_TODAY:1:2026-10-19", "CLOSES_TODAY:2:2026-10-10", "junk")
        assertEquals(setOf("OPENS_TODAY:1:2026-10-19"), pruneReminderKeys(keys, today))
    }

    @Test
    fun globalSearchGroupsResults() {
        val history = listOf(
            ListedIpoRecord("1", "Tonbo Imaging", "TONBO", "Mainboard", 700.0, null, null, null, null, null, ""),
            ListedIpoRecord("9", "Tonbridge Steel", "TBS", "SME", 50.0, 55.0, 10.0, 60.0, 20.0, today, "")
        )
        val directory = IntermediaryDirectory(
            mapOf(
                IntermediaryRole.REGISTRAR to listOf(
                    IntermediaryEntry(IntermediaryRole.REGISTRAR, "kfin", "Kfin Technologies", null, listOf(IpoRef("1", "Tonbo", "Mainboard", null, null, "")))
                )
            ),
            1, 1
        )
        val apps = listOf(IpoApplication(id = "a", ipoName = "Tonbo Imaging", panHolderName = "Rahul"))
        val results = globalSearch("ton", ipos, history, directory, apps, mapOf("2" to IpoExtras(symbol = "FUSIONCX")))
        assertEquals(listOf("1"), results.ipos.map { it.id })
        assertEquals(listOf("9"), results.listed.map { it.id }) // the live Tonbo isn't repeated
        assertEquals(1, results.applications.size)
        assertEquals(listOf("2"), globalSearch("fusioncx", ipos, history, directory, apps, mapOf("2" to IpoExtras(symbol = "FUSIONCX"))).ipos.map { it.id })
        assertEquals(1, globalSearch("kfin", ipos, history, directory, apps, emptyMap()).intermediaries.size)
        assertTrue(globalSearch("t", ipos, history, directory, apps, emptyMap()).isEmpty)
    }
}
