package components;

import core.DistributedZMQ;

import java.util.LinkedList;
import java.util.UUID;

public class ZWorker {
    public UUID id;
    public LinkedList<DistributedZMQ.Worker> workers;

    public ZWorker() {
        System.out.println("Initializing Briareos ZWorker");
        this.id = UUID.randomUUID();
        this.workers = new LinkedList<>();
        this.initWorkers();
    }

    private int getMaxWorkers() {
        return 1;
        // return Runtime.getRuntime().availableProcessors();
    }

    private void initWorkers() {
        int maxWorkers = this.getMaxWorkers();

        for (int i = 0; i < maxWorkers; i++) {
            this.workers.addLast(new DistributedZMQ.Worker(this.id));
        }
    }

    public void start() {
        for (DistributedZMQ.Worker worker : this.workers)
            worker.start();

        System.out.println("ZWorker ID: " + this.id);
        System.out.println("Briareos ZWorker is running");
    }

    public void stop() {
        for (DistributedZMQ.Worker worker : workers)
            worker.stop();
    }
}
