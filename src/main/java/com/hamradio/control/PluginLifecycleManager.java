package com.hamradio.control;

import com.hamradio.plugin.Plugin;
import com.hamradio.plugin.PluginRegistry;

import java.util.ServiceLoader;

public class PluginLifecycleManager {

    private final PluginRegistry registry;

    public PluginLifecycleManager(PluginRegistry registry) {
        this.registry = registry;
    }

    public void discover() {
        ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class);
        for (Plugin plugin : loader) {
            registry.register(plugin);
        }
    }

    public void initAll() {
        for (Plugin p : registry.getAll()) {
            p.init();
        }
    }

    public void startAll() {
        for (Plugin p : registry.getAll()) {
            p.start();
        }
    }

    public void stopAll() {
        for (Plugin p : registry.getAll()) {
            p.stop();
        }
    }

    public void destroyAll() {
        for (Plugin p : registry.getAll()) {
            p.destroy();
        }
    }
}
