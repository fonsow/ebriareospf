package engine_modules;

import core.JMSInterface;
import engine_modules_helpers.IOType;
import engine_modules_helpers.ModuleIO;
import engine_modules_helpers.ModuleType;
import utils.BPacket;
import utils.Common;

public class IncidentProcessor extends EngineModule {
    public IncidentProcessor() {
        super(ModuleType.IncidentProcessor);
    }

    @Override
    public IOType getInputType() {
        return IOType.List;
    }

    @Override
    public IOType getOutputType() {
        return IOType.None;
    }

    @Override
    public Common.InputMode getInputMode() {
        return Common.InputMode.MultipleInput;
    }

    @Override
    public ModuleIO process(BPacket packet, ModuleIO data, JMSInterface jmsInterface) {
        return new ModuleIO();
    }
}
