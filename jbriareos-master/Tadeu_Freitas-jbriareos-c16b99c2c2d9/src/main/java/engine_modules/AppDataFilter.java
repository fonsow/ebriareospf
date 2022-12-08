package engine_modules;

import core.JMSInterface;
import engine_modules_helpers.IOData;
import engine_modules_helpers.IOType;
import engine_modules_helpers.ModuleIO;
import engine_modules_helpers.ModuleType;
import utils.BPacket;
import utils.Common;

public class AppDataFilter extends EngineModule {
    public AppDataFilter() {
        super(ModuleType.AppDataFilter);
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
        String packetData = packet.getApplicationData();

        if (packetData.isEmpty()) {
            packet.accept();
            return new ModuleIO(IOType.String, new IOData.IOString(""));
        }

        if (packetData.contains("hack")) {
            // packet.drop();
            packet.drop();
            System.out.println("\nDropping packet:");
            System.out.println(packetData);
            System.out.println("And blocking its source IP address\n");

            if (jmsInterface != null)
                jmsInterface.blockIPAddress(packet);
            else
                packet.blockIPAddress();

            return new ModuleIO(IOType.String, new IOData.IOString(""));
        }

        return new ModuleIO(IOType.String, new IOData.IOString(packetData));
    }
}
