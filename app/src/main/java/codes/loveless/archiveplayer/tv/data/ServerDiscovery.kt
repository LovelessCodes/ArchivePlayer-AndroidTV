package codes.loveless.archiveplayer.tv.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import codes.loveless.archiveplayer.tv.util.ServerUrl
import java.io.IOException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch

@Suppress("DEPRECATION")
class ServerDiscovery(context: Context) {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    fun discover(): Flow<List<DiscoveredServer>> = callbackFlow {
        val found = LinkedHashMap<String, DiscoveredServer>()
        val queue = ArrayDeque<NsdServiceInfo>()
        val lock = Any()
        var resolving = false
        var stopped = false

        fun publish() {
            trySend(found.values.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }))
        }

        fun resolveNext() {
            val next = synchronized(lock) {
                if (stopped || resolving || queue.isEmpty()) {
                    null
                } else {
                    resolving = true
                    queue.removeFirst()
                }
            } ?: return
            val listener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    synchronized(lock) { resolving = false }
                    resolveNext()
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val server = serviceInfo.toDiscoveredServer()
                    synchronized(lock) {
                        if (server != null) found[serviceInfo.serviceName] = server
                        resolving = false
                    }
                    publish()
                    resolveNext()
                }
            }
            runCatching { nsd.resolveService(next, listener) }
                .onFailure {
                    synchronized(lock) { resolving = false }
                    resolveNext()
                }
        }

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                synchronized(lock) { queue.addLast(serviceInfo) }
                resolveNext()
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                synchronized(lock) { found.remove(serviceInfo.serviceName) }
                publish()
            }

            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                close(IOException("LAN discovery could not start (code $errorCode)."))
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }

        try {
            nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (error: Exception) {
            close(error)
            return@callbackFlow
        }

        awaitClose {
            synchronized(lock) { stopped = true }
            runCatching { nsd.stopServiceDiscovery(listener) }
        }
    }.catch {
        // Discovery is optional. The setup screen still offers a manual address
        // when NSD is unavailable (Ethernet-only TVs, restricted guest networks,
        // or a device with multicast disabled).
        emit(emptyList())
    }

    private fun NsdServiceInfo.toDiscoveredServer(): DiscoveredServer? {
        val address = host?.hostAddress?.substringBefore('%') ?: return null
        if (port <= 0) return null
        val hostPart = if (address.contains(':')) "[$address]" else address
        val path = attribute("path")?.let { if (it.startsWith("/")) it else "/$it" } ?: "/"
        val lanUrl = runCatching {
            ServerUrl.normalize("http://$hostPart:$port$path")
        }.getOrNull() ?: return null
        val publicUrl = attribute("url")
            ?.takeIf { it.startsWith("https://", ignoreCase = true) }
            ?.let { runCatching { ServerUrl.normalize(it) }.getOrNull() }
        val name = attribute("name")
            ?.takeIf { it.isNotBlank() }
            ?: serviceName.substringBefore("._").ifBlank { "ArchivePlayer" }
        return DiscoveredServer(
            id = attribute("id")?.takeIf { it.isNotBlank() } ?: serviceName,
            name = name.take(80),
            url = lanUrl,
            remoteUrl = publicUrl?.takeIf { it != lanUrl },
            apiVersion = attribute("api")?.toIntOrNull() ?: 1,
            version = attribute("version")?.takeIf { it.isNotBlank() },
        )
    }

    private fun NsdServiceInfo.attribute(key: String): String? {
        val raw = attributes[key] ?: attributes.entries.firstOrNull { it.key.equals(key, true) }?.value
        return raw?.toString(Charsets.UTF_8)?.trim()?.trim('"')?.takeIf { it.isNotEmpty() }
    }

    companion object {
        private const val SERVICE_TYPE = "_archiveplayer._tcp."
    }
}
