package com.pw999.lnxsense.android.ui.connection

import com.pw999.lnxsense.android.data.SensorConfigStore
import com.pw999.lnxsense.android.data.SensorRepository
import com.pw999.lnxsense.android.data.model.ConnectionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class ConnectionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: SensorRepository
    private lateinit var configStore: SensorConfigStore

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        configStore = mockk(relaxed = true)
        every { repository.connectionState } returns MutableStateFlow(ConnectionState.Disconnected)
        every { configStore.connectionHost } returns flowOf("")
        every { configStore.connectionPort } returns flowOf(9999)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ConnectionViewModel(repository, configStore)

    // --- Init: loads saved config ---

    @Test
    fun `init loads saved host and port`() {
        every { configStore.connectionHost } returns flowOf("192.168.1.100")
        every { configStore.connectionPort } returns flowOf(8080)

        val vm = createViewModel()
        assertEquals("192.168.1.100", vm.host.value)
        assertEquals("8080", vm.port.value)
    }

    @Test
    fun `init with empty host keeps default empty`() {
        every { configStore.connectionHost } returns flowOf("")
        val vm = createViewModel()
        assertEquals("", vm.host.value)
    }

    @Test
    fun `init defaults port to 9999`() {
        val vm = createViewModel()
        assertEquals("9999", vm.port.value)
    }

    // --- Input handling ---

    @Test
    fun `onHostChanged stores value as-is`() {
        val vm = createViewModel()
        vm.onHostChanged("my-server.local")
        assertEquals("my-server.local", vm.host.value)
    }

    @Test
    fun `onPortChanged filters non-digit characters`() {
        val vm = createViewModel()
        vm.onPortChanged("80ab80")
        assertEquals("8080", vm.port.value)
    }

    @Test
    fun `onPortChanged accepts empty string`() {
        val vm = createViewModel()
        vm.onPortChanged("")
        assertEquals("", vm.port.value)
    }

    @Test
    fun `onPortChanged accepts valid port`() {
        val vm = createViewModel()
        vm.onPortChanged("12345")
        assertEquals("12345", vm.port.value)
    }

    // --- Connect validation ---

    @Test
    fun `connect with valid host and port triggers repository`() {
        coEvery { configStore.saveConnection(any(), any()) } returns Unit
        val vm = createViewModel()
        vm.onHostChanged("192.168.1.1")
        vm.onPortChanged("9999")

        vm.connect()

        verify { repository.connect("192.168.1.1", 9999) }
    }

    @Test
    fun `connect trims whitespace from host`() {
        coEvery { configStore.saveConnection(any(), any()) } returns Unit
        val vm = createViewModel()
        vm.onHostChanged("  myhost  ")
        vm.onPortChanged("9999")

        vm.connect()

        verify { repository.connect("myhost", 9999) }
    }

    @Test
    fun `connect with empty host does not trigger repository`() {
        val vm = createViewModel()
        vm.onHostChanged("")
        vm.onPortChanged("9999")

        vm.connect()

        verify(exactly = 0) { repository.connect(any(), any()) }
    }

    @Test
    fun `connect with blank host does not trigger repository`() {
        val vm = createViewModel()
        vm.onHostChanged("   ")
        vm.onPortChanged("9999")

        vm.connect()

        verify(exactly = 0) { repository.connect(any(), any()) }
    }

    @Test
    fun `connect with invalid port does not trigger repository`() {
        val vm = createViewModel()
        vm.onHostChanged("myhost")
        vm.onPortChanged("")

        vm.connect()

        verify(exactly = 0) { repository.connect(any(), any()) }
    }

    @Test
    fun `connect saves connection to config store`() = runTest {
        coEvery { configStore.saveConnection(any(), any()) } returns Unit
        val vm = createViewModel()
        vm.onHostChanged("myhost")
        vm.onPortChanged("8080")

        vm.connect()
        testScheduler.advanceUntilIdle()

        coVerify { configStore.saveConnection("myhost", 8080) }
    }

    // --- Connection state exposed from repository ---

    @Test
    fun `connectionState reflects repository state`() {
        val stateFlow = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        every { repository.connectionState } returns stateFlow

        val vm = createViewModel()
        assertEquals(ConnectionState.Disconnected, vm.connectionState.value)

        stateFlow.value = ConnectionState.Connecting
        assertEquals(ConnectionState.Connecting, vm.connectionState.value)

        stateFlow.value = ConnectionState.Connected
        assertEquals(ConnectionState.Connected, vm.connectionState.value)

        stateFlow.value = ConnectionState.Error("timeout")
        assertTrue(vm.connectionState.value is ConnectionState.Error)
        assertEquals("timeout", (vm.connectionState.value as ConnectionState.Error).message)
    }
}
