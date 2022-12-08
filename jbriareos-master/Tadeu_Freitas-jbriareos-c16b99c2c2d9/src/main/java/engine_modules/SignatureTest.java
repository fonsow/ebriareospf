package engine_modules;

import core.JMSInterface;
import engine_modules_helpers.IOType;
import engine_modules_helpers.ModuleIO;
import engine_modules_helpers.ModuleType;
import utils.BPacket;
import utils.Common;

public class SignatureTest extends EngineModule {

    public SignatureTest() {
        super(ModuleType.SignatureTest);
    }

    @Override
    public IOType getInputType() {
        return IOType.None;
    }

    @Override
    public IOType getOutputType() {
        return IOType.None;
    }

    @Override
    public Common.InputMode getInputMode() {
        return Common.InputMode.SingleInput;
    }

    @Override
    public ModuleIO process(BPacket packet, ModuleIO data, JMSInterface jmsInterface) {
        String packetData = packet.getApplicationData();
        if (packetData.isEmpty()) {
            packet.accept();
            return new ModuleIO();
        }

        for (String signature : this.signatures) {
            if (packetData.contains(signature)) {
                packet.drop();
                return new ModuleIO();
            }
        }

        packet.accept();
        return new ModuleIO();
    }
}
