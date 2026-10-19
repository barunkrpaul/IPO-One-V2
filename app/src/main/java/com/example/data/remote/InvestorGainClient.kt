package com.example.data.remote

import com.example.data.model.IpoDates
import com.example.data.model.IpoPageDetails
import com.example.data.model.IpoPageParser
import com.example.data.model.NamedIntermediary
import com.example.data.model.ListedIpoRecord
import com.example.data.model.LiveIpoItem
import com.example.data.model.parseListingPriceFromNameCell
import com.example.data.model.parsePriceAndPercent
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class InvestorGainClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val BASE_API = "https://webnodejs.investorgain.com/cloud/v2"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36"
    }

    suspend fun fetchLiveIpos(): Result<List<LiveIpoItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val cal = Calendar.getInstance()
            val month = cal.get(Calendar.MONTH) + 1
            val year = cal.get(Calendar.YEAR)
            val fyStart = if (month >= 4) year else year - 1
            val fyEndShort = (fyStart + 1) % 100
            val fy = String.format("%d-%02d", fyStart, fyEndShort)

            val gmpDeferred = async {
                fetchReportTable(331, month, year, fy)
            }
            val subDeferred = async {
                runCatching { fetchReportTable(333, month, year, fy) }.getOrNull()
            }
            val metaDeferred = async {
                runCatching { fetchIpoListMeta() }.getOrDefault(emptyMap())
            }

            val gmpArray = gmpDeferred.await()
            val subArray = subDeferred.await()
            val metaMap = metaDeferred.await()

            val subMap = mutableMapOf<String, JSONObject>()
            if (subArray != null) {
                for (i in 0 until subArray.length()) {
                    val obj = subArray.optJSONObject(i) ?: continue
                    val id = obj.opt("~id")?.toString()?.trim().orEmpty()
                    if (id.isNotEmpty()) {
                        subMap[id] = obj
                    }
                }
            }

            val items = mutableListOf<LiveIpoItem>()
            for (i in 0 until gmpArray.length()) {
                val row = gmpArray.optJSONObject(i) ?: continue
                val id = row.opt("~id")?.toString()?.trim().orEmpty()
                if (id.isEmpty()) continue

                val rawName = row.optString("~ipo_name").trim().ifEmpty {
                    stripHtml(row.optString("Name")).replace(Regex("\\s+(IPO|SME)\\s+[A-Z]{1,2}$"), "").trim()
                }
                if (rawName.isEmpty()) continue

                val statusCode = row.optString("~ipo_status1", "U").trim()
                val statusLabel = when (statusCode) {
                    "O" -> "Open Now"
                    "CT" -> "Closing Today"
                    "U" -> "Upcoming"
                    "C" -> "Allotment / BoA"
                    "LP" -> "Listed (Gain)"
                    "LN" -> "Listed"
                    else -> "Active"
                }

                val categoryRaw = row.optString("~IPO_Category", row.optString("~ipo_category1", "IPO")).trim()
                val category = if (categoryRaw.contains("SME", ignoreCase = true)) "SME" else "Mainboard"

                val priceStr = stripHtml(row.optString("Price (₹)", "0")).replace(Regex("[^0-9.]"), "")
                val issuePrice = priceStr.toDoubleOrNull()?.roundToInt() ?: 0

                val gmpHtml = row.optString("GMP", "")
                val gmpAmount = parseGmpAmount(gmpHtml)
                val gmpPercent = row.optString("~gmp_percent_calc", "0")
                    .replace(Regex("[^0-9.-]"), "")
                    .toDoubleOrNull()
                    ?: if (issuePrice > 0) ((gmpAmount / issuePrice.toDouble()) * 100.0) else 0.0

                val gmpRangeText = parseGmpRange(gmpHtml)
                val estimatedListingPrice = if (issuePrice > 0) issuePrice + gmpAmount else gmpAmount

                val ratingHtml = row.optString("&#128293;Rating", "")
                val fireRating = Regex("&#128293;|🔥").findAll(ratingHtml).count().coerceIn(0, 5)

                val lotSize = stripHtml(row.optString("Lot", "0"))
                    .replace(Regex("[^0-9]"), "")
                    .toIntOrNull() ?: 0

                val minInvestmentInr = if (issuePrice > 0 && lotSize > 0) issuePrice * lotSize else 0
                val estProfitPerLotInr = if (lotSize > 0) (gmpAmount * lotSize).roundToInt() else 0

                val issueSizeRaw = decodeHtmlEntities(stripHtml(row.optString("IPO Size", "-"))).trim()
                val issueSizeCr = if (issueSizeRaw.isEmpty() || issueSizeRaw == "-") "TBA" else issueSizeRaw

                val peRatio = stripHtml(row.optString("~P/E", "-")).trim().ifEmpty { "-" }

                val openDate = extractMainDate(row.optString("Open", "-"))
                val closeDate = extractMainDate(row.optString("Close", "-"))
                val boaDate = extractMainDate(row.optString("BoA Dt", "-"))
                val listingDate = extractMainDate(row.optString("Listing", "-"))

                val sortOpenDate = row.optString("~Srt_Open", "")
                val sortCloseDate = row.optString("~Srt_Close", "")
                val sortBoaDate = row.optString("~Srt_BoA_Dt", "")
                val sortListingDate = row.optString("~Str_Listing", "")
                // Listed rows carry "L@221.00 (12.76%)" in the name cell
                val listingPrice = parseListingPriceFromNameCell(row.optString("Name", ""))
                val updatedOn = stripHtml(row.optString("Updated-On", "Live")).trim().ifEmpty { "Live" }
                val hasAnchor = row.optString("Anchor", "").contains("✅")

                val folderPath = row.optString("~urlrewrite_folder_name", "/report/live-ipo-gmp/331/")
                val investorGainUrl = if (folderPath.startsWith("http")) {
                    folderPath
                } else {
                    "https://www.investorgain.com/${folderPath.trimStart('/')}"
                }

                // Merge live subscription details from Report 333 if available
                val subObj = subMap[id]
                val rawSub = stripHtml(row.optString("Sub", "-")).trim().ifEmpty { "-" }
                val totalSub = if (rawSub != "-" && rawSub.isNotEmpty()) {
                    rawSub
                } else {
                    val subFrom333 = subObj?.optString("Total")?.let { extractMainDate(it) }.orEmpty()
                    if (subFrom333.isNotEmpty() && subFrom333 != "-") "${subFrom333}x" else "-"
                }

                val qibSub = formatSubMult(subObj?.optString("QIB"))
                val niiSub = formatSubMult(subObj?.optString("NII"))
                val shniSub = formatSubMult(subObj?.optString("SHNI"))
                val bhniSub = formatSubMult(subObj?.optString("BHNI"))
                val riiSub = formatSubMult(subObj?.optString("RII"))

                // Merge company sector and listing exchange from ipo/list-read metadata
                val meta = metaMap[id]
                val sector = meta?.sector?.ifEmpty { null }
                    ?: if (category == "SME") "SME Growth Enterprise" else "Mainboard Corporate"
                val listingExchange = meta?.listingAt?.ifEmpty { null }
                    ?: if (category == "SME") "NSE / BSE SME" else "BSE, NSE"

                items.add(
                    LiveIpoItem(
                        id = id,
                        name = rawName,
                        statusCode = statusCode,
                        statusLabel = statusLabel,
                        category = category,
                        sector = sector,
                        listingExchange = listingExchange,
                        issuePrice = issuePrice,
                        gmpAmount = gmpAmount,
                        gmpPercent = gmpPercent,
                        gmpRangeText = gmpRangeText,
                        estimatedListingPrice = estimatedListingPrice,
                        fireRating = fireRating,
                        totalSubscription = totalSub,
                        qibSubscription = qibSub,
                        niiSubscription = niiSub,
                        shniSubscription = shniSub,
                        bhniSubscription = bhniSub,
                        riiSubscription = riiSub,
                        issueSizeCr = issueSizeCr,
                        lotSize = lotSize,
                        minInvestmentInr = minInvestmentInr,
                        estProfitPerLotInr = estProfitPerLotInr,
                        peRatio = peRatio,
                        openDate = openDate,
                        closeDate = closeDate,
                        boaDate = boaDate,
                        listingDate = listingDate,
                        sortOpenDate = sortOpenDate,
                        sortCloseDate = sortCloseDate,
                        updatedOn = updatedOn,
                        hasAnchor = hasAnchor,
                        investorGainUrl = investorGainUrl,
                        listingPrice = listingPrice,
                        sortBoaDate = sortBoaDate,
                        sortListingDate = sortListingDate
                    )
                )
            }

            if (items.isEmpty()) {
                throw IllegalStateException("Live market feed returned 0 IPO records.")
            }
            items
        }
    }

    /**
     * Listed IPOs with listing and current (LTP) performance, from report 377.
     * The report returns every recent listing in one response.
     */
    suspend fun fetchListedHistory(): Result<List<ListedIpoRecord>> = withContext(Dispatchers.IO) {
        runCatching {
            val cal = Calendar.getInstance()
            val month = cal.get(Calendar.MONTH) + 1
            val year = cal.get(Calendar.YEAR)
            val fyStart = if (month >= 4) year else year - 1
            val fy = String.format("%d-%02d", fyStart, (fyStart + 1) % 100)
            val rows = fetchReportTable(377, month, year, fy)
            val today = LocalDate.now()
            val records = mutableListOf<ListedIpoRecord>()
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val id = row.opt("~id")?.toString()?.trim().orEmpty()
                val name = decodeHtmlEntities(stripHtml(row.optString("IPO", "").substringBefore("<span"))).trim()
                if (id.isEmpty() || name.isEmpty()) continue

                val (issuePrice, _) = parsePriceAndPercent(row.optString("Price", ""))
                val (listingPrice, listingPctFromCell) = parsePriceAndPercent(row.optString("Listing Price", ""))
                val (currentPrice, currentPctFromCell) = parsePriceAndPercent(row.optString("Closing Price (LTP)", ""))
                val listingDate = IpoDates.parse(row.optString("Listing Dt", ""), today)
                    ?: IpoDates.parse(row.optString("~orderby", "").take(10), today)
                val folderPath = row.optString("~URLRewrite_Folder_Name", "")

                records.add(
                    ListedIpoRecord(
                        id = id,
                        name = name,
                        symbol = stripHtml(row.optString("Symbol", "")).trim(),
                        category = if (row.optString("~IPO_Category", "").contains("SME", ignoreCase = true)) "SME" else "Mainboard",
                        issuePrice = issuePrice ?: 0.0,
                        listingPrice = listingPrice,
                        listingGainPct = row.optNumberOrNull("~str_listing_gain_in_per") ?: listingPctFromCell,
                        currentPrice = currentPrice,
                        currentGainPct = row.optNumberOrNull("~str_ltp_per_calc") ?: currentPctFromCell,
                        listingDate = listingDate,
                        url = if (folderPath.isBlank()) "" else "https://www.investorgain.com/${folderPath.trimStart('/')}"
                    )
                )
            }
            records
        }
    }

    /**
     * Intermediaries, extra dates, symbol, logo and subscription breakdown
     * embedded in an IPO's InvestorGain page.
     */
    suspend fun fetchIpoPageDetails(pageUrl: String): Result<IpoPageDetails> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(pageUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html")
                .build()
            val html = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw java.io.IOException("HTTP ${resp.code} for $pageUrl")
                resp.body?.string().orEmpty()
            }
            val text = IpoPageParser.unescape(html)

            val leadManagers = IpoPageParser.extractJsonArray(text, "ipoLeadManagersList")
                ?.let { runCatching { JSONArray(it) }.getOrNull() }
                ?.let { arr ->
                    (0 until arr.length()).mapNotNull { i ->
                        val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                        val name = decodeHtmlEntities(obj.optString("comp_name", "")).trim()
                        if (name.isEmpty()) null else NamedIntermediary(obj.opt("id")?.toString().orEmpty(), name)
                    }
                }
                .orEmpty()

            val registrarObj = IpoPageParser.extractJsonArray(text, "registrarInfo")
                ?.let { runCatching { JSONArray(it).optJSONObject(0) }.getOrNull() }
            val registrar = registrarObj?.let { obj ->
                val name = decodeHtmlEntities(obj.optString("registrar_name", "")).trim()
                if (name.isEmpty()) null else NamedIntermediary(obj.opt("id")?.toString().orEmpty(), name)
            }

            // floor_price / cap_price are usually blank; the band lives in issue_price_lower / _upper
            val capPrice = IpoPageParser.extractValue(text, "issue_price_upper")?.toDoubleOrNull()
                ?: IpoPageParser.extractValue(text, "cap_price")?.toDoubleOrNull()
            val latestBidding = IpoPageParser.extractJsonArray(text, "ipoBiddingData")
                ?.let { runCatching { JSONArray(it) }.getOrNull() }
                ?.let { arr -> if (arr.length() > 0) arr.optJSONObject(arr.length() - 1) else null }
            val biddingValues = latestBidding?.let { obj ->
                obj.keys().asSequence().associateWith { key -> obj.opt(key)?.toString().orEmpty() }
            }.orEmpty()

            val logoFile = IpoPageParser.extractValue(text, "logo_url")
            IpoPageDetails(
                leadManagers = leadManagers,
                registrar = registrar,
                registrarWebsite = IpoPageParser.firstLink(registrarObj?.optString("registrar_basic_info")),
                marketMaker = IpoPageParser.extractMarketMaker(text),
                symbol = IpoPageParser.extractValue(text, "nse_symbol")
                    ?: IpoPageParser.extractValue(text, "bse_scripcode"),
                logoUrl = logoFile?.let { if (it.startsWith("http")) it else "https://www.chittorgarh.net/images/ipo/$it" },
                floorPrice = IpoPageParser.extractValue(text, "issue_price_lower")?.toDoubleOrNull()
                    ?: IpoPageParser.extractValue(text, "floor_price")?.toDoubleOrNull(),
                settlementDate = IpoPageParser.extractIsoDate(text, "timetable_share_credit_dt"),
                anchorLockIn50Date = IpoPageParser.extractIsoDate(text, "timetable_anchor_lockin_end_dt_1"),
                anchorLockIn100Date = IpoPageParser.extractIsoDate(text, "timetable_anchor_lockin_end_dt_2"),
                subscription = IpoPageParser.subscriptionFromBidding(biddingValues, capPrice),
                subscriptionUpdatedAtMillis = biddingValues["create_date"]
                    ?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() },
                fetchedAtMillis = System.currentTimeMillis()
            )
        }
    }

    /** True once the IPO page reports the basis of allotment as finalised. */
    suspend fun isAllotmentFinalized(pageUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(pageUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html")
                .build()
            val html = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw java.io.IOException("HTTP ${resp.code} for $pageUrl")
                resp.body?.string().orEmpty()
            }
            val flag = IpoPageParser.extractValue(IpoPageParser.unescape(html), "boa_finalized")?.lowercase()
            flag == "1" || flag == "true" || flag == "y" || flag == "yes"
        }
    }

    private fun JSONObject.optNumberOrNull(key: String): Double? =
        if (isNull(key)) null else opt(key)?.toString()?.trim()?.toDoubleOrNull()

    private fun fetchReportTable(reportId: Int, month: Int, year: Int, fy: String) =
        fetchReportTableInternal("$BASE_API/report/data-read/$reportId/1/$month/$year/$fy/0/all?search=")
            ?: fetchReportTableInternal("$BASE_API/report/data-read/$reportId/1/10/2026/2026-27/0/all?search=")
            ?: throw IllegalStateException("Unable to load live market report $reportId")

    private fun fetchReportTableInternal(url: String): org.json.JSONArray? {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("Referer", "https://www.investorgain.com/")
            .build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            val json = JSONObject(body)
            val arr = json.optJSONArray("reportTableData")
            if (arr != null && arr.length() > 0) return arr
            return null
        }
    }

    private data class IpoMeta(val sector: String, val listingAt: String)

    private fun fetchIpoListMeta(): Map<String, IpoMeta> {
        val req = Request.Builder()
            .url("$BASE_API/ipo/list-read")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("Referer", "https://www.investorgain.com/")
            .build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyMap()
            val body = resp.body?.string() ?: return emptyMap()
            val json = JSONObject(body)
            val arr = json.optJSONArray("ipoList") ?: return emptyMap()
            val map = mutableMapOf<String, IpoMeta>()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val id = item.opt("id")?.toString()?.trim().orEmpty()
                if (id.isNotEmpty()) {
                    map[id] = IpoMeta(
                        sector = decodeHtmlEntities(item.optString("company_sector", "")).trim(),
                        listingAt = decodeHtmlEntities(item.optString("ipo_listing_at", "")).trim()
                    )
                }
            }
            return map
        }
    }

    private fun parseGmpAmount(gmpHtml: String): Double {
        val boldMatch = Regex("<b>\\s*([+-]?[0-9]+(?:\\.[0-9]+)?)\\s*</b>").find(gmpHtml)
        if (boldMatch != null) {
            return boldMatch.groupValues[1].toDoubleOrNull() ?: 0.0
        }
        return 0.0
    }

    private fun parseGmpRange(gmpHtml: String): String {
        val smallMatch = Regex("<small[^>]*>\\s*<b>(.*?)</b>\\s*</small>", RegexOption.IGNORE_CASE).find(gmpHtml)
        if (smallMatch != null) {
            return decodeHtmlEntities(stripHtml(smallMatch.groupValues[1])).trim()
        }
        return ""
    }

    private fun extractMainDate(rawHtml: String): String {
        val beforeBr = rawHtml.substringBefore("<br").substringBefore("<small")
        return decodeHtmlEntities(stripHtml(beforeBr)).trim().ifEmpty { "-" }
    }

    private fun formatSubMult(raw: String?): String {
        val clean = raw?.let { stripHtml(it).trim() }.orEmpty()
        if (clean.isEmpty() || clean == "-" || clean == "0") return "-"
        return if (clean.endsWith("x", ignoreCase = true)) clean else "${clean}x"
    }

    private fun stripHtml(input: String): String {
        return input.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
    }

    private fun decodeHtmlEntities(input: String): String {
        return input
            .replace("&#8377;", "₹")
            .replace("&amp;", "&")
            .replace("&nbsp;", " ")
            .replace("&#128293;", "🔥")
    }
}
