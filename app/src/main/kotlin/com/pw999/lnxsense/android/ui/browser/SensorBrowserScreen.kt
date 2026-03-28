package com.pw999.lnxsense.android.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pw999.lnxsense.android.data.SensorConfigStore
import com.pw999.lnxsense.android.data.SensorRepository
import com.pw999.lnxsense.android.data.model.ConnectionState
import com.pw999.lnxsense.android.ui.theme.ThemeMode
import com.pw999.lnxsense.android.ui.theme.StatusCritical
import com.pw999.lnxsense.android.ui.theme.StatusError
import com.pw999.lnxsense.android.ui.theme.StatusHigh
import com.pw999.lnxsense.android.ui.theme.StatusOk
import com.pw999.lnxsense.api.Device
import com.pw999.lnxsense.api.Reading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorBrowserScreen(
    repository: SensorRepository,
    configStore: SensorConfigStore,
    onConfigureDials: () -> Unit,
    onViewDials: () -> Unit,
    onDisconnect: () -> Unit
) {
    val viewModel = remember { SensorBrowserViewModel(repository) }
    val groups by viewModel.groups.collectAsState()
    val readings by viewModel.readings.collectAsState()
    val connectionState by repository.connectionState.collectAsState()
    val expandedDevices = remember { mutableStateMapOf<String, Boolean>() }
    val currentTheme by configStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }

    // Navigate to connection screen only on errors (not transient disconnects from backgrounding,
    // since the lifecycle observer will auto-reconnect on foreground return)
    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.Error) {
            onDisconnect()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LnxSense") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onConfigureDials) {
                        Icon(Icons.Default.Settings, contentDescription = "Configure Dials")
                    }
                    IconButton(onClick = onViewDials) {
                        Icon(Icons.Default.Dashboard, contentDescription = "View Dials")
                    }
                    IconButton(onClick = onDisconnect) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Disconnect")
                    }
                    // Overflow menu for theme selection
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        for (mode in ThemeMode.entries) {
                            DropdownMenuItem(
                                text = { Text(ThemeMode.label(mode)) },
                                leadingIcon = {
                                    RadioButton(
                                        selected = currentTheme == mode,
                                        onClick = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    scope.launch { configStore.saveThemeMode(mode) }
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            for (group in groups) {
                // Group header
                item(key = "group_${group.groupType().name}") {
                    GroupHeader(group.groupType().label)
                }

                // Devices within group
                for (device in group.devices()) {
                    val isExpanded = expandedDevices[device.id()] ?: device.expandedByDefault()

                    item(key = "device_${device.id()}") {
                        DeviceRow(
                            device = device,
                            isExpanded = isExpanded,
                            summaryText = if (!isExpanded) computeSummary(device, readings) else null,
                            onToggle = {
                                expandedDevices[device.id()] = !isExpanded
                            }
                        )
                    }

                    // Sensors within device
                    if (isExpanded) {
                        items(
                            items = device.sensors().toList(),
                            key = { "sensor_${device.id()}_${it.id()}" }
                        ) { sensor ->
                            val reading = readings[device.id() to sensor.id()]
                            SensorRow(
                                sensorName = sensor.name(),
                                reading = reading,
                                format = sensor.format() ?: "%,.1f",
                                unit = sensor.unit() ?: ""
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Computes the summary value for a collapsed device row, mirroring the JavaFX UI logic.
 *
 * - OF_SENSOR: use the reading of the device's designated summary sensor
 * - MIN/MAX/AVG: compute across all sensor readings for this device
 * - NONE: no summary shown
 */
internal fun computeSummary(
    device: Device,
    readings: Map<Pair<String, String>, Reading>
): String? {
    val summaryType = device.summaryValue()
    if (summaryType == Device.SummaryType.NONE) return null

    val format = device.format() ?: "%,.1f"
    val unit = device.unit() ?: ""

    when (summaryType) {
        Device.SummaryType.OF_SENSOR -> {
            val sensorId = device.summarySensorId() ?: return null
            val reading = readings[device.id() to sensorId] ?: return null
            // OF_SENSOR devices have null format/unit — use the summary sensor's own
            val sensor = device.sensors().firstOrNull { it.id() == sensorId }
            val sensorFormat = sensor?.format() ?: format
            val sensorUnit = sensor?.unit() ?: unit
            return formatReading(reading.value(), sensorFormat, sensorUnit)
        }
        Device.SummaryType.MIN -> {
            val values = deviceReadingValues(device, readings)
            if (values.isEmpty()) return null
            return formatReading(values.min(), format, unit)
        }
        Device.SummaryType.MAX -> {
            val values = deviceReadingValues(device, readings)
            if (values.isEmpty()) return null
            return formatReading(values.max(), format, unit)
        }
        Device.SummaryType.AVG -> {
            val values = deviceReadingValues(device, readings)
            if (values.isEmpty()) return null
            return formatReading(values.average(), format, unit)
        }
        else -> return null
    }
}

internal fun deviceReadingValues(
    device: Device,
    readings: Map<Pair<String, String>, Reading>
): List<Double> =
    device.sensors()
        .mapNotNull { sensor -> readings[device.id() to sensor.id()] }
        .filter { it.status() != Reading.Status.ERROR && it.status() != Reading.Status.DISABLED }
        .map { it.value() }

internal fun formatReading(value: Double, format: String, unit: String): String {
    val formatted = try {
        String.format(format, value)
    } catch (_: Exception) {
        value.toString()
    }
    return "$formatted $unit".trim()
}

@Composable
private fun GroupHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
    HorizontalDivider()
}

@Composable
private fun DeviceRow(
    device: Device,
    isExpanded: Boolean,
    summaryText: String?,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = device.name(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (summaryText != null) {
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SensorRow(
    sensorName: String,
    reading: Reading?,
    format: String,
    unit: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = sensorName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (reading != null) {
            val formattedValue = try {
                String.format(format, reading.value())
            } catch (_: Exception) {
                reading.value().toString()
            }
            Text(
                text = "$formattedValue $unit".trim(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = statusColor(reading.status())
            )
        } else {
            Text(
                text = "--",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun statusColor(status: Reading.Status) = when (status) {
    Reading.Status.OK -> StatusOk
    Reading.Status.HIGH -> StatusHigh
    Reading.Status.CRITICAL -> StatusCritical
    Reading.Status.ERROR -> StatusError
    Reading.Status.LOW -> MaterialTheme.colorScheme.tertiary
    Reading.Status.DISABLED -> StatusError
}
