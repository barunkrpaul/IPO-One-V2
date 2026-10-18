package com.example.data.local

import com.example.data.local.dao.ApplicationDao
import com.example.data.local.dao.DematDao
import com.example.data.local.dao.IpoDao
import com.example.data.local.entity.DematAccountEntity
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.IpoApplicationEntity
import com.example.data.local.entity.IpoEntity
import com.example.data.local.entity.StatusEventEntity
import com.example.data.local.entity.UpiAccountEntity
import com.example.model.AllotmentStatus
import com.example.model.IpoCategory
import com.example.model.IpoSeries
import com.example.model.IpoStatus
import com.example.model.MandateStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SampleDataSeeder {

    /**
     * Complete live IPO feed covering:
     * - Open Now: Applications currently going on (Waaree Energies, Hyundai Motor, TechNova SME, Danish Power SME)
     * - Upcoming: High-profile issues opening soon (Swiggy, NTPC Green Energy, Afcons Infra, ACME Solar, Sagility India)
     * - Recent Allotted / Listed benchmarks for tracking
     */
    fun getLiveIposFeed(): List<IpoEntity> {
        return listOf(
            // 1. Waaree Energies Ltd - Open Now (India's Largest Solar Module Manufacturer)
            IpoEntity(
                companyName = "Waaree Energies Ltd",
                symbol = "WAAREEENER",
                series = IpoSeries.MAINBOARD.displayName,
                minPrice = 1427.0,
                maxPrice = 1503.0,
                lotSize = 9,
                issueSizeCr = 4321.44,
                openDate = "2026-10-21",
                closeDate = "2026-10-23",
                allotmentDate = "2026-10-24",
                refundDate = "2026-10-25",
                listingDate = "2026-10-28",
                registrarName = "Link Intime India",
                registrarUrl = "https://linkintime.co.in/initial_offer/public-issues.html",
                status = IpoStatus.OPEN.displayName,
                gmpInr = 1425.0,
                gmpPercent = 94.8,
                subscriptionTimes = 76.34,
                qibSub = 65.20,
                niiSub = 62.48,
                retailSub = 10.79,
                totalSub = 76.34,
                aboutCompany = "India's largest manufacturer of solar PV modules with an aggregate installed capacity of 12 GW and extensive exports across USA & Europe.",
                isFavorite = true
            ),

            // 2. Hyundai Motor India Ltd - Open Now (India's Largest Ever Mega IPO)
            IpoEntity(
                companyName = "Hyundai Motor India Ltd",
                symbol = "HYUNDAI",
                series = IpoSeries.MAINBOARD.displayName,
                minPrice = 1865.0,
                maxPrice = 1960.0,
                lotSize = 7,
                issueSizeCr = 27870.16,
                openDate = "2026-10-15",
                closeDate = "2026-10-17",
                allotmentDate = "2026-10-18",
                refundDate = "2026-10-21",
                listingDate = "2026-10-22",
                registrarName = "KFin Technologies",
                registrarUrl = "https://kosmic.kfintech.com/ipostatus/",
                status = IpoStatus.OPEN.displayName,
                gmpInr = 65.0,
                gmpPercent = 3.3,
                subscriptionTimes = 2.37,
                qibSub = 6.97,
                niiSub = 0.60,
                retailSub = 0.50,
                totalSub = 2.37,
                aboutCompany = "India's second-largest passenger vehicle manufacturer offering iconic models (Creta, Venue, Verna) with dedicated R&D and EV manufacturing hubs.",
                isFavorite = true
            ),

            // 3. TechNova Solutions Ltd - Open Now (AI Automation & BFSI Tech SME)
            IpoEntity(
                companyName = "TechNova Solutions Ltd",
                symbol = "TECHNOVA",
                series = IpoSeries.SME.displayName,
                minPrice = 120.0,
                maxPrice = 128.0,
                lotSize = 1000,
                issueSizeCr = 48.50,
                openDate = "2026-10-17",
                closeDate = "2026-10-20",
                allotmentDate = "2026-10-21",
                refundDate = "2026-10-22",
                listingDate = "2026-10-23",
                registrarName = "Bigshare Services",
                registrarUrl = "https://www.bigshareonline.com/ipo_Allotment.html",
                status = IpoStatus.OPEN.displayName,
                gmpInr = 95.0,
                gmpPercent = 74.2,
                subscriptionTimes = 112.50,
                qibSub = 88.40,
                niiSub = 164.20,
                retailSub = 98.70,
                totalSub = 112.50,
                aboutCompany = "Emerging enterprise AI automation platform delivering cognitive document workflows and LLM agent orchestration for global fintechs.",
                isFavorite = false
            ),

            // 4. Danish Power Ltd - Open Now (Power & Inverter Transformers SME)
            IpoEntity(
                companyName = "Danish Power Ltd",
                symbol = "DANISHPOW",
                series = IpoSeries.SME.displayName,
                minPrice = 360.0,
                maxPrice = 380.0,
                lotSize = 300,
                issueSizeCr = 197.90,
                openDate = "2026-10-22",
                closeDate = "2026-10-24",
                allotmentDate = "2026-10-25",
                refundDate = "2026-10-28",
                listingDate = "2026-10-29",
                registrarName = "Link Intime India",
                registrarUrl = "https://linkintime.co.in/initial_offer/public-issues.html",
                status = IpoStatus.OPEN.displayName,
                gmpInr = 250.0,
                gmpPercent = 65.8,
                subscriptionTimes = 78.40,
                qibSub = 41.20,
                niiSub = 102.50,
                retailSub = 65.30,
                totalSub = 78.40,
                aboutCompany = "Leading transformer manufacturing powerhouse engineering oil-immersed distribution, dry-type, and renewable substation power transformers.",
                isFavorite = false
            ),

            // 5. Swiggy Ltd - Upcoming (Food Delivery & Instamart Quick Commerce)
            IpoEntity(
                companyName = "Swiggy Ltd",
                symbol = "SWIGGY",
                series = IpoSeries.MAINBOARD.displayName,
                minPrice = 371.0,
                maxPrice = 390.0,
                lotSize = 38,
                issueSizeCr = 11327.43,
                openDate = "2026-11-06",
                closeDate = "2026-11-08",
                allotmentDate = "2026-11-11",
                refundDate = "2026-11-12",
                listingDate = "2026-11-13",
                registrarName = "Link Intime India",
                registrarUrl = "https://linkintime.co.in/initial_offer/public-issues.html",
                status = IpoStatus.UPCOMING.displayName,
                gmpInr = 32.0,
                gmpPercent = 8.2,
                subscriptionTimes = 0.0,
                qibSub = 0.0,
                niiSub = 0.0,
                retailSub = 0.0,
                totalSub = 0.0,
                aboutCompany = "Pioneering Indian consumer-first tech platform operating online food delivery, Instamart 10-minute grocery delivery, Dineout, and Genie.",
                isFavorite = true
            ),

            // 6. NTPC Green Energy Ltd - Upcoming (Maharatna PSU Clean Energy)
            IpoEntity(
                companyName = "NTPC Green Energy Ltd",
                symbol = "NTPCGREEN",
                series = IpoSeries.MAINBOARD.displayName,
                minPrice = 102.0,
                maxPrice = 108.0,
                lotSize = 138,
                issueSizeCr = 10000.00,
                openDate = "2026-11-19",
                closeDate = "2026-11-22",
                allotmentDate = "2026-11-25",
                refundDate = "2026-11-26",
                listingDate = "2026-11-27",
                registrarName = "KFin Technologies",
                registrarUrl = "https://kosmic.kfintech.com/ipostatus/",
                status = IpoStatus.UPCOMING.displayName,
                gmpInr = 22.0,
                gmpPercent = 20.4,
                subscriptionTimes = 0.0,
                qibSub = 0.0,
                niiSub = 0.0,
                retailSub = 0.0,
                totalSub = 0.0,
                aboutCompany = "India's largest renewable energy public sector enterprise and wholly-owned clean power subsidiary of NTPC Limited with 24 GW solar pipeline.",
                isFavorite = false
            ),

            // 7. Afcons Infrastructure Ltd - Upcoming (Shapoorji Pallonji Mega Engineering)
            IpoEntity(
                companyName = "Afcons Infrastructure Ltd",
                symbol = "AFCONS",
                series = IpoSeries.MAINBOARD.displayName,
                minPrice = 440.0,
                maxPrice = 463.0,
                lotSize = 32,
                issueSizeCr = 5430.00,
                openDate = "2026-10-25",
                closeDate = "2026-10-29",
                allotmentDate = "2026-10-30",
                refundDate = "2026-10-31",
                listingDate = "2026-11-04",
                registrarName = "Link Intime India",
                registrarUrl = "https://linkintime.co.in/initial_offer/public-issues.html",
                status = IpoStatus.UPCOMING.displayName,
                gmpInr = 80.0,
                gmpPercent = 17.3,
                subscriptionTimes = 0.0,
                qibSub = 0.0,
                niiSub = 0.0,
                retailSub = 0.0,
                totalSub = 0.0,
                aboutCompany = "Shapoorji Pallonji Group's flagship engineering infrastructure giant with historic mega-projects including the Atal Tunnel and Chenab Bridge.",
                isFavorite = false
            ),

            // 8. ACME Solar Holdings Ltd - Upcoming (Renewable Energy Producer)
            IpoEntity(
                companyName = "ACME Solar Holdings Ltd",
                symbol = "ACMESOLAR",
                series = IpoSeries.MAINBOARD.displayName,
                minPrice = 275.0,
                maxPrice = 289.0,
                lotSize = 51,
                issueSizeCr = 2900.00,
                openDate = "2026-11-06",
                closeDate = "2026-11-08",
                allotmentDate = "2026-11-11",
                refundDate = "2026-11-12",
                listingDate = "2026-11-13",
                registrarName = "KFin Technologies",
                registrarUrl = "https://kosmic.kfintech.com/ipostatus/",
                status = IpoStatus.UPCOMING.displayName,
                gmpInr = 35.0,
                gmpPercent = 12.1,
                subscriptionTimes = 0.0,
                qibSub = 0.0,
                niiSub = 0.0,
                retailSub = 0.0,
                totalSub = 0.0,
                aboutCompany = "One of India's prominent renewable independent power producers (IPP) with over 2.7 GW operational utility solar and battery energy storage.",
                isFavorite = false
            ),

            // 9. Sagility India Ltd - Upcoming (Healthcare Services Tech)
            IpoEntity(
                companyName = "Sagility India Ltd",
                symbol = "SAGILITY",
                series = IpoSeries.MAINBOARD.displayName,
                minPrice = 28.0,
                maxPrice = 30.0,
                lotSize = 500,
                issueSizeCr = 2106.60,
                openDate = "2026-11-05",
                closeDate = "2026-11-07",
                allotmentDate = "2026-11-08",
                refundDate = "2026-11-11",
                listingDate = "2026-11-12",
                registrarName = "Link Intime India",
                registrarUrl = "https://linkintime.co.in/initial_offer/public-issues.html",
                status = IpoStatus.UPCOMING.displayName,
                gmpInr = 3.5,
                gmpPercent = 11.7,
                subscriptionTimes = 0.0,
                qibSub = 0.0,
                niiSub = 0.0,
                retailSub = 0.0,
                totalSub = 0.0,
                aboutCompany = "Pure-play healthcare operations and analytics services partner delivering tech-enabled solutions for top American health insurers.",
                isFavorite = false
            ),

            // 10. Premier Energies Ltd - Allotment Finalized Benchmark
            IpoEntity(
                companyName = "Premier Energies Ltd",
                symbol = "PREMIERENE",
                series = IpoSeries.MAINBOARD.displayName,
                minPrice = 427.0,
                maxPrice = 450.0,
                lotSize = 33,
                issueSizeCr = 2830.00,
                openDate = "2026-10-16",
                closeDate = "2026-10-18",
                allotmentDate = "2026-10-19",
                refundDate = "2026-10-20",
                listingDate = "2026-10-21",
                registrarName = "KFin Technologies",
                registrarUrl = "https://kosmic.kfintech.com/ipostatus/",
                status = IpoStatus.ALLOTTED.displayName,
                gmpInr = 380.0,
                gmpPercent = 84.4,
                subscriptionTimes = 74.30,
                qibSub = 216.67,
                niiSub = 50.04,
                retailSub = 7.69,
                totalSub = 74.30,
                aboutCompany = "Integrated solar cell and solar module manufacturer with automated high-efficiency production lines.",
                isFavorite = true
            ),

            // 11. Bajaj Housing Finance Ltd - Listed Benchmark
            IpoEntity(
                companyName = "Bajaj Housing Finance Ltd",
                symbol = "BAJAJHFL",
                series = IpoSeries.MAINBOARD.displayName,
                minPrice = 66.0,
                maxPrice = 70.0,
                lotSize = 214,
                issueSizeCr = 6560.00,
                openDate = "2026-10-05",
                closeDate = "2026-10-07",
                allotmentDate = "2026-10-08",
                refundDate = "2026-10-09",
                listingDate = "2026-10-10",
                registrarName = "KFin Technologies",
                registrarUrl = "https://kosmic.kfintech.com/ipostatus/",
                status = IpoStatus.LISTED.displayName,
                gmpInr = 75.0,
                gmpPercent = 107.1,
                subscriptionTimes = 63.60,
                qibSub = 209.36,
                niiSub = 41.51,
                retailSub = 7.04,
                totalSub = 63.60,
                aboutCompany = "India's premier non-deposit housing finance company focused on prime residential mortgage lending.",
                isFavorite = false
            ),

            // 12. Tata Technologies Ltd - Listed Benchmark
            IpoEntity(
                companyName = "Tata Technologies Ltd",
                symbol = "TATATECH",
                series = IpoSeries.MAINBOARD.displayName,
                minPrice = 475.0,
                maxPrice = 500.0,
                lotSize = 30,
                issueSizeCr = 3042.00,
                openDate = "2026-10-01",
                closeDate = "2026-10-03",
                allotmentDate = "2026-10-04",
                refundDate = "2026-10-05",
                listingDate = "2026-10-08",
                registrarName = "Link Intime India",
                registrarUrl = "https://linkintime.co.in/initial_offer/public-issues.html",
                status = IpoStatus.LISTED.displayName,
                gmpInr = 410.0,
                gmpPercent = 82.0,
                subscriptionTimes = 69.40,
                qibSub = 203.41,
                niiSub = 62.11,
                retailSub = 16.50,
                totalSub = 69.40,
                aboutCompany = "Global product engineering and digital services company subsidiary of Tata Motors.",
                isFavorite = false
            )
        )
    }

    /**
     * Upserts all live IPOs into the database:
     * - Preserves user favorites and Room primary keys if an IPO already exists.
     * - Updates prices, issue sizes, GMP, real-time subscription figures, and T+3 dates.
     * - Adds new upcoming & open IPOs.
     */
    suspend fun deployLiveIpoData(ipoDao: IpoDao): Int = withContext(Dispatchers.IO) {
        val feed = getLiveIposFeed()
        var updatedCount = 0
        feed.forEach { liveItem ->
            val existing = ipoDao.getIpoBySymbol(liveItem.symbol)
            if (existing != null) {
                ipoDao.updateIpo(
                    liveItem.copy(
                        id = existing.id,
                        isFavorite = existing.isFavorite,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                ipoDao.insertIpo(liveItem.copy(updatedAt = System.currentTimeMillis()))
            }
            updatedCount++
        }
        updatedCount
    }

    suspend fun seedIfEmpty(
        ipoDao: IpoDao,
        dematDao: DematDao,
        applicationDao: ApplicationDao
    ) = withContext(Dispatchers.IO) {
        // If accounts already exist, ensure live IPOs are synced without overwriting accounts
        if (dematDao.getMemberCount() > 0) {
            deployLiveIpoData(ipoDao)
            return@withContext
        }

        // 1. Seed Family Members
        val rahulId = dematDao.insertFamilyMember(
            FamilyMemberEntity(
                fullName = "Rahul Sharma",
                relationship = "Self",
                pan = "ABCPS1234F",
                isPrimary = true
            )
        )
        val poojaId = dematDao.insertFamilyMember(
            FamilyMemberEntity(
                fullName = "Pooja Sharma",
                relationship = "Spouse",
                pan = "ABCPS5678M",
                isPrimary = false
            )
        )
        val rameshId = dematDao.insertFamilyMember(
            FamilyMemberEntity(
                fullName = "Ramesh Sharma",
                relationship = "Father",
                pan = "ABFPS9012K",
                isPrimary = false
            )
        )

        // 2. Seed Demat Accounts
        val rahulZerodhaId = dematDao.insertDematAccount(
            DematAccountEntity(
                familyMemberId = rahulId,
                brokerName = "Zerodha",
                depository = "CDSL",
                dpId = "12081600",
                clientId = "01234567",
                accountAlias = "Rahul Zerodha",
                isDefault = true
            )
        )
        val rahulGrowwId = dematDao.insertDematAccount(
            DematAccountEntity(
                familyMemberId = rahulId,
                brokerName = "Groww",
                depository = "CDSL",
                dpId = "12088800",
                clientId = "89012345",
                accountAlias = "Rahul Groww",
                isDefault = false
            )
        )
        val poojaAngelId = dematDao.insertDematAccount(
            DematAccountEntity(
                familyMemberId = poojaId,
                brokerName = "Angel One",
                depository = "CDSL",
                dpId = "12033200",
                clientId = "34567890",
                accountAlias = "Pooja AngelOne",
                isDefault = true
            )
        )
        val rameshUpstoxId = dematDao.insertDematAccount(
            DematAccountEntity(
                familyMemberId = rameshId,
                brokerName = "Upstox",
                depository = "CDSL",
                dpId = "12081800",
                clientId = "56789012",
                accountAlias = "Dad Upstox",
                isDefault = true
            )
        )

        // 3. Seed UPI Accounts
        val rahulUpiId = dematDao.insertUpiAccount(
            UpiAccountEntity(
                familyMemberId = rahulId,
                vpa = "rahul@okhdfcbank",
                bankName = "HDFC Bank",
                isDefault = true
            )
        )
        val poojaUpiId = dematDao.insertUpiAccount(
            UpiAccountEntity(
                familyMemberId = poojaId,
                vpa = "pooja@icici",
                bankName = "ICICI Bank",
                isDefault = true
            )
        )
        val rameshUpiId = dematDao.insertUpiAccount(
            UpiAccountEntity(
                familyMemberId = rameshId,
                vpa = "ramesh@sbi",
                bankName = "State Bank of India",
                isDefault = true
            )
        )

        // 4. Deploy complete live IPO roster
        deployLiveIpoData(ipoDao)

        val hyundai = ipoDao.getIpoBySymbol("HYUNDAI")
        val tataTech = ipoDao.getIpoBySymbol("TATATECH")
        val premier = ipoDao.getIpoBySymbol("PREMIERENE")

        val hyundaiId = hyundai?.id ?: 1L
        val tataTechId = tataTech?.id ?: 2L
        val premierId = premier?.id ?: 3L

        // 5. Seed Applications
        val app1Id = applicationDao.insertApplication(
            IpoApplicationEntity(
                ipoId = hyundaiId,
                dematAccountId = rahulZerodhaId,
                familyMemberId = rahulId,
                upiVpa = "rahul@okhdfcbank",
                upiAccountId = rahulUpiId,
                category = IpoCategory.RETAIL.displayName,
                lotCount = 1,
                bidPrice = 1960.0,
                isCutoff = true,
                totalAmount = 13720.0,
                applicationNumber = "1049283741",
                mandateStatus = MandateStatus.PENDING.displayName,
                allotmentStatus = AllotmentStatus.AWAITING.displayName,
                mandateExpiry = "Today 5:00 PM",
                checklistBrokerSubmitted = true,
                checklistMandateAccepted = false,
                checklistAllotmentChecked = false,
                notes = "Bid placed on Zerodha Console. Awaiting GPay UPI notification."
            )
        )
        applicationDao.insertStatusEvent(
            StatusEventEntity(
                applicationId = app1Id,
                eventType = "SUBMITTED",
                description = "Bid submitted on broker for 1 Lot @ Cutoff ₹1,960. Mandate requested on rahul@okhdfcbank."
            )
        )

        val app2Id = applicationDao.insertApplication(
            IpoApplicationEntity(
                ipoId = hyundaiId,
                dematAccountId = poojaAngelId,
                familyMemberId = poojaId,
                upiVpa = "pooja@icici",
                upiAccountId = poojaUpiId,
                category = IpoCategory.RETAIL.displayName,
                lotCount = 1,
                bidPrice = 1960.0,
                isCutoff = true,
                totalAmount = 13720.0,
                applicationNumber = "1049283742",
                mandateStatus = MandateStatus.APPROVED.displayName,
                allotmentStatus = AllotmentStatus.AWAITING.displayName,
                mandateExpiry = "Approved",
                checklistBrokerSubmitted = true,
                checklistMandateAccepted = true,
                checklistAllotmentChecked = false,
                notes = "Mandate authorized on iMobile app. Bank funds blocked."
            )
        )
        applicationDao.insertStatusEvent(
            StatusEventEntity(
                applicationId = app2Id,
                eventType = "MANDATE_APPROVED",
                description = "UPI Mandate successfully accepted for ₹13,720."
            )
        )

        val app3Id = applicationDao.insertApplication(
            IpoApplicationEntity(
                ipoId = tataTechId,
                dematAccountId = rahulGrowwId,
                familyMemberId = rahulId,
                upiVpa = "rahul@okhdfcbank",
                upiAccountId = rahulUpiId,
                category = IpoCategory.RETAIL.displayName,
                lotCount = 1,
                bidPrice = 500.0,
                isCutoff = true,
                totalAmount = 15000.0,
                applicationNumber = "1038291048",
                mandateStatus = MandateStatus.APPROVED.displayName,
                allotmentStatus = AllotmentStatus.ALLOTTED.displayName,
                sharesAllotted = 30,
                refundAmount = 0.0,
                checklistBrokerSubmitted = true,
                checklistMandateAccepted = true,
                checklistAllotmentChecked = true,
                notes = "Allotment confirmed! 30 shares credited to Demat account."
            )
        )
        applicationDao.insertStatusEvent(
            StatusEventEntity(
                applicationId = app3Id,
                eventType = "ALLOTTED",
                description = "Congratulations! 30 shares allotted by Link Intime."
            )
        )

        val app4Id = applicationDao.insertApplication(
            IpoApplicationEntity(
                ipoId = premierId,
                dematAccountId = rameshUpstoxId,
                familyMemberId = rameshId,
                upiVpa = "ramesh@sbi",
                upiAccountId = rameshUpiId,
                category = IpoCategory.RETAIL.displayName,
                lotCount = 1,
                bidPrice = 450.0,
                isCutoff = true,
                totalAmount = 14850.0,
                applicationNumber = "1059483719",
                mandateStatus = MandateStatus.APPROVED.displayName,
                allotmentStatus = AllotmentStatus.AWAITING.displayName,
                checklistBrokerSubmitted = true,
                checklistMandateAccepted = true,
                checklistAllotmentChecked = false,
                notes = "Mandate approved on Yono SBI."
            )
        )
        applicationDao.insertStatusEvent(
            StatusEventEntity(
                applicationId = app4Id,
                eventType = "MANDATE_APPROVED",
                description = "UPI Mandate accepted for ₹14,850."
            )
        )
    }
}
