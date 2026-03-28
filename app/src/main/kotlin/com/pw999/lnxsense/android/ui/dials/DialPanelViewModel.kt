package com.pw999.lnxsense.android.ui.dials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pw999.lnxsense.android.data.SensorConfigStore
import com.pw999.lnxsense.android.data.SensorRepository
import com.pw999.lnxsense.android.data.model.DialConfig
import com.pw999.lnxsense.api.Reading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DialState(
    val config: DialConfig?,
    val reading: Reading? = null
)

class DialPanelViewModel(
    private val repository: SensorRepository,
    configStore: SensorConfigStore
) : ViewModel() {

    private val _columns = MutableStateFlow(SensorConfigStore.DEFAULT_COLUMNS)
    val columns: StateFlow<Int> = _columns.asStateFlow()

    private val _dialConfigs = MutableStateFlow<List<DialConfig?>>(emptyList())

    val dialStates: StateFlow<List<DialState>> = combine(
        _dialConfigs,
        repository.readings
    ) { configs, readings ->
        configs.map { config ->
            if (config != null) {
                val reading = readings[config.deviceId to config.sensorId]
                DialState(config = config, reading = reading)
            } else {
                DialState(config = null)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val rows = configStore.dialRows.first()
            val cols = configStore.dialColumns.first()
            _columns.value = cols
            _dialConfigs.value = configStore.dialConfigs(rows * cols).first()
        }
    }
}
