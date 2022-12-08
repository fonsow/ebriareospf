package components;

import core.DistributedZMQ;

public class ZBroker {
    DistributedZMQ.Broker broker;

    public ZBroker() {
        this.broker = new DistributedZMQ.Broker();
    }

    public void start() {
        this.broker.start();
    }

    public void stop() {
        this.broker.stop();
    }

}
