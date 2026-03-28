package com.pw999.lnxsense.api;


import java.io.Serializable;
import java.util.Objects;

// A sensor is a representation of a measurable entity. This can be a physical state (e.g. voltage, fan rpm, ...)
// or a virtual state (e.g. cpu frequency, memory utilization, ...)
// The id of a sensor should be unique per device. A sensor should always be passed to functions with a device.
// A sensor should never be separated from it's device, without the device object, the sensor has no meaning and is
// useless.
//
// @param id A unique identifier for this sensor, throughout the lifetime of the application it should not change
// @param name The name of the sensor which will be shown to the user
// @param unit the unit to show (if present)
// @param format the format to use (defaults to %.1f)
// @param index Index used to sort a set
public record Sensor(String id, String name, String unit, String format, String description, int index) implements Comparable<Sensor>, Serializable {

    public Sensor(String id, String name, String unit, String format, String description, int index) {
        Objects.requireNonNull(id, "id is mandatory");
        Objects.requireNonNull(name, "name is mandatory");
        if (id.indexOf('\0') >= 0) throw new IllegalArgumentException("Nul characters are not allowed, found one in id");
        if (name.indexOf('\0') >= 0) throw new IllegalArgumentException("Nul characters are not allowed, found one in name");

        if (unit != null && unit.indexOf('\0') >= 0) throw new IllegalArgumentException("Nul characters are not allowed, found one in id");
        if (format != null && format.indexOf('\0') >= 0) throw new IllegalArgumentException("Nul characters are not allowed, found one in name");

        if (id.isEmpty()) throw new IllegalArgumentException("String id can not be empty");
        if (name.isEmpty()) throw new IllegalArgumentException("String name can not be empty");

        checkForNul(id);
        checkForNul(name);
        checkForNul(description);
        checkPathTraversal(id);
        checkPathTraversal(name);

        this.id = id;
        this.name = name;
        this.unit = unit;
        this.format = format;
        this.description = description;
        this.index = index;
    }

    // Checks if the (nullable) input string contains any nul charachters.
    // Nul terminators in the strings is usually a bad sign (and causes downstream issues), mainly with Paths.
    private static void checkForNul(String in) {
        if (in != null && in.indexOf('\0') >= 0) throw new IllegalArgumentException("Nul characters are not allowed");
    }

    // Checks for potential path traversal
    private static void checkPathTraversal(String in) {
        if (in.contains("..")) throw new IllegalArgumentException(".. is not allowed in ids and names");
        if (in.contains("~")) throw new IllegalArgumentException("~ is not allowed in ids and names");
    }

    // Convenience constructor with default format of 1 decimal place and thousands separator
    // Contains no description or unit.
    public Sensor(String id, String name, int index) {
        this(id, name, "", "%,.1f", null, index);
    }

    // Convenience constructor with default format of 1 decimal place and thousands separator
    // Contains no description or unit.
    public Sensor(String id, String name, String unit, int index) {
        this(id, name, unit, "%,.1f", null, index);
    }

    // Convenience constructor with no description.
    public Sensor(String id, String name, String unit, String format, int index) {
        this(id, name, unit, format, null, index);
    }

    @Override
    public int compareTo(Sensor sensor) {
        return Integer.compare(index(), sensor.index());
    }
}
