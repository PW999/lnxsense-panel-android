package com.pw999.lnxsense.android.ui.browser

import com.pw999.lnxsense.api.Device
import com.pw999.lnxsense.api.Group
import com.pw999.lnxsense.api.Reading
import com.pw999.lnxsense.api.Sensor
import com.pw999.lnxsense.api.SortedSetUtil
import org.junit.Assert.*
import org.junit.Test

class SensorBrowserHelpersTest {

    // --- formatReading ---

    @Test
    fun `formatReading with valid format and unit`() {
        assertEquals("72.5 °C", formatReading(72.5, "%.1f", "°C"))
    }

    @Test
    fun `formatReading with empty unit`() {
        assertEquals("72.5", formatReading(72.5, "%.1f", ""))
    }

    @Test
    fun `formatReading with thousands separator`() {
        assertEquals("1,234.5 MHz", formatReading(1234.5, "%,.1f", "MHz"))
    }

    @Test
    fun `formatReading with integer format`() {
        assertEquals("42 RPM", formatReading(42.0, "%.0f", "RPM"))
    }

    @Test
    fun `formatReading with bad format falls back to toString`() {
        val result = formatReading(3.14, "%z", "")
        // Should not crash, falls back to value.toString()
        assertTrue(result.contains("3.14"))
    }

    @Test
    fun `formatReading with zero value`() {
        assertEquals("0.0 V", formatReading(0.0, "%.1f", "V"))
    }

    @Test
    fun `formatReading with negative value`() {
        assertEquals("-5.0 °C", formatReading(-5.0, "%.1f", "°C"))
    }

    // --- deviceReadingValues ---

    private fun makeDevice(vararg sensorIds: String): Device {
        val sensors = sensorIds.mapIndexed { idx, id ->
            Sensor(id, "Sensor $id", "°C", "%.1f", idx)
        }
        return Device(
            "dev1", "Device", "provider", Group.GroupType.CPU, "desc", 0,
            SortedSetUtil.of(sensors)
        )
    }

    private fun makeReadings(vararg pairs: Triple<String, Double, Reading.Status>): Map<Pair<String, String>, Reading> {
        return pairs.associate { (sensorId, value, status) ->
            ("dev1" to sensorId) to Reading("dev1", sensorId, value, status)
        }
    }

    @Test
    fun `deviceReadingValues returns values for OK sensors`() {
        val device = makeDevice("s1", "s2")
        val readings = makeReadings(
            Triple("s1", 50.0, Reading.Status.OK),
            Triple("s2", 60.0, Reading.Status.OK)
        )
        val values = deviceReadingValues(device, readings)
        assertEquals(listOf(50.0, 60.0), values)
    }

    @Test
    fun `deviceReadingValues filters out ERROR readings`() {
        val device = makeDevice("s1", "s2")
        val readings = makeReadings(
            Triple("s1", 50.0, Reading.Status.OK),
            Triple("s2", 0.0, Reading.Status.ERROR) // ERROR value is 0 from Reading constructor
        )
        val values = deviceReadingValues(device, readings)
        assertEquals(listOf(50.0), values)
    }

    @Test
    fun `deviceReadingValues filters out DISABLED readings`() {
        val device = makeDevice("s1", "s2")
        val readings = makeReadings(
            Triple("s1", 50.0, Reading.Status.OK),
            Triple("s2", 0.0, Reading.Status.DISABLED)
        )
        val values = deviceReadingValues(device, readings)
        assertEquals(listOf(50.0), values)
    }

    @Test
    fun `deviceReadingValues includes HIGH and CRITICAL`() {
        val device = makeDevice("s1", "s2", "s3")
        val readings = makeReadings(
            Triple("s1", 50.0, Reading.Status.OK),
            Triple("s2", 80.0, Reading.Status.HIGH),
            Triple("s3", 95.0, Reading.Status.CRITICAL)
        )
        val values = deviceReadingValues(device, readings)
        assertEquals(listOf(50.0, 80.0, 95.0), values)
    }

    @Test
    fun `deviceReadingValues returns empty for missing readings`() {
        val device = makeDevice("s1")
        val readings = emptyMap<Pair<String, String>, Reading>()
        val values = deviceReadingValues(device, readings)
        assertTrue(values.isEmpty())
    }

    // --- computeSummary ---

    @Test
    fun `computeSummary returns null for NONE type`() {
        val device = Device(
            "dev1", "Device", "provider", Group.GroupType.CPU, "desc", 0,
            SortedSetUtil.of(Sensor("s1", "S1", "°C", "%.1f", 0))
        )
        assertNull(computeSummary(device, emptyMap()))
    }

