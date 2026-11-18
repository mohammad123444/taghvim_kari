package ir.taghvimkari.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.taghvimkari.app.domain.JalaliDate
import ir.taghvimkari.app.domain.Person
import ir.taghvimkari.app.domain.Shift
import ir.taghvimkari.app.domain.ShiftEngine
import ir.taghvimkari.app.domain.sortedByTeam
import ir.taghvimkari.app.domain.toPersian
import java.time.LocalDate

private const val MAX_DAYS = 400
private val DayColor = Color(0xFF2E9E57)
private val NightColor = Color(0xFF4F46C8)
private val RestColor = Color(0xFFD9D9E6)
private val NAME_WIDTH = 96.dp

/** 0 = rest, 1 = day / work, 2 = night. */
private fun kindOf(person: Person, date: LocalDate): Int = when (ShiftEngine.shift(person, date)) {
    Shift.REST -> 0
    Shift.NIGHT -> 2
    else -> 1
}

private fun kindColor(kind: Int): Color = when (kind) {
    1 -> DayColor
    2 -> NightColor
    else -> RestColor
}

/** Run-length encoding of a person's status over the range: list of (kind, length in days). */
private fun runsOf(person: Person, from: LocalDate, days: Int): List<Pair<Int, Int>> {
    val out = ArrayList<Pair<Int, Int>>()
    var kind = -1
    var length = 0
    for (i in 0 until days) {
        val k = kindOf(person, from.plusDays(i.toLong()))
        if (k == kind) {
            length++
        } else {
            if (length > 0) out.add(kind to length)
            kind = k
            length = 1
        }
    }
    if (length > 0) out.add(kind to length)
    return out
}

private data class CompareRow(val person: Person, val runs: List<Pair<Int, Int>>) {
    val workDays: Int get() = runs.filter { it.first != 0 }.sumOf { it.second }
    val restDays: Int get() = runs.filter { it.first == 0 }.sumOf { it.second }
}

