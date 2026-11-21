package ir.taghvimkari.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.taghvimkari.app.BuildConfig
import ir.taghvimkari.app.domain.Person
import ir.taghvimkari.app.domain.toPersian
import ir.taghvimkari.app.domain.toPersianDigits

@Composable
fun TaghvimApp(viewModel: PeopleViewModel) {
    var page by rememberSaveable { mutableStateOf(0) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Person?>(null) }
    var editing by remember { mutableStateOf<Person?>(null) }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            AppHeader()
        },
        bottomBar = { AppNavigation(selected = page, onSelect = { page = it }) }
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            when (page) {
                0 -> DashboardScreen(viewModel.people)
                1 -> PeopleScreen(
                    people = viewModel.people,
                    onAdd = { showAdd = true },
                    onEdit = { editing = it }
                )
                2 -> CompareScreen(viewModel.people)
                else -> SettingsScreen(viewModel)
            }
        }
    }

    if (showAdd) {
        AddPersonDialog(
            onDismiss = { showAdd = false },
            onSave = { head, members, _ -> viewModel.add(head, members); showAdd = false }
        )
    }

    editing?.let { person ->
        AddPersonDialog(
            initial = person,
            memberCount = viewModel.people.count { it.leaderId == person.id },
            onDelete = { editing = null; pendingDelete = person },
            onDismiss = { editing = null },
            onSave = { head, members, apply -> viewModel.update(head, members, apply); editing = null }
        )
    }

    UpdateDialog(
        state = viewModel.updateUi,
        onDismiss = viewModel::dismissUpdate,
        onDownload = viewModel::downloadUpdate
    )

    viewModel.pendingImport?.let { incoming ->
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text("دریافت اطلاعات", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "در فایل دریافتی ${incoming.size.toPersian()} نفر هست. اطلاعات فعلی این گوشی " +
                        "(${viewModel.people.size.toPersian()} نفر) کاملاً با آن جایگزین می‌شود. ادامه می‌دهید؟"
                )
            },
            confirmButton = { TextButton(onClick = viewModel::confirmImport) { Text("جایگزین کن", fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = viewModel::cancelImport) { Text("انصراف") } }
        )
    }

    viewModel.importError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissImportError,
            title = { Text("فایل پذیرفته نشد", fontWeight = FontWeight.Bold) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::dismissImportError) { Text("باشه") } }
        )
    }

    pendingDelete?.let { person ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("حذف نیرو", fontWeight = FontWeight.Bold) },
            text = {
                val members = viewModel.people.count { it.leaderId == person.id }
                Text(
                    "«${person.name}» حذف شود؟ این کار قابل بازگشت نیست." +
                        if (person.isLeader && members > 0) " زیرمجموعه‌های او حذف نمی‌شوند." else ""
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.remove(person.id); pendingDelete = null }) {
                    Text("حذف", color = Color.Red)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun AppHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Navy)
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("تقویم کاری", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "مدیریت نیروها و شیفت‌ها • نسخه ${BuildConfig.VERSION_NAME.toPersianDigits()}",
                color = Color(0xFFDAD9FF),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun AppNavigation(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = selected == 0, onClick = { onSelect(0) },
            icon = { Icon(Icons.Default.DateRange, contentDescription = null) }, label = { Text("تقویم") }
        )
        NavigationBarItem(
            selected = selected == 1, onClick = { onSelect(1) },
            icon = { Icon(Icons.Default.Person, contentDescription = null) }, label = { Text("نیروها") }
        )
        NavigationBarItem(
            selected = selected == 2, onClick = { onSelect(2) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }, label = { Text("مقایسه") }
        )
        NavigationBarItem(
            selected = selected == 3, onClick = { onSelect(3) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) }, label = { Text("تنظیمات") }
        )
    }
}
