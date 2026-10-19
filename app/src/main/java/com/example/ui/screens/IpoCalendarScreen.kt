package com.example.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.ipo
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CalendarEvent
import com.example.data.model.LiveIpoItem
import com.example.data.model.TimelineEvent
import com.example.data.model.monthGrid
import com.example.ui.viewmodel.UiState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Event colours shared by the calendar, its legend and the dashboard's today
 * strip; deeper shades on light backgrounds so dots and labels keep contrast.
 */
@Composable
internal fun calendarEventColor(event: TimelineEvent): Color {
    val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
    return when (event) {
        TimelineEvent.OPENS -> MaterialTheme.ipo.gain
        TimelineEvent.CLOSES -> MaterialTheme.ipo.warning
        TimelineEvent.ALLOTMENT -> MaterialTheme.ipo.accent
        TimelineEvent.SETTLEMENT -> if (light) Color(0xFF0F766E) else Color(0xFF2DD4BF)
        TimelineEvent.LISTING -> MaterialTheme.ipo.info
        TimelineEvent.MANDATE_END -> if (light) Color(0xFFBE185D) else Color(0xFFF472B6)
        TimelineEvent.ANCHOR_50 -> if (light) Color(0xFFC2410C) else Color(0xFFFB923C)
        TimelineEvent.ANCHOR_100 -> MaterialTheme.ipo.loss
    }
}

@StringRes
internal fun calendarEventLabelRes(event: TimelineEvent): Int = when (event) {
    TimelineEvent.OPENS -> R.string.cal_event_opens
    TimelineEvent.CLOSES -> R.string.cal_event_closes
    TimelineEvent.ALLOTMENT -> R.string.cal_event_allotment
    TimelineEvent.SETTLEMENT -> R.string.cal_event_settlement
    TimelineEvent.LISTING -> R.string.cal_event_listing
    TimelineEvent.MANDATE_END -> R.string.cal_event_mandate_end
    TimelineEvent.ANCHOR_50 -> R.string.cal_event_anchor_50
    TimelineEvent.ANCHOR_100 -> R.string.cal_event_anchor_100
}

private val MonthTitleFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val DayTitleFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

/**
 * Month grid of IPO milestones. [focusDate] jumps to a day (deep link from the
 * detail timeline); [onFocusConsumed] is called once it has been applied.
 */
@Composable
fun IpoCalendarScreen(
    liveIposState: UiState<List<LiveIpoItem>>,
    events: List<CalendarEvent>,
    focusDate: LocalDate?,
    onFocusConsumed: () -> Unit,
    onOpenIpo: (String) -> Unit,
    onRetry: () -> Unit
) {
    val today = remember { LocalDate.now() }
    // Saved as ISO strings so the selection survives rotation
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var selectedText by rememberSaveable { mutableStateOf(today.toString()) }
    val month = YearMonth.parse(monthText)
    val selectedDate = LocalDate.parse(selectedText)

    LaunchedEffect(focusDate) {
        if (focusDate != null) {
            monthText = YearMonth.from(focusDate).toString()
            selectedText = focusDate.toString()
            onFocusConsumed()
        }
    }

    val eventsByDate = remember(events) { events.groupBy { it.date } }
    val dayEvents = eventsByDate[selectedDate].orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ipo_calendar_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    MonthHeader(
                        month = month,
                        onPrevious = { monthText = month.minusMonths(1).toString() },
                        onNext = { monthText = month.plusMonths(1).toString() },
                        onToday = {
                            monthText = YearMonth.from(today).toString()
                            selectedText = today.toString()
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    MonthGrid(
                        month = month,
                        today = today,
                        selectedDate = selectedDate,
                        eventsByDate = eventsByDate,
                        onSelect = { selectedText = it.toString() },
                        onSwipe = { forward ->
                            monthText = (if (forward) month.plusMonths(1) else month.minusMonths(1)).toString()
                        }
                    )
                }
            }
        }

        item { CalendarLegend() }

        item {
            Text(
                text = selectedDate.format(DayTitleFormat),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        when {
            liveIposState is UiState.Loading -> item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.calendar_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            liveIposState is UiState.Error -> item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.calendar_error),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = liveIposState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = onRetry, modifier = Modifier.testTag("calendar_retry_button")) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.calendar_retry))
                        }
                    }
                }
            }

            dayEvents.isEmpty() -> item {
                Text(
                    text = stringResource(R.string.calendar_no_events),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            else -> items(dayEvents, key = { "${it.ipoId}_${it.event.name}" }) { event ->
                CalendarEventRow(event = event, onClick = { onOpenIpo(event.ipoId) })
            }
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.testTag("calendar_prev_month")) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.calendar_prev_month)
            )
        }
        Text(
            text = month.format(MonthTitleFormat),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .testTag("calendar_month_title")
        )
        TextButton(onClick = onToday) {
            Text(stringResource(R.string.calendar_today))
        }
        IconButton(onClick = onNext, modifier = Modifier.testTag("calendar_next_month")) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.calendar_next_month)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    onSelect: (LocalDate) -> Unit,
    onSwipe: (forward: Boolean) -> Unit
) {
    val cells = remember(month) { monthGrid(month) }
    Column(
        modifier = Modifier.pointerInput(month) {
            var totalDrag = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalDrag = 0f },
                onDragEnd = {
                    val threshold = 60.dp.toPx()
                    if (totalDrag <= -threshold) onSwipe(true)
                    if (totalDrag >= threshold) onSwipe(false)
                }
            ) { _, dragAmount -> totalDrag += dragAmount }
        }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                events = eventsByDate[date].orEmpty(),
                                onClick = { onSelect(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    events: List<CalendarEvent>,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    // One dot per distinct event type, at most three, then "+N"
    val kinds = events.map { it.event }.distinct()
    Column(
        modifier = Modifier
            .padding(2.dp)
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .then(
                if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(top = 6.dp)
            .testTag("calendar_day_$date"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            kinds.take(3).forEach { kind ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(calendarEventColor(kind))
                )
            }
        }
        if (kinds.size > 3) {
            Text(
                text = stringResource(R.string.calendar_more, kinds.size - 3),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarLegend() {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calendar_legend"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TimelineEvent.entries.forEach { event ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(calendarEventColor(event))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(calendarEventLabelRes(event)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CalendarEventRow(event: CalendarEvent, onClick: () -> Unit) {
    val color = calendarEventColor(event.event)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("calendar_event_${event.ipoId}_${event.event.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.ipoName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(calendarEventLabelRes(event.event)),
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                    if (event.estimated) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.calendar_event_estimated),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
