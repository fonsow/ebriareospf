package core;

import engines.Engine;
import engines.WorkerEngine;
import nemo.it.unipr.netsec.ipstack.ip4.Ip4Packet;
import nemo.it.unipr.netsec.ipstack.ip4.IpAddress;
import nemo.it.unipr.netsec.ipstack.ip4.SocketAddress;
import nemo.it.unipr.netsec.ipstack.tcp.TcpPacket;
import org.json.simple.JSONObject;
import org.zeromq.*;
import utils.BPacket;
import utils.Common;
import utils.Loader;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

public class DistributedZMQ {
    public static class Client {
        public UUID id;
        ZContext context;
        String brokerAddress;
        ZMQ.Socket connection;

        public Client(UUID id) {
            // Init sockets
            JSONObject brokerConfig = (JSONObject) Loader.importZClientConfig().get("broker");
            this.brokerAddress = formAddress((String) brokerConfig.get("ip"), (Long) brokerConfig.get("port"));

            this.id = id;
            this.context = new ZContext();
            this.connection = context.createSocket(SocketType.DEALER);
            this.connection.setIdentity(id.toString().getBytes());

            // Can only queue 500,000 messages
            // To prevent mem leaks when trying to run distributed and no broker exists
            // this.connection.setHWM(500000);
        }

        public void start() {
            System.out.println("Connecting to ZBroker: " + this.brokerAddress);
            this.connection.connect(this.brokerAddress);
        }

        public void stop() {
            System.out.println("Stopping ZClient...");

            this.connection.close();
            this.context.destroy();
        }

        public void process(byte[] data, String extraParam) {
            try {
                this.connection.send(data, ZMQ.SNDMORE);
                this.connection.send(extraParam);
            } catch (RuntimeException ignored) {
            }
        }
    }

    public static class Worker {
        public static final String READY_MSG = "WORKER READY";

        public Engine engine = null;
        public UUID id;
        ZContext context;
        String brokerAddress;
        ZMQ.Socket connection;
        ZCert cert;

        byte[] serverKey;

        Thread t1;

        public Worker(UUID id) {
            JSONObject workerConfig = Loader.importZWorkerConfig();
            JSONObject brokerConfig = (JSONObject) workerConfig.get("broker");
            String ip = (String) brokerConfig.get("ip");
            long port = (Long) brokerConfig.get("port");

            try {
                ZConfig config = ZConfig.load(Common.CLUSTER_CERT_FOLDER + "/cluster.secret");
                System.out.println("Found existing certificate...");
                String pubKey = config.getValue("curve/public-key");
                String secret = config.getValue("curve/secret-key");
                String certId = config.getValue("metadata/id");

                this.cert = new ZCert(pubKey, secret);
                this.cert.setMeta("id", certId);
            } catch (Exception ignored) {
                System.out.println("No previous certificate found...");
                System.out.println("Not connecting to the JMS and using local configurations...");
                this.engine = new WorkerEngine(workerConfig);
            }

            // If the engine was not initialized above,
            // the worker has a certificate and may communicate with the JMS.
            if (this.engine == null) {
                try {
                    this.serverKey = ZMQ.Curve.z85Decode(
                            ZConfig.load(Common.SERVER_CERT_FOLDER + "/server.pubkey").getValue("curve/public-key"));

                    JMSInterface jmsItf;
                    if (workerConfig.get("jms") != null) {
                        jmsItf = new JMSInterface(workerConfig, this.cert, this.serverKey);
                        if (jmsItf.ping() && jmsItf.checkCertInJMS()) {
                            this.engine = new WorkerEngine(workerConfig, jmsItf);
                        } else {
                            System.out.println("Couldn't connect to the JMS...");
                            System.out.println("Trying to continue using the local configurations...");
                            this.engine = new WorkerEngine(workerConfig);
                        }
                    } else {
                        System.out.println("No JMS information present in the configuration file...");
                        System.out.println("Trying to continue using the local configurations...");
                        this.engine = new WorkerEngine(workerConfig);
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    System.out.println("Couldn't read the JMS' key. Trying to continue using the local configurations...");

                    this.engine = new WorkerEngine(workerConfig);
                }
            }

            // this.engine = new WorkerEngine(workerConfig, new JMSInterface(workerConfig, cert, serverKey));
            this.id = id;
            this.context = new ZContext();
            this.brokerAddress = formAddress(ip, port);
            this.connection = this.context.createSocket(SocketType.REQ);
            this.connection.setLinger(0);
            this.connection.setIdentity(id.toString().getBytes());
        }

        public void start() {
            this.engine.start();
            this.connection.connect(this.brokerAddress);

            t1 = new Thread(this::run);
            t1.start();
        }

        public void stop() {
            System.out.println("Stopping ZWorker...");

            try {
                t1.interrupt();
                t1.join();
            } catch (InterruptedException ignored) {
            }

            this.connection.close();
            this.context.destroy();
            this.engine.stop();
        }

        public void run() {
            this.connection.send(Worker.READY_MSG);

            while (!Thread.interrupted()) {
                try {
                    String address = this.connection.recvStr();
                    byte[] data = this.connection.recv();
                    String extraParam = this.connection.recvStr();

                    Engine.ProcessResult result = this.engine.process(data, extraParam);

                    System.out.println("Result: " + result.packet.verdict);
                    this.connection.send(address, ZMQ.SNDMORE);
                    if (result.packet.verdict == Common.Verdict.Drop) {
                        this.connection.send(String.valueOf(result.packet.verdict), ZMQ.SNDMORE);
                        this.connection.send(data);
                    } else {
                        this.connection.send(String.valueOf(result.packet.verdict));
                    }
                } catch (RuntimeException ignored) {
                    System.out.println("Context terminated");
                    return;
                }
            }
        }
    }

