package com.pw999.lnxsense.android.data

import com.pw999.lnxsense.android.data.model.ConnectionState
import com.pw999.lnxsense.api.Group
import com.pw999.lnxsense.api.Reading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.io.IOException
import android.util.Log
import java.io.InvalidClassException
import java.io.ObjectInputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.coroutines.coroutineContext

/**
 * Connects to the lnxsense server via a plain TCP socket using Java's ObjectInputStream.
 *
 * Protocol (defined by the server's Socket.java):
 * 1. Server sends Group[] (all devices grouped by category) via ObjectOutputStream
 * 2. Server loops: collects readings, serializes each as byte[] via Reading.asByteArray(),
 *    sends the batch as byte[][] via ObjectOutputStream, then sleeps until next interval
 *
 * The type dispatch below uses javaClass.componentType because Kotlin's type erasure
 * prevents `is Array<Group>` from working at runtime — both Group[] and byte[][] are
 * just Object[] at the JVM level.
 */
class SocketClient {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _readings = MutableStateFlow<Map<Pair<String, String>, Reading>>(emptyMap())
    val readings: StateFlow<Map<Pair<String, String>, Reading>> = _readings.asStateFlow()

    private var socket: Socket? = null

    companion object {
        private const val TAG = "SocketClient"
        private const val CONNECT_TIMEOUT_MS = 5000
    }

    suspend fun connect(host: String, port: Int) = withContext(Dispatchers.IO) {
        disconnect()
        _connectionState.value = ConnectionState.Connecting

        try {
            val sock = Socket()
            sock.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            socket = sock

            // LenientObjectInputStream ignores serialVersionUID mismatches for our API types.
            // The server (Java 25) and this app (Java 17) compute different UIDs for the
            // same record definitions due to bytecode differences between JVM versions.
            // ObjectInputFilter is also not available on Android, but we trust the server
            // since the user explicitly configures the connection.
            val ois = LenientObjectInputStream(sock.getInputStream())

            _connectionState.value = ConnectionState.Connected

            while (coroutineContext.isActive) {
                val msg = ois.readObject()
                when {
                    msg is Array<*> && msg.javaClass.componentType == Group::class.java -> {
                        @Suppress("UNCHECKED_CAST")
                        val groups = msg as Array<Group>
                        _groups.value = groups.toList()
                    }
                    msg is Array<*> && msg.javaClass.componentType == ByteArray::class.java -> {
                        @Suppress("UNCHECKED_CAST")
                        val bytes = msg as Array<ByteArray>
                        val readingMap = mutableMapOf<Pair<String, String>, Reading>()
                        for (b in bytes) {
                            val reading = Reading.fromByteArray(b)
                            readingMap[reading.deviceId() to reading.sensorId()] = reading
                        }
                        _readings.value = readingMap
                    }
                    msg is Array<*> && msg.javaClass.componentType == Reading::class.java -> {
                        // Legacy protocol: Reading[] instead of byte[][]
                        @Suppress("UNCHECKED_CAST")
                        val readings = msg as Array<Reading>
                        val readingMap = mutableMapOf<Pair<String, String>, Reading>()
                        for (r in readings) {
                            readingMap[r.deviceId() to r.sensorId()] = r
                        }
                        _readings.value = readingMap
                    }
                }
            }
        } catch (e: EOFException) {
            Log.w(TAG, "Server disconnected (EOF)")
            _connectionState.value = ConnectionState.Error("Server disconnected")
        } catch (e: InvalidClassException) {
            Log.e(TAG, "Serialization mismatch — API record definitions may differ from server", e)
            _connectionState.value = ConnectionState.Error("Protocol version mismatch: ${e.message}")
        } catch (e: IOException) {
            val message = e.message ?: "Connection failed"
            Log.e(TAG, "IOException: $message", e)
            _connectionState.value = when {
                _connectionState.value is ConnectionState.Connecting -> ConnectionState.Error("Could not connect: $message")
                else -> ConnectionState.Error("Connection lost: $message")
            }
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "Class not found during deserialization", e)
            _connectionState.value = ConnectionState.Error("Protocol error: ${e.message}")
        } finally {
            closeSocket()
        }
    }

    fun disconnect() {
        closeSocket()
        _connectionState.value = ConnectionState.Disconnected
        _groups.value = emptyList()
        _readings.value = emptyMap()
    }

    private fun closeSocket() {
        try {
            socket?.close()
        } catch (_: IOException) {
            // ignore
        }
        socket = null
    }
}
