package com.pw999.lnxsense.api;

import org.junit.Test;

import static org.junit.Assert.*;

public class SensorTest {

    // --- Valid construction ---

    @Test
    public void fullConstructorStoresAllFields() {
        var sensor = new Sensor("temp1", "CPU Temp", "\u00B0C", "%.2f", "Core temperature", 0);
        assertEquals("temp1", sensor.id());
        assertEquals("CPU Temp", sensor.name());
        assertEquals("\u00B0C", sensor.unit());
        assertEquals("%.2f", sensor.format());
        assertEquals("Core temperature", sensor.description());
        assertEquals(0, sensor.index());
    }

    @Test
    public void convenienceConstructorWithIdNameIndex() {
        var sensor = new Sensor("fan1", "Fan Speed", 3);
        assertEquals("fan1", sensor.id());
        assertEquals("Fan Speed", sensor.name());
        assertEquals("", sensor.unit());
        assertEquals("%,.1f", sensor.format());
        assertNull(sensor.description());
        assertEquals(3, sensor.index());
    }

    @Test
    public void convenienceConstructorWithUnit() {
        var sensor = new Sensor("volt", "Voltage", "V", 1);
        assertEquals("V", sensor.unit());
        assertEquals("%,.1f", sensor.format());
    }

    @Test
    public void convenienceConstructorWithUnitAndFormat() {
        var sensor = new Sensor("rpm", "Fan", "RPM", "%d", 2);
        assertEquals("RPM", sensor.unit());
        assertEquals("%d", sensor.format());
    }

    @Test
    public void nullUnitAndFormatAllowed() {
        var sensor = new Sensor("id", "name", null, null, null, 0);
        assertNull(sensor.unit());
        assertNull(sensor.format());
    }

    // --- Null validation ---

    @Test(expected = NullPointerException.class)
    public void nullIdThrows() {
        new Sensor(null, "name", 0);
    }

    @Test(expected = NullPointerException.class)
    public void nullNameThrows() {
        new Sensor("id", null, 0);
    }

    // --- Empty string validation ---

    @Test(expected = IllegalArgumentException.class)
    public void emptyIdThrows() {
        new Sensor("", "name", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyNameThrows() {
        new Sensor("id", "", 0);
    }

    // --- Nul character validation ---

    @Test(expected = IllegalArgumentException.class)
    public void nulInIdThrows() {
        new Sensor("id\0bad", "name", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nulInNameThrows() {
        new Sensor("id", "name\0bad", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nulInUnitThrows() {
        new Sensor("id", "name", "V\0", "%f", null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nulInFormatThrows() {
        new Sensor("id", "name", "V", "%f\0", null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nulInDescriptionThrows() {
        new Sensor("id", "name", "V", "%f", "desc\0bad", 0);
    }

    // --- Path traversal validation ---

    @Test(expected = IllegalArgumentException.class)
    public void dotDotInIdThrows() {
        new Sensor("../etc/passwd", "name", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void dotDotInNameThrows() {
        new Sensor("id", "..sneaky", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tildeInIdThrows() {
        new Sensor("~root", "name", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tildeInNameThrows() {
        new Sensor("id", "~name", 0);
    }

    // --- Comparable / sorting ---

    @Test
    public void compareToSortsByIndex() {
        var a = new Sensor("a", "A", 1);
        var b = new Sensor("b", "B", 5);
        var c = new Sensor("c", "C", 3);

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(c) < 0);
        assertEquals(0, a.compareTo(a));
    }
}
