package com.pw999.lnxsense.api;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

// This record contains a single reading of a sensor.
// In normal circumstances, the status should always be OK.
//
// Some devices might report high, critical or low thresholds which should be used to set the status of the reading.
// If something when wrong while reading the sensor, the value will be ignored and will be set to 0.
//
// @param value The value returned by the sensor
// @param status The status of this reading
public record Reading(String deviceId, String sensorId, double value, Status status) implements Serializable  {

    public Reading(String deviceId, String sensorId, double value, Status status) {
        this.deviceId = deviceId;
        this.sensorId = sensorId;
        if (status.equals(Status.ERROR)) {
            this.value = 0.0d;
        } else {
            this.value = value;
        }
        this.status = status;
    }

    // Read out status
    public enum Status {
        // Everything is ok
        OK,
        // The value that is read is too low
        LOW,
        // There was en error when reading the value
        ERROR,
        // The value that is read is high but not problematic
        HIGH,
        // The value that is read is critical and might result in instability, slowdowns or damage
        CRITICAL,
        // In case a sensor gets disabled
        DISABLED
    }

    // Converts a reading to a byte array for sending over sockets. This should be faster compared to regular object serialization.
    // The format is
    // * length of deviceId as short
    // * UTF-8 string with previously defined length representing the deviceId
    // * length of sensorId as short
    // * UTF-8 string with previously defined length representing the sensorId
    // * The reading as a double
    // * length of Status string as short
    // * UTF-8 string with previously defined length representing the Status enum
    public byte[] asByteArray() {
        byte[] deviceIdBytes = deviceId.getBytes(StandardCharsets.UTF_8);
        short deviceIdLength = (short) deviceIdBytes.length;
        byte[] sensorIdBytes = sensorId.getBytes(StandardCharsets.UTF_8);
        short sensorIdLength = (short) sensorIdBytes.length;
        short statusLength = (short) status.toString().length();
        ByteBuffer buffer = ByteBuffer.allocate(deviceIdLength + sensorIdLength + statusLength + 16);
        buffer.putShort(deviceIdLength);
        buffer.put(deviceIdBytes);
        buffer.putShort(sensorIdLength);
        buffer.put(sensorIdBytes);
        buffer.putDouble(value);
        buffer.putShort(statusLength);
        buffer.put(status.toString().getBytes(StandardCharsets.UTF_8));
        return buffer.array();
    }

    // Converts a byte array back into a Reading object.
    // See asByteArray for details on the format
    public static Reading fromByteArray(byte[] data) {
        ByteBuffer newBuffer = ByteBuffer.wrap(data);
        short deviceLen = newBuffer.getShort();
        byte[] deviceBytes = new byte[deviceLen];
        newBuffer.get(deviceBytes);
        String d = new String(deviceBytes, StandardCharsets.UTF_8);

        short sensorLen = newBuffer.getShort();
        byte[] sensorBytes = new byte[sensorLen];
        newBuffer.get(sensorBytes);
        String s = new String(sensorBytes, StandardCharsets.UTF_8);

        double v = newBuffer.getDouble();

        short statusLen = newBuffer.getShort();
        byte[] statusBytes = new byte[statusLen];
        newBuffer.get(statusBytes);
        Reading.Status st = Reading.Status.valueOf(new String(statusBytes, StandardCharsets.UTF_8));

        return new Reading(d, s, v, st);
    }
}
