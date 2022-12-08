package engine_modules;

import core.JMSInterface;
import engine_modules_helpers.IOData;
import engine_modules_helpers.IOType;
import engine_modules_helpers.ModuleIO;
import engine_modules_helpers.ModuleType;
import utils.BPacket;
import utils.Common;

public class SqliDetector extends EngineModule {

    public SqliDetector() {
        super(ModuleType.SQLIDetector);
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
        if (data.ioType != IOType.HTTPObject) {
            System.out.println("-- SqliDetector --");
            System.out.println("Data's type is not the same as the data type needed by the module");
            System.exit(-1);
        }

        String url = ((IOData.IOHttpObj)data.ioData).httpObject.url;
        if (url.contains("hack")) {
            packet.drop();
            System.out.println("Dropping packet: " + url);
            System.out.println("And blocking its source IP address ");

            if (jmsInterface != null)
                jmsInterface.blockIPAddress(packet);
            else
                packet.blockIPAddress();
        }

        return new ModuleIO(IOType.String, new IOData.IOString("SQLi Detector output"));
    }
}
