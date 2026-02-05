package com.hamradio.plugin;

import java.util.*;

public class PluginRegistry {

    private final Map<String, Plugin> plugins = new LinkedHashMap<>();

    public void register(Plugin plugin) {
        plugins.put(plugin.getId(), plugin);
    }

    public void unregister(String pluginId) {
        plugins.remove(pluginId);
    }

    public Plugin getPlugin(String id) {
        return plugins.get(id);
    }


    public Collection<Plugin> getAll() {
        return Collections.unmodifiableCollection(plugins.values());
    }
}