/** Compare the work status of several people over one day, a few days or several months on a single page. */
@Composable
fun CompareScreen(people: List<Person>) {
    val today = remember { LocalDate.now() }
    var selectedText by rememberSaveable { mutableStateOf("") }
    var fromEpoch by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var toEpoch by rememberSaveable { mutableStateOf(today.plusDays(13).toEpochDay()) }
    var picker by rememberSaveable { mutableStateOf(0) } // 0 = closed, 1 = from, 2 = to
    var showPeople by rememberSaveable { mutableStateOf(false) }

    val from = LocalDate.ofEpochDay(fromEpoch)
    val to = LocalDate.ofEpochDay(toEpoch)
    val days = (toEpoch - fromEpoch + 1).toInt()
    val error: String? = when {
        days < 1 -> "تاریخ پایان نباید قبل از تاریخ شروع باشد."
        days > MAX_DAYS -> "حداکثر طول بازه ${MAX_DAYS.toPersian()} روز است."
        else -> null
    }

    val active = people.filter { it.active }
    val leaderNames = active.filter { it.isLeader }.associate { it.id to it.name }
    val selectedIds = selectedText.split(",").filter { it.isNotBlank() }.toSet()
    val chosen = active.filter { it.id in selectedIds }.sortedByTeam(leaderNames) { it }

    val rows = remember(chosen, fromEpoch, toEpoch) {
        if (error != null) emptyList() else chosen.map { CompareRow(it, runsOf(it, from, days)) }
    }
    val together = remember(chosen, fromEpoch, toEpoch) {
        if (error != null || chosen.size < 2) {
            null
        } else {
            var allWork = 0
            var allRest = 0
            for (i in 0 until days) {
                val d = from.plusDays(i.toLong())
                val kinds = chosen.map { kindOf(it, d) }
                if (kinds.all { it != 0 }) allWork++
                if (kinds.all { it == 0 }) allRest++
            }
            allWork to allRest
        }
    }

    // Week lengths inside the range; weeks start on Saturday, so the first and last may be partial.
    val weekLengths = remember(fromEpoch, toEpoch) {
        val out = ArrayList<Int>()
        var length = 0
        for (i in 0 until days.coerceAtLeast(0)) {
            if (i > 0 && ShiftEngine.persianWeekday(from.plusDays(i.toLong())) == 0) {
                out.add(length)
                length = 0
            }
            length++
        }
        if (length > 0) out.add(length)
        out
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("مقایسهٔ وضعیت کاری", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Navy)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { picker = 1 }, modifier = Modifier.weight(1f)) {
                Text("از: ${JalaliDate.from(from).display()}")
            }
            OutlinedButton(onClick = { picker = 2 }, modifier = Modifier.weight(1f)) {
                Text("تا: ${JalaliDate.from(to).display()}")
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("۱ روز" to 1, "۱ هفته" to 7, "۲ هفته" to 14, "۱ ماه" to 30, "۳ ماه" to 90, "۶ ماه" to 180).forEach { (label, n) ->
                Surface(
                    color = Purple.copy(alpha = .12f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { toEpoch = fromEpoch + n - 1 }
                ) {
                    Text(
                        label,
                        Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        color = Purple,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
        OutlinedButton(onClick = { showPeople = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (chosen.isEmpty()) "انتخاب نیروها" else "نیروهای انتخاب‌شده: ${chosen.size.toPersian()} نفر (ویرایش)")
        }

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        } else if (rows.isEmpty()) {
            EmptyMessage("نیروها را انتخاب کنید تا وضعیت کاری‌شان در بازهٔ انتخابی کنار هم نمایش داده شود.")
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Legend(DayColor, "روزکار / کار")
                        Legend(NightColor, "شب‌کار")
                        Legend(RestColor, "استراحت")
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Spacer(Modifier.width(NAME_WIDTH))
                        Text(JalaliDate.from(from).display(), Modifier.weight(1f), fontSize = 11.sp, color = Color.DarkGray)
                        Text("${days.toPersian()} روز", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy)
                        Text(
                            JalaliDate.from(to).display(),
                            Modifier.weight(1f),
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.End
                        )
                    }
                    if (days <= 14) {
                        Row(Modifier.fillMaxWidth()) {
                            Spacer(Modifier.width(NAME_WIDTH))
                            Row(Modifier.weight(1f)) {
                                for (i in 0 until days) {
                                    Text(
                                        JalaliDate.from(from.plusDays(i.toLong())).day.toPersian(),
                                        Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                    rows.forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.width(NAME_WIDTH)) {
                                Text(
                                    if (row.person.isLeader) "★ ${row.person.name}" else row.person.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${row.workDays.toPersian()} کار • ${row.restDays.toPersian()} استراحت",
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    maxLines = 1
                                )
                            }
                            Box(Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(8.dp))) {
                                Row(Modifier.fillMaxSize()) {
                                    row.runs.forEach { (kind, length) ->
                                        Box(Modifier.weight(length.toFloat()).fillMaxHeight().background(kindColor(kind)))
                                    }
                                }
                                // A white divider at the start of every week (Saturday).
                                if (days > 7) {
                                    Row(Modifier.fillMaxSize()) {
                                        weekLengths.forEachIndexed { index, length ->
                                            Box(Modifier.weight(length.toFloat()).fillMaxHeight()) {
                                                if (index > 0) {
                                                    Box(Modifier.width(2.dp).fillMaxHeight().background(Color.White))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            together?.let { (allWork, allRest) ->
                Text(
                    "همه با هم سر کار: ${allWork.toPersian()} روز • همه با هم در استراحت: ${allRest.toPersian()} روز",
                    color = Navy,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (picker != 0) {
        JalaliDatePickerDialog(
            initial = if (picker == 1) from else to,
            title = if (picker == 1) "تاریخ شروع بازه" else "تاریخ پایان بازه",
            onDismiss = { picker = 0 },
            onPick = { d ->
                if (picker == 1) {
                    fromEpoch = d.toEpochDay()
                    if (toEpoch < fromEpoch) toEpoch = fromEpoch
                } else {
                    toEpoch = d.toEpochDay()
                }
                picker = 0
            }
        )
    }

    if (showPeople) {
        PeoplePickerDialog(
            people = active,
            initial = selectedIds,
            onDismiss = { showPeople = false },
            onConfirm = { selectedText = it.joinToString(","); showPeople = false }
        )
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text("  $label", fontSize = 11.sp, color = Color.DarkGray)
    }
}

@Composable
private fun PeoplePickerDialog(
    people: List<Person>,
    initial: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var draft by remember { mutableStateOf(initial) }
    var query by remember { mutableStateOf("") }
    val leaderNames = remember(people) { people.filter { it.isLeader }.associate { it.id to it.name } }
    val shown = people.filter { it.name.contains(query.trim(), ignoreCase = true) }.sortedByTeam(leaderNames) { it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب نیروها (${draft.size.toPersian()})", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    label = { Text("جستجو") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row {
                    TextButton(onClick = { draft = draft + shown.map { it.id } }) { Text("همهٔ نتایج") }
                    TextButton(onClick = { draft = emptySet() }) { Text("هیچ‌کدام") }
                }
                Text(
                    "با انتخاب سرشیفت، زیرمجموعه‌هایش هم انتخاب می‌شوند؛ می‌توانید تکی برشان دارید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    shown.forEach { p ->
                        val checked = p.id in draft
                        val isMember = p.leaderId != null && !p.isLeader
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val ids = if (p.isLeader) {
                                        setOf(p.id) + people.filter { it.leaderId == p.id }.map { it.id }
                                    } else {
                                        setOf(p.id)
                                    }
                                    draft = if (checked) draft - ids else draft + ids
                                }
                                .padding(start = if (isMember) 18.dp else 0.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Column(Modifier.padding(start = 8.dp)) {
                                Text(if (p.isLeader) "${p.name} • سرشیفت" else p.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "${p.employment.title} • ${p.category.title}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(draft) }) { Text("تأیید") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
