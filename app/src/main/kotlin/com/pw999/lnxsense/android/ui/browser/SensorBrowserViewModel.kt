package com.pw999.lnxsense.android.ui.browser

import androidx.lifecycle.ViewModel
import com.pw999.lnxsense.android.data.SensorRepository
import com.pw999.lnxsense.api.Group
import com.pw999.lnxsense.api.Reading
import kotlinx.coroutines.flow.StateFlow

class SensorBrowserViewModel(repository: SensorRepository) : ViewModel() {
    val groups: StateFlow<List<Group>> = repository.groups
    val readings: StateFlow<Map<Pair<String, String>, Reading>> = repository.readings
}
