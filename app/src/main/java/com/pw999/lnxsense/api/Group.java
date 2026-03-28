package com.pw999.lnxsense.api;

import java.io.Serializable;
import java.util.SortedSet;

// A group combines a set of Devices from different providers.
public record Group(SortedSet<Device> devices, GroupType groupType) implements Serializable {
    public enum GroupType {
        CPU("CPU"), GPU("GPU"), MEMORY("Memory"), DISK("Disk"), VOLTAGE("Voltage"), FAN("Fan"), OTHER("Other");
        private final String label;

        GroupType(String label) {
            this.label = label;
        }

        // Label to show in the UI
        public String getLabel() {
            return label;
        }
    }

}

