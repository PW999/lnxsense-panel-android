package com.pw999.lnxsense.api;

import org.junit.Test;

import static org.junit.Assert.*;

public class ReadingTest {

    // --- Byte serialization round-trip ---

    @Test
    public void roundTripPreservesAllFields() {
        var original = new Reading("cpu-0", "temp1", 72.5, Reading.Status.OK);
        var restored = Reading.fromByteArray(original.asByteArray());

        assertEquals(original.deviceId(), restored.deviceId());
        assertEquals(original.sensorId(), restored.sensorId());
        assertEquals(original.value(), restored.value(), 0.0);
        assertEquals(original.status(), restored.status());
    }

    @Test
    public void roundTripWithEveryStatus() {
        for (var status : Reading.Status.values()) {
            var original = new Reading("dev", "sen", 42.0, status);
            var restored = Reading.fromByteArray(original.asByteArray());

            assertEquals(status, restored.status());
            // ERROR zeroes the value, so compare with expected
            assertEquals(original.value(), restored.value(), 0.0);
        }
    }

    @Test
    public void roundTripWithUnicodeIds() {
        var original = new Reading("geraet-\u00FC\u00E4\u00F6", "sensor-\u2103", 99.9, Reading.Status.HIGH);
        var restored = Reading.fromByteArray(original.asByteArray());

        assertEquals(original.deviceId(), restored.deviceId());
        assertEquals(original.sensorId(), restored.sensorId());
    }

    @Test
    public void roundTripWithZeroValue() {
        var original = new Reading("dev", "sen", 0.0, Reading.Status.OK);
        var restored = Reading.fromByteArray(original.asByteArray());
        assertEquals(0.0, restored.value(), 0.0);
    }

    @Test
    public void roundTripWithNegativeValue() {
        var original = new Reading("dev", "sen", -12.34, Reading.Status.LOW);
        var restored = Reading.fromByteArray(original.asByteArray());
        assertEquals(-12.34, restored.value(), 0.0);
    }

    @Test
    public void roundTripWithLargeValue() {
        var original = new Reading("dev", "sen", Double.MAX_VALUE, Reading.Status.CRITICAL);
        var restored = Reading.fromByteArray(original.asByteArray());
        assertEquals(Double.MAX_VALUE, restored.value(), 0.0);
    }

    // --- ERROR status zeroes value ---

    @Test
    public void errorStatusSetsValueToZero() {
        var reading = new Reading("dev", "sen", 99.9, Reading.Status.ERROR);
        assertEquals(0.0, reading.value(), 0.0);
        assertEquals(Reading.Status.ERROR, reading.status());
    }

    @Test
    public void nonErrorStatusPreservesValue() {
        var reading = new Reading("dev", "sen", 42.5, Reading.Status.OK);
        assertEquals(42.5, reading.value(), 0.0);

        reading = new Reading("dev", "sen", 80.0, Reading.Status.HIGH);
        assertEquals(80.0, reading.value(), 0.0);

        reading = new Reading("dev", "sen", 95.0, Reading.Status.CRITICAL);
        assertEquals(95.0, reading.value(), 0.0);

        reading = new Reading("dev", "sen", 10.0, Reading.Status.LOW);
        assertEquals(10.0, reading.value(), 0.0);

        reading = new Reading("dev", "sen", 0.0, Reading.Status.DISABLED);
        assertEquals(0.0, reading.value(), 0.0);
    }

    // --- Byte array format consistency ---

    @Test
    public void byteArrayIsNotEmpty() {
        var reading = new Reading("d", "s", 1.0, Reading.Status.OK);
        byte[] bytes = reading.asByteArray();
        assertTrue(bytes.length > 0);
    }

    @Test
    public void errorStatusRoundTripPreservesZeroedValue() {
        // Value gets zeroed on construction, then serialized as 0
        var original = new Reading("dev", "sen", 123.456, Reading.Status.ERROR);
        assertEquals(0.0, original.value(), 0.0);

        var restored = Reading.fromByteArray(original.asByteArray());
        assertEquals(0.0, restored.value(), 0.0);
        assertEquals(Reading.Status.ERROR, restored.status());
    }
}
