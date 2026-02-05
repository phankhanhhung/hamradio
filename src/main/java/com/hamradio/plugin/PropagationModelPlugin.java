package com.hamradio.plugin;

import com.hamradio.rf.PropagationModel;
import java.util.List;

public interface PropagationModelPlugin extends Plugin {
    List<PropagationModel> getModels();
}
