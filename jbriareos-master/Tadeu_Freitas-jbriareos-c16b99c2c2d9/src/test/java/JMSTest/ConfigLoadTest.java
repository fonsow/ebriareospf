package JMSTest;

import org.zeromq.ZCert;
import org.zeromq.ZConfig;

import java.io.IOException;
import java.util.Map;

public class ConfigLoadTest {
    static String PATH = "./src/test/java/cert.pub";

    public static void main(String[] args) throws IOException {
        try {
            ZConfig.load(PATH);
        } catch (Exception e) {
            System.out.println("No previous certificate found. Creating new certificate...");
            new ZCert().saveSecret(PATH);
        }

        for (Map.Entry<String, String> entry : ZConfig.load(PATH).getValues().entrySet())
            System.out.println(entry.getKey() + ": " + entry.getValue());

        System.out.println();
        System.out.println("Rebuilding new certificate the saved one...");

        ZConfig config = ZConfig.load(PATH);
        String pubKey = config.getValue("curve/public-key");
        String secret = config.getValue("curve/secret-key");

        ZCert cert = new ZCert(pubKey, secret);
        System.out.println("Rebuilt certificate's public key");
        System.out.println("    " + cert.getPublicKeyAsZ85());

        System.out.println("Rebuilt certificate's secret key");
        System.out.println("    " + cert.getSecretKeyAsZ85());
    }
}
