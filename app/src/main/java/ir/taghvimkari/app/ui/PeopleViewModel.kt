package ir.taghvimkari.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.taghvimkari.app.BuildConfig
import ir.taghvimkari.app.data.BackupClient
import ir.taghvimkari.app.data.BackupPrefs
import ir.taghvimkari.app.data.LocalStore
import ir.taghvimkari.app.data.UpdateChecker
import ir.taghvimkari.app.data.UpdateInfo
import ir.taghvimkari.app.data.UpdateInstaller
import ir.taghvimkari.app.data.UpdateResult
import ir.taghvimkari.app.domain.Person
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

sealed interface UpdateUi {
    data object Idle : UpdateUi
    data object Checking : UpdateUi
    data object UpToDate : UpdateUi
    data object Failed : UpdateUi
    data class Available(val info: UpdateInfo) : UpdateUi
    data class Downloading(val percent: Int) : UpdateUi
    data class Downloaded(val path: String) : UpdateUi
    data class DownloadFailed(val info: UpdateInfo) : UpdateUi
}

/** State of the online backup shown in Settings. */
data class BackupUi(
    val enabled: Boolean = false,
    val busy: Boolean = false,
    val lastSuccess: Long = 0L,
    val message: String? = null,
    val isError: Boolean = false,
    /** An older backup exists online; the user must choose before anything is overwritten. */
    val foundExisting: Boolean = false
)

class PeopleViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app)
    private val backupPrefs = BackupPrefs(app)
    private var backupJob: Job? = null
    private var checkInFlight = false

    var people: List<Person> by mutableStateOf(store.load())
        private set

    var updateUi: UpdateUi by mutableStateOf(UpdateUi.Idle)
        private set

    /** Update checking is off when the build has no GitHub repository configured. */
    val updatesEnabled: Boolean = BuildConfig.UPDATE_REPO.isNotBlank()

    var backupUi: BackupUi by mutableStateOf(BackupUi(enabled = backupPrefs.enabled, lastSuccess = backupPrefs.lastSuccess))
        private set

    init {
        if (updatesEnabled) checkForUpdate(manual = false)
        // Changes made while offline are uploaded the next time the app opens.
        if (backupPrefs.enabled && backupPrefs.dirty) scheduleBackup()
    }

    /** Every change goes through here: saved locally, then uploaded shortly after. */
    private fun commit() {
        store.save(people)
        scheduleBackup()
    }

    private fun scheduleBackup() {
        backupPrefs.dirty = true
        if (!backupPrefs.enabled || backupUi.foundExisting) return
        backupJob?.cancel()
        backupJob = viewModelScope.launch {
            delay(1200) // several quick edits become one upload
            uploadNow()
        }
    }

    private suspend fun uploadNow() {
        backupUi = backupUi.copy(busy = true)
        val content = LocalStore.encode(people).toString()
        BackupClient.upload(backupPrefs.token, backupPrefs.gistId.ifBlank { null }, content)
            .onSuccess { id ->
                backupPrefs.gistId = id
                backupPrefs.lastSuccess = System.currentTimeMillis()
                backupPrefs.dirty = false
                backupUi = backupUi.copy(
                    busy = false, lastSuccess = backupPrefs.lastSuccess,
                    message = "پشتیبان‌گیری انجام شد.", isError = false
                )
            }
            .onFailure {
                backupUi = backupUi.copy(
                    busy = false,
                    message = (it.message ?: "پشتیبان‌گیری ناموفق بود.") + " دفعهٔ بعد دوباره تلاش می‌شود.",
                    isError = true
                )
            }
    }

    fun connectBackup(token: String) {
        val t = token.trim()
        if (t.isEmpty() || backupUi.busy) return
        backupUi = backupUi.copy(busy = true, message = null)
        viewModelScope.launch {
            BackupClient.findExisting(t)
                .onSuccess { id ->
                    backupPrefs.token = t
                    backupPrefs.enabled = true
                    if (id != null) {
                        backupPrefs.gistId = id
                        backupUi = backupUi.copy(enabled = true, busy = false, foundExisting = true)
                    } else {
                        backupUi = backupUi.copy(enabled = true)
                        uploadNow()
                    }
                }
                .onFailure {
                    backupUi = backupUi.copy(busy = false, message = it.message ?: "اتصال ناموفق بود.", isError = true)
                }
        }
    }

    /** The user keeps the data on this phone and replaces the older online backup. */
    fun keepCurrentData() {
        backupUi = backupUi.copy(foundExisting = false)
        viewModelScope.launch { uploadNow() }
    }

    fun backupNow() {
        if (!backupPrefs.enabled || backupUi.busy) return
        viewModelScope.launch { uploadNow() }
    }

    fun restoreBackup() {
        if (!backupPrefs.enabled || backupUi.busy) return
        backupUi = backupUi.copy(busy = true, message = null, foundExisting = false)
        viewModelScope.launch {
            BackupClient.download(backupPrefs.token, backupPrefs.gistId)
                .mapCatching { LocalStore.decode(JSONArray(it)) }
                .onSuccess { list ->
                    people = list
                    store.save(list)
                    backupPrefs.dirty = false
                    backupUi = backupUi.copy(busy = false, message = "اطلاعات بازیابی شد (${list.size} نفر).", isError = false)
                }
                .onFailure {
                    backupUi = backupUi.copy(busy = false, message = it.message ?: "بازیابی ناموفق بود.", isError = true)
                }
        }
    }

    fun disableBackup() {
        backupJob?.cancel()
        backupPrefs.enabled = false
        backupPrefs.token = ""
        backupUi = BackupUi()
    }

    /** Registers [head] and all [members] (who share the head's conditions) in one go. */
    fun add(head: Person, members: List<Person>) {
        people = people + head + members
        commit()
    }

    /**
     * Edits one person; any [members] typed during the edit are added as new people.
     * With [applyToMembers], only the conditions that changed in this edit are copied to the
     * leader's existing members, so their other individual settings stay untouched.
     */
    fun update(head: Person, members: List<Person>, applyToMembers: Boolean = false) {
        val old = people.firstOrNull { it.id == head.id }
        people = people.map { p ->
            when {
                p.id == head.id -> head
                applyToMembers && old != null && p.leaderId == head.id -> {
                    val dateChanged = head.startDate != old.startDate
                    p.copy(
                        employment = if (head.employment != old.employment) head.employment else p.employment,
                        category = if (head.category != old.category) head.category else p.category,
                        pattern = if (head.pattern != old.pattern) head.pattern else p.pattern,
                        startDate = if (dateChanged) head.startDate else p.startDate,
                        startWeekday = if (dateChanged) head.startWeekday else p.startWeekday
                    )
                }
                else -> p
            }
        } + members
        commit()
    }

    fun remove(id: String) {
        people = people.filterNot { it.id == id }
        commit()
    }

    /** [manual] = the user tapped the button, so "up to date" and errors are reported; automatic checks stay silent. */
    fun checkForUpdate(manual: Boolean) {
        if (!updatesEnabled || checkInFlight) return
        checkInFlight = true
        if (manual) updateUi = UpdateUi.Checking
        viewModelScope.launch {
            val result = UpdateChecker.fetchLatest(BuildConfig.UPDATE_REPO, BuildConfig.VERSION_NAME)
            updateUi = when (result) {
                is UpdateResult.Available -> UpdateUi.Available(result.info)
                UpdateResult.UpToDate -> if (manual) UpdateUi.UpToDate else UpdateUi.Idle
                UpdateResult.Failed -> if (manual) UpdateUi.Failed else UpdateUi.Idle
            }
            checkInFlight = false
        }
    }

    fun downloadUpdate(info: UpdateInfo) {
        val url = info.apkUrl?.takeIf { it.isNotBlank() }
        if (url == null) {
            updateUi = UpdateUi.DownloadFailed(info)
            return
        }
        updateUi = UpdateUi.Downloading(0)
        viewModelScope.launch {
            val file = UpdateInstaller.download(getApplication(), url) { p -> updateUi = UpdateUi.Downloading(p) }
            updateUi = if (file != null) UpdateUi.Downloaded(file.absolutePath) else UpdateUi.DownloadFailed(info)
        }
    }

    fun dismissUpdate() {
        updateUi = UpdateUi.Idle
    }
}
