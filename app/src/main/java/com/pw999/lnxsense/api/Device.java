package com.pw999.lnxsense.api;

import java.io.Serializable;
import java.util.*;

// Representation of either a physical device or a system component.
// This can be a hardware sensor running over I2C or SMBUS, a CPU, GPU or individual RAM sticks.
// It can also be the representation of a software component like CPU usage, memory usage, disk utilization, ...
// The goal of this object is to group a set of sensors into a logical block per provider.
// The id of each device must be unique amongst all devices that could possibly exist.
//
// @param id A unique identifier for this device, throughout the lifetime of the application it should not change
// @param name The name of the device that will be shown to the user
// @param provider The name of the provider (module) that provides this device
// @param groupType a logical group enumeration to group the devices in a group
// @param expandedByDefault indicates if the device should be expanded by default in the UI
// @param summaryValue when collapsed in the UI, the content of the value column is determined by this SummaryType
// @param summaryMin when collapsed in the UI, the content of the min column is determined by this SummaryType
// @param summaryMax when collapsed in the UI, the content of the max column is determined by this SummaryType
// @param summaryAvg when collapsed in the UI, the content of the avg column is determined by this SummaryType
// @param summarySensorId if any of the summary types is OF_SENSOR, it will use this sensor ID to get the value from
// @param format the format to use for the summary row (except for OF_SENSOR where the format of the sensor is used)
// @param unit the unit to use for the summary row  (except for OF_SENSOR where the unit of the sensor is used)
// @param index An index used for sorting the devices, this index must be unique for all devices in the system
// @param sensors A unique list of sensors that are provided by this device
public record Device(String id,
                     String name,
                     String provider,
                     Group.GroupType groupType,
                     boolean expandedByDefault,
                     SummaryType summaryValue,
                     SummaryType summaryMin,
                     SummaryType summaryMax,
                     SummaryType summaryAvg,
                     String summarySensorId,
                     String format,
                     String unit,
                     String description,
                     int index,
                     SortedSet<Sensor> sensors) implements Comparable<Device>, Serializable {

    public Device(String id, String name, String provider, Group.GroupType groupType, boolean expandedByDefault, SummaryType summaryValue, SummaryType summaryMin, SummaryType summaryMax, SummaryType summaryAvg, String summarySensorId, String format, String unit, String description, int index, SortedSet<Sensor> sensors) {
        Objects.requireNonNull(id, "id is mandatory");
        Objects.requireNonNull(name, "name is mandatory");
        Objects.requireNonNull(provider, "provider is mandatory");
        Objects.requireNonNull(groupType, "groupType is mandatory");
        Objects.requireNonNull(summaryValue, "summaryValue is mandatory");
        Objects.requireNonNull(summaryMin, "summaryMin is mandatory");
        Objects.requireNonNull(summaryMax, "summaryMax is mandatory");
        Objects.requireNonNull(summaryAvg, "summaryAvg is mandatory");
        Objects.requireNonNull(sensors, "sensors is mandatory");
        var summaryTypes = List.of(summaryValue, summaryMin, summaryMax, summaryAvg);
        if (summaryTypes.contains(SummaryType.OF_SENSOR)) {
            Objects.requireNonNull(summarySensorId, "summarySensorId is mandatory if SummaryType.OF_SENSOR is used");
            checkForNul(summarySensorId);
        }
        if (summaryTypes.contains(SummaryType.AVG) || summaryTypes.contains(SummaryType.MIN) || summaryTypes.contains(SummaryType.MAX)) {
            Objects.requireNonNull(unit, "unit is mandatory if SummaryType.AVG, SummaryType.MIN, SummaryType.MAX are used");
            Objects.requireNonNull(format, "format is mandatory if SummaryType.AVG, SummaryType.MIN, SummaryType.MAX are used");
            checkForNul(unit);
            checkForNul(format);
        }
        checkForNul(id);
        checkForNul(name);
        checkForNul(provider);
        checkForNul(description);
        checkPathTraversal(id);
        checkPathTraversal(name);

        if (id.isEmpty()) throw new IllegalArgumentException("String id can not be empty");
        if (name.isEmpty()) throw new IllegalArgumentException("String name can not be empty");

        this.id = id;
        this.name = name;
        this.provider = provider;
        this.groupType = groupType;
        this.expandedByDefault = expandedByDefault;
        this.summaryValue = summaryValue;
        this.summaryMin = summaryMin;
        this.summaryMax = summaryMax;
        this.summaryAvg = summaryAvg;
        this.summarySensorId = summarySensorId;
        this.format = format;
        this.unit = unit;
        this.description = description;
        this.index = index;
        this.sensors = sensors;
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

    // Convenience constructor for a device without summary which is expanded by default
    public Device(String id,
                  String name,
                  String provider,
                  Group.GroupType groupType,
                  String description,
                  int index,
                  SortedSet<Sensor> sensors) {
        this(id,
                name,
                provider,
                groupType,
                true,
                SummaryType.NONE,
                SummaryType.NONE,
                SummaryType.NONE,
                SummaryType.NONE,
                null,
                null,
                null,
                description,
                index,
                sensors
                );
    }

    // Convenience constructor for a device without summary which you can expand or not by default
    public Device(String id,
                  String name,
                  String provider,
                  Group.GroupType groupType,
                  boolean expandedByDefault,
                  String description,
                  int index,
                  SortedSet<Sensor> sensors) {
        this(id,
                name,
                provider,
                groupType,
                expandedByDefault,
                SummaryType.NONE,
                SummaryType.NONE,
                SummaryType.NONE,
                SummaryType.NONE,
                null,
                null,
                null,
                description,
                index,
                sensors
        );
    }

    // Convenience constructor for a default collapsed device where all summaries are OF_SENSOR
    public Device(String id,
                  String name,
                  String provider,
                  Group.GroupType groupType,
                  String summarySensorId,
                  String description,
                  int index,
                  SortedSet<Sensor> sensors) {
        this(id,
                name,
                provider,
                groupType,
                false,
                SummaryType.OF_SENSOR,
                SummaryType.OF_SENSOR,
                SummaryType.OF_SENSOR,
                SummaryType.OF_SENSOR,
                summarySensorId,
                null,
                null,
                description,
                index,
                sensors
        );
    }

    // The summary type defines how the value, min, max and avg columns for the device's row are filled in
    public enum SummaryType {
        // Nothing is shown when the device's row is collapsed
        NONE,
        // Use on the device's sensor values
        OF_SENSOR,
        // Uses the minimum value of all sensors in the given column
        MIN,
        // Uses the maximum value of all sensors in the given column
        MAX,
        // Uses the average value of all sensors in the given column
        AVG
    }

    @Override
    public int compareTo(Device device) {
        return Integer.compare(index(), device.index());
    }
}