    public static class Broker {
        ZContext context;
        ZMQ.Socket frontend;
        ZMQ.Socket backend;
        ZMQ.Poller poller;
        String frontendAddress;
        String backendAddress;
        LinkedList<UUID> availableWorkers;
        WorkerManager workerManager;
        HashMap<String, ConnectionInfo> tcb;    // Transmission Control Block

        Thread t1;

        public Broker() {
            // Init sockets
            JSONObject zBrokerConfig = Loader.importZBrokerConfig();

            JSONObject frontendConfig = (JSONObject) zBrokerConfig.get("frontend");
            JSONObject backendConfig = (JSONObject) zBrokerConfig.get("backend");

            String frontendIp = (String) frontendConfig.get("ip");
            long frontendPort = (Long) frontendConfig.get("port");

            String backendIp = (String) backendConfig.get("ip");
            long backendPort = (Long) backendConfig.get("port");

            if (frontendIp == null || frontendIp.isEmpty())
                frontendIp = "*";
            if (backendIp == null || backendIp.isEmpty())
                backendIp = "*";

            this.frontendAddress = formAddress(frontendIp, frontendPort);
            this.backendAddress = formAddress(backendIp, backendPort);

            this.context = new ZContext();
            this.frontend = this.context.createSocket(SocketType.ROUTER);
            this.frontend.setLinger(0);
            this.backend = this.context.createSocket(SocketType.ROUTER);
            this.backend.setLinger(0);

            this.poller = null;
            this.availableWorkers = new LinkedList<>();
            this.workerManager = new WorkerManager();
        }

        public void start() {
            this.workerManager.start();

            System.out.println("Starting ZBroker");
            System.out.println("Frontend address: " + this.frontendAddress);
            System.out.println("Backend address: " + this.backendAddress);

            this.frontend.bind(this.frontendAddress);
            this.backend.bind(this.backendAddress);

            this.poller = this.context.createPoller(2);
            this.poller.register(backend, ZMQ.Poller.POLLIN);
            this.poller.register(frontend, ZMQ.Poller.POLLIN);

            t1 = new Thread(this::run);
            t1.start();
        }

        public void stop() {
            this.workerManager.stop();

            System.out.println("Stopping ZBroker...");
            try {
                t1.interrupt();
                t1.join();
            } catch (InterruptedException ignored) {
            } finally {
                this.frontend.close();
                this.backend.close();
                this.poller.close();
                this.context.destroy();
            }
        }

        private static class ConnectionInfo {
            UUID workerId;
            private boolean receivedFin;

            public ConnectionInfo(UUID workerId) {
                this.workerId = workerId;
                this.receivedFin = false;
            }

            void receivedFin() {
                receivedFin = true;
            }
        }

