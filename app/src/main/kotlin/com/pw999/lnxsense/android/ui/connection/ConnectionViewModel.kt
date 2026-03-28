package com.pw999.lnxsense.android.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pw999.lnxsense.android.data.SensorConfigStore
import com.pw999.lnxsense.android.data.SensorRepository
import com.pw999.lnxsense.android.data.model.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConnectionViewModel(
    private val repository: SensorRepository,
    private val configStore: SensorConfigStore
) : ViewModel() {

    private val _host = MutableStateFlow("")
    val host: StateFlow<String> = _host.asStateFlow()

    private val _port = MutableStateFlow("9999")
    val port: StateFlow<String> = _port.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = repository.connectionState

    init {
        viewModelScope.launch {
            val savedHost = configStore.connectionHost.first()
            val savedPort = configStore.connectionPort.first()
            if (savedHost.isNotEmpty()) _host.value = savedHost
            _port.value = savedPort.toString()
        }
    }

    fun onHostChanged(value: String) {
        _host.value = value
    }

    fun onPortChanged(value: String) {
        _port.value = value.filter { it.isDigit() }
    }

    fun connect() {
        val host = _host.value.trim()
        val portNum = _port.value.toIntOrNull() ?: return
        if (host.isEmpty()) return

        viewModelScope.launch {
            configStore.saveConnection(host, portNum)
        }
        repository.connect(host, portNum)
    }
}
