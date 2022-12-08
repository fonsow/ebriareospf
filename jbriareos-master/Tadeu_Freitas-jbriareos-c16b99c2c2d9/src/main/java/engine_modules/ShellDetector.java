package engine_modules;

import core.JMSInterface;
import engine_modules_helpers.IOData;
import engine_modules_helpers.IOType;
import engine_modules_helpers.ModuleIO;
import engine_modules_helpers.ModuleType;
import utils.BPacket;
import utils.Common;

public class ShellDetector extends EngineModule {
    public ShellDetector() {
        super(ModuleType.ShellDetector);
    }

    @Override
    public IOType getInputType() {
        return IOType.Tuple;
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
        if (data.ioType != IOType.Tuple) {
            System.out.println("-- ShellDetector --");
            System.out.println("Data's type is not the same as the data type needed by the module");
            System.exit(-1);
        }

        String appName = ((IOData.IOTuple)data.ioData).string;
        if (appName.equals("/bin/sh") || appName.equals("/bin/bash")) {
            System.out.println("Shell detected > Dropping connection");
            packet.drop();
            return new ModuleIO();
        }

        return new ModuleIO();
    }
}
