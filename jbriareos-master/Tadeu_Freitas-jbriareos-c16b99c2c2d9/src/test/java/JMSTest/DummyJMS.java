package JMSTest;

import core.DistributedZMQ;
import org.json.simple.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;


public class DummyJMS {
    static JSONObject componentConfig = new JSONObject();
    static String sinkAddress;
    static String publisherAddress;

    static ZContext context;
    static ZMQ.Socket sinkPull;
    static ZMQ.Socket publisher;

    @SuppressWarnings("unchecked")
    private static void componentConfigInit() {
        JSONObject jms = new JSONObject();
        jms.put("sink_port", (long) 22222);
        jms.put("publisher_port", (long) 22223);

        componentConfig.put("jms", jms);
    }

    private static void stop() {
        System.out.println("Stopping...");

        context.destroy();
    }

    private static void socketsInit() {
        JSONObject jmsConfig = (JSONObject) componentConfig.get("jms");
        String ip = (String) jmsConfig.get("ip");
        if (ip == null || ip.isEmpty())
            ip = "*";

        long publisherPort = (Long) jmsConfig.get("publisher_port");
        publisherAddress = DistributedZMQ.formAddress(ip, publisherPort);
        long sinkPort = (Long) jmsConfig.get("sink_port");
        sinkAddress = DistributedZMQ.formAddress(ip, sinkPort);

        context = new ZContext();
        sinkPull = context.createSocket(SocketType.PULL);
        publisher = context.createSocket(SocketType.PUB);

        sinkPull.bind(sinkAddress);
        publisher.bind(publisherAddress);
    }

    public static void main(String[] args) {
        componentConfigInit();
        socketsInit();
        Runtime.getRuntime().addShutdownHook(new Thread(DummyJMS::stop));

        while (true) {
            try {
                String message = sinkPull.recvStr();

                System.out.println("Received " + message);
                System.out.println("Sending to subscribers...");

                publisher.send(message);
            } catch (RuntimeException e) {
                return;
            }
        }
    }
}
