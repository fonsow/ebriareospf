package engine_modules;

import core.JMSInterface;
import engine_modules_helpers.IOType;
import engine_modules_helpers.ModuleIO;
import engine_modules_helpers.ModuleType;
import utils.BPacket;
import utils.Common;

public class TcpStream extends EngineModule {
    public TcpStream() {
        super(ModuleType.TCPStream);
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
        if (packet.isNewConnection())
            System.out.println("New connection");
        else if (packet.isConnectionClosed())
            System.out.println("Connection closed");

        return new ModuleIO();
    }
}
