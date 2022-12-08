package engine_modules;

import core.JMSInterface;
import engine_modules_helpers.*;
import utils.BPacket;
import utils.Common;

public class PidFetcher extends EngineModule {
    public PidFetcher() {
        super(ModuleType.PidFetcher);
    }

    @Override
    public IOType getInputType() {
        return IOType.None;
    }

    @Override
    public IOType getOutputType() {
        return IOType.Tuple;
    }

    @Override
    public Common.InputMode getInputMode() {
        return Common.InputMode.SingleInput;
    }

    @Override
    public ModuleIO process(BPacket packet, ModuleIO data, JMSInterface jmsInterface) {
        String srcIp = packet.getSrcIp();
        int srcPort = packet.getSrcPort();
        String dstIp = packet.getDstIP();
        int dstPort = packet.getDstPort();

        PsUtil.ConnectionAppInfo appInfo =
                PsUtil.getPidFromNetConnections(packet.getProtocol(), srcIp, srcPort, dstIp, dstPort);
        if (appInfo == null)
            appInfo = new PsUtil.ConnectionAppInfo(-1, "Unknown");

        return new ModuleIO(IOType.Tuple, new IOData.IOTuple(appInfo.pid, appInfo.appName));
    }
}
