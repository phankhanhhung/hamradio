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

    @SuppressWarnings("unchecked")
    public <T extends Plugin> List<T> getPluginsByType(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Plugin p : plugins.values()) {
            if (type.isInstance(p)) {
                result.add((T) p);
            }
        }
        return result;
    }

    public Collection<Plugin> getAll() {
        return Collections.unmodifiableCollection(plugins.values());
    }
}