    @Test
    fun `computeSummary OF_SENSOR returns sensor reading`() {
        val sensor = Sensor("s1", "S1", "°C", "%.1f", 0)
        val device = Device(
            "dev1", "Device", "provider", Group.GroupType.CPU,
            "s1", "desc", 0,
            SortedSetUtil.of(sensor)
        )
        val readings = mapOf(
            ("dev1" to "s1") to Reading("dev1", "s1", 72.5, Reading.Status.OK)
        )
        assertEquals("72.5 °C", computeSummary(device, readings))
    }

    @Test
    fun `computeSummary OF_SENSOR returns null when no reading`() {
        val device = Device(
            "dev1", "Device", "provider", Group.GroupType.CPU,
            "s1", "desc", 0,
            SortedSetUtil.of(Sensor("s1", "S1", "°C", "%.1f", 0))
        )
        assertNull(computeSummary(device, emptyMap()))
    }

    @Test
    fun `computeSummary MIN returns minimum value`() {
        val device = Device(
            "dev1", "Device", "provider", Group.GroupType.CPU, true,
            Device.SummaryType.MIN, Device.SummaryType.NONE,
            Device.SummaryType.NONE, Device.SummaryType.NONE,
            null, "%.1f", "°C", null, 0,
            SortedSetUtil.of(
                Sensor("s1", "S1", "°C", "%.1f", 0),
                Sensor("s2", "S2", "°C", "%.1f", 1)
            )
        )
        val readings = mapOf(
            ("dev1" to "s1") to Reading("dev1", "s1", 50.0, Reading.Status.OK),
            ("dev1" to "s2") to Reading("dev1", "s2", 30.0, Reading.Status.OK)
        )
        assertEquals("30.0 °C", computeSummary(device, readings))
    }

    @Test
    fun `computeSummary MAX returns maximum value`() {
        val device = Device(
            "dev1", "Device", "provider", Group.GroupType.CPU, true,
            Device.SummaryType.MAX, Device.SummaryType.NONE,
            Device.SummaryType.NONE, Device.SummaryType.NONE,
            null, "%.1f", "°C", null, 0,
            SortedSetUtil.of(
                Sensor("s1", "S1", "°C", "%.1f", 0),
                Sensor("s2", "S2", "°C", "%.1f", 1)
            )
        )
        val readings = mapOf(
            ("dev1" to "s1") to Reading("dev1", "s1", 50.0, Reading.Status.OK),
            ("dev1" to "s2") to Reading("dev1", "s2", 80.0, Reading.Status.OK)
        )
        assertEquals("80.0 °C", computeSummary(device, readings))
    }

    @Test
    fun `computeSummary AVG returns average value`() {
        val device = Device(
            "dev1", "Device", "provider", Group.GroupType.CPU, true,
            Device.SummaryType.AVG, Device.SummaryType.NONE,
            Device.SummaryType.NONE, Device.SummaryType.NONE,
            null, "%.1f", "°C", null, 0,
            SortedSetUtil.of(
                Sensor("s1", "S1", "°C", "%.1f", 0),
                Sensor("s2", "S2", "°C", "%.1f", 1)
            )
        )
        val readings = mapOf(
            ("dev1" to "s1") to Reading("dev1", "s1", 40.0, Reading.Status.OK),
            ("dev1" to "s2") to Reading("dev1", "s2", 60.0, Reading.Status.OK)
        )
        assertEquals("50.0 °C", computeSummary(device, readings))
    }

    @Test
    fun `computeSummary returns null when no readings available`() {
        val device = Device(
            "dev1", "Device", "provider", Group.GroupType.CPU, true,
            Device.SummaryType.AVG, Device.SummaryType.NONE,
            Device.SummaryType.NONE, Device.SummaryType.NONE,
            null, "%.1f", "°C", null, 0,
            SortedSetUtil.of(Sensor("s1", "S1", "°C", "%.1f", 0))
        )
        assertNull(computeSummary(device, emptyMap()))
    }

    @Test
    fun `computeSummary OF_SENSOR uses sensor format not device format`() {
        val sensor = Sensor("s1", "S1", "RPM", "%,.0f", 0)
        val device = Device(
            "dev1", "Device", "provider", Group.GroupType.FAN,
            "s1", "desc", 0,
            SortedSetUtil.of(sensor)
        )
        val readings = mapOf(
            ("dev1" to "s1") to Reading("dev1", "s1", 1500.0, Reading.Status.OK)
        )
        assertEquals("1,500 RPM", computeSummary(device, readings))
    }
}
