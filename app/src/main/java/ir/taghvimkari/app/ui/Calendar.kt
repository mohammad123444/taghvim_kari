package ir.taghvimkari.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.taghvimkari.app.domain.JalaliDate
import ir.taghvimkari.app.domain.ShiftEngine
import ir.taghvimkari.app.domain.toPersian
import java.time.LocalDate

private val FridayRed = Color(0xFFD32F2F)
private val WEEKDAY_SHORT = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

/**
 * Monthly Jalali calendar (Saturday first). The layout direction is RTL app-wide,
 * so the first column is drawn on the right.
 * [badge] may return a tiny label (e.g. a head-count) shown under a day number.
 */
@Composable
fun JalaliMonthCalendar(
    year: Int,
    month: Int,
    onMonthChange: (year: Int, month: Int) -> Unit,
    today: LocalDate,
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    badge: ((LocalDate) -> String?)? = null
) {
    val first = JalaliDate(year, month, 1).toLocalDate()
    val offset = ShiftEngine.persianWeekday(first)
    val length = JalaliDate.monthLength(year, month)
    val rows = (offset + length + 6) / 7
    var yearMenu by remember { mutableStateOf(false) }

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (month == 1) onMonthChange(year - 1, 12) else onMonthChange(year, month - 1)
            }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ماه قبل") }

            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                TextButton(onClick = { yearMenu = true }) {
                    Text(
                        "${JalaliDate.MONTH_NAMES[month - 1]} ${year.toPersian()}",
                        fontWeight = FontWeight.Bold,
                        color = Navy,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                DropdownMenu(expanded = yearMenu, onDismissRequest = { yearMenu = false }) {
                    for (y in (year - 10)..(year + 10)) {
                        DropdownMenuItem(
                            text = { Text(y.toPersian(), fontWeight = if (y == year) FontWeight.Bold else FontWeight.Normal) },
                            onClick = { onMonthChange(y, month); yearMenu = false }
                        )
                    }
                }
            }

            IconButton(onClick = {
                if (month == 12) onMonthChange(year + 1, 1) else onMonthChange(year, month + 1)
            }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "ماه بعد") }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            WEEKDAY_SHORT.forEachIndexed { i, name ->
                Text(
                    name,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = if (i == 6) FridayRed else Color.Gray,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val day = r * 7 + c - offset + 1
                    if (day !in 1..length) {
                        Box(Modifier.weight(1f).height(CELL_HEIGHT))
                    } else {
                        val date = JalaliDate(year, month, day).toLocalDate()
                        DayCell(
                            day = day,
                            isFriday = c == 6,
                            isToday = date == today,
                            isSelected = date == selected,
                            badge = badge?.invoke(date),
                            onClick = { onSelect(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private val CELL_HEIGHT = 58.dp

@Composable
private fun DayCell(
    day: Int,
    isFriday: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    badge: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val textColor = when {
        isSelected -> Color.White
        isFriday -> FridayRed
        else -> Color(0xFF222244)
    }
    Box(
        modifier
            .padding(2.dp)
            .height(CELL_HEIGHT - 4.dp)
            .clip(shape)
            .background(if (isSelected) Purple else Color.Transparent)
            .then(if (isToday && !isSelected) Modifier.border(BorderStroke(1.5.dp, Purple), shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.toPersian(),
                color = textColor,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
            if (badge != null) {
                // A small solid pill keeps the head-count readable on any background.
                Box(
                    Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (isSelected) Color.White else Color(0xFFDCD8F5))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        badge,
                        color = if (isSelected) Purple else Navy,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/** Tap a day and it is chosen immediately. */
@Composable
fun JalaliDatePickerDialog(
    initial: LocalDate,
    title: String,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    val start = JalaliDate.from(initial)
    var year by rememberSaveable { mutableStateOf(start.year) }
    var month by rememberSaveable { mutableStateOf(start.month) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            JalaliMonthCalendar(
                year = year,
                month = month,
                onMonthChange = { y, m -> year = y; month = m },
                today = today,
                selected = initial,
                onSelect = onPick
            )
        },
        confirmButton = { TextButton(onClick = { onPick(today) }) { Text("امروز") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
