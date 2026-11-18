package ir.taghvimkari.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ir.taghvimkari.app.BuildConfig
import ir.taghvimkari.app.domain.JalaliDate
import ir.taghvimkari.app.domain.toPersian
import ir.taghvimkari.app.domain.toPersianDigits
import java.time.Instant
import java.time.ZoneId

/** Everything about the app itself rather than the staff: version, updates, online backup, stored data. */
@Composable
fun SettingsScreen(viewModel: PeopleViewModel) {
    val people = viewModel.people
    val backup = viewModel.backupUi
    val checking = viewModel.updateUi is UpdateUi.Checking
    var token by rememberSaveable { mutableStateOf("") }
    var confirmRestore by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("تنظیمات نرم‌افزار", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Navy)

        SettingsCard("پشتیبان‌گیری آنلاین") {
            if (!backup.enabled) {
                Text(
                    "پس از هر تغییر، اطلاعات خودکار در یک Gist خصوصی حساب GitHub شما ذخیره می‌شود. " +
                        "در GitHub: Settings ← Developer settings ← Personal access tokens ← Tokens (classic) ← " +
                        "Generate new token، فقط گزینهٔ gist را تیک بزنید و توکن را اینجا بگذارید.",
                    color = Color.DarkGray,
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = token, onValueChange = { token = it },
                    label = { Text("توکن GitHub") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.connectBackup(token); token = "" },
                    enabled = token.isNotBlank() && !backup.busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("فعال‌سازی پشتیبان‌گیری") }
            } else {
                Text(
                    if (backup.lastSuccess > 0) "آخرین پشتیبان: ${formatBackupTime(backup.lastSuccess)}" else "هنوز پشتیبانی ثبت نشده است.",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "پشتیبان‌گیری خودکار فعال است؛ چند ثانیه پس از هر تغییر انجام می‌شود.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
                Button(onClick = viewModel::backupNow, enabled = !backup.busy, modifier = Modifier.fillMaxWidth()) {
                    Text("پشتیبان‌گیری همین حالا")
                }
                OutlinedButton(onClick = { confirmRestore = true }, enabled = !backup.busy, modifier = Modifier.fillMaxWidth()) {
                    Text("بازیابی از پشتیبان آنلاین")
                }
                TextButton(onClick = viewModel::disableBackup, enabled = !backup.busy) {
                    Text("قطع اتصال و حذف توکن از این گوشی", color = Color.Red)
                }
            }
            if (backup.busy) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            }
            backup.message?.let {
                Text(
                    it,
                    color = if (backup.isError) MaterialTheme.colorScheme.error else Color(0xFF218838),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        SettingsCard("بروزرسانی") {
            Text("نسخهٔ فعلی: ${BuildConfig.VERSION_NAME.toPersianDigits()}")
            if (viewModel.updatesEnabled) {
                Button(
                    onClick = { viewModel.checkForUpdate(manual = true) },
                    enabled = !checking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (checking) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Text("  در حال بررسی…")
                    } else {
                        Text("بررسی بروزرسانی")
                    }
                }
                Text(
                    "برنامه هنگام باز شدن هم خودش نسخهٔ جدید را بررسی می‌کند.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text("بروزرسانی برای این نسخه فعال نیست.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }

        SettingsCard("اطلاعات ذخیره‌شده") {
            Text("نیروهای ثبت‌شده: ${people.size.toPersian()} نفر")
            Text("سرشیفت‌ها: ${people.count { it.isLeader }.toPersian()} نفر")
            Text(
                "اطلاعات روی همین دستگاه هم ذخیره می‌شود و با حذف برنامه پاک می‌شود؛ برای نگهداری، پشتیبان‌گیری آنلاین را فعال کنید.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (backup.foundExisting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("پشتیبان قبلی پیدا شد", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "یک پشتیبان آنلاین از قبل وجود دارد. برای برگرداندن اطلاعات قبلی «بازیابی» را بزنید. " +
                        "اگر ادامه با اطلاعات فعلی این گوشی را بزنید، پشتیبان قبلی با آن جایگزین می‌شود."
                )
            },
            confirmButton = { Button(onClick = viewModel::restoreBackup) { Text("بازیابی") } },
            dismissButton = { TextButton(onClick = viewModel::keepCurrentData) { Text("ادامه با اطلاعات فعلی") } }
        )
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("بازیابی اطلاعات", fontWeight = FontWeight.Bold) },
            text = { Text("اطلاعات فعلی این گوشی با آخرین پشتیبان آنلاین جایگزین می‌شود. ادامه می‌دهید؟") },
            confirmButton = {
                Button(onClick = { confirmRestore = false; viewModel.restoreBackup() }) { Text("بازیابی") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("انصراف") } }
        )
    }
}

private fun formatBackupTime(millis: Long): String {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val clock = String.format(java.util.Locale.ROOT, "%02d:%02d", time.hour, time.minute)
    return "${JalaliDate.from(time.toLocalDate()).display()} ساعت ${clock.toPersianDigits()}"
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Purple, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
