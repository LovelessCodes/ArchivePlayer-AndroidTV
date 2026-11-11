package codes.loveless.archiveplayer.tv.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val store = ServerStore(context)
    val api = ApiClient()
    val repository = ServerRepository(store, api, scope)
    val discovery = ServerDiscovery(context)
}
