package com.example

import com.example.data.model.IpoApplication
import com.example.data.model.IpoBoardFilter
import com.example.data.model.IpoStatusFilter
import com.example.data.model.PanProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IpoDomainUnitTest {

    @Test
    fun testPanProfileCreation() {
        val profile = PanProfile(
            id = "pan_1",
            userId = "user_123",
            holderName = "Rahul Sharma",
            panNumber = "ABCPS1234F",
            dematId = "1208160012345678",
            upiId = "rahul@okhdfcbank",
            category = "Retail"
        )
        assertEquals("Rahul Sharma", profile.holderName)
        assertEquals("ABCPS1234F", profile.panNumber)
        assertEquals("Retail", profile.category)
    }

    @Test
    fun testIpoApplicationCalculation() {
        val lots = 2L
        val lotSize = 50L
        val bidPrice = 300L
        val shares = lots * lotSize
        val totalAmount = shares * bidPrice

        val application = IpoApplication(
            id = "app_1",
            userId = "user_123",
            ipoId = "ipo_101",
            ipoName = "Acme Technologies",
            panHolderName = "Rahul Sharma",
            panNumber = "ABCPS1234F",
            lotsApplied = lots,
            sharesApplied = shares,
            bidPrice = bidPrice,
            totalAmount = totalAmount,
            status = "Applied"
        )
        assertEquals(100L, application.sharesApplied)
        assertEquals(30000L, application.totalAmount)
        assertEquals("Applied", application.status)
    }

    @Test
    fun testFilterLabels() {
        assertEquals("All Boards", IpoBoardFilter.ALL.label)
        assertEquals("Mainboard", IpoBoardFilter.MAINBOARD.label)
        assertEquals("SME", IpoBoardFilter.SME.label)
        assertEquals("Open Now", IpoStatusFilter.OPEN.label)
        assertEquals("All Live", IpoStatusFilter.ALL.label)
    }
}
