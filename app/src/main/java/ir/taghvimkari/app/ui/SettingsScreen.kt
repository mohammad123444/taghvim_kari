package ir.taghvimkari.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.taghvimkari.app.BuildConfig
import ir.taghvimkari.app.domain.Person
import ir.taghvimkari.app.domain.toPersian
import ir.taghvimkari.app.domain.toPersianDigits

/** Everything that is about the app itself rather than the staff: version, updates, stored data. */
@Composable
fun SettingsScreen(
    people: List<Person>,
    updatesEnabled: Boolean,
    checking: Boolean,
    onCheckUpdate: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("تنظیمات نرم‌افزار", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Navy)

        SettingsCard("بروزرسانی") {
            Text("نسخهٔ فعلی: ${BuildConfig.VERSION_NAME.toPersianDigits()}")
            if (updatesEnabled) {
                Button(onClick = onCheckUpdate, enabled = !checking, modifier = Modifier.fillMaxWidth()) {
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
                "اطلاعات فقط روی همین دستگاه ذخیره می‌شود و با حذف برنامه پاک می‌شود.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, color = Purple, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}
