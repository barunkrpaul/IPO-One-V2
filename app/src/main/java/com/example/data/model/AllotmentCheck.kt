package com.example.data.model

/** Outcome of one PAN's allotment check, stored on IpoApplication.allotmentResult. */
enum class AllotmentResult(val stored: String) {
    ALLOTTED("ALLOTTED"),
    NOT_ALLOTTED("NOT_ALLOTTED"),
    UNVERIFIED("UNVERIFIED");

    companion object {
        fun fromStored(value: String?): AllotmentResult? = entries.firstOrNull { it.stored == value }
    }
}

data class AllotmentOutcome(
    val result: AllotmentResult,
    val sharesAllotted: Long? = null,
    val note: String? = null
)

/** One PAN to check: a saved vault PAN and/or a logged application. */
data class AllotmentTarget(
    val holderName: String,
    val panNumber: String,
    val applicationId: String? = null
)

/**
 * Reads a registrar's result page. Only lines that appeared after the form was
 * filled are passed in, so instructions such as "allotted / not allotted" on
 * the blank form are not mistaken for a result. Returns null while no result
 * is visible yet.
 */
fun classifyAllotmentText(newText: String): AllotmentOutcome? {
    val text = newText.lowercase().replace(Regex("\\s+"), " ")
    if (text.isBlank()) return null

    val notFound = listOf(
        "no record found", "no records found", "record not found", "no data found", "details not found",
        "pan not found", "invalid pan", "not applied", "no application found", "data not available"
    )
    if (notFound.any { it in text }) {
        return AllotmentOutcome(AllotmentResult.UNVERIFIED, note = "No application found for this PAN")
    }

    val countPatterns = listOf(
        Regex("(?:securities|shares|qty|quantity|no\\.? of shares)\\s*allot+ed\\s*[:\\-]?\\s*(\\d[\\d,]*)"),
        Regex("allot+ed\\s*(?:securities|shares|qty|quantity)\\s*[:\\-]?\\s*(\\d[\\d,]*)"),
        Regex("allot+ment\\s*(?:qty|quantity)\\s*[:\\-]?\\s*(\\d[\\d,]*)")
    )
    for (pattern in countPatterns) {
        val shares = pattern.find(text)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull() ?: continue
        return if (shares > 0) {
            AllotmentOutcome(AllotmentResult.ALLOTTED, sharesAllotted = shares)
        } else {
            AllotmentOutcome(AllotmentResult.NOT_ALLOTTED, sharesAllotted = 0)
        }
    }

    val notAllotted = listOf(
        "not allotted", "not alloted", "non allotted", "non alloted", "non-allotted", "non-alloted",
        "not been allotted", "no allotment", "allotment not made"
    )
    if (notAllotted.any { it in text }) return AllotmentOutcome(AllotmentResult.NOT_ALLOTTED, sharesAllotted = 0)

    val allotted = listOf("congratulations", "successfully allotted", "you have been allotted", "status : allotted", "status: allotted")
    if (allotted.any { it in text }) return AllotmentOutcome(AllotmentResult.ALLOTTED)

    return null
}

/** Lines of [current] page text that were not present in [baseline]. */
fun newPageLines(baseline: Set<String>, current: String): String =
    current.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && it !in baseline }
        .joinToString("\n")

fun pageLineSet(text: String): Set<String> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }.toSet()

/** Exponential backoff for slow registrar sites: three attempts, waiting 2 s then 4 s. */
object RegistrarRetry {
    const val MAX_ATTEMPTS = 3
    private const val BASE_DELAY_MS = 2_000L

    /** Wait before [attempt] (1-based); the first attempt starts immediately. */
    fun delayBeforeAttempt(attempt: Int): Long =
        if (attempt <= 1) 0L else BASE_DELAY_MS shl (attempt - 2)
}

/** "rahul.sharma@okhdfcbank" -> "ra*********@okhdfcbank"; blank stays blank. */
fun maskUpiId(upi: String): String {
    val clean = upi.trim()
    if (clean.isEmpty()) return ""
    val handle = clean.substringBefore('@')
    val provider = clean.substringAfter('@', "")
    val visible = handle.take(2)
    val masked = visible + "*".repeat((handle.length - visible.length).coerceAtLeast(1))
    return if (provider.isEmpty()) masked else "$masked@$provider"
}

/** The result label used in the UI and by the allotment search. */
fun allotmentResultLabel(result: AllotmentResult?, shares: Long?): String = when (result) {
    AllotmentResult.ALLOTTED -> if (shares != null && shares > 0) "Allotted ($shares shares)" else "Allotted"
    AllotmentResult.NOT_ALLOTTED -> "Not allotted"
    AllotmentResult.UNVERIFIED -> "Could not verify"
    null -> ""
}

/**
 * Everyone to check for one IPO: each saved vault PAN plus any logged
 * application's PAN that is not in the vault, one entry per PAN, sorted by
 * holder name. Targets carry the matching application id when there is one.
 */
fun allotmentTargets(
    ipoId: String,
    ipoName: String,
    panProfiles: List<PanProfile>,
    applications: List<IpoApplication>
): List<AllotmentTarget> {
    val forIpo = applications.filter { it.ipoId == ipoId || it.ipoName.equals(ipoName, ignoreCase = true) }
    val byPan = linkedMapOf<String, AllotmentTarget>()
    panProfiles.forEach { pan ->
        val key = pan.panNumber.trim().uppercase()
        if (key.isEmpty()) return@forEach
        byPan[key] = AllotmentTarget(
            holderName = pan.holderName,
            panNumber = key,
            applicationId = forIpo.firstOrNull { it.panNumber.equals(key, ignoreCase = true) }?.id
        )
    }
    forIpo.forEach { app ->
        val key = app.panNumber.trim().uppercase()
        if (key.isNotEmpty() && key !in byPan) byPan[key] = AllotmentTarget(app.panHolderName, key, app.id)
    }
    return byPan.values.sortedBy { it.holderName.lowercase() }
}

/** Search across applications by IPO name, PAN holder or allotment result/status. */
fun matchesAllotmentSearch(app: IpoApplication, query: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    val resultLabel = allotmentResultLabel(AllotmentResult.fromStored(app.allotmentResult), app.sharesAllotted)
    return listOf(app.ipoName, app.panHolderName, app.status, resultLabel)
        .any { it.lowercase().contains(q) }
}
