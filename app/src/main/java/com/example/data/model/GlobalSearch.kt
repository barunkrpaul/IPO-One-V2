package com.example.data.model

data class GlobalSearchResults(
    val ipos: List<LiveIpoItem> = emptyList(),
    val listed: List<ListedIpoRecord> = emptyList(),
    val intermediaries: List<IntermediaryEntry> = emptyList(),
    val applications: List<IpoApplication> = emptyList()
) {
    val isEmpty: Boolean get() = ipos.isEmpty() && listed.isEmpty() && intermediaries.isEmpty() && applications.isEmpty()
}

/**
 * Searches IPO names and symbols across every stage (live feed and listed
 * history), intermediaries, and the user's logged applications. Each group is
 * capped at [limitPerGroup]; queries shorter than two characters return nothing.
 */
fun globalSearch(
    query: String,
    live: List<LiveIpoItem>,
    history: List<ListedIpoRecord>,
    directory: IntermediaryDirectory,
    applications: List<IpoApplication>,
    extras: Map<String, IpoExtras>,
    limitPerGroup: Int = 20
): GlobalSearchResults {
    val q = query.trim()
    if (q.length < 2) return GlobalSearchResults()
    fun hit(text: String?) = text != null && text.contains(q, ignoreCase = true)

    val ipos = live.filter { hit(it.name) || hit(extras[it.id]?.symbol) || hit(it.sector) }
    val liveIds = live.map { it.id }.toSet()
    val listed = history.filter { it.id !in liveIds && (hit(it.name) || hit(it.symbol)) }
    val intermediaries = directory.entries.values.flatten().filter { hit(it.name) }
        .sortedByDescending { it.ipos.size }
    val apps = applications.filter { matchesAllotmentSearch(it, q) }

    return GlobalSearchResults(
        ipos = ipos.take(limitPerGroup),
        listed = listed.take(limitPerGroup),
        intermediaries = intermediaries.take(limitPerGroup),
        applications = apps.take(limitPerGroup)
    )
}
