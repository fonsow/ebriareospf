package components;

import core.Cluster;

public class ZCluster {
    public Cluster cluster;

    public ZCluster() {
        System.out.println("Initializing Briareos ZCluster");
        this.cluster = new Cluster();
    }

    public boolean start() {
        if(!this.cluster.start())
            return false;

        System.out.println("ZCluster ID: " + this.cluster.clusterId);
        return true;
    }

    public void stop() {
        System.out.println("Stopping Briareos ZCluster " + this.cluster.clusterId);
        this.cluster.stop();
    }
}
