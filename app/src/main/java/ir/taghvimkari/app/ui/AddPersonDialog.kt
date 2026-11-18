package ir.taghvimkari.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.taghvimkari.app.domain.Employment
import ir.taghvimkari.app.domain.JalaliDate
import ir.taghvimkari.app.domain.Pattern
import ir.taghvimkari.app.domain.Person
import ir.taghvimkari.app.domain.ShiftEngine
import ir.taghvimkari.app.domain.WorkTypes
import java.time.LocalDate
import java.util.UUID

@Composable
fun AddPersonDialog(onDismiss: () -> Unit, onSave: (Person) -> Unit) {
    val today = remember { LocalDate.now() }
    var name by rememberSaveable { mutableStateOf("") }
    var workType by rememberSaveable { mutableStateOf(WorkTypes.default) }
    var employment by remember { mutableStateOf(Employment.OFFICIAL) }
    var pattern by remember { mutableStateOf(Pattern.DAY_NIGHT_REST) }
    var startText by rememberSaveable { mutableStateOf(JalaliDate.from(today).format()) }
    var weekdayOverride by rememberSaveable { mutableStateOf<Int?>(null) }
    var showErrors by remember { mutableStateOf(false) }

    val startDate = JalaliDate.parse(startText)?.toLocalDate()
    // Unless the user picks one explicitly, the cycle begins on the weekday of the start date.
    val weekday = weekdayOverride ?: startDate?.let { ShiftEngine.persianWeekday(it) } ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت نیروی جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("نام و نام خانوادگی") },
                    singleLine = true, isError = showErrors && name.isBlank()
                )
                FilterDropdown("نوع کاری", WorkTypes.options, workType) { workType = it }
                FilterDropdown("وضعیت استخدامی", Employment.entries.map { it.title }, employment.title) { v ->
                    employment = Employment.entries.first { it.title == v }
                }
                FilterDropdown("الگوی شیفت", Pattern.entries.map { it.title }, pattern.title) { v ->
                    pattern = Pattern.entries.first { it.title == v }
                }
                OutlinedTextField(
                    value = startText, onValueChange = { startText = it },
                    label = { Text("تاریخ شروع (شمسی، مثل ${JalaliDate.from(today).display()})") },
                    singleLine = true, isError = startDate == null
                )
                FilterDropdown(
                    "روز شروع الگو",
                    JalaliDate.WEEKDAY_NAMES,
                    JalaliDate.WEEKDAY_NAMES[weekday]
                ) { v -> weekdayOverride = JalaliDate.WEEKDAY_NAMES.indexOf(v) }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && startDate != null) {
                    onSave(
                        Person(
                            id = UUID.randomUUID().toString(),
                            name = name.trim(),
                            workType = workType,
                            employment = employment,
                            pattern = pattern,
                            startDate = startDate,
                            startWeekday = weekday
                        )
                    )
                } else {
                    showErrors = true
                }
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
