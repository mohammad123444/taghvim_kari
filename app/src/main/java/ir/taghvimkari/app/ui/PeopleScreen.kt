package ir.taghvimkari.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.taghvimkari.app.domain.Person

@Composable
fun PeopleScreen(people: List<Person>, onAdd: () -> Unit, onEdit: (Person) -> Unit, onDelete: (Person) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val leaderNames = people.filter { it.isLeader }.associate { it.id to it.name }
    // Each leader is followed by their members.
    val groupOrder = people.map { it.leaderId ?: it.id }.distinct()
    val filtered = people
        .filter { it.name.contains(query.trim(), ignoreCase = true) }
        .sortedWith(compareBy({ groupOrder.indexOf(it.leaderId ?: it.id) }, { if (it.isLeader) 0 else 1 }))
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "مدیریت نیروها",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Navy,
                modifier = Modifier.weight(1f)
            )
            FilledIconButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = "افزودن نیرو") }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("جستجو بر اساس نام") },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        if (filtered.isEmpty()) EmptyMessage("نیرویی پیدا نشد. برای ثبت نیرو، دکمه + را بزنید. برای ویرایش، روی نام هر نفر بزنید.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { p ->
                Row(
                    Modifier.padding(start = if (p.leaderId != null && !p.isLeader) 20.dp else 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        PersonCard(p, null, leaderName = p.leaderId?.let { leaderNames[it] }, onClick = { onEdit(p) })
                    }
                    IconButton(onClick = { onDelete(p) }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                    }
                }
            }
        }
    }
}
