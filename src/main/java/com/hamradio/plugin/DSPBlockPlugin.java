package com.hamradio.plugin;

import com.hamradio.dsp.graph.DSPBlock;
import java.util.List;

public interface DSPBlockPlugin extends Plugin {
    List<Class<? extends DSPBlock>> getBlockTypes();
}
