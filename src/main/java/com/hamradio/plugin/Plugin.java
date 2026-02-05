package com.hamradio.plugin;

public interface Plugin {
    String getId();
    String getVersion();
    void init();
    void start();
    void stop();
    void destroy();
}
