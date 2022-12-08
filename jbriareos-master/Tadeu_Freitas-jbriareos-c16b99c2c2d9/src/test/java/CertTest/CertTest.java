package CertTest;

import org.zeromq.*;

import java.io.IOException;

public class CertTest {
    private static final String CERTIFICATE_FOLDER=".curve";

    public static void main(String[] args) throws IOException {
        ZContext serverContext = new ZContext();

        ZAuth serverAuth = new ZAuth(serverContext);
        serverAuth.setVerbose(true);
        serverAuth.configureCurve(CERTIFICATE_FOLDER);

        ZCert server_cert = new ZCert();

        ZMQ.Socket sinkPull = serverContext.createSocket(SocketType.PULL);
        sinkPull.setZAPDomain("global".getBytes());
        sinkPull.setCurveServer(true);
        sinkPull.setCurvePublicKey(server_cert.getPublicKey());
        sinkPull.setCurveSecretKey(server_cert.getSecretKey());
        sinkPull.bind("tcp://*:9000");

        // -------------------------------------------------------------------

        ZContext clientContext = new ZContext();

        ZCert client_cert = new ZCert();
        client_cert.setMeta("name", "Client test certificate");
        client_cert.savePublic(CERTIFICATE_FOLDER+"/testcert.pub");

        ZMQ.Socket sinkPush = clientContext.createSocket(SocketType.PUSH);
        sinkPush.setCurvePublicKey(client_cert.getPublicKey());
        sinkPush.setCurveSecretKey(client_cert.getSecretKey());
        sinkPush.setCurveServerKey(server_cert.getPublicKey());
        sinkPush.connect("tcp://127.0.0.1:9000");

        // -------------------------------------------------------------------

        sinkPush.send("Hello world!");
        String message = sinkPull.recvStr();

        System.out.println(message);
    }
}
