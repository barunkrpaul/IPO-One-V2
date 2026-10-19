package com.example.data.model

import java.time.LocalDate

/** An IPO as the intermediary directory sees it (live feed or listed history). */
data class IpoRef(
    val id: String,
    val name: String,
    val category: String, // "Mainboard" or "SME"
    val listingGainPct: Double?,
    val listingDate: LocalDate?,
    val url: String
)

data class IntermediaryEntry(
    val role: IntermediaryRole,
    val key: String,
    val name: String,
    val website: String?,
    val ipos: List<IpoRef>
) {
    val mainboardCount: Int get() = ipos.count { it.category != "SME" }
    val smeCount: Int get() = ipos.count { it.category == "SME" }
    val averageListingGainPct: Double?
        get() = ipos.mapNotNull { it.listingGainPct }.takeIf { it.isNotEmpty() }?.average()
}

data class IntermediaryDirectory(
    val entries: Map<IntermediaryRole, List<IntermediaryEntry>>,
    val coveredIpoCount: Int,
    val totalIpoCount: Int
) {
    fun find(role: IntermediaryRole, name: String): IntermediaryEntry? =
        entries[role].orEmpty().firstOrNull { it.key == intermediaryKey(name) }
}

/** Names vary in punctuation ("Pvt.Ltd." vs "Pvt Ltd"); compare letters and digits only. */
fun intermediaryKey(name: String): String = name.lowercase().filter { it.isLetterOrDigit() }

/**
 * Groups IPOs by lead manager, registrar and market maker using whatever
 * intermediary data is known for each IPO. Each list is sorted by IPO count.
 */
fun buildIntermediaryDirectory(refs: List<IpoRef>, extras: Map<String, IpoExtras>): IntermediaryDirectory {
    data class Acc(val name: String, var website: String?, val ipos: MutableList<IpoRef>)

    val maps = IntermediaryRole.entries.associateWith { linkedMapOf<String, Acc>() }
    var covered = 0
    val uniqueRefs = refs.distinctBy { it.id }

    fun add(role: IntermediaryRole, name: String?, website: String?, ref: IpoRef) {
        val clean = name?.trim()?.takeIf { it.isNotEmpty() } ?: return
        val acc = maps.getValue(role).getOrPut(intermediaryKey(clean)) { Acc(clean, website, mutableListOf()) }
        if (acc.website == null) acc.website = website
        if (acc.ipos.none { it.id == ref.id }) acc.ipos += ref
    }

    for (ref in uniqueRefs) {
        val e = extras[ref.id] ?: continue
        if (e.leadManagers.isEmpty() && e.registrar == null && e.marketMaker == null) continue
        covered++
        e.leadManagers.forEach { add(IntermediaryRole.LEAD_MANAGER, it, null, ref) }
        add(IntermediaryRole.REGISTRAR, e.registrar, e.registrarWebsite, ref)
        add(IntermediaryRole.MARKET_MAKER, e.marketMaker, null, ref)
    }

    val entries = maps.mapValues { (role, accs) ->
        accs.map { (key, acc) ->
            IntermediaryEntry(
                role = role,
                key = key,
                name = acc.name,
                website = acc.website,
                ipos = acc.ipos.sortedWith(compareByDescending<IpoRef, LocalDate?>(nullsFirst()) { it.listingDate })
            )
        }.sortedWith(compareByDescending<IntermediaryEntry> { it.ipos.size }.thenBy { it.name })
    }
    return IntermediaryDirectory(entries, covered, uniqueRefs.size)
}
