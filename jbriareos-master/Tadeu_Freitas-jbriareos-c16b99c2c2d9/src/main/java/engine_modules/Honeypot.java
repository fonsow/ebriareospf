package engine_modules;

import core.JMSInterface;
import engine_modules_helpers.IOData;
import engine_modules_helpers.IOType;
import engine_modules_helpers.ModuleIO;
import engine_modules_helpers.ModuleType;
import nemo.it.unipr.netsec.ipstack.ip4.Ip4Address;
import nemo.it.unipr.netsec.ipstack.ip4.IpAddress;
import nemo.it.unipr.netsec.ipstack.ip4.SocketAddress;
import utils.BPacket;
import utils.Common;

public class Honeypot extends EngineModule {
    public Honeypot() {
        super(ModuleType.Honeypot);
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

    private void changePort(BPacket packet, IpAddress address) {
        SocketAddress socketAddress = new SocketAddress(address, (short) 8001);

        packet.pkt.setDestAddress(socketAddress);
    }

    @Override
    public ModuleIO process(BPacket packet, ModuleIO data, JMSInterface jmsInterface) {
        IpAddress address = new Ip4Address("10.0.2.15");
        changePort(packet, address);

        return new ModuleIO(IOType.String, new IOData.IOString("Output"));
    }
}
