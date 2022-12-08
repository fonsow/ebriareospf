package engine_modules;

import core.JMSInterface;
import engine_modules_helpers.IOType;
import engine_modules_helpers.ModuleIO;
import engine_modules_helpers.ModuleType;
import utils.BPacket;
import utils.Common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

public abstract class EngineModule {
    ModuleType modType;
    LinkedList<String> signatures;

    public EngineModule(ModuleType modType) {
        this.modType = modType;
        this.signatures = this.loadSignatures();
    }

    public static EngineModule createModule(String modName) {
        switch (modName) {
            case "app_data_filter":
                return new AppDataFilter();
            case "generic_url_exploit_detector":
                return new GenericURLExploitDetector();
            case "honeypot":
                return new Honeypot();
            case "honeypotV2":
                return new HoneypotV2();
            case "incident_processor":
                return new IncidentProcessor();
            case "leak_detector":
                return new LeakDetector();
            case "pid_fetcher":
                return new PidFetcher();
            case "shell_detector":
                return new ShellDetector();
            case "signature_test_module":
                return new SignatureTest();
            case "simple_http_parser":
                return new SimpleHttpParser();
            case "sqli_detector":
                return new SqliDetector();
            case "tcp_stream":
                return new TcpStream();
            case "xss_detector":
                return new XssDetector();
            default:
                System.out.println("There is no module with that name");
                System.exit(-1);
        }

        return null;
    }

    LinkedList<String> loadSignatures() {
        LinkedList<String> signatures = new LinkedList<>();

        if (this.modType.equals(ModuleType.SignatureTest)) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(Common.SIGNATURES_PATH));
                String line = reader.readLine();
                while (line != null) {
                    signatures.add(line);
                    line = reader.readLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return signatures;
    }

    public ModuleType getModType() {
        return modType;
    }

    public abstract IOType getInputType();

    public abstract IOType getOutputType();

    public abstract Common.InputMode getInputMode();

    public abstract ModuleIO process(BPacket packet, ModuleIO data, JMSInterface jmsInterface);
}
