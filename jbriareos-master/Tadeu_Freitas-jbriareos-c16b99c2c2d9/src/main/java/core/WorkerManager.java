package core;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import utils.Loader;
import utils.WorkerStats;

import java.util.*;
import java.util.concurrent.Semaphore;

public class WorkerManager {
    ZContext context;
    ZMQ.Socket publisher;
    ZMQ.Socket sink;
    String publisherAddress;
    String sinkAddress;
    long interval;
    HashMap<String, LinkedList<WorkerStats>> workerStats;
    Double averageCPU;
    Double averageMem;
    public boolean alreadyRequested;
    public int numWorkers;
    public int numTasks;
    HashMap<UUID, Integer> workersPerCluster;

    private final Semaphore mutexPublisher = new Semaphore(1);

    public static final String USAGE_MSG = "REPORT USAGE";
    public static final String START_NEW_INSTANCE_MSG = "NEW INSTANCE";
    public static final String STOP_INSTANCE_MSG = "STOP INSTANCE";

    public static final int METRIC_INTERVAL = 15000;    // 15 secs
    public static final int LOWER_BOUND = 10;
    public static final int UPPER_BOUND = 70;

    Thread t1, t2, t3;

    public WorkerManager() {
        JSONObject brokerConfig = Loader.importZBrokerConfig();

        Object ipObj = brokerConfig.get("ip");
        String ip = "*";
        if (ipObj != null)
            ip = (String) ipObj;

        JSONObject config = (JSONObject) brokerConfig.get("worker_manager");
        long publisherPort = (Long) config.get("port");
        long sinkPort = (Long) config.get("sink_port");

        this.context = new ZContext();
        this.publisherAddress = DistributedZMQ.formAddress(ip, publisherPort);
        this.sinkAddress = DistributedZMQ.formAddress(ip, sinkPort);

        this.publisher = this.context.createSocket(SocketType.PUB);
        this.sink = this.context.createSocket(SocketType.PULL);

        this.interval = (long) config.get("interval");
        this.workerStats = new HashMap<>();
        this.averageCPU = null;
        this.averageMem = null;
        this.alreadyRequested = false;
        this.numWorkers = 0;
        this.numTasks = 0;
        this.workersPerCluster = new HashMap<>();
    }

    private Double average(LinkedList<Double> list) {
        if (list.isEmpty())
            return null;

        double sum = 0;
        for (double item : list)
            sum += item;

        return sum / list.size();
    }

    private void slidingWindows() {
        LinkedList<Double> cpuValues = new LinkedList<>();
        LinkedList<Double> memValues = new LinkedList<>();
        LinkedList<String> workerStatsToRemove = new LinkedList<>();

        long currentTime = new Date().getTime();
        for (Map.Entry<String, LinkedList<WorkerStats>> entry : this.workerStats.entrySet())
            for (WorkerStats stat : entry.getValue()) {
                if (currentTime - stat.revision <= METRIC_INTERVAL) {
                    cpuValues.addLast(stat.cpuUsage);
                    memValues.addLast(stat.memUsage);
                } else {
                    workerStatsToRemove.addLast(entry.getKey());
                }
            }

        for (String workerId : workerStatsToRemove)
            this.workerStats.remove(workerId);

        this.averageCPU = this.average(cpuValues);
        this.averageMem = this.average(memValues);
    }

    @SuppressWarnings("SameParameterValue")
    private UUID chooseClusterId(boolean withLeastWorkers) {
        String clusterId = "";
        int workers;

        if (withLeastWorkers)
            workers = 0xffffffff;
        else
            workers = 0;

        for (Map.Entry<UUID, Integer> entry : workersPerCluster.entrySet()) {
            if (entry.getValue() < workers) {
                clusterId = entry.getKey().toString();
                workers = entry.getValue();
            }
        }

        if (clusterId.isEmpty()) {
            System.out.println("There is no initialized cluster on the WorkerManager");
            System.exit(-1);
        }

        return UUID.fromString(clusterId);
    }

