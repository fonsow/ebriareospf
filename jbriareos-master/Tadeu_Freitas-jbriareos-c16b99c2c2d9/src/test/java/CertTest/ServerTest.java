package CertTest;

import core.DistributedZMQ;
import org.zeromq.*;
import utils.Common;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ServerTest {
    private static final String CERT_FOLDER = "./.curve";
    private static final String CERT_LOCAL = ".curve";

    private static final String ADDRESS = DistributedZMQ.formAddress("*", 11336);
    private static final String SINK_ADDRESS = DistributedZMQ.formAddress("*", 11337);

    private static ZContext context;
    private static ZCert cert;

    public static void main(String[] args) {
        context = new ZContext();

        ZAuth auth = new ZAuth(context);
        auth.setVerbose(true);
        auth.configureCurve(CERT_LOCAL);

        cert = new ZCert();
        System.out.println("Server PubKey: " + cert.getPublicKeyAsZ85());

        new Thread(ServerTest::run).start();
        new Thread(ServerTest::runSink).start();
    }

    private static void run() {
        System.out.println("Binding rep");

        ZMQ.Socket rep = context.createSocket(SocketType.REP);
        rep.bind(ADDRESS);

        while (true) {
            byte[] certificate = rep.recv();

            try {
                OutputStream os = new FileOutputStream(CERT_FOLDER + "/client.pub");

                os.write(certificate);
                System.out.println("Added a certificate from the client");

                os.close();
                rep.send(cert.getPublicKeyAsZ85());
            } catch (IOException e) {
                e.printStackTrace();
                rep.send("Server error");
            }
        }
    }

    private static void runSink() {
        System.out.println("Binding sink");

        ZMQ.Socket sink = context.createSocket(SocketType.PULL);
        sink.setCurveServer(true);
        sink.setCurveSecretKey(cert.getSecretKey());
        sink.bind(SINK_ADDRESS);

        while (true) {
            System.out.println("\nWaiting messages...");
            System.out.println(sink.recvStr());
        }
    }
}
