package com.pw999.lnxsense.android.ui.dials

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.pw999.lnxsense.android.data.SensorConfigStore
import com.pw999.lnxsense.android.data.SensorRepository

@Composable
fun DialPanelScreen(
    repository: SensorRepository,
    configStore: SensorConfigStore,
    onBack: () -> Unit
) {
    val viewModel = remember { DialPanelViewModel(repository, configStore) }
    val dialStates by viewModel.dialStates.collectAsState()
    val configuredColumns by viewModel.columns.collectAsState()
    val configuration = LocalConfiguration.current
    // In landscape the configured rows become columns and vice versa,
    // so the grid fills the wider screen naturally
    val totalSlots = dialStates.size
    val columns = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        // landscape: use rows-worth of columns (total/configuredCols) to swap the grid
        (totalSlots / configuredColumns).coerceAtLeast(1)
    } else {
        configuredColumns
    }

    // No top bar — maximizes screen space for gauges.
    // Users navigate back with the system back gesture/button.
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(dialStates) { dialState ->
                if (dialState.config != null) {
                    GaugeComposable(
                        deviceName = dialState.config.deviceName,
                        sensorName = dialState.config.sensorName,
                        unit = dialState.config.unit,
                        value = dialState.reading?.value(),
                        minValue = dialState.config.minValue,
                        maxValue = dialState.config.maxValue,
                        format = dialState.config.format,
                        status = dialState.reading?.status()
                    )
                } else {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Not configured",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
}
