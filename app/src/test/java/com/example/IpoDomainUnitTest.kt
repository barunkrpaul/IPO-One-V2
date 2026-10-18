package com.example

import com.example.data.local.entity.FamilyMemberEntity
import com.example.model.IpoCategory
import com.example.model.IpoSeries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IpoDomainUnitTest {

    @Test
    fun testPanMasking() {
        val member = FamilyMemberEntity(
            fullName = "Rahul Sharma",
            relationship = "Self",
            pan = "ABCPS1234F",
            isPrimary = true
        )
        assertEquals("ABC****4F", member.maskedPan)
    }

    @Test
    fun testRetailCategoryLimit() {
        val lotSize = 7
        val price = 1960.0
        val lots = 1
        val total = lots * lotSize * price // 13,720.0
        assertTrue(total <= IpoCategory.RETAIL.maxLimitInr)
    }

    @Test
    fun testShniCategoryLimit() {
        val lotSize = 33
        val price = 450.0
        val lots = 14
        val total = lots * lotSize * price // 2,07,900.0
        assertTrue(total > IpoCategory.RETAIL.maxLimitInr)
        assertTrue(total <= IpoCategory.SHNI.maxLimitInr)
    }

    @Test
    fun testIpoSeriesDisplayNames() {
        assertEquals("Mainboard", IpoSeries.MAINBOARD.displayName)
        assertEquals("SME", IpoSeries.SME.displayName)
    }
}
