package components;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.zeromq.*;
import utils.Common;
import utils.IpTables;
import utils.Loader;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JMS {
    private static final String RULES_PATH = "./src/config/rules.json";
    private static final String CERT_FOLDER = "./.curve";

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    ZContext context;
    private ZAuth auth;
    private ZCert cert;
    private ZMQ.Socket unsecure;
    private ZMQ.Socket rep;
    private ZMQ.Socket sink;
    private ZMQ.Socket publisher;
    String registerAddress;
    String address;
    String sinkAddress;
    String publisherAddress;

    Thread t1, t2, t3;

    public JMS() {
        JSONObject jmsConfig = Loader.importJmsConfig();

        String ip = (String) jmsConfig.get("ip");
        long port = (Long) jmsConfig.get("port");
        long sinkPort = (Long) jmsConfig.get("sink_port");
        long publisherPort = (Long) jmsConfig.get("publisher_port");
        long registerPort = (Long) jmsConfig.get("unsecure_port");

        if (ip == null || ip.isEmpty())
            ip = "*";

        this.address = formAddress(ip, port);
        this.sinkAddress = formAddress(ip, sinkPort);
        this.publisherAddress = formAddress(ip, publisherPort);
        this.registerAddress = formAddress(ip, registerPort);

        this.context = new ZContext();

        this.auth = new ZAuth(this.context);
        this.auth.setVerbose(true);
        this.auth.configureCurve(CERT_FOLDER);

        try {
            ZConfig config = ZConfig.load(Common.SERVER_CERT_FOLDER + "/server.secret");
            System.out.println("Found existing certificate. Reusing it...");
            String pubKey = config.getValue("curve/public-key");
            String secret = config.getValue("curve/secret-key");

            this.cert = new ZCert(pubKey, secret);
        } catch (Exception ignored) {
            System.out.println("No previous certificate found. Creating new certificate...");
            this.cert = new ZCert();

            try {
                this.cert.savePublic(Common.SERVER_CERT_FOLDER + "/server.pubkey");
                this.cert.saveSecret(Common.SERVER_CERT_FOLDER + "/server.secret");
            } catch (Exception ignored2) {
                System.out.println("Couldn't save new certificate to disk...");
            }
        } finally {
            System.out.println("\nServer PubKey: " + this.cert.getPublicKeyAsZ85() + "\n");
        }

        System.out.println("Binding register socket");
        this.unsecure = this.context.createSocket(SocketType.REP);

        System.out.println("Binding sink");
        this.sink = this.context.createSocket(SocketType.PULL);
        this.sink.setCurveServer(true);
        this.sink.setCurveSecretKey(this.cert.getSecretKey());

        System.out.println("Binding publisher");
        this.publisher = this.context.createSocket(SocketType.PUB);
        this.publisher.setCurveServer(true);
        this.publisher.setCurveSecretKey(this.cert.getSecretKey());

        System.out.println("Binding responder");
        this.rep = this.context.createSocket(SocketType.REP);
        this.rep.setCurveServer(true);
        this.rep.setCurveSecretKey(this.cert.getSecretKey());

        System.out.println();
    }

    public void start() {
        this.unsecure.bind(this.registerAddress);
        this.sink.bind(this.sinkAddress);
        this.publisher.bind(this.publisherAddress);
        this.rep.bind(this.address);

        // new Thread(this::runRegister).start();
        // new Thread(this::run).start();
        // new Thread(this::runSink).start();

        t1 = new Thread(this::runUnsecure);
        t2 = new Thread(this::run);
        t3 = new Thread(this::runSink);

        t1.start();
        t2.start();
        t3.start();
    }

    public void stop() {
        System.out.println("Stopping JMS...");

        try {
            t1.interrupt();
            t2.interrupt();
            t3.interrupt();

            t1.join();
            t2.join();
            t3.join();
        } catch (Exception ignored) {
        }

        this.unsecure.close();
        this.rep.close();
        this.sink.close();
        this.publisher.close();
        this.context.destroy();
    }

    /*------------------------------------------------
    --------   JMS sink/publisher functions   --------
    ------------------------------------------------*/

    @SuppressWarnings("unchecked")
    private void writeRuleToFile(JSONObject object) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(RULES_PATH, "rw")) {
            try (FileLock lock = file.getChannel().lock()) {
                InputStream is = Channels.newInputStream(file.getChannel());

                JSONParser parser = new JSONParser();
                JSONArray array = new JSONArray();
                try {
                    array = (JSONArray) parser.parse(new InputStreamReader(is, StandardCharsets.UTF_8));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParseException ignored) {
                    lock.release();

                    System.out.println("The file rules.json couldn't be parsed as it is supposed to");
                    System.out.println("Please check if the file has a top-level JSON array");
                    return;
                }

                array.add(object);

                file.setLength(0);
                file.write(array.toJSONString().getBytes());
                lock.release();
            }
        } catch (FileNotFoundException e) {
            System.out.println("File rules.json doesn't exist as it is supposed to");
            System.out.println("Please create a rules.json file on src/config, and open an empty JSON array on it");
            System.out.println("Couldn't write a rule to the file:");
            System.out.println("    " + object.toJSONString());
        }
    }

    /**
     * Watches the sink for new rules to share with the hosts/subscribers, and shares them.
     */
    private void runSink() {
        JSONParser parser = new JSONParser();

        while (!Thread.interrupted()) {
            try {
                String jsonString = this.sink.recvStr();

                JSONObject ruleOptions = (JSONObject) parser.parse(jsonString);

                String ruleCommand;
                try {
                    ruleCommand = IpTables.formRuleFromJSON(ruleOptions);
                } catch (Exception ignored) {
                    continue;
                }

                System.out.println("Received rule:");
                System.out.println("    " + ruleCommand);

                Runnable writeRuleToFileRunnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            writeRuleToFile(ruleOptions);
                        } catch (IOException ignored) {
                            // The file is locked, as it is being read
                            // Wait 2 seconds before trying to rewrite it
                            scheduler.schedule(this, 2, TimeUnit.SECONDS);
                        }
                    }
                };

                scheduler.schedule(writeRuleToFileRunnable, 0, TimeUnit.SECONDS);

                System.out.println("Publishing the received rule...\n");
                this.publisher.send(jsonString);
            } catch (RuntimeException ignored) {
                return;
            } catch (ParseException ignored) {
                System.out.println("Couldn't parse the received rule options");
            }
        }
    }

    /*-----------------------------------------------
    ----------   JMS responder functions   ----------
    -----------------------------------------------*/

    private void sendError() {
        this.rep.send("JMS error");
    }

    private void sendUnsecureError() {
        this.unsecure.send("JMS error");
    }

    /**
     * Sends all saved rules to the requester, as they are written on the rules file
     */
    @SuppressWarnings("BusyWait")
    private void sendRules() {
        System.out.println("Distributing rules to a client...");

        while (true) {
            try (RandomAccessFile file = new RandomAccessFile(RULES_PATH, "rw")) {
                try (FileLock lock = file.getChannel().lock()) {
                    InputStream is = Channels.newInputStream(file.getChannel());

                    JSONParser parser = new JSONParser();
                    JSONArray array;

                    try {
                        array = (JSONArray) parser.parse(new InputStreamReader(is, StandardCharsets.UTF_8));
                    } catch (ParseException ignored) {
                        lock.release();
                        file.close();

                        System.out.println("The file rules.json couldn't be parsed as it is supposed to");
                        System.out.println("Please check if the file has a top-level JSON array");

                        sendError();
                        return;
                    } catch (IOException e) {
                        lock.release();
                        file.close();

                        e.printStackTrace();

                        sendError();
                        continue;
                    }

                    lock.release();

                    this.rep.send(array.toJSONString());
                } catch (Exception ignored) {
                    // The file is locked, as it is being written.
                    // Wait 2 seconds before trying to read it again.
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored2) {
                        sendError();
                    }
                }

                file.close();
                return;
            } catch (RuntimeException re) {
                System.out.println("Couldn't deploy saved rules...");
                re.printStackTrace();
                return;
            } catch (FileNotFoundException ignored) {
                System.out.println("File rules.json doesn't exist as it is supposed to");
                System.out.println("Please create a rules.json file on src/config and open an empty JSON array on it");
                sendError();
                return;
            } catch (IOException ie) {
                System.out.println("Couldn't deploy rules onto a host, as it wasn't possible to read the rule's file");
                ie.printStackTrace();
                sendError();
                return;
            }
        }
    }

    /**
     * Sends all the requested pipelines to the requester.
     * The response format is a JSONArray of JSONObjects, so that the pipelines
     * maintain their original sequence.
     *
     * i.e. [{"pipeline1_name":{pipeline1_configs}}, {"pipeline2_name":{pipeline2_configs}}]
     *
     * @param request The requested pipelines
     */
    @SuppressWarnings("unchecked")
    private void sendPipelines(String request) {
        JSONParser parser = new JSONParser();

        try {
            JSONArray pipelines = (JSONArray) parser.parse(request);

            System.out.println("Providing these pipelines:");
            System.out.println("     " + pipelines + "\n");

            JSONObject responseObj = new JSONObject();
            for (Object obj : pipelines) {
                String pipelineName = (String) obj;

                String pipelinePath = Common.PIPELINES_ROOT_PATH + pipelineName + ".json";
                JSONObject pipelineJSON = Loader.loadPipelineJSON(pipelinePath);

                if (pipelineJSON.isEmpty())
                    throw new IOException();

                responseObj.put(pipelineName, pipelineJSON);
            }

            this.rep.send(responseObj.toJSONString());
        } catch (ParseException ignored) {
            this.rep.send("Malformatted pipeline request");
        } catch (IOException ignored) {
            this.rep.send("Couldn't load some requested pipeline");
        }
    }

    /**
     * Responds to requests to share pipeline configs and rules
     */
    private void run() {
        while (!Thread.interrupted()) {
            try {
                String request = this.rep.recvStr();
                if (request.equals("GET RULES"))
                    sendRules();
                else
                    sendPipelines(request);
            } catch (RuntimeException ignored) {
                return;
            }
        }
    }

    /*-----------------------------------------------
    -------   JMS register socket functions   -------
    -----------------------------------------------*/


    /*
     * Places a new client's certificate/public key into the public key pool of the accepted connections
     */
    /*
    private void putKey() {
        String clientPk = this.unsecure.recvStr();
        byte[] certificate = this.unsecure.recv();

        if (!new File(CERT_FOLDER + "/" + clientPk + ".pub").exists()) {
            try {
                OutputStream os = new FileOutputStream(CERT_FOLDER + "/" + clientPk + ".pub");

                os.write(certificate);
                System.out.println("Added a certificate from a client");
                System.out.println();

                os.close();
                this.unsecure.send("OK");
            } catch (IOException e) {
                e.printStackTrace();
                sendUnsecureError();
            }
        } else {
            // This should never happen, but just in case...

            System.out.println("Received connection from known client");
            this.unsecure.send("OK");
        }
    }
    */

    /**
     * Checks if a certain certificate is present in the pool of the accepted connections
     */
    private void checkKey() {
        String certID = this.unsecure.recvStr();

        if (new File(CERT_FOLDER + "/" + certID + ".pubkey").exists()) {
            System.out.println(certID + " certificate found...");
            this.unsecure.send("OK");
        } else {
            System.out.println("No " + certID + " certificate found...");
            this.unsecure.send("ERROR: Not found");
        }
    }

    private void runUnsecure() {
        while (!Thread.interrupted()) {
            try {
                String request = this.unsecure.recvStr();
                switch (request) {
                    /*
                    case "PUT KEY":
                        System.out.println("Received PUT KEY request");
                        putKey();
                        break;
                    */
                    case "PING":
                        System.out.println("Received PING");
                        this.unsecure.send("OK");
                        break;
                    case "CHECK KEY":
                        System.out.println("Received CHECK KEY request");
                        checkKey();
                        break;
                    default:
                        this.unsecure.send("Unknown request: " + request);
                        break;
                }
            } catch (RuntimeException ignored) {
                return;
            }
        }
    }

    private static String formAddress(String ip, long port) {
        return "tcp://" + ip + ":" + port;
    }
}