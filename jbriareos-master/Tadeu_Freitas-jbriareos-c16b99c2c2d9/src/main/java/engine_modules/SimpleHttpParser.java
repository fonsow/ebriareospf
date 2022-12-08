package engine_modules;

import core.JMSInterface;
import engine_modules_helpers.*;
import utils.BPacket;
import utils.Common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Hashtable;

public class SimpleHttpParser extends EngineModule {

    public SimpleHttpParser() {
        super(ModuleType.HttpParser);
    }

    @Override
    public IOType getInputType() {
        return IOType.String;
    }

    @Override
    public IOType getOutputType() {
        return IOType.HTTPObject;
    }

    @Override
    public Common.InputMode getInputMode() {
        return Common.InputMode.SingleInput;
    }

    @Override
    public ModuleIO process(BPacket packet, ModuleIO data, JMSInterface jmsInterface) {
        if (data.ioType != IOType.String) {
            System.out.println("-- SimpleHttpParser --");
            System.out.println("Data's type is not the same as the data type needed by the module");
            return null;
        }

        try {
            String dataString = ((IOData.IOString) data.ioData).string;
            HttpParser parser = new HttpParser(new ByteArrayInputStream(dataString.getBytes()));

            int statusCode = parser.parseRequest();
            String method = parser.getMethod();
            String url = parser.getRequestURL();
            Hashtable<String, String> headers = parser.getHeaders();

            HttpObject httpObject = new HttpObject(url, method, headers, statusCode);

            return new ModuleIO(IOType.HTTPObject, new IOData.IOHttpObj(httpObject));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Couldn't parse http request");
            System.exit(-1);
        }

        return null;
    }
}
