package core;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.zeromq.SocketType;
import org.zeromq.ZCert;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import utils.BPacket;
import utils.IpTables;

import java.io.CharArrayWriter;
import java.io.IOException;

public class JMSInterface {
    String jmsAddress;
    String jmsSinkAddress;
    String jmsUnsecureAddress;

    private static final int TIMEOUT = 10000;       // 10 secs

    ZCert cert;
    ZContext context;
    ZMQ.Socket req;
    ZMQ.Socket unsecure;
    ZMQ.Socket sinkPush;

    ZMQ.Poller poller;

    public JMSInterface(JSONObject componentConfig, ZCert cert) {
        JSONObject config = (JSONObject) componentConfig.get("jms");
        String ip = (String) config.get("ip");
        long registerPort = (Long) config.get("unsecure_port");
        this.jmsUnsecureAddress = DistributedZMQ.formAddress(ip, registerPort);

        this.cert = cert;

        this.context = new ZContext();
    }

    public JMSInterface(JSONObject componentConfig, ZCert cert, byte[] serverKey) {
        JSONObject config = (JSONObject) componentConfig.get("jms");
        String ip = (String) config.get("ip");
        long port = (Long) config.get("port");
        long sinkPort = (Long) config.get("sink_port");
        long registerPort = (Long) config.get("unsecure_port");
        this.jmsAddress = DistributedZMQ.formAddress(ip, port);
        this.jmsSinkAddress = DistributedZMQ.formAddress(ip, sinkPort);
        this.jmsUnsecureAddress = DistributedZMQ.formAddress(ip, registerPort);

        this.cert = cert;

        this.context = new ZContext();

        this.req = context.createSocket(SocketType.REQ);
        this.req.setCurvePublicKey(this.cert.getPublicKey());
        this.req.setCurveSecretKey(this.cert.getSecretKey());
        this.req.setCurveServerKey(serverKey);

        this.sinkPush = context.createSocket(SocketType.PUSH);
        this.sinkPush.setCurvePublicKey(this.cert.getPublicKey());
        this.sinkPush.setCurveSecretKey(this.cert.getSecretKey());
        this.sinkPush.setCurveServerKey(serverKey);
    }

    public void start() {
        this.req.connect(this.jmsAddress);
        this.sinkPush.connect(this.jmsSinkAddress);
    }

    public void stop() {
        this.req.close();
        this.sinkPush.close();
        this.context.destroy();
    }

    public boolean ping() {
        boolean ok = true;
        this.unsecure = context.createSocket(SocketType.REQ);
        this.unsecure.connect(this.jmsUnsecureAddress);

        poller = this.context.createPoller(1);
        poller.register(this.unsecure, ZMQ.Poller.POLLIN);

        this.unsecure.send("PING");
        if (poller.poll(TIMEOUT) != 1)
            ok = false;

        this.unsecure.close();
        return ok;
    }

    public JSONObject requestPipelines(JSONArray request) {
        this.req.send(request.toJSONString());
        String responseString = this.req.recvStr();

        try {
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(responseString);
        } catch (ParseException ignored) {
            System.out.println(responseString);
            return new JSONObject();
        }
    }

    public boolean checkCertInJMS() {
        this.unsecure = context.createSocket(SocketType.REQ);
        this.unsecure.connect(this.jmsUnsecureAddress);

        System.out.println("Checking if the certificate is present on the JMS...");
        this.unsecure.send("CHECK KEY", ZMQ.SNDMORE);
        this.unsecure.send(this.cert.getMeta("id"));

        String result = this.unsecure.recvStr();
        if (!result.equals("OK")) {
            System.out.println(result);
            System.out.println("The " + this.cert.getMeta("id") + " certificate wasn't found on the JMS...");
            return false;
        }

        this.unsecure.close();
        return true;
    }

    /*
    public boolean sendCertToServer() {
        boolean ok = true;

        this.unsecure = context.createSocket(SocketType.REQ);
        this.unsecure.connect(this.jmsUnsecureAddress);

        System.out.println("Sending certificate to the JMS");
        this.unsecure.send("PUT KEY", ZMQ.SNDMORE);
        this.unsecure.send(this.cert.getMeta("id"), ZMQ.SNDMORE);

        try {
            CharArrayWriter writer = new CharArrayWriter();
            this.cert.savePublic(writer);
            this.unsecure.send(new String(writer.toCharArray()));
        } catch (IOException ioException) {
            ioException.printStackTrace();
            System.out.println("Couldn't send certificate to the server...");
            System.out.println("Continuing without connecting to the JMS...");
            ok = false;
        }

        String result = this.unsecure.recvStr();
        if (!result.equals("OK")) {
            System.out.println(result);
            System.out.println("Couldn't save certificate in the server...");
            System.out.println("Continuing without connecting to the JMS...");
            ok = false;
        }

        this.unsecure.close();
        return ok;
    }
    */

    public void loadRules() {
        this.req.send("GET RULES");
        String response = this.req.recvStr();

        try {
            JSONParser parser = new JSONParser();
            JSONArray rules = (JSONArray) parser.parse(response);

            IpTables.createRulesFromJSON(rules);
        } catch (ParseException ignored) {
            System.out.println(response);
            System.out.println("Couldn't load any the rules stored on the JMS");
        }
    }

    public void blockIPAddress(BPacket packet) {
        this.blockIPAddress( packet.getSrcIp(), null,
                null, null, null, null);
    }

    public void blockIPAddress(String srcAddress, String srcPort,
                               String dstAddress, String dstPort,
                               String itf, String protocol) {
        this.sendRuleToSink(null,"INPUT", protocol, itf, srcAddress, srcPort,
                dstAddress, dstPort, "DROP");
    }

    @SuppressWarnings("unused")
    public void redirectPackages(String srcAddress, String srcPort,
                                 String dstAddress, String dstPort,
                                 String itf, String protocol, String redirDst) {
        String action = "DNAT --to-destination " + redirDst;

        this.sendRuleToSink("nat","PREROUTING", protocol, itf, srcAddress, srcPort,
                dstAddress, dstPort, action);
    }

    public void sendRuleToSink(JSONObject jsonObject) {
        System.out.println("Sending rule to JMS...");
        sinkPush.send(jsonObject.toJSONString());
    }

    public void sendRuleToSink(String table, String chain, String protocol, String itf,
                                       String srcIp, String srcPort, String dstIp, String dstPort,
                                       String action) {
        JSONObject object = IpTables.formJSONfromRuleArgs(table, chain, protocol, itf,
                srcIp, srcPort, dstIp, dstPort, action);

        this.sendRuleToSink(object);
    }
}
