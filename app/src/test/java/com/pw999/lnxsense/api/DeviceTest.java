package com.pw999.lnxsense.api;

import org.junit.Test;

import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.*;

public class DeviceTest {

    private static final SortedSet<Sensor> SENSORS = SortedSetUtil.of(
            new Sensor("s1", "Sensor 1", 0),
            new Sensor("s2", "Sensor 2", 1)
    );

    private static java.util.SortedSet<Sensor> sensors() {
        return new TreeSet<>(SENSORS);
    }

    // --- Convenience constructors ---

    @Test
    public void simpleConstructorSetsDefaults() {
        var device = new Device("dev1", "Device 1", "provider", Group.GroupType.CPU, "desc", 0, sensors());
        assertTrue(device.expandedByDefault());
        assertEquals(Device.SummaryType.NONE, device.summaryValue());
        assertNull(device.summarySensorId());
    }

    @Test
    public void constructorWithExpandedFlag() {
        var device = new Device("dev1", "Device 1", "provider", Group.GroupType.CPU, false, "desc", 0, sensors());
        assertFalse(device.expandedByDefault());
        assertEquals(Device.SummaryType.NONE, device.summaryValue());
    }

    @Test
    public void ofSensorConvenienceConstructor() {
        var device = new Device("dev1", "Device 1", "provider", Group.GroupType.GPU, "s1", "desc", 0, sensors());
        assertFalse(device.expandedByDefault());
        assertEquals(Device.SummaryType.OF_SENSOR, device.summaryValue());
        assertEquals(Device.SummaryType.OF_SENSOR, device.summaryMin());
        assertEquals(Device.SummaryType.OF_SENSOR, device.summaryMax());
        assertEquals(Device.SummaryType.OF_SENSOR, device.summaryAvg());
        assertEquals("s1", device.summarySensorId());
    }

    // --- Null validation ---

    @Test(expected = NullPointerException.class)
    public void nullIdThrows() {
        new Device(null, "name", "prov", Group.GroupType.CPU, "desc", 0, sensors());
    }

    @Test(expected = NullPointerException.class)
    public void nullNameThrows() {
        new Device("id", null, "prov", Group.GroupType.CPU, "desc", 0, sensors());
    }

    @Test(expected = NullPointerException.class)
    public void nullProviderThrows() {
        new Device("id", "name", null, Group.GroupType.CPU, "desc", 0, sensors());
    }

    @Test(expected = NullPointerException.class)
    public void nullGroupTypeThrows() {
        new Device("id", "name", "prov", null, "desc", 0, sensors());
    }

    @Test(expected = NullPointerException.class)
    public void nullSensorsThrows() {
        new Device("id", "name", "prov", Group.GroupType.CPU, "desc", 0, null);
    }

    // --- Conditional validation: OF_SENSOR requires summarySensorId ---

    @Test(expected = NullPointerException.class)
    public void ofSensorWithoutSummarySensorIdThrows() {
        new Device("id", "name", "prov", Group.GroupType.CPU, true,
                Device.SummaryType.OF_SENSOR, Device.SummaryType.NONE,
                Device.SummaryType.NONE, Device.SummaryType.NONE,
                null, null, null, null, 0, sensors());
    }

    // --- Conditional validation: AVG/MIN/MAX require format and unit ---

    @Test(expected = NullPointerException.class)
    public void avgSummaryWithoutUnitThrows() {
        new Device("id", "name", "prov", Group.GroupType.CPU, true,
                Device.SummaryType.AVG, Device.SummaryType.NONE,
                Device.SummaryType.NONE, Device.SummaryType.NONE,
                null, "%.1f", null, null, 0, sensors());
    }

    @Test(expected = NullPointerException.class)
    public void maxSummaryWithoutFormatThrows() {
        new Device("id", "name", "prov", Group.GroupType.CPU, true,
                Device.SummaryType.NONE, Device.SummaryType.NONE,
                Device.SummaryType.MAX, Device.SummaryType.NONE,
                null, null, "V", null, 0, sensors());
    }

    @Test
    public void avgSummaryWithFormatAndUnitSucceeds() {
        var device = new Device("id", "name", "prov", Group.GroupType.CPU, true,
                Device.SummaryType.AVG, Device.SummaryType.MIN,
                Device.SummaryType.MAX, Device.SummaryType.AVG,
                null, "%.1f", "V", null, 0, sensors());
        assertEquals(Device.SummaryType.AVG, device.summaryValue());
        assertEquals("%.1f", device.format());
        assertEquals("V", device.unit());
    }

    // --- Empty string validation ---

    @Test(expected = IllegalArgumentException.class)
    public void emptyIdThrows() {
        new Device("", "name", "prov", Group.GroupType.CPU, "desc", 0, sensors());
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyNameThrows() {
        new Device("id", "", "prov", Group.GroupType.CPU, "desc", 0, sensors());
    }

    // --- Nul character validation ---

    @Test(expected = IllegalArgumentException.class)
    public void nulInIdThrows() {
        new Device("id\0x", "name", "prov", Group.GroupType.CPU, "desc", 0, sensors());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nulInNameThrows() {
        new Device("id", "name\0x", "prov", Group.GroupType.CPU, "desc", 0, sensors());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nulInProviderThrows() {
        new Device("id", "name", "prov\0x", Group.GroupType.CPU, "desc", 0, sensors());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nulInDescriptionThrows() {
        new Device("id", "name", "prov", Group.GroupType.CPU, true,
                Device.SummaryType.NONE, Device.SummaryType.NONE,
                Device.SummaryType.NONE, Device.SummaryType.NONE,
                null, null, null, "desc\0x", 0, sensors());
    }

    // --- Path traversal validation ---

    @Test(expected = IllegalArgumentException.class)
    public void dotDotInIdThrows() {
        new Device("../id", "name", "prov", Group.GroupType.CPU, "desc", 0, sensors());
    }

    @Test(expected = IllegalArgumentException.class)
    public void tildeInNameThrows() {
        new Device("id", "~name", "prov", Group.GroupType.CPU, "desc", 0, sensors());
    }

    // --- Comparable / sorting ---

    @Test
    public void compareToSortsByIndex() {
        var a = new Device("a", "A", "p", Group.GroupType.CPU, "desc", 1, sensors());
        var b = new Device("b", "B", "p", Group.GroupType.CPU, "desc", 5, sensors());
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(a));
    }

    // --- Nul in conditional fields ---

    @Test(expected = IllegalArgumentException.class)
    public void nulInSummarySensorIdThrows() {
        new Device("id", "name", "prov", Group.GroupType.CPU, true,
                Device.SummaryType.OF_SENSOR, Device.SummaryType.NONE,
                Device.SummaryType.NONE, Device.SummaryType.NONE,
                "s\0x", null, null, null, 0, sensors());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nulInUnitWhenAvgSummaryThrows() {
        new Device("id", "name", "prov", Group.GroupType.CPU, true,
                Device.SummaryType.AVG, Device.SummaryType.NONE,
                Device.SummaryType.NONE, Device.SummaryType.NONE,
                null, "%.1f", "V\0x", null, 0, sensors());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nulInFormatWhenMinSummaryThrows() {
        new Device("id", "name", "prov", Group.GroupType.CPU, true,
                Device.SummaryType.MIN, Device.SummaryType.NONE,
                Device.SummaryType.NONE, Device.SummaryType.NONE,
                null, "%.1f\0x", "V", null, 0, sensors());
    }
}
