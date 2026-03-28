package com.pw999.lnxsense.android.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pw999.lnxsense.android.data.SensorConfigStore
import com.pw999.lnxsense.android.data.SensorRepository
import com.pw999.lnxsense.android.data.model.ConnectionState

@Composable
fun ConnectionScreen(
    repository: SensorRepository,
    configStore: SensorConfigStore,
    onConnected: () -> Unit
) {
    val viewModel = remember { ConnectionViewModel(repository, configStore) }
    val host by viewModel.host.collectAsState()
    val port by viewModel.port.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.Connected) {
            onConnected()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "LnxSense",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connect to your server",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = host,
                    onValueChange = viewModel::onHostChanged,
                    label = { Text("Server IP") },
                    placeholder = { Text("192.168.1.x") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = viewModel::onPortChanged,
                    label = { Text("Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(100.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = connectionState) {
                is ConnectionState.Connecting -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Connecting...", style = MaterialTheme.typography.bodyMedium)
                }
                is ConnectionState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = viewModel::connect,
                        enabled = host.isNotBlank() && port.isNotBlank()
                    ) {
                        Text("Retry")
                    }
                }
                else -> {
                    Button(
                        onClick = viewModel::connect,
                        enabled = host.isNotBlank() && port.isNotBlank()
                    ) {
                        Text("Connect")
                    }
                }
            }
        }
    }
}
