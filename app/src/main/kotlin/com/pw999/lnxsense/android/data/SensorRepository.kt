package com.pw999.lnxsense.android.data

import com.pw999.lnxsense.android.data.model.ConnectionState
import com.pw999.lnxsense.api.Group
import com.pw999.lnxsense.api.Reading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Application-scoped singleton holding the socket connection and sensor data.
 *
 * Manual singleton (double-checked locking) instead of Hilt/DI to keep the dependency
 * graph simple — this app has no other DI needs. The repository must outlive any single
 * Activity or ViewModel so it can maintain the socket connection across config changes.
 *
 * SupervisorJob ensures that a failure in the socket coroutine does not cancel the
 * entire scope (which would prevent reconnection).
 */
class SensorRepository private constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val socketClient = SocketClient()
    private var connectJob: Job? = null

    // Last-used connection parameters for auto-reconnect after backgrounding
    private var lastHost: String? = null
    private var lastPort: Int? = null

    val connectionState: StateFlow<ConnectionState> = socketClient.connectionState
    val groups: StateFlow<List<Group>> = socketClient.groups
    val readings: StateFlow<Map<Pair<String, String>, Reading>> = socketClient.readings

    fun connect(host: String, port: Int) {
        lastHost = host
        lastPort = port
        connectJob?.cancel()
        connectJob = scope.launch {
            socketClient.connect(host, port)
        }
    }

    fun disconnect() {
        connectJob?.cancel()
        socketClient.disconnect()
    }

    /** Reconnects to the last-used server if there was an active connection before. */
    fun reconnect() {
        val host = lastHost ?: return
        val port = lastPort ?: return
        connect(host, port)
    }

    /** Whether a reconnect is possible (i.e. we have a previous host/port). */
    val canReconnect: Boolean get() = lastHost != null && lastPort != null

    companion object {
        @Volatile
        private var instance: SensorRepository? = null

        fun getInstance(): SensorRepository =
            instance ?: synchronized(this) {
                instance ?: SensorRepository().also { instance = it }
            }
    }
}
