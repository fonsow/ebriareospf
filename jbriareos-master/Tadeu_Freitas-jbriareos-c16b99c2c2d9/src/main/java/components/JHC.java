package components;

import core.JDSInterface;
import core.JMSInterface;
import core.DistributedZMQ;
import core.Interceptor;
import engines.JHCEngine;
import engines.Engine;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.zeromq.*;
import utils.Common;
import utils.IpTables;
import utils.Loader;

import java.io.IOException;
import java.util.UUID;

public class JHC {
    JSONObject config;
    private Interceptor interceptor;
    private UUID id;

    private ZMQ.Socket subscriber;
    ZContext context;
    String jmsPublisherAddress;
    String jmsUnsecureAddress;
    private byte[] serverKey;

    private ZCert cert;
    private boolean connectionOk;

    Thread t1;

    private boolean getCertificate() {
        try {
            ZConfig config = ZConfig.load(Common.CLIENT_CERT_FOLDER + "/mycert.secret");
            System.out.println("Found existing certificate...");

            String pubKey = config.getValue("curve/public-key");
            String secret = config.getValue("curve/secret-key");
            String id = config.getValue("metadata/id");

            this.cert = new ZCert(pubKey, secret);
            this.cert.setMeta("id", id);
        } catch (Exception ignored) {
            System.out.println("No previous certificate found...");
            System.out.println("Please generate a certificate with the GenerateZMQCertificate .jar," +
                    "and place it on the JMS.");

            return false;
        }

        this.id = UUID.fromString(this.cert.getMeta("id"));
        System.out.println("\nMy CertID: " + this.id + "\n");

        return true;
    }

    public JHC() {
        this.config = Loader.importJhcConfig();
        this.context = new ZContext();

        // Initializing the engine, the interceptor and all the interfaces
        Engine engine;
        if (this.getCertificate()) {        // Loading / Creating the certificate
            try {
                this.serverKey = ZMQ.Curve.z85Decode(
                        ZConfig.load(Common.SERVER_CERT_FOLDER + "/server.pubkey").getValue("curve/public-key"));

                JMSInterface jmsItf;
                if (config.get("jms") != null) {
                    jmsItf = new JMSInterface(this.config, this.cert, this.serverKey);

                    this.connectionOk = (jmsItf.ping() && jmsItf.checkCertInJMS());
                    if (this.connectionOk)
                        engine = new JHCEngine(this.config, new JDSInterface(this.id), jmsItf);
                    else {
                        System.out.println("Couldn't connect to the JMS...");
                        System.out.println("Trying to continue using the local configurations...");

                        engine = new JHCEngine(this.config, new JDSInterface(this.id));
                    }
                } else {
                    System.out.println("No JMS information present in the configuration file.");
                    System.out.println("Trying to continue using the local configurations...");

                    this.connectionOk = false;
                    engine = new JHCEngine(this.config, new JDSInterface(this.id));
                }
            } catch (IOException ioException) {
                System.out.println("Couldn't read the JMS' key.");
                System.out.println("Trying to continue using the local configurations...");

                this.connectionOk = false;
                engine = new JHCEngine(this.config, new JDSInterface(this.id));
            }
        } else {
            System.out.println("Trying to continue using the local configurations...");

            this.connectionOk = false;
            engine = new JHCEngine(this.config, new JDSInterface(this.id));
        }

        this.interceptor = new Interceptor(engine);

        JSONObject jmsConfig = (JSONObject) this.config.get("jms");
        String ip = (String) jmsConfig.get("ip");
        long publisherPort = (Long) jmsConfig.get("publisher_port");
        long unsecurePort = (Long) jmsConfig.get("unsecure_port");
        this.jmsPublisherAddress = DistributedZMQ.formAddress(ip, publisherPort);
        this.jmsUnsecureAddress = DistributedZMQ.formAddress(ip, unsecurePort);
    }

    public void start() {
        /*
        try {
            IpTables.initMasquerade();
        } catch (IOException ignored) {
            System.out.println("Couldn't initialize masquerading by running " +
                    "'iptables -t nat -A POSTROUTING -j MASQUERADE'");
        }
         */

        if (this.connectionOk) {
            System.out.println("Connecting to the JMS on " + this.jmsPublisherAddress);
            t1 = new Thread(this::runSubscriber);
            t1.start();
        }

        this.interceptor.start();
        System.out.println("JHC is running");
    }

    public void stop() {
        System.out.println("Stopping JHC...");
        this.interceptor.stop();

        if (this.connectionOk) {
            try {
                t1.interrupt();
                t1.join();
            } catch (Exception ignored) {}

            this.subscriber.close();
        }

        System.out.println("Destroying the context...");
        this.context.destroy();
    }

    private void runSubscriber() {
        this.subscriber = context.createSocket(SocketType.SUB);
        this.subscriber.subscribe("".getBytes());
        this.subscriber.setCurvePublicKey(cert.getPublicKey());
        this.subscriber.setCurveSecretKey(cert.getSecretKey());
        this.subscriber.setCurveServerKey(this.serverKey);
        this.subscriber.connect(jmsPublisherAddress);

        String ruleJsonString = "";
        while(!Thread.interrupted()) {
            try {
                ruleJsonString = this.subscriber.recvStr();
                System.out.println("Received rule...");

                JSONParser parser = new JSONParser();
                JSONObject ruleJSON = (JSONObject) parser.parse(ruleJsonString);

                try {
                    System.out.println("    -> " + IpTables.formRuleFromJSON(ruleJSON));
                    System.out.println("Creating rule locally...");

                    IpTables.createRulesFromJSON(ruleJSON);
                } catch (IOException ignored) {
                    try {
                        System.out.println("Couldn't create a rule:");
                        System.out.println("    " + IpTables.formRuleFromJSON(ruleJSON));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception ignored) { }
            } catch (RuntimeException ignored) {
                System.out.println("Context terminated");
                return;
            } catch (ParseException ignored) {
                System.out.println("Couldn't parse the options received to compose a new rule");
                System.out.println("    " + ruleJsonString);
            }
        }
    }
}
