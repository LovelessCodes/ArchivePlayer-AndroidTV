package codes.loveless.archiveplayer.tv

import android.app.Application
import codes.loveless.archiveplayer.tv.data.AppContainer

class ArchivePlayerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
