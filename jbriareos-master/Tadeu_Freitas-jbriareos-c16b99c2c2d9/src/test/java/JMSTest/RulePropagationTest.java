package JMSTest;

import core.JMSInterface;
import core.DistributedZMQ;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.zeromq.*;
import utils.Common;
import utils.IpTables;
import zmq.ZError;

import java.io.IOException;

public class RulePropagationTest {
    static JSONObject componentConfig = new JSONObject();
    static String jmsSinkAddress;
    static String jmsPublisherAddress;

    static ZContext subscriberContext;
    static ZCert cert;
    static byte[] serverKey;

    @SuppressWarnings("unchecked")
    private static void componentConfigInit() {
        JSONObject jms = new JSONObject();
        jms.put("ip", "127.0.0.1");
        jms.put("port", (long) 11335);
        jms.put("unsecure_port", (long) 11336);
        jms.put("sink_port", (long) 22222);
        jms.put("publisher_port", (long) 22223);

        componentConfig.put("jms", jms);
    }

    private static void getCertificateAndKey() {
        try {
            ZConfig config = ZConfig.load(Common.CLIENT_CERT_FOLDER + "/mycert.secret");
            System.out.println("Found existing certificate. Reusing it...");
            String pubKey = config.getValue("curve/public-key");
            String secret = config.getValue("curve/secret-key");
            String id = config.getValue("metadata/id");

            cert = new ZCert(pubKey, secret);
            cert.setMeta("id", id);
        } catch (Exception ignored) {
            System.out.println("Exception 1");
            return;
        }

        try {
            serverKey = ZMQ.Curve.z85Decode(
                    ZConfig.load(Common.SERVER_CERT_FOLDER + "/server.pubkey").getValue("curve/public-key"));
        } catch (Exception ignored) {
            System.out.println("Exception 2");
        }
    }

    private static void socketsInit() {
        JSONObject jmsConfig = (JSONObject) componentConfig.get("jms");
        String ip = (String) jmsConfig.get("ip");
        long publisherPort = (Long) jmsConfig.get("publisher_port");
        jmsPublisherAddress = DistributedZMQ.formAddress(ip, publisherPort);
        long sinkPort = (Long) jmsConfig.get("sink_port");
        jmsSinkAddress = DistributedZMQ.formAddress(ip, sinkPort);

        subscriberContext = new ZContext();
        getCertificateAndKey();
    }

    private static void runSubscriber() {
        ZMQ.Socket subscriber = subscriberContext.createSocket(SocketType.SUB);
        subscriber.subscribe("".getBytes());
        subscriber.setCurvePublicKey(cert.getPublicKey());
        subscriber.setCurveSecretKey(cert.getSecretKey());
        subscriber.setCurveServerKey(serverKey);
        subscriber.connect(jmsPublisherAddress);

        System.out.println("Subscriber running...");
        while(true) {
            try {
                String command = subscriber.recvStr();

                System.out.println("Received rule...");
                runCommand(command);
            } catch (ZMQException | ZError.CtxTerminatedException ignored) {
                System.out.println("Context terminated");
                return;
            } catch (IOException ignored) {
                System.out.println("Couldn't run requested command");
                return;
            }
        }
    }

    public static void runCommand(String command) throws IOException {
        JSONParser parser = new JSONParser();

        try {
            JSONObject ruleJSON = (JSONObject) parser.parse(command);
            System.out.println("    -> " + IpTables.formRuleFromJSON(ruleJSON));
        } catch (Exception ignored) { }

        /*
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", command);

        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        while(line != null) {
            System.out.println(line);
            line = reader.readLine();
        }
         */
    }

    private static void stop() {
        System.out.println("Stopping...");

        subscriberContext.destroy();
    }

    public static void sendHelloWorld() {
        ZContext context = new ZContext();
        ZMQ.Socket sink = context.createSocket(SocketType.PUSH);
        sink.connect(jmsSinkAddress);

        System.out.println("Sending command...");

        sink.send("echo \"Hello world!\"");
    }

    public static void sendHello() {
        ZContext context = new ZContext();
        ZMQ.Socket sink = context.createSocket(SocketType.PUSH);
        sink.connect(jmsSinkAddress);

        System.out.println("Sending command...");

        sink.send("echo \"Hello to you too!\"");
    }

    @SuppressWarnings("unchecked")
    public static void sendRuleToJMS() {
        ZContext context = new ZContext();
        ZMQ.Socket sink = context.createSocket(SocketType.PUSH);
        sink.setCurvePublicKey(cert.getPublicKey());
        sink.setCurveSecretKey(cert.getSecretKey());
        sink.setCurveServerKey(serverKey);
        sink.connect(jmsSinkAddress);

        JSONObject ruleOptions = new JSONObject();
        ruleOptions.put("chain", "INPUT");
        ruleOptions.put("srcIp", "189.34.94.2");
        ruleOptions.put("action", "DROP");

        System.out.println("Caught attacker! Sending rule to JMS...");
        JMSInterface jmsInterface = new JMSInterface(componentConfig, cert, serverKey);
        jmsInterface.start();
        jmsInterface.sendRuleToSink(ruleOptions);
    }

    public static void main(String[] args) {
        componentConfigInit();
        socketsInit();
        Runtime.getRuntime().addShutdownHook(new Thread(RulePropagationTest::stop));

        new Thread(RulePropagationTest::runSubscriber).start();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }

        //sendHelloWorld();
        //sendHello();
        sendRuleToJMS();
    }
}
