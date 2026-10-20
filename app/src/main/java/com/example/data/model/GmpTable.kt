package com.example.data.model

import com.example.util.formatIndianNumber
import java.time.LocalDate

/** Stages shown in the simple table, in display order. Listed and cancelled IPOs are left out. */
private val TableStages = listOf(
    IpoLifecycleStage.OPEN,
    IpoLifecycleStage.UPCOMING,
    IpoLifecycleStage.CLOSED,
    IpoLifecycleStage.ALLOTTED
)

/** Current IPOs for [board], open ones first, each group ordered by its last date to apply. */
fun simpleTableRows(ipos: List<LiveIpoItem>, board: IpoBoardFilter, today: LocalDate): List<Pair<LiveIpoItem, IpoLifecycleStage>> =
    ipos
        .filter {
            when (board) {
                IpoBoardFilter.ALL -> true
                IpoBoardFilter.SME -> it.category.equals("SME", ignoreCase = true)
                IpoBoardFilter.MAINBOARD -> !it.category.equals("SME", ignoreCase = true)
            }
        }
        .map { it to it.lifecycleStage(today) }
        .filter { it.second in TableStages }
        .sortedWith(
            compareBy<Pair<LiveIpoItem, IpoLifecycleStage>> { TableStages.indexOf(it.second) }
                .thenBy { it.first.closeLocalDate(today) ?: LocalDate.MAX }
        )

/** "₹30213.00 Cr" -> "₹30,213 Cr"; anything unparseable is shown as it came, blank as "-". */
fun formatIssueSize(raw: String): String {
    val number = raw.replace("₹", "").replace(",", "").replace("Cr", "", ignoreCase = true).trim().toDoubleOrNull()
    return when {
        number != null && number > 0 -> "₹" + formatIndianNumber(number, 2) + " Cr"
        raw.isBlank() -> "-"
        else -> raw.trim()
    }
}
