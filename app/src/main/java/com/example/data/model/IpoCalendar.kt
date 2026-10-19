package com.example.data.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** One dated IPO milestone shown on the calendar. */
data class CalendarEvent(
    val ipoId: String,
    val ipoName: String,
    val event: TimelineEvent,
    val date: LocalDate,
    val estimated: Boolean
)

/**
 * Every timeline date of every IPO in the feed, sorted by date then by event
 * order. Cancelled IPOs are left out.
 */
fun calendarEvents(
    ipos: List<LiveIpoItem>,
    extras: Map<String, IpoExtras>,
    today: LocalDate
): List<CalendarEvent> =
    ipos.asSequence()
        .map { it.withExtras(extras[it.id]) }
        .filter { it.lifecycleStage(today) != IpoLifecycleStage.CANCELLED }
        .flatMap { ipo ->
            ipoTimeline(ipo, extras[ipo.id], today).asSequence().map { entry ->
                CalendarEvent(ipo.id, ipo.name, entry.event, entry.date, entry.estimated)
            }
        }
        .sortedWith(compareBy<CalendarEvent>({ it.date }, { it.event.ordinal }, { it.ipoName }))
        .toList()

/** Confirmed (not estimated) events falling on [day], for the dashboard strip. */
fun eventsOn(events: List<CalendarEvent>, day: LocalDate, includeEstimated: Boolean = false): List<CalendarEvent> =
    events.filter { it.date == day && (includeEstimated || !it.estimated) }

/**
 * Cells for a Monday-first month grid: leading/trailing nulls pad the first and
 * last week so the list length is a multiple of seven.
 */
fun monthGrid(month: YearMonth): List<LocalDate?> {
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val cells = List<LocalDate?>(leading) { null } + days
    val trailing = (7 - cells.size % 7) % 7
    return cells + List<LocalDate?>(trailing) { null }
}
