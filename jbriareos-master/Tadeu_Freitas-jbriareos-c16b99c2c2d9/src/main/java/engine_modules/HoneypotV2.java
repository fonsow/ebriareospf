package engine_modules;

import core.JMSInterface;
import engine_modules_helpers.IOData;
import engine_modules_helpers.IOType;
import engine_modules_helpers.ModuleIO;
import engine_modules_helpers.ModuleType;
import utils.BPacket;
import utils.Common;

// If this module is used, masquerading should be active on all machines connected to the JMS:
// iptables -t nat -A POSTROUTING -j MASQUERADE
public class HoneypotV2 extends EngineModule {
    public HoneypotV2() {
        super(ModuleType.HoneypotV2);
    }

    @Override
    public IOType getInputType() {
        return IOType.None;
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
        packet.redirectPackages("10.0.2.15:8001");

        return new ModuleIO(IOType.String, new IOData.IOString("Output"));
    }
}
