package ir.taghvimkari.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.taghvimkari.app.domain.Employment
import ir.taghvimkari.app.domain.JalaliDate
import ir.taghvimkari.app.domain.Person
import ir.taghvimkari.app.domain.Shift
import ir.taghvimkari.app.domain.ShiftEngine
import ir.taghvimkari.app.domain.WorkTypes
import ir.taghvimkari.app.domain.toPersian
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Upper bound so a typo like year 1700 cannot freeze the UI. */
private const val MAX_RANGE_DAYS = 366L

@Composable
fun RangeScreen(people: List<Person>) {
    val today = remember { LocalDate.now() }
    var fromText by rememberSaveable { mutableStateOf(JalaliDate.from(today).format()) }
    var toText by rememberSaveable { mutableStateOf(JalaliDate.from(today.plusDays(4)).format()) }
    var employment by rememberSaveable { mutableStateOf(WorkTypes.ALL) }

    val fromDate = JalaliDate.parse(fromText)?.toLocalDate()
    val toDate = JalaliDate.parse(toText)?.toLocalDate()
    val error: String? = when {
        fromDate == null -> "تاریخ شروع معتبر نیست."
        toDate == null -> "تاریخ پایان معتبر نیست."
        toDate.isBefore(fromDate) -> "تاریخ پایان نباید قبل از تاریخ شروع باشد."
        ChronoUnit.DAYS.between(fromDate, toDate) >= MAX_RANGE_DAYS -> "حداکثر طول بازه ${MAX_RANGE_DAYS.toInt().toPersian()} روز است."
        else -> null
    }

    val results: List<Pair<Person, List<Pair<LocalDate, Shift>>>> =
        remember(people, fromDate, toDate, employment, error) {
            if (error != null || fromDate == null || toDate == null) {
                emptyList()
            } else {
                people
                    .filter { p ->
                        p.active &&
                            (employment == WorkTypes.ALL || p.employment.title == employment)
                    }
                    .map { p -> p to ShiftEngine.workingDays(p, fromDate, toDate) }
                    .filter { it.second.isNotEmpty() }
            }
        }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("جستجوی بازه زمانی", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Navy)
        Text(
            "تاریخ شمسی را به شکل سال/ماه/روز وارد کنید؛ نمونه: ${JalaliDate.from(today).display()}",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            value = fromText, onValueChange = { fromText = it },
            label = { Text("از تاریخ") }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, isError = fromDate == null
        )
        OutlinedTextField(
            value = toText, onValueChange = { toText = it },
            label = { Text("تا تاریخ") }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, isError = toDate == null
        )
        FilterDropdown("وضعیت استخدامی", listOf(WorkTypes.ALL) + Employment.entries.map { it.title }, employment) { employment = it }

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        } else {
            Text(
                "افرادی که حداقل یک روز در بازه کار دارند: ${results.size.toPersian()} نفر",
                fontWeight = FontWeight.Bold,
                color = Navy
            )
        }
        results.forEach { (p, days) ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(p.name, fontWeight = FontWeight.Bold, color = Navy)
                    Text(p.employment.title, color = Color.DarkGray)
                    Text(
                        days.joinToString("، ") { (d, s) ->
                            val j = JalaliDate.from(d)
                            "${j.month.toPersian()}/${j.day.toPersian()} (${s.title})"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
