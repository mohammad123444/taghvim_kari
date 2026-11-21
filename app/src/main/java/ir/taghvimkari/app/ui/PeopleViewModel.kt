package ir.taghvimkari.app.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.taghvimkari.app.BuildConfig
import ir.taghvimkari.app.data.DataExchange
import ir.taghvimkari.app.data.LocalStore
import ir.taghvimkari.app.data.UpdateChecker
import ir.taghvimkari.app.data.UpdateInfo
import ir.taghvimkari.app.data.UpdateInstaller
import ir.taghvimkari.app.data.UpdateResult
import ir.taghvimkari.app.domain.Person
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

class PeopleViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app)
    private var checkInFlight = false

    var people: List<Person> by mutableStateOf(store.load())
        private set

    var updateUi: UpdateUi by mutableStateOf(UpdateUi.Idle)
        private set

    /** Update checking is off when the build has no GitHub repository configured. */
    val updatesEnabled: Boolean = BuildConfig.UPDATE_REPO.isNotBlank()

    /** People received from a file, waiting for the user to confirm the replacement. */
    var pendingImport: List<Person>? by mutableStateOf(null)
        private set

    var importError: String? by mutableStateOf(null)
        private set

    init {
        if (updatesEnabled) checkForUpdate(manual = false)
    }

    private fun commit() {
        store.save(people)
    }

    /** Reads a received data file; the data only replaces the current list after [confirmImport]. */
    fun offerImport(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { DataExchange.decode(DataExchange.readFile(getApplication(), uri)) }
            }
                .onSuccess { pendingImport = it }
                .onFailure { importError = it.message ?: "فایل معتبر نیست." }
        }
    }

    fun confirmImport() {
        pendingImport?.let {
            people = it
            commit()
        }
        pendingImport = null
    }

    fun cancelImport() {
        pendingImport = null
    }

    fun dismissImportError() {
        importError = null
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
