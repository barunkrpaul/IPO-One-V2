package com.example.data.local

import android.content.Context
import com.example.data.model.IpoPageDetails
import com.example.data.model.NamedIntermediary
import com.example.data.model.SubscriptionCategory
import com.example.data.model.SubscriptionFigures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * Keeps IPO page details on the device so intermediaries and dates are
 * available offline and pages already read are not downloaded again.
 */
class IpoPageDetailsCache(context: Context) {

    private val file = File(context.applicationContext.filesDir, "ipo_page_details.json")

    suspend fun load(): Map<String, IpoPageDetails> = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) return@runCatching emptyMap()
            val root = JSONObject(file.readText())
            root.keys().asSequence().mapNotNull { id ->
                root.optJSONObject(id)?.let { id to fromJson(it) }
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    suspend fun save(details: Map<String, IpoPageDetails>) = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject()
            details.forEach { (id, d) -> root.put(id, toJson(d)) }
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(root.toString())
            tmp.renameTo(file)
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) { runCatching { file.delete() } }

    private fun toJson(d: IpoPageDetails): JSONObject = JSONObject().apply {
        put("leadManagers", JSONArray().apply { d.leadManagers.forEach { put(named(it)) } })
        d.registrar?.let { put("registrar", named(it)) }
        putOpt("registrarWebsite", d.registrarWebsite)
        d.marketMaker?.let { put("marketMaker", named(it)) }
        putOpt("symbol", d.symbol)
        putOpt("logoUrl", d.logoUrl)
        d.floorPrice?.let { put("floorPrice", it) }
        putOpt("settlementDate", d.settlementDate?.toString())
        putOpt("anchorLockIn50Date", d.anchorLockIn50Date?.toString())
        putOpt("anchorLockIn100Date", d.anchorLockIn100Date?.toString())
        put("subscription", JSONObject().apply {
            d.subscription.forEach { (category, f) ->
                put(category.key, JSONObject().apply {
                    f.quotaPct?.let { put("quotaPct", it) }
                    f.sharesOfferedCr?.let { put("sharesOfferedCr", it) }
                    f.sharesAppliedCr?.let { put("sharesAppliedCr", it) }
                    f.applicationsReserved?.let { put("applicationsReserved", it) }
                    f.applicationsReceived?.let { put("applicationsReceived", it) }
                    f.times?.let { put("times", it) }
                })
            }
        })
        d.subscriptionUpdatedAtMillis?.let { put("subscriptionUpdatedAt", it) }
        put("fetchedAt", d.fetchedAtMillis)
    }

    private fun fromJson(o: JSONObject): IpoPageDetails {
        val subscription = o.optJSONObject("subscription")
        return IpoPageDetails(
            leadManagers = o.optJSONArray("leadManagers")?.let { arr ->
                (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(::readNamed) }
            }.orEmpty(),
            registrar = o.optJSONObject("registrar")?.let(::readNamed),
            registrarWebsite = o.optStringOrNull("registrarWebsite"),
            marketMaker = o.optJSONObject("marketMaker")?.let(::readNamed),
            symbol = o.optStringOrNull("symbol"),
            logoUrl = o.optStringOrNull("logoUrl"),
            floorPrice = o.optDoubleOrNull("floorPrice"),
            settlementDate = o.optStringOrNull("settlementDate")?.let(LocalDate::parse),
            anchorLockIn50Date = o.optStringOrNull("anchorLockIn50Date")?.let(LocalDate::parse),
            anchorLockIn100Date = o.optStringOrNull("anchorLockIn100Date")?.let(LocalDate::parse),
            subscription = SubscriptionCategory.entries.mapNotNull { category ->
                subscription?.optJSONObject(category.key)?.let { f ->
                    category to SubscriptionFigures(
                        quotaPct = f.optDoubleOrNull("quotaPct"),
                        sharesOfferedCr = f.optDoubleOrNull("sharesOfferedCr"),
                        sharesAppliedCr = f.optDoubleOrNull("sharesAppliedCr"),
                        applicationsReserved = f.optDoubleOrNull("applicationsReserved")?.toLong(),
                        applicationsReceived = f.optDoubleOrNull("applicationsReceived")?.toLong(),
                        times = f.optDoubleOrNull("times")
                    )
                }
            }.toMap(),
            subscriptionUpdatedAtMillis = if (o.has("subscriptionUpdatedAt")) o.optLong("subscriptionUpdatedAt") else null,
            fetchedAtMillis = o.optLong("fetchedAt")
        )
    }

    private fun named(n: NamedIntermediary) = JSONObject().put("id", n.id).put("name", n.name)

    private fun readNamed(o: JSONObject): NamedIntermediary? =
        o.optStringOrNull("name")?.let { NamedIntermediary(o.optString("id", ""), it) }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (isNull(key)) null else optDouble(key).takeIf { !it.isNaN() }
}
