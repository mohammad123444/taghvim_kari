package ir.taghvimkari.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.taghvimkari.app.domain.JalaliDate
import ir.taghvimkari.app.domain.Person
import ir.taghvimkari.app.domain.ShiftEngine
import ir.taghvimkari.app.domain.toPersian
import java.time.LocalDate

@Composable
fun DashboardScreen(people: List<Person>) {
    val today = LocalDate.now()
    val working = people.filter { it.active && ShiftEngine.isWorking(it, today) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("وضعیت امروز", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Navy)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text(JalaliDate.longText(today), color = Purple, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("تعداد افراد حاضر", color = Color.DarkGray)
                Text(
                    "${working.size.toPersian()} نفر",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
                Text("از ${people.count { it.active }.toPersian()} نیروی فعال", color = Color.Gray)
            }
        }
        Text("افراد حاضر امروز", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (working.isEmpty()) EmptyMessage("هنوز نیرویی ثبت نشده یا امروز فردی در شیفت نیست.")
        working.forEach { p -> PersonCard(p, ShiftEngine.shift(p, today)) }
    }
}
