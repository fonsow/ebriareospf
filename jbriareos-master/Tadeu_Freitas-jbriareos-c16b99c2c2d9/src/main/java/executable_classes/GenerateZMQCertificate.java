package executable_classes;

import org.zeromq.ZCert;
import org.zeromq.ZConfig;
import utils.Common;

import java.util.UUID;

public class GenerateZMQCertificate {
    private static void useHelp() {
        System.out.println("Please add a single option to the command:\n");
        System.out.println("    $ java -jar GenerateZMQCertificate.jar [option]\n");
        System.out.println("    - Options -");
        System.out.println("    client: Generate JHC certificate");
        System.out.println("    server: Generate JMS certificate");
        System.out.println("    cluster: Generate ZCluster certificate");
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            useHelp();
            return;
        }

        String pathPub, pathSecret;

        switch (args[0]) {
            case "client":
                pathPub = Common.CLIENT_CERT_FOLDER + "/mycert.pubkey";
                pathSecret = Common.CLIENT_CERT_FOLDER + "/mycert.secret";
                break;
            case "server":
                pathPub = Common.SERVER_CERT_FOLDER + "/server.pubkey";
                pathSecret = Common.SERVER_CERT_FOLDER + "/server.secret";
                break;
            case "cluster":
                pathPub = Common.CLUSTER_CERT_FOLDER + "/cluster.pubkey";
                pathSecret = Common.CLUSTER_CERT_FOLDER + "/cluster.secret";
                break;
            default:
                useHelp();
                return;
        }

        String pubKey, secret, id;

        try {
            ZConfig config = ZConfig.load(pathSecret);
            System.out.println("Found existing certificate...");
            pubKey = config.getValue("curve/public-key");
            secret = config.getValue("curve/secret-key");
            id = config.getValue("metadata/id");
        } catch (Exception ignored) {
            System.out.println("No previous certificate found. Creating new certificate...");
            ZCert cert = new ZCert();

            try {
                id = String.valueOf(UUID.randomUUID());

                cert.setMeta("id", id);
                cert.savePublic(pathPub);
                cert.saveSecret(pathSecret);

                pubKey = cert.getPublicKeyAsZ85();
                secret = cert.getSecretKeyAsZ85();
            } catch (Exception ignored2) {
                System.out.println("Couldn't save new certificate to disk...\n");
                System.out.println("No certificate was created...");
                return;
            }
        }

        System.out.println();
        System.out.println("    PubKey: " + pubKey);
        System.out.println("    Secret: " + secret);
        System.out.println("        ID: " + id);

        if (args[0].equals("server")) {
            System.out.println();
            System.out.println("If you haven't already, please deploy this certificate to all the JHCs and ZClusters");
        } else {
            System.out.println();
            System.out.println("If you haven't already, please deploy this component's public certificate to the JMS.");
            System.out.println("On the JMS, change this certificate's filename to:");
            System.out.println("    " + id + ".pubkey");
        }
    }
}
