package CertTest;

import core.DistributedZMQ;
import org.zeromq.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClientTest {
    private static final String CERT_LOCAL = ".mycert/testcert.pub";

    private static final String address = DistributedZMQ.formAddress("127.0.0.1", 11336);
    private static final String sinkAddress = DistributedZMQ.formAddress("127.0.0.1", 11337);

    public static void main(String[] args) throws IOException {
        ZContext context = new ZContext();

        new ZAuth(context);

        System.out.println("Building certificate");
        ZCert cert = new ZCert();
        cert.savePublic(CERT_LOCAL);

        System.out.println("Connecting rep");
        ZMQ.Socket rep = context.createSocket(SocketType.REQ);
        rep.connect(address);

        System.out.println("Connecting sink");
        ZMQ.Socket sink = context.createSocket(SocketType.PUSH);

        rep.send(Files.readAllBytes(Paths.get(CERT_LOCAL)));
        String serverKey = rep.recvStr();
        System.out.println("Server PubKey: " + serverKey);

        sink.setCurveServerKey(serverKey.getBytes());
        sink.setCurvePublicKey(cert.getPublicKey());
        sink.setCurveSecretKey(cert.getSecretKey());

        sink.connect(sinkAddress);

        sink.send("Hello World!");
    }
}
