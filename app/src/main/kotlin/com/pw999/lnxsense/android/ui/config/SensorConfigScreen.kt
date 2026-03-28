package com.pw999.lnxsense.android.ui.config

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pw999.lnxsense.android.data.SensorConfigStore
import com.pw999.lnxsense.android.data.SensorRepository
import com.pw999.lnxsense.android.data.model.DialConfig
import com.pw999.lnxsense.api.Group

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorConfigScreen(
    repository: SensorRepository,
    configStore: SensorConfigStore,
    onViewDials: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel = remember { SensorConfigViewModel(repository, configStore) }
    val dialConfigs by viewModel.dialConfigs.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val activeSlot by viewModel.activeSlot.collectAsState()
    val rowsText by viewModel.rowsText.collectAsState()
    val columnsText by viewModel.columnsText.collectAsState()
    val usedSensors by viewModel.usedSensors.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Dials") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Grid size configuration
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Grid layout",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rowsText,
                            onValueChange = viewModel::onRowsChanged,
                            label = { Text("Rows (1–6)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = columnsText,
                            onValueChange = viewModel::onColumnsChanged,
                            label = { Text("Columns (1–6)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${dialConfigs.size} dials total (columns swap in landscape)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Dial slot cards
                items(dialConfigs.size) { slot ->
                    DialSlotCard(
                        slotIndex = slot,
                        config = dialConfigs[slot],
                        onTap = { viewModel.onSlotTapped(slot) },
                        onMinChanged = { viewModel.onMinChanged(slot, it) },
                        onMaxChanged = { viewModel.onMaxChanged(slot, it) },
                        onClear = { viewModel.onClearSlot(slot) }
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            Button(
                onClick = { viewModel.save(onViewDials) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = dialConfigs.any { it != null }
            ) {
                Text("Save & View Dials")
            }
        }
    }

    // Sensor picker bottom sheet
    if (activeSlot != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissPicker() },
            sheetState = sheetState
        ) {
            SensorPicker(
                groups = groups,
                usedSensors = usedSensors,
                onSensorSelected = { deviceId, sensorId, deviceName, sensorName, unit, format ->
                    viewModel.onSensorSelected(
                        activeSlot!!, deviceId, sensorId, deviceName, sensorName, unit, format
                    )
                }
            )
        }
    }
}

@Composable
private fun DialSlotCard(
    slotIndex: Int,
    config: DialConfig?,
    onTap: () -> Unit,
    onMinChanged: (String) -> Unit,
    onMaxChanged: (String) -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (config != null)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dial ${slotIndex + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                if (config != null) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            }

            if (config != null) {
                Text(
                    text = "${config.deviceName} > ${config.sensorName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (config.unit.isNotEmpty()) {
                    Text(
                        text = "Unit: ${config.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = config.minValue.toBigDecimal().stripTrailingZeros().toPlainString(),
                        onValueChange = onMinChanged,
                        label = { Text("Min") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = config.maxValue.toBigDecimal().stripTrailingZeros().toPlainString(),
                        onValueChange = onMaxChanged,
                        label = { Text("Max") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to assign a sensor",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onTap)
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun SensorPicker(
    groups: List<Group>,
    usedSensors: Set<Pair<String, String>>,
    onSensorSelected: (deviceId: String, sensorId: String, deviceName: String, sensorName: String, unit: String, format: String) -> Unit
) {
    val expandedDevices = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Text(
                text = "Select a sensor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        for (group in groups) {
            item(key = "pick_group_${group.groupType().name}") {
                Text(
                    text = group.groupType().label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                HorizontalDivider()
            }

            for (device in group.devices()) {
                val isExpanded = expandedDevices[device.id()] ?: false

                item(key = "pick_device_${device.id()}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedDevices[device.id()] = !isExpanded }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = device.name(),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isExpanded) {
                    for (sensor in device.sensors()) {
                        val isUsed = (device.id() to sensor.id()) in usedSensors
                        item(key = "pick_sensor_${device.id()}_${sensor.id()}") {
                            val label = buildString {
                                append(sensor.name())
                                if (!sensor.unit().isNullOrEmpty()) {
                                    append(" (${sensor.unit()})")
                                }
                                if (isUsed) {
                                    append(" — already assigned")
                                }
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUsed)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isUsed) Modifier
                                        else Modifier.clickable {
                                            onSensorSelected(
                                                device.id(),
                                                sensor.id(),
                                                device.name(),
                                                sensor.name(),
                                                sensor.unit() ?: "",
                                                sensor.format() ?: "%,.1f"
                                            )
                                        }
                                    )
                                    .padding(start = 16.dp, top = 10.dp, bottom = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}
