package ir.taghvimkari.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.taghvimkari.app.domain.Employment
import ir.taghvimkari.app.domain.JalaliDate
import ir.taghvimkari.app.domain.Person
import ir.taghvimkari.app.domain.DayStatus
import ir.taghvimkari.app.domain.bucketOf
import ir.taghvimkari.app.domain.toPersian

@Composable
fun EmptyMessage(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        textAlign = TextAlign.Center,
        color = Color.Gray
    )
}

@Composable
fun FilterDropdown(label: String, options: List<String>, value: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: $value")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onChange(option); expanded = false }
                )
            }
        }
    }
}

@Composable
fun PersonCard(person: Person, status: DayStatus? = null, leaderName: String? = null, onClick: (() -> Unit)? = null) {
    val employmentColor = when (person.employment) {
        Employment.OFFICIAL -> Blue
        Employment.CONTRACTUAL -> Green
        Employment.CONTRACTOR -> Orange
    }
    Card(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (person.isLeader) "${person.name} • سرشیفت" else person.name,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
                if (leaderName != null) {
                    Text("زیرمجموعهٔ $leaderName", style = MaterialTheme.typography.bodySmall, color = Purple)
                }
                Text(
                    person.pattern.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
                if (status != null) {
                    val bucket = bucketOf(person, status)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "وضعیت کاری در تاریخ انتخاب‌شده:",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            bucket.title,
                            color = if (bucket.working) Green else Color.DarkGray,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        statusLine(status),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(color = employmentColor.copy(alpha = .13f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        person.employment.title,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        color = employmentColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Surface(color = Purple.copy(alpha = .10f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        person.category.title,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Purple,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/** One-line Persian summary of a [DayStatus]: how long the current work/rest stretch has lasted and how long remains. */
fun statusLine(status: DayStatus): String = when {
    !status.started -> "هنوز شروع نشده • ${status.daysToChange.toPersian()} روز تا شروع کار"
    status.working -> "${status.elapsedDays.toPersian()} روز از کار گذشته • ${status.daysToChange.toPersian()} روز تا استراحت"
    else -> "${status.elapsedDays.toPersian()} روز از استراحت گذشته • ${status.daysToChange.toPersian()} روز تا شروع کار"
}
