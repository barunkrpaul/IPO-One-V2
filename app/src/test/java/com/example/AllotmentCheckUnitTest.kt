package com.example

import com.example.data.model.AllotmentOutcome
import com.example.data.model.AllotmentResult
import com.example.data.model.IpoApplication
import com.example.data.model.PanProfile
import com.example.data.model.RegistrarRetry
import com.example.data.model.allotmentTargets
import com.example.data.model.classifyAllotmentText
import com.example.data.model.maskUpiId
import com.example.data.model.matchesAllotmentSearch
import com.example.data.model.newPageLines
import com.example.data.model.pageLineSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AllotmentCheckUnitTest {

    @Test
    fun readsAllottedShareCounts() {
        assertEquals(
            AllotmentOutcome(AllotmentResult.ALLOTTED, sharesAllotted = 21),
            classifyAllotmentText("Name: RAHUL SHARMA\nSecurities Allotted : 21")
        )
        assertEquals(1200L, classifyAllotmentText("Allotted Shares - 1,200")!!.sharesAllotted)
        assertEquals(
            AllotmentResult.NOT_ALLOTTED,
            classifyAllotmentText("Shares Applied 21\nShares Allotted 0")!!.result
        )
        assertEquals(AllotmentResult.NOT_ALLOTTED, classifyAllotmentText("Status: NON ALLOTTED")!!.result)
        assertEquals(AllotmentResult.ALLOTTED, classifyAllotmentText("Congratulations! You have been allotted")!!.result)
    }

    @Test
    fun noRecordIsUnverifiedAndBlankIsPending() {
        val none = classifyAllotmentText("No Record Found for the given PAN")!!
        assertEquals(AllotmentResult.UNVERIFIED, none.result)
        assertNull(classifyAllotmentText(""))
        assertNull(classifyAllotmentText("Please wait while we fetch your details"))
    }

    @Test
    fun formInstructionsAreNotMistakenForResults() {
        val form = "Select Company\nPAN\nCheck whether allotted or not allotted\nCaptcha"
        val baseline = pageLineSet(form)
        // Nothing new yet: the instruction line is ignored
        assertNull(classifyAllotmentText(newPageLines(baseline, form)))
        val after = form + "\nSecurities Allotted : 0"
        assertEquals(AllotmentResult.NOT_ALLOTTED, classifyAllotmentText(newPageLines(baseline, after))!!.result)
    }

    @Test
    fun backoffDoublesUpToThreeAttempts() {
        assertEquals(3, RegistrarRetry.MAX_ATTEMPTS)
        assertEquals(0L, RegistrarRetry.delayBeforeAttempt(1))
        assertEquals(2_000L, RegistrarRetry.delayBeforeAttempt(2))
        assertEquals(4_000L, RegistrarRetry.delayBeforeAttempt(3))
    }

    @Test
    fun targetsMergeVaultAndApplications() {
        val pans = listOf(
            PanProfile(id = "p1", holderName = "zara", panNumber = "zzzzz1111z"),
            PanProfile(id = "p2", holderName = "Amit", panNumber = "AAAAA1111A")
        )
        val apps = listOf(
            IpoApplication(id = "a1", ipoId = "42", ipoName = "Acme", panHolderName = "Amit", panNumber = "AAAAA1111A"),
            IpoApplication(id = "a2", ipoId = "42", ipoName = "Acme", panHolderName = "Neha", panNumber = "NNNNN1111N"),
            IpoApplication(id = "a3", ipoId = "7", ipoName = "Other", panHolderName = "Om", panNumber = "OOOOO1111O")
        )
        val targets = allotmentTargets("42", "Acme", pans, apps)
        assertEquals(listOf("Amit", "Neha", "zara"), targets.map { it.holderName })
        assertEquals(listOf("a1", "a2", null), targets.map { it.applicationId })
        assertEquals("ZZZZZ1111Z", targets.last().panNumber)
    }

    @Test
    fun searchMatchesIpoHolderAndResult() {
        val app = IpoApplication(
            ipoName = "Tonbo Imaging", panHolderName = "Rahul Sharma", status = "Allotted",
            allotmentResult = "ALLOTTED", sharesAllotted = 21
        )
        assertTrue(matchesAllotmentSearch(app, "tonbo"))
        assertTrue(matchesAllotmentSearch(app, "rahul"))
        assertTrue(matchesAllotmentSearch(app, "21 shares"))
        assertTrue(matchesAllotmentSearch(app, ""))
        assertFalse(matchesAllotmentSearch(app, "not allotted"))
        val unverified = app.copy(status = "Applied", allotmentResult = "UNVERIFIED", sharesAllotted = null)
        assertTrue(matchesAllotmentSearch(unverified, "could not verify"))
    }

    @Test
    fun masksUpi() {
        assertEquals("ra**********@okhdfcbank", maskUpiId("rahul.sharma@okhdfcbank"))
        assertEquals("a*@ybl", maskUpiId("a@ybl"))
        assertEquals("", maskUpiId("  "))
    }
}