    private void updateWorkerStats(UUID clusterId, String zworkerContainerId, double cpuUsage, double memUsage) {
        WorkerStats stats = new WorkerStats(cpuUsage, memUsage);

        this.workerStats.computeIfAbsent(zworkerContainerId, k -> new LinkedList<>());
        this.workersPerCluster.putIfAbsent(clusterId, 1);

        LinkedList<WorkerStats> statsList = this.workerStats.get(zworkerContainerId);
        statsList.addLast(stats);
        this.workerStats.put(zworkerContainerId, statsList);
    }

    private void processUsageStats(UUID clusterId, JSONObject workerStats) {
        for (Object keyObj : workerStats.keySet()) {
            String containerId = (String) keyObj;

            JSONObject stats = (JSONObject) workerStats.get(containerId);
            double cpuUsage = (double) stats.get("cpu");
            double memUsage = (double) stats.get("memory");

            this.updateWorkerStats(clusterId, containerId, cpuUsage, memUsage);
        }
    }

    @SuppressWarnings("BusyWait")
    public void runPublisher() {
        while (!Thread.interrupted()) {
            try {
                mutexPublisher.acquire();

                this.publisher.send(USAGE_MSG);
                Thread.sleep(this.interval * 1000 + 10);
            } catch (RuntimeException | InterruptedException ignored) {
                return;
            } finally {
                mutexPublisher.release();
            }
        }
    }

    public void runSink() {
        JSONParser parser = new JSONParser();

        while (!Thread.interrupted()) {
            try {
                UUID clusterId = UUID.fromString(this.sink.recvStr());
                JSONObject workerStats = (JSONObject) parser.parse(this.sink.recvStr());
                this.processUsageStats(clusterId, workerStats);
            } catch (RuntimeException ignored) {
                return;
            } catch (ParseException ignored2) {
                System.out.println("Failure parsing worker statistics");
            }
        }
    }

    public void printMetrics() {
        System.out.println("\nMetrics:\n    AvgCPU = " + this.averageCPU + "\n    AvgMem = " + this.averageMem);
    }

    @SuppressWarnings("all")
    public void run() {
        while (!Thread.interrupted()) {
            this.slidingWindows();

            if (this.averageCPU != null && this.averageMem != null) {
                this.printMetrics();
                if (this.averageCPU >= UPPER_BOUND || this.averageMem >= UPPER_BOUND) {
                    if (!this.alreadyRequested) {
                        UUID clusterId = this.chooseClusterId(true);
                        int numWorkers = this.workersPerCluster.get(clusterId);
                        this.workersPerCluster.put(clusterId, numWorkers + 1);

                        try {
                            mutexPublisher.acquire();

                            this.publisher.send(START_NEW_INSTANCE_MSG + " " + clusterId);
                        } catch (RuntimeException | InterruptedException ignored) {
                            System.out.println("Couldn't send " + START_NEW_INSTANCE_MSG +
                                    " message to the cluster");
                            continue;
                        } finally {
                            mutexPublisher.release();
                        }

                        this.alreadyRequested = true;
                    }
                } else if ((this.averageCPU <= LOWER_BOUND && this.averageMem < UPPER_BOUND)
                        || this.averageMem <= LOWER_BOUND && averageCPU < UPPER_BOUND) {
                    /*
                        -> Acquire mutex and send STOP_INSTANCE_MSG to cluster
                        -> Send notice to broker about worker stopping, to remove it
                     */
                }
            }

            try {
                Thread.sleep(this.interval * 1000);
                System.out.println("Tasks: " + this.numTasks + " | Workers: " + this.numWorkers);
                System.out.println();
            } catch (InterruptedException ignored) {
                return;
            }
        }
    }

    public void start() {
        System.out.println("Worker Manager address: '" + this.publisherAddress + "' | '" + this.sinkAddress + "'");
        this.publisher.bind(this.publisherAddress);
        this.sink.bind(this.sinkAddress);

        t1 = new Thread(this::runPublisher);
        t2 = new Thread(this::runSink);
        t3 = new Thread(this::run);

        t1.start();
        t2.start();
        t3.start();
    }

    public void stop() {
        System.out.println("Stopping WorkerManager...");

        try {
            t1.interrupt();
            t2.interrupt();
            t3.interrupt();

            t1.join();
            t2.join();
            t3.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.publisher.close();
        this.sink.close();
        this.context.destroy();
    }
}
