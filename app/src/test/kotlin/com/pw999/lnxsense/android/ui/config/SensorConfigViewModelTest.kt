package com.pw999.lnxsense.android.ui.config

import app.cash.turbine.test
import com.pw999.lnxsense.android.data.SensorConfigStore
import com.pw999.lnxsense.android.data.SensorRepository
import com.pw999.lnxsense.android.data.model.DialConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class SensorConfigViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: SensorRepository
    private lateinit var configStore: SensorConfigStore
    private lateinit var viewModel: SensorConfigViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        configStore = mockk(relaxed = true)
        every { configStore.dialRows } returns flowOf(3)
        every { configStore.dialColumns } returns flowOf(2)
        every { configStore.dialConfigs(any()) } returns flowOf(List(6) { null })
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SensorConfigViewModel {
        viewModel = SensorConfigViewModel(repository, configStore)
        return viewModel
    }

    // --- Init: loads config from store ---

    @Test
    fun `init loads rows and columns from config store`() {
        val vm = createViewModel()
        assertEquals("3", vm.rowsText.value)
        assertEquals("2", vm.columnsText.value)
    }

    @Test
    fun `init creates correct number of empty slots`() {
        val vm = createViewModel()
        assertEquals(6, vm.dialConfigs.value.size)
        assertTrue(vm.dialConfigs.value.all { it == null })
    }

    @Test
    fun `init loads existing dial configs`() {
        val config = DialConfig(0, "dev1", "sen1", "Dev", "Sen", "°C", "%.1f", 0.0, 100.0)
        every { configStore.dialConfigs(6) } returns flowOf(listOf(config, null, null, null, null, null))

        val vm = createViewModel()
        assertEquals(config, vm.dialConfigs.value[0])
        assertNull(vm.dialConfigs.value[1])
    }

    // --- Input filtering ---

    @Test
    fun `onRowsChanged filters non-digit characters`() {
        val vm = createViewModel()
        vm.onRowsChanged("2a3b")
        assertEquals("23", vm.rowsText.value)
    }

    @Test
    fun `onRowsChanged accepts empty string for mid-edit`() {
        val vm = createViewModel()
        vm.onRowsChanged("")
        assertEquals("", vm.rowsText.value)
    }

    @Test
    fun `onColumnsChanged filters non-digit characters`() {
        val vm = createViewModel()
        vm.onColumnsChanged("x4y")
        assertEquals("4", vm.columnsText.value)
    }

    // --- Grid resizing ---

    @Test
    fun `increasing grid size adds null slots`() {
        val vm = createViewModel()
        assertEquals(6, vm.dialConfigs.value.size) // 3*2

        vm.onRowsChanged("4") // 4*2 = 8
        assertEquals(8, vm.dialConfigs.value.size)
        // New slots should be null
        assertNull(vm.dialConfigs.value[6])
        assertNull(vm.dialConfigs.value[7])
    }

    @Test
    fun `decreasing grid size truncates slots`() {
        val vm = createViewModel()
        assertEquals(6, vm.dialConfigs.value.size) // 3*2

        vm.onRowsChanged("2") // 2*2 = 4
        assertEquals(4, vm.dialConfigs.value.size)
    }

    @Test
    fun `grid size clamps to 1 minimum when empty input`() {
        val vm = createViewModel()
        vm.onRowsChanged("")    // parsedRows falls back to DEFAULT_ROWS
        vm.onColumnsChanged("") // parsedColumns falls back to DEFAULT_COLUMNS
        // Default is 3*2 = 6
        assertEquals(6, vm.dialConfigs.value.size)
    }

    @Test
    fun `grid size clamps to max 6`() {
        val vm = createViewModel()
        vm.onRowsChanged("9")    // coerced to 6
        vm.onColumnsChanged("9") // coerced to 6
        assertEquals(36, vm.dialConfigs.value.size) // 6*6
    }

    @Test
    fun `resizing preserves existing configs`() {
        val config = DialConfig(0, "dev1", "sen1", "Dev", "Sen", "°C", "%.1f", 0.0, 100.0)
        every { configStore.dialConfigs(6) } returns flowOf(listOf(config, null, null, null, null, null))
        val vm = createViewModel()

        vm.onRowsChanged("4") // expand to 8
        assertEquals(config, vm.dialConfigs.value[0])
    }

    // --- Sensor selection ---

    @Test
    fun `onSensorSelected populates slot with correct config`() {
        val vm = createViewModel()
        vm.onSensorSelected(2, "dev1", "temp1", "CPU", "Temperature", "°C", "%.1f")

        val config = vm.dialConfigs.value[2]
        assertNotNull(config)
        assertEquals("dev1", config!!.deviceId)
        assertEquals("temp1", config.sensorId)
        assertEquals("CPU", config.deviceName)
        assertEquals("Temperature", config.sensorName)
        assertEquals("°C", config.unit)
        assertEquals(0.0, config.minValue, 0.0)
        assertEquals(100.0, config.maxValue, 0.0) // guessDefaultMax for °C
    }

    @Test
    fun `onSensorSelected dismisses picker`() {
        val vm = createViewModel()
        vm.onSlotTapped(2)
        assertEquals(2, vm.activeSlot.value)

        vm.onSensorSelected(2, "dev1", "temp1", "CPU", "Temperature", "°C", "%.1f")
        assertNull(vm.activeSlot.value)
    }

    // --- guessDefaultMax via onSensorSelected ---

    @Test
    fun `guessDefaultMax for celsius returns 100`() {
        val vm = createViewModel()
        vm.onSensorSelected(0, "d", "s", "D", "S", "°C", "%.1f")
        assertEquals(100.0, vm.dialConfigs.value[0]!!.maxValue, 0.0)
    }

    @Test
    fun `guessDefaultMax for fahrenheit returns 212`() {
        val vm = createViewModel()
        vm.onSensorSelected(0, "d", "s", "D", "S", "°F", "%.1f")
        assertEquals(212.0, vm.dialConfigs.value[0]!!.maxValue, 0.0)
    }

    @Test
    fun `guessDefaultMax for MHz returns 5000`() {
        val vm = createViewModel()
        vm.onSensorSelected(0, "d", "s", "D", "S", "MHz", "%.0f")
        assertEquals(5000.0, vm.dialConfigs.value[0]!!.maxValue, 0.0)
    }

    @Test
    fun `guessDefaultMax for GHz returns 6`() {
        val vm = createViewModel()
        vm.onSensorSelected(0, "d", "s", "D", "S", "GHz", "%.2f")
        assertEquals(6.0, vm.dialConfigs.value[0]!!.maxValue, 0.0)
    }

    @Test
    fun `guessDefaultMax for RPM returns 3000`() {
        val vm = createViewModel()
        vm.onSensorSelected(0, "d", "s", "D", "S", "RPM", "%d")
        assertEquals(3000.0, vm.dialConfigs.value[0]!!.maxValue, 0.0)
    }

    @Test
    fun `guessDefaultMax for watts returns 150`() {
        val vm = createViewModel()
        vm.onSensorSelected(0, "d", "s", "D", "S", "W", "%.1f")
        assertEquals(150.0, vm.dialConfigs.value[0]!!.maxValue, 0.0)
    }

    @Test
    fun `guessDefaultMax for volts returns 15`() {
        val vm = createViewModel()
        vm.onSensorSelected(0, "d", "s", "D", "S", "V", "%.2f")
        assertEquals(15.0, vm.dialConfigs.value[0]!!.maxValue, 0.0)
    }

    @Test
    fun `guessDefaultMax for percent returns 100`() {
        val vm = createViewModel()
        vm.onSensorSelected(0, "d", "s", "D", "S", "%", "%.0f")
        assertEquals(100.0, vm.dialConfigs.value[0]!!.maxValue, 0.0)
    }

    @Test
    fun `guessDefaultMax for MB returns 32768`() {
        val vm = createViewModel()
        vm.onSensorSelected(0, "d", "s", "D", "S", "MB", "%.0f")
        assertEquals(32768.0, vm.dialConfigs.value[0]!!.maxValue, 0.0)
    }

    @Test
    fun `guessDefaultMax for GB returns 128`() {
        val vm = createViewModel()
        vm.onSensorSelected(0, "d", "s", "D", "S", "GB", "%.1f")
        assertEquals(128.0, vm.dialConfigs.value[0]!!.maxValue, 0.0)
    }

    @Test
    fun `guessDefaultMax for unknown unit defaults to 100`() {
        val vm = createViewModel()
        vm.onSensorSelected(0, "d", "s", "D", "S", "parsecs", "%.1f")
        assertEquals(100.0, vm.dialConfigs.value[0]!!.maxValue, 0.0)
    }

    // --- Min/Max editing ---

    @Test
    fun `onMinChanged updates existing config`() {
        val vm = createViewModel()
        vm.onSensorSelected(0, "d", "s", "D", "S", "°C", "%.1f")

        vm.onMinChanged(0, "10.5")
        assertEquals(10.5, vm.dialConfigs.value[0]!!.minValue, 0.0)
    }

    @Test
    fun `onMaxChanged updates existing config`() {
        val vm = createViewModel()
        vm.onSensorSelected(0, "d", "s", "D", "S", "°C", "%.1f")

        vm.onMaxChanged(0, "200")
        assertEquals(200.0, vm.dialConfigs.value[0]!!.maxValue, 0.0)
    }

    @Test
    fun `onMinChanged with invalid input keeps original value`() {
        val vm = createViewModel()
        vm.onSensorSelected(0, "d", "s", "D", "S", "°C", "%.1f")

        vm.onMinChanged(0, "abc")
        assertEquals(0.0, vm.dialConfigs.value[0]!!.minValue, 0.0) // unchanged
    }

    @Test
    fun `onMaxChanged with invalid input keeps original value`() {
        val vm = createViewModel()
        vm.onSensorSelected(0, "d", "s", "D", "S", "°C", "%.1f")

        vm.onMaxChanged(0, "")
        assertEquals(100.0, vm.dialConfigs.value[0]!!.maxValue, 0.0) // unchanged
    }

    @Test
    fun `onMinChanged on null slot does nothing`() {
        val vm = createViewModel()
        vm.onMinChanged(0, "10") // slot 0 is null
        assertNull(vm.dialConfigs.value[0])
    }

    // --- Clear slot ---

    @Test
    fun `onClearSlot removes config`() {
        val vm = createViewModel()
        vm.onSensorSelected(0, "d", "s", "D", "S", "°C", "%.1f")
        assertNotNull(vm.dialConfigs.value[0])

        vm.onClearSlot(0)
        assertNull(vm.dialConfigs.value[0])
    }

    // --- Slot tapping ---

    @Test
    fun `onSlotTapped sets active slot`() {
        val vm = createViewModel()
        assertNull(vm.activeSlot.value)

        vm.onSlotTapped(3)
        assertEquals(3, vm.activeSlot.value)
    }

    @Test
    fun `dismissPicker clears active slot`() {
        val vm = createViewModel()
        vm.onSlotTapped(3)
        vm.dismissPicker()
        assertNull(vm.activeSlot.value)
    }

    // --- Used sensors tracking ---

    @Test
    fun `usedSensors excludes active slot`() = runTest {
        val vm = createViewModel()
        vm.onSensorSelected(0, "dev1", "sen1", "D1", "S1", "°C", "%.1f")
        vm.onSensorSelected(1, "dev2", "sen2", "D2", "S2", "V", "%.2f")

        vm.usedSensors.test {
            // Without active slot, both are used
            val allUsed = awaitItem()
            assertTrue(allUsed.contains("dev1" to "sen1"))
            assertTrue(allUsed.contains("dev2" to "sen2"))

            // With slot 0 active, its sensor should be excluded
            vm.onSlotTapped(0)
            val usedWhileEditing = awaitItem()
            assertFalse(usedWhileEditing.contains("dev1" to "sen1"))
            assertTrue(usedWhileEditing.contains("dev2" to "sen2"))
        }
    }

    // --- Save ---

    @Test
    fun `save delegates to config store`() = runTest {
        coEvery { configStore.saveGridSize(any(), any()) } returns Unit
        coEvery { configStore.saveAllDialConfigs(any()) } returns Unit

        val vm = createViewModel()
        var saved = false
        vm.save { saved = true }
        testScheduler.advanceUntilIdle()

        assertTrue(saved)
        coVerify { configStore.saveGridSize(3, 2) }
        coVerify { configStore.saveAllDialConfigs(any()) }
    }
}
