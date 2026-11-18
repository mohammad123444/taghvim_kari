package ir.taghvimkari.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.taghvimkari.app.BuildConfig
import ir.taghvimkari.app.data.LocalStore
import ir.taghvimkari.app.data.UpdateChecker
import ir.taghvimkari.app.data.UpdateInfo
import ir.taghvimkari.app.data.UpdateResult
import ir.taghvimkari.app.domain.Person
import kotlinx.coroutines.launch

sealed interface UpdateUi {
    data object Idle : UpdateUi
    data object Checking : UpdateUi
    data object UpToDate : UpdateUi
    data object Failed : UpdateUi
    data class Available(val info: UpdateInfo) : UpdateUi
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

    init {
        if (updatesEnabled) checkForUpdate(manual = false)
    }

    fun add(person: Person) {
        people = people + person
        store.save(people)
    }

    fun remove(id: String) {
        people = people.filterNot { it.id == id }
        store.save(people)
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

    fun dismissUpdate() {
        updateUi = UpdateUi.Idle
    }
}
