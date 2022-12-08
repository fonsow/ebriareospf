package components;

import core.DistributedZMQ;

import java.util.UUID;

public class ZClient {
    DistributedZMQ.Client client;

    public ZClient(UUID id) {
        this.client = new DistributedZMQ.Client(id);
    }

    public void start() {
        this.client.start();
    }

    public void stop() {
        this.client.stop();
    }

    public void process(byte[] packetPayload, String pipelineName) {
        this.client.process(packetPayload, pipelineName);
    }
}
