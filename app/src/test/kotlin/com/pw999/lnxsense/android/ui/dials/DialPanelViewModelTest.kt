package com.pw999.lnxsense.android.ui.dials

import app.cash.turbine.test
import com.pw999.lnxsense.android.data.SensorConfigStore
import com.pw999.lnxsense.android.data.SensorRepository
import com.pw999.lnxsense.android.data.model.DialConfig
import com.pw999.lnxsense.api.Reading
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DialPanelViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: SensorRepository
    private lateinit var configStore: SensorConfigStore
    private val readingsFlow = MutableStateFlow<Map<Pair<String, String>, Reading>>(emptyMap())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        configStore = mockk(relaxed = true)
        every { repository.readings } returns readingsFlow
        every { configStore.dialRows } returns flowOf(2)
        every { configStore.dialColumns } returns flowOf(2)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads columns from config store`() {
        every { configStore.dialConfigs(4) } returns flowOf(List(4) { null })

        val vm = DialPanelViewModel(repository, configStore)
        assertEquals(2, vm.columns.value)
    }

    @Test
    fun `dialStates has correct size from config`() = runTest {
        every { configStore.dialConfigs(4) } returns flowOf(List(4) { null })

        val vm = DialPanelViewModel(repository, configStore)
        vm.dialStates.test {
            val initial = awaitItem()
            if (initial.isEmpty()) {
                // WhileSubscribed may emit empty first
                val populated = awaitItem()
                assertEquals(4, populated.size)
            } else {
                assertEquals(4, initial.size)
            }
        }
    }

    @Test
    fun `dialStates matches readings to configs`() = runTest {
        val config = DialConfig(0, "dev1", "temp1", "CPU", "Temp", "°C", "%.1f", 0.0, 100.0)
        every { configStore.dialConfigs(4) } returns flowOf(listOf(config, null, null, null))

        val vm = DialPanelViewModel(repository, configStore)
        vm.dialStates.test {
            // Skip until we get the populated list
            var states = awaitItem()
            if (states.isEmpty()) states = awaitItem()

            assertEquals(config, states[0].config)
            assertNull(states[0].reading) // no reading yet
            assertNull(states[1].config)  // null config slot

            // Add a reading
            readingsFlow.value = mapOf(
                ("dev1" to "temp1") to Reading("dev1", "temp1", 72.5, Reading.Status.OK)
            )
            val updated = awaitItem()
            assertNotNull(updated[0].reading)
            assertEquals(72.5, updated[0].reading!!.value(), 0.0)
        }
    }

    @Test
    fun `dialStates updates when readings change`() = runTest {
        val config = DialConfig(0, "dev1", "temp1", "CPU", "Temp", "°C", "%.1f", 0.0, 100.0)
        every { configStore.dialConfigs(4) } returns flowOf(listOf(config, null, null, null))

        val vm = DialPanelViewModel(repository, configStore)
        vm.dialStates.test {
            // Skip to populated state
            var states = awaitItem()
            if (states.isEmpty()) states = awaitItem()

            readingsFlow.value = mapOf(
                ("dev1" to "temp1") to Reading("dev1", "temp1", 50.0, Reading.Status.OK)
            )
            val first = awaitItem()
            assertEquals(50.0, first[0].reading!!.value(), 0.0)

            readingsFlow.value = mapOf(
                ("dev1" to "temp1") to Reading("dev1", "temp1", 85.0, Reading.Status.HIGH)
            )
            val second = awaitItem()
            assertEquals(85.0, second[0].reading!!.value(), 0.0)
            assertEquals(Reading.Status.HIGH, second[0].reading!!.status())
        }
    }
}
