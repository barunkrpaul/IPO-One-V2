package com.example

import com.example.data.model.IntermediaryRole
import com.example.data.model.IpoExtras
import com.example.data.model.IpoPageDetails
import com.example.data.model.IpoPageParser
import com.example.data.model.IpoRef
import com.example.data.model.NamedIntermediary
import com.example.data.model.SubscriptionCategory
import com.example.data.model.buildIntermediaryDirectory
import com.example.data.model.mergedOver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class IntermediaryUnitTest {

    // Trimmed from an InvestorGain IPO page's flight payload (quotes arrive escaped)
    private val raw = """push([1,"{\"registrar_id\":2,\"lead_managers\":\"18,71\",\"nse_symbol\":\"TONBO\",""" +
        """\"logo_url\":\"tonbo-imaging-india-ltd-logo.png\",\"floor_price\":\"665.00\",""" +
        """\"timetable_share_credit_dt\":\"2026-10-22T00:00:00.000Z\",""" +
        """\"timetable_anchor_lockin_end_dt_1\":\"2026-11-20T00:00:00.000Z\",\"market_maker_id\":\"\",""" +
        """\"ipoLeadManagersList\":[{\"id\":18,\"comp_name\":\"JM Financial Ltd.\",\"urlrewrite_folder_name\":\"jm\"},""" +
        """{\"id\":71,\"comp_name\":\"IIFL [Capital] Ltd.\",\"urlrewrite_folder_name\":\"iifl\"}],\"next\":1}"])""" +
        """Market Maker of the company is <a href=\"/report/ipo-market-maker-vs-listing-gain/127/250/\" title=\"Hem Finlease Pvt.Ltd. Review\" style=\"x\">"""

    @Test
    fun parsesPagePayload() {
        val text = IpoPageParser.unescape(raw)
        val array = IpoPageParser.extractJsonArray(text, "ipoLeadManagersList")!!
        assert(array.startsWith("[{") && array.endsWith("}]")) { array }
        assert(array.contains("IIFL [Capital] Ltd.")) { "brackets inside strings must not end the array" }
        assertEquals("TONBO", IpoPageParser.extractValue(text, "nse_symbol"))
        assertEquals("665.00", IpoPageParser.extractValue(text, "floor_price"))
        assertEquals(2.toString(), IpoPageParser.extractValue(text, "registrar_id"))
        assertNull(IpoPageParser.extractValue(text, "market_maker_id"))
        assertEquals(LocalDate.of(2026, 10, 22), IpoPageParser.extractIsoDate(text, "timetable_share_credit_dt"))
        assertNull(IpoPageParser.extractIsoDate(text, "timetable_anchor_lockin_end_dt_2"))
        assertEquals(NamedIntermediary("250", "Hem Finlease Pvt.Ltd."), IpoPageParser.extractMarketMaker(text))
        assertEquals(
            "https://ipostatus.kfintech.com/",
            IpoPageParser.firstLink("<strong>Website:</strong> <a href=\"https://ipostatus.kfintech.com/\" target=\"_blank\">")
        )
        assertNull(IpoPageParser.extractJsonArray(text, "missingList"))
    }

    @Test
    fun unescapeKeepsNestedJsonValid() {
        // JS string holding JSON whose value holds an HTML attribute: \\\" must stay an escaped quote
        val raw = """[{\"info\":\"\u003ca href=\\\"https://x.in/\\\"\u003e\"}]"""
        val text = IpoPageParser.unescape(raw)
        assertEquals("""[{"info":"\u003ca href=\"https://x.in/\"\u003e"}]""", text)
        assertEquals(text, IpoPageParser.extractJsonArray("\"registrarInfo\":" + text, "registrarInfo"))
        assertEquals("https://x.in/", IpoPageParser.firstLink(text))
    }

    @Test
    fun subscriptionFromBiddingRow() {
        // Values from a real ipoBiddingData row
        val rows = IpoPageParser.subscriptionFromBidding(
            mapOf(
                "qib_offered" to "24,32,010", "qib_bid_amt" to "668.03", "qib" to "3.92",
                "nii_offered_big" to "12,16,005", "nii_bid_amt_big" to "388.67", "nii_big" to "4.57",
                "rii_offered" to "42,56,016", "rii" to "3.21",
                "emp_offered" to "14,000", "emp" to "6.52", "shareholder_offered" to "0",
                "total_offered" to "85,26,033", "total_bid_amt" to "2,392.33", "total" to "4.01"
            ),
            capPrice = 700.0
        )
        val qib = rows.getValue(SubscriptionCategory.QIB)
        assertEquals(3.92, qib.times!!, 0.0)
        assertEquals(170.24, qib.sharesOfferedCr!!, 0.01)
        assertEquals(668.03, qib.sharesAppliedCr!!, 0.0)
        assertEquals(28.52, qib.quotaPct!!, 0.01)
        assertEquals(6.52, rows.getValue(SubscriptionCategory.EMPLOYEE).times!!, 0.0)
        assertFalse(rows.containsKey(SubscriptionCategory.SHAREHOLDER))
        assertFalse(rows.containsKey(SubscriptionCategory.SNII))
        // SME pages hide the split and only give the total
        val sme = IpoPageParser.subscriptionFromBidding(mapOf("total" to "124.23", "total_offered" to "41,28,600"), null)
        assertEquals(setOf(SubscriptionCategory.TOTAL), sme.keys)
    }

    @Test
    fun firestoreExtrasWinOverPageDetails() {
        val page = IpoPageDetails(
            leadManagers = listOf(NamedIntermediary("18", "JM Financial Ltd.")),
            registrar = NamedIntermediary("2", "Kfin Technologies Ltd."),
            symbol = "TONBO"
        ).toExtras()
        val merged = IpoExtras(symbol = "TONBOX", preApplyOpen = true).mergedOver(page)
        assertEquals("TONBOX", merged.symbol)
        assertEquals(listOf("JM Financial Ltd."), merged.leadManagers)
        assertEquals("Kfin Technologies Ltd.", merged.registrar)
        assertEquals(true, merged.preApplyOpen)
    }

    @Test
    fun buildsDirectoryWithCounts() {
        fun ref(id: String, cat: String, gain: Double?) =
            IpoRef(id, "IPO $id", cat, gain, LocalDate.of(2026, 10, id.toInt()), "")
        val refs = listOf(ref("1", "Mainboard", 10.0), ref("2", "SME", -5.0), ref("3", "Mainboard", null), ref("4", "SME", 2.0))
        val extras = mapOf(
            "1" to IpoExtras(leadManagers = listOf("JM Financial Ltd.", "IIFL"), registrar = "Kfin Technologies Ltd.",
                registrarWebsite = "https://ipostatus.kfintech.com/"),
            "2" to IpoExtras(leadManagers = listOf("Hem Securities"), registrar = "Bigshare Services Pvt.Ltd.", marketMaker = "Hem Finlease"),
            "3" to IpoExtras(leadManagers = listOf("JM Financial Ltd"), registrar = "KFin Technologies Ltd.")
        )
        val dir = buildIntermediaryDirectory(refs, extras)
        assertEquals(3, dir.coveredIpoCount)
        assertEquals(4, dir.totalIpoCount)
        val lms = dir.entries.getValue(IntermediaryRole.LEAD_MANAGER)
        assertEquals("JM Financial Ltd.", lms.first().name)
        assertEquals(2, lms.first().mainboardCount)
        val kfin = dir.find(IntermediaryRole.REGISTRAR, "Kfin Technologies Ltd")!!
        assertEquals(2, kfin.ipos.size)
        assertEquals("https://ipostatus.kfintech.com/", kfin.website)
        assertEquals(10.0, kfin.averageListingGainPct!!, 0.0)
        assertEquals(listOf("3", "1"), kfin.ipos.map { it.id })
        assertEquals(1, dir.entries.getValue(IntermediaryRole.MARKET_MAKER).single().smeCount)
    }
}