        /**
         * Checks if a captured packet is TCP, and fetches a worker to process it.
         *
         * If the packet is TCP, and the connection is not new,
         * then it is processed by the same worker that processed it before.
         *
         * If the packet is the beginning of a new TCP connection or isn't a TCP packet,
         * it is processed by a worker chosen following a LRU order.
         *
         * @param data The packet's data
         * @return The worker for which the data will be sent
         */
        private UUID getWorkerIdForAddress(byte[] data) {
            // return this.availableWorkers.pop();

            String address;
            boolean hasFin;
            try {
                TcpPacket pkt = TcpPacket.parseTcpPacket(Ip4Packet.parseIp4Packet(data));

                hasFin = pkt.hasFin();

                String ip = ((IpAddress)pkt.getSourceAddress()).toInetAddress().getHostAddress();
                int port = ((SocketAddress)pkt.getSourceAddress()).getPort();
                address = ip + ":" + port;
            } catch (Exception ignored) {
                // It isn't a TCP packet, so...
                // ... distribute the data following a LRU order.

                return this.availableWorkers.pop();
            }

            // It is a TCP packet, so...
            // ... distribute the data to the worker handling this specific TCP connection.
            ConnectionInfo info = this.tcb.get(address);
            UUID workerId;
            if (info == null) {                             // This connection has no previous data recorded, so...
                workerId = this.availableWorkers.pop();     // ... distribute the packet following a LRU order.

                this.tcb.put(address, new ConnectionInfo(workerId));
            } else {
                workerId = info.workerId;
                this.availableWorkers.remove(workerId);

                if (info.receivedFin)
                    // This TCP connection has previously received a FIN,
                    // which means this packet is an ACK terminating the connection,
                    // which can be removed from the TCB

                    this.tcb.remove(address);
                else if (hasFin) {
                    // This TCP connection has received a FIN,
                    // which means that the next packet will trigger the condition above

                    info.receivedFin();
                    this.tcb.put(address, info);
                }
            }

            return workerId;
        }

        long startTime;

        public void run() {
            while (!Thread.interrupted()) {
                try {
                    this.poller.poll(2000);
                    // Poll the backend
                    if (this.poller.pollin(0)) {
                        UUID workerId = UUID.fromString(this.backend.recvStr());
                        this.availableWorkers.addLast(workerId);

                        this.backend.recvStr(); // Empty
                        String clientMsg = this.backend.recvStr();
                        if (clientMsg.equals(Worker.READY_MSG)) {
                            System.out.println("New worker: " + workerId);
                            this.workerManager.alreadyRequested = false;
                            this.workerManager.numWorkers++;
                        } else {
                            String result = this.backend.recvStr();
                            if (result.equals(String.valueOf(Common.Verdict.Drop))) {
                                byte[] data = this.backend.recv();
                                System.out.println("A packet was dropped:");
                                try {
                                    System.out.println(new BPacket(data));
                                } catch (Exception ignored) {}
                            }

                            this.workerManager.numTasks++;
                            if (this.workerManager.numTasks!=0 && this.workerManager.numTasks%125000==0) {
                                System.out.println(System.currentTimeMillis() - startTime);
                                this.workerManager.printMetrics();
                            }
                        }
                    }

                    if (!this.availableWorkers.isEmpty()) {
                        // Poll the frontend
                        if (this.poller.pollin(1)) {
                            // This information is received from a Client,
                            // which is invoked the JdsInterface,
                            // after the JHC receives a packet to analise
                            String clientId = this.frontend.recvStr();
                            byte[] data = this.frontend.recv();
                            String extraParam = this.frontend.recvStr();

                            UUID workerId = getWorkerIdForAddress(data);

                            if (this.workerManager.numTasks == 0)
                                startTime = System.currentTimeMillis();

                            // Broker the information
                            this.backend.send(workerId.toString(), ZMQ.SNDMORE);
                            this.backend.send("", ZMQ.SNDMORE);
                            this.backend.send(clientId, ZMQ.SNDMORE);
                            this.backend.send(data, ZMQ.SNDMORE);
                            this.backend.send(extraParam);
                        }
                    }
                } catch (RuntimeException ignored) {
                    return;
                }
            }
        }
    }

    public static String formAddress(String ip, long port) {
        return "tcp://" + ip + ":" + port;
    }
}
