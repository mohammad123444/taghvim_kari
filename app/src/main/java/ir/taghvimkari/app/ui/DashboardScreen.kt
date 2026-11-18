package ir.taghvimkari.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.taghvimkari.app.domain.DayStatus
import ir.taghvimkari.app.domain.Employment
import ir.taghvimkari.app.domain.JalaliDate
import ir.taghvimkari.app.domain.Person
import ir.taghvimkari.app.domain.ShiftEngine
import ir.taghvimkari.app.domain.toPersian
import java.time.LocalDate

private val PresentGreen = Color(0xFF218838)
private val AbsentGray = Color(0xFF757575)

@Composable
fun DashboardScreen(people: List<Person>) {
    val today = remember { LocalDate.now() }
    val todayJalali = remember { JalaliDate.from(today) }
    var viewYear by rememberSaveable { mutableStateOf(todayJalali.year) }
    var viewMonth by rememberSaveable { mutableStateOf(todayJalali.month) }
    var selectedEpoch by rememberSaveable { mutableStateOf<Long?>(null) }

    val active = people.filter { it.active }
    val presentToday = active.count { ShiftEngine.isWorking(it, today) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("وضعیت امروز", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Navy)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(JalaliDate.longText(today), color = Purple, fontWeight = FontWeight.Bold)
                    Text("از ${active.size.toPersian()} نیروی فعال", color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        presentToday.toPersian(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                    Text("نفر حاضر", color = Color.DarkGray, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                JalaliMonthCalendar(
                    year = viewYear,
                    month = viewMonth,
                    onMonthChange = { y, m -> viewYear = y; viewMonth = m },
                    today = today,
                    selected = selectedEpoch?.let { LocalDate.ofEpochDay(it) },
                    onSelect = { selectedEpoch = it.toEpochDay() },
                    badge = if (active.isEmpty()) null else { date ->
                        active.count { ShiftEngine.isWorking(it, date) }.toPersian()
                    }
                )
                Text(
                    "روی هر روز بزنید تا وضعیت حضور نیروها نمایش داده شود. عدد زیر هر روز، تعداد افراد حاضر است.",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    selectedEpoch?.let { epoch ->
        DayDetailSheet(
            date = LocalDate.ofEpochDay(epoch),
            people = active,
            onDismiss = { selectedEpoch = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailSheet(date: LocalDate, people: List<Person>, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by rememberSaveable { mutableStateOf(0) }
    val statuses = remember(date, people) { people.map { it to ShiftEngine.status(it, date) } }
    val employments = Employment.entries

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = AppBackground) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.88f).navigationBarsPadding()) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                Text(
                    JalaliDate.longText(date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
                val present = statuses.count { it.second.working }
                Text(
                    "کل: ${present.toPersian()} نفر حاضر • ${(statuses.size - present).toPersian()} نفر غایب",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TabRow(selectedTabIndex = tab, containerColor = Color.White) {
                employments.forEachIndexed { i, e ->
                    val group = statuses.filter { it.first.employment == e }
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(e.title, fontWeight = FontWeight.Bold)
                                Text(
                                    "${group.count { it.second.working }.toPersian()} از ${group.size.toPersian()}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    )
                }
            }
            val group = statuses.filter { it.first.employment == employments[tab] }
            val present = group.filter { it.second.working }
            val absent = group.filter { !it.second.working }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (group.isEmpty()) {
                    EmptyMessage("نیروی ${employments[tab].title} ثبت نشده است.")
                } else {
                    Section("حاضر (${present.size.toPersian()} نفر)", PresentGreen, present, "کسی حاضر نیست.")
                    Section("غایب (${absent.size.toPersian()} نفر)", AbsentGray, absent, "همه حاضرند.")
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, color: Color, items: List<Pair<Person, DayStatus>>, emptyText: String) {
    Text(title, fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.titleSmall)
    if (items.isEmpty()) {
        Text(emptyText, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
    items.forEach { (p, s) -> StatusRow(p, s, color) }
}

@Composable
private fun StatusRow(person: Person, status: DayStatus, color: Color) {
    val line = when {
        !status.started -> "هنوز شروع نشده • ${status.daysToChange.toPersian()} روز تا شروع کار"
        status.working -> "${status.elapsedDays.toPersian()} روز از کار گذشته • ${status.daysToChange.toPersian()} روز تا استراحت"
        else -> "${status.elapsedDays.toPersian()} روز از استراحت گذشته • ${status.daysToChange.toPersian()} روز تا شروع کار"
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (person.isLeader) "${person.name} • سرشیفت" else person.name,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
                Text(line, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                Text(
                    "${person.pattern.title} • شروع ${JalaliDate.from(person.startDate).display()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Surface(color = color.copy(alpha = .13f), shape = RoundedCornerShape(8.dp)) {
                Text(
                    status.shift.title,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    color = color,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
