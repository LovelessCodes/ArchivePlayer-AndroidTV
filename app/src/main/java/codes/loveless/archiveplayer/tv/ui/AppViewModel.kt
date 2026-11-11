package codes.loveless.archiveplayer.tv.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import codes.loveless.archiveplayer.tv.ArchivePlayerApplication
import codes.loveless.archiveplayer.tv.data.ServerProfile
import codes.loveless.archiveplayer.tv.data.ServerTarget

sealed interface StartupState {
    data object Loading : StartupState
    data object Setup : StartupState
    data class Pairing(val target: ServerTarget, val attempt: Int) : StartupState
    data object Ready : StartupState
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as ArchivePlayerApplication).container

    var startup by mutableStateOf<StartupState>(StartupState.Loading)
        private set
    var profile by mutableStateOf<ServerProfile?>(null)
        private set

    private var pairingAttempt = 0

    val deviceName: String get() = container.store.deviceName()
    val activeUrl: String? get() = container.store.activeUrl()

    init {
        load()
    }

    private fun load() {
        val stored = container.store.loadProfile()
        profile = stored
        startup = when {
            stored?.token != null -> StartupState.Ready
            stored != null -> StartupState.Pairing(
                container.repository.serverTarget(stored),
                pairingAttempt,
            )
            else -> StartupState.Setup
        }
    }

    fun selectServer(target: ServerTarget) {
        pairingAttempt += 1
        startup = StartupState.Pairing(target, pairingAttempt)
    }

    fun paired(profile: ServerProfile) {
        container.store.saveProfile(profile)
        this.profile = profile
        startup = StartupState.Ready
    }

    fun updateProfile(profile: ServerProfile) {
        container.store.saveProfile(profile)
        this.profile = profile
    }

    fun expireSession() {
        val current = profile
        if (current == null) {
            startup = StartupState.Setup
            return
        }
        container.store.clearToken()
        val cleared = current.copy(token = null)
        profile = cleared
        pairingAttempt += 1
        startup = StartupState.Pairing(
            container.repository.serverTarget(cleared),
            pairingAttempt,
        )
    }

    fun unpair() {
        expireSession()
    }

    fun switchServer() {
        container.store.clearAll()
        profile = null
        startup = StartupState.Setup
    }

    fun backToSetup() {
        startup = StartupState.Setup
    }

    fun setDeviceName(name: String) {
        container.store.setDeviceName(name)
    }
}
