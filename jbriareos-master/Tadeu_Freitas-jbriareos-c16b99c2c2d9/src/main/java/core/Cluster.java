package core;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.InvocationBuilder;
import org.json.simple.JSONObject;
import org.zeromq.*;
import utils.Common;
import utils.Loader;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class Cluster {
    public static final String DOCKER_IMAGE_NAME = "zworker_java:latest";
    public static final long CPU_QUOTA = 120000;

    public UUID clusterId;
    ZContext context;
    String workerManagerAddress;
    String sinkAddress;
    ZMQ.Socket publisher;
    ZMQ.Socket sink;
    int numInstances;
    DockerClient dockerClient;

    Thread t1;

    public Cluster() {
        JSONObject zClusterConfig = Loader.importZClusterConfig();
        JSONObject workerManagerConfig = (JSONObject) zClusterConfig.get("worker_manager");

        Object ipObj = workerManagerConfig.get("ip");
        String ip = "*";
        if (ipObj != null)
            ip = (String) ipObj;


        long port = (Long) workerManagerConfig.get("port");
        long sinkPort = (Long) workerManagerConfig.get("sink_port");

        this.workerManagerAddress = DistributedZMQ.formAddress(ip, port);
        this.sinkAddress = DistributedZMQ.formAddress(ip, sinkPort);

        this.context = new ZContext();
        this.publisher = this.context.createSocket(SocketType.SUB);
        this.publisher.subscribe("".getBytes());
        this.sink = this.context.createSocket(SocketType.PUSH);

        this.clusterId = UUID.randomUUID();
        this.numInstances = 0;
        this.dockerClient = DockerClientBuilder.getInstance().build();
    }

    private boolean getCertificate() {
        ZCert cert;

        try {
            ZConfig config = ZConfig.load(Common.CLUSTER_CERT_FOLDER + "/cluster.secret");
            System.out.println("Found existing certificate...");

            String pubKey = config.getValue("curve/public-key");
            String secret = config.getValue("curve/secret-key");
            String id = config.getValue("metadata/id");

            cert = new ZCert(pubKey, secret);
            cert.setMeta("id", id);
        } catch (Exception ignored) {
            System.out.println("No previous certificate found...");
            System.out.println("Please generate a certificate with the GenerateZMQCertificate .jar," +
                    "and place it on the JMS.");

            return false;
        }

        // just for debugging purposes
        JMSInterface jmsItf = new JMSInterface(Loader.importZClusterConfig(), cert);
        if (jmsItf.ping()) {
            jmsItf.checkCertInJMS();
        }

        return true;
    }

    private void buildDockerImage() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("bash", "-c", Common.DOCKER_IMAGE_BUILDER);

            System.out.println("Building ZWorker docker image...");
            Process process = processBuilder.start();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Couldn't build ZWorker docker image. Exiting...");
            System.exit(-1);
        }
    }

    private void startNewInstance() {
        System.out.println("Starting new ZWorker instance");
        this.numInstances++;

        CreateContainerResponse response = this.dockerClient.createContainerCmd(DOCKER_IMAGE_NAME)
                .withHostConfig(new HostConfig()
                        .withNetworkMode("host")
                        .withCpuQuota(CPU_QUOTA))
                .exec();

        this.dockerClient.startContainerCmd(response.getId()).exec();

        System.out.println(this.dockerClient.listContainersCmd().exec().size() + " containers are running");
    }

    private void stopInstance() {
        List<Container> containerList = this.dockerClient.listContainersCmd().exec();
        if (!containerList.isEmpty()) {
            String containerId = containerList.get(0).getId();
            this.dockerClient.stopContainerCmd(containerId).exec();
            this.dockerClient.removeContainerCmd(containerId).exec();
        }
    }

    private void stopAllInstances() {
        try {
            for (Container container : this.dockerClient.listContainersCmd().exec()) {
                this.dockerClient.stopContainerCmd(container.getId()).exec();
                this.dockerClient.removeContainerCmd(container.getId()).exec();
            }
        } catch (Exception ignored) {
            System.out.println("-- ERROR --");
            System.out.println("Couldn't stop, kill nor remove the created docker containers");
            System.out.println("Please try to do it manually");
        }
    }

    public boolean start() {
        if(!this.getCertificate())
            return false;

        this.buildDockerImage();
        this.startNewInstance();

        System.out.println("Connecting to WorkerManager: '" + this.workerManagerAddress + "' | '" + this.sinkAddress + "'");
        this.publisher.connect(this.workerManagerAddress);
        this.sink.connect(this.sinkAddress);

        t1 = new Thread(this::run);
        t1.start();

        return true;
    }

    public void stop() {
        try {
            t1.interrupt();
            t1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.publisher.close();
        this.sink.close();
        this.context.destroy();

        this.stopAllInstances();
    }

    @SuppressWarnings("all")
    private double getCpuUsage(Statistics stats) {
        double cpuPercent = 0.0;
        try {
            CpuStatsConfig preCpuStats = stats.getPreCpuStats();
            CpuStatsConfig cpuStats = stats.getCpuStats();

            Long cpuTotalUsage = cpuStats.getCpuUsage().getTotalUsage();
            if (cpuTotalUsage == null)
                cpuTotalUsage = (long) 0;

            Long preCpuTotalUsage = preCpuStats.getCpuUsage().getTotalUsage();
            if (preCpuTotalUsage == null)
                preCpuTotalUsage = (long) 0;


            Long systemCpuUsage = cpuStats.getSystemCpuUsage();
            if (systemCpuUsage == null)
                systemCpuUsage = (long) 0;

            Long preSystemCpuUsage = preCpuStats.getSystemCpuUsage();
            if (preSystemCpuUsage == null)
                preSystemCpuUsage = (long) 0;

            List<Long> perCpuUsage = cpuStats.getCpuUsage().getPercpuUsage();

            long cpuDelta = cpuTotalUsage - preCpuTotalUsage;
            long systemDelta = systemCpuUsage - preSystemCpuUsage;

            if (cpuDelta > 0 && systemDelta > 0)
                cpuPercent = ((double) cpuDelta / systemDelta) * perCpuUsage.size() * 100;

            return cpuPercent;
        } catch (NullPointerException ignored) {
            System.out.println("Error fetching the usage metrics");
            return 0;
        }
    }

    @SuppressWarnings("all")
    private double getMemUsage(Statistics stats) {
        try {
            MemoryStatsConfig memStats = stats.getMemoryStats();
            Long usage = memStats.getUsage();
            Long limit = memStats.getLimit();

            return (double)usage * 100 / limit;
        } catch (NullPointerException ignored) {
            System.out.println("Error fetching the memory usage statistics");
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private JSONObject getUsageStats() {
        JSONObject usageStats = new JSONObject();
        for (Container container : this.dockerClient.listContainersCmd().exec()) {
            try {
                InvocationBuilder.AsyncResultCallback<Statistics> callback = new InvocationBuilder.AsyncResultCallback<>();
                this.dockerClient.statsCmd(container.getId()).exec(callback);

                Statistics stats = callback.awaitResult();
                callback.close();

                double cpuUsage = this.getCpuUsage(stats);
                double memUsage = this.getMemUsage(stats);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("memory", memUsage);
                jsonObject.put("cpu", cpuUsage);
                usageStats.put(container.getId(), jsonObject);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return usageStats;
    }

    public void run() {
        while (!Thread.interrupted()) {
            try {
                String data = this.publisher.recvStr();

                System.out.println("Received " + data);
                switch (data) {
                    case WorkerManager.USAGE_MSG:
                        JSONObject usageStats = this.getUsageStats();
                        this.sink.send(this.clusterId.toString());
                        this.sink.send(usageStats.toJSONString());
                        break;
                    case WorkerManager.START_NEW_INSTANCE_MSG:
                        String clusterId = data.split(" ")[2];
                        if (this.clusterId.toString().equals(clusterId))
                            if (this.numInstances < 5) {
                                System.out.println("Starting new ZWorker instance");
                                this.startNewInstance();
                            }
                        break;
                    case WorkerManager.STOP_INSTANCE_MSG:
                        System.out.println("Stopping a single ZWorker instance");
                        this.stopInstance();
                        break;
                }
            } catch (RuntimeException ignored) {
                return;
            }
        }
    }
}
