package engine_modules;

import core.JMSInterface;
import engine_modules_helpers.IOData;
import engine_modules_helpers.IOType;
import engine_modules_helpers.ModuleIO;
import engine_modules_helpers.ModuleType;
import utils.BPacket;
import utils.Common;

public class XssDetector extends EngineModule {
    public XssDetector() {
        super(ModuleType.XSSDetector);
    }

    @Override
    public IOType getInputType() {
        return IOType.HTTPObject;
    }

    @Override
    public IOType getOutputType() {
        return IOType.String;
    }

    @Override
    public Common.InputMode getInputMode() {
        return Common.InputMode.SingleInput;
    }

    @Override
    public ModuleIO process(BPacket packet, ModuleIO data, JMSInterface jmsInterface) {
        return new ModuleIO(IOType.String, new IOData.IOString("XSS Detector output"));
    }
}
