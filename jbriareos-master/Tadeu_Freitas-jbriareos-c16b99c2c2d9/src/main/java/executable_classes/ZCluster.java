package executable_classes;

public class ZCluster {
    private static components.ZCluster zCluster;
    private static volatile boolean flag = true;

    private static void stop() {
        zCluster.stop();
        System.out.println("Done");
        flag = false;
    }

    public static void main(String[] args) {
        zCluster = new components.ZCluster();

        if (!zCluster.start())
            return;

        Runtime.getRuntime().addShutdownHook(new Thread(ZCluster::stop));

        // This makes Briareos run forever.
        // To stop it, send a SIGINT (Ctrl + C) or SIGTERM (kill -15) signal
        // When it receives one of those signals, it stops the zcluster and exits
        while (flag) Thread.onSpinWait();
    }
}
