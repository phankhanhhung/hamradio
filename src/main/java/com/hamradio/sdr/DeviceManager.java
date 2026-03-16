package com.hamradio.sdr;

import java.util.*;

public class DeviceManager {

    private final Map<String, SDRDevice> devices = new LinkedHashMap<>();

    public DeviceManager() {
        // Register built-in simulated device
        register(new SimulatedDevice());
    }

    public void register(SDRDevice device) {
        devices.put(device.getName(), device);
    }

    public SDRDevice getDevice(String name) {
        return devices.get(name);
    }

    public List<String> listDevices() {
        return new ArrayList<>(devices.keySet());
    }
}
