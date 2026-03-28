package com.pw999.lnxsense.android.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pw999.lnxsense.android.data.SensorConfigStore
import com.pw999.lnxsense.android.data.SensorRepository
import com.pw999.lnxsense.android.data.model.DialConfig
import com.pw999.lnxsense.api.Group
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SensorConfigViewModel(
    repository: SensorRepository,
    private val configStore: SensorConfigStore
) : ViewModel() {

    val groups: StateFlow<List<Group>> = repository.groups

    // String state so the text field can show intermediate values like "" while editing
    private val _rowsText = MutableStateFlow(SensorConfigStore.DEFAULT_ROWS.toString())
    val rowsText: StateFlow<String> = _rowsText.asStateFlow()

    private val _columnsText = MutableStateFlow(SensorConfigStore.DEFAULT_COLUMNS.toString())
    val columnsText: StateFlow<String> = _columnsText.asStateFlow()

    private val parsedRows: Int get() = _rowsText.value.toIntOrNull()?.coerceIn(1, 6) ?: SensorConfigStore.DEFAULT_ROWS
    private val parsedColumns: Int get() = _columnsText.value.toIntOrNull()?.coerceIn(1, 6) ?: SensorConfigStore.DEFAULT_COLUMNS

    private val _dialConfigs = MutableStateFlow<List<DialConfig?>>(emptyList())
    val dialConfigs: StateFlow<List<DialConfig?>> = _dialConfigs.asStateFlow()

    private val _activeSlot = MutableStateFlow<Int?>(null)
    val activeSlot: StateFlow<Int?> = _activeSlot.asStateFlow()

    // Set of (deviceId, sensorId) pairs already assigned to a slot
    val usedSensors: StateFlow<Set<Pair<String, String>>> = _dialConfigs
        .combine(_activeSlot) { configs, activeSlot ->
            configs.filterNotNull()
                // Don't gray out the sensor assigned to the slot being edited
                .filter { it.slotIndex != activeSlot }
                .map { it.deviceId to it.sensorId }
                .toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        viewModelScope.launch {
            val rows = configStore.dialRows.first()
            val cols = configStore.dialColumns.first()
            _rowsText.value = rows.toString()
            _columnsText.value = cols.toString()
            _dialConfigs.value = configStore.dialConfigs(rows * cols).first()
        }
    }

    fun onRowsChanged(value: String) {
        // Accept only digits, allow empty for mid-edit state
        _rowsText.value = value.filter { it.isDigit() }
        resizeSlots()
    }

    fun onColumnsChanged(value: String) {
        _columnsText.value = value.filter { it.isDigit() }
        resizeSlots()
    }

    private fun resizeSlots() {
        val newCount = parsedRows * parsedColumns
        val current = _dialConfigs.value
        _dialConfigs.value = if (newCount <= current.size) {
            current.take(newCount)
        } else {
            current + List(newCount - current.size) { null }
        }
    }

    fun onSlotTapped(slotIndex: Int) {
        _activeSlot.value = slotIndex
    }

    fun dismissPicker() {
        _activeSlot.value = null
    }

    fun onSensorSelected(
        slotIndex: Int,
        deviceId: String,
        sensorId: String,
        deviceName: String,
        sensorName: String,
        unit: String,
        format: String
    ) {
        val current = _dialConfigs.value.toMutableList()
        current[slotIndex] = DialConfig(
            slotIndex = slotIndex,
            deviceId = deviceId,
            sensorId = sensorId,
            deviceName = deviceName,
            sensorName = sensorName,
            unit = unit,
            format = format,
            minValue = 0.0,
            maxValue = guessDefaultMax(unit)
        )
        _dialConfigs.value = current
        _activeSlot.value = null
    }

    fun onMinChanged(slotIndex: Int, value: String) {
        updateConfig(slotIndex) { it.copy(minValue = value.toDoubleOrNull() ?: it.minValue) }
    }

    fun onMaxChanged(slotIndex: Int, value: String) {
        updateConfig(slotIndex) { it.copy(maxValue = value.toDoubleOrNull() ?: it.maxValue) }
    }

    fun onClearSlot(slotIndex: Int) {
        val current = _dialConfigs.value.toMutableList()
        current[slotIndex] = null
        _dialConfigs.value = current
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            // Coerce to valid range on save
            configStore.saveGridSize(parsedRows, parsedColumns)
            configStore.saveAllDialConfigs(_dialConfigs.value)
            onSaved()
        }
    }

    private fun updateConfig(slotIndex: Int, transform: (DialConfig) -> DialConfig) {
        val current = _dialConfigs.value.toMutableList()
        current[slotIndex]?.let { current[slotIndex] = transform(it) }
        _dialConfigs.value = current
    }

    private fun guessDefaultMax(unit: String): Double = when {
        unit.contains("°C", ignoreCase = true) || unit.contains("C") -> 100.0
        unit.contains("°F", ignoreCase = true) -> 212.0
        unit.contains("MHz", ignoreCase = true) -> 5000.0
        unit.contains("GHz", ignoreCase = true) -> 6.0
        unit.contains("RPM", ignoreCase = true) -> 3000.0
        unit.contains("W", ignoreCase = true) -> 150.0
        unit.contains("V", ignoreCase = true) -> 15.0
        unit.contains("%") -> 100.0
        unit.contains("MB") -> 32768.0
        unit.contains("GB") -> 128.0
        else -> 100.0
    }
}
