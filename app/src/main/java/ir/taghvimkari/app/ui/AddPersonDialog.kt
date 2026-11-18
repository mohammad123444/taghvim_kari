package ir.taghvimkari.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
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
    var employment by remember { mutableStateOf(Employment.OFFICIAL) }
    var pattern by remember { mutableStateOf(Pattern.DAY_NIGHT_REST) }
    var startEpochDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    val startDate = LocalDate.ofEpochDay(startEpochDay)

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
                FilterDropdown("وضعیت استخدامی", Employment.entries.map { it.title }, employment.title) { v ->
                    employment = Employment.entries.first { it.title == v }
                }
                FilterDropdown("الگوی شیفت", Pattern.entries.map { it.title }, pattern.title) { v ->
                    pattern = Pattern.entries.first { it.title == v }
                }
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("تاریخ شروع: ${JalaliDate.from(startDate).display()}")
                }
                Text(
                    JalaliDate.longText(startDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        Person(
                            id = UUID.randomUUID().toString(),
                            name = name.trim(),
                            workType = WorkTypes.default,
                            employment = employment,
                            pattern = pattern,
                            startDate = startDate,
                            // The cycle begins on the weekday of the chosen start date.
                            startWeekday = ShiftEngine.persianWeekday(startDate)
                        )
                    )
                } else {
                    showErrors = true
                }
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )

    if (showPicker) {
        JalaliDatePickerDialog(
            initial = startDate,
            title = "انتخاب تاریخ شروع",
            onDismiss = { showPicker = false },
            onPick = { startEpochDay = it.toEpochDay(); showPicker = false }
        )
    }
}
