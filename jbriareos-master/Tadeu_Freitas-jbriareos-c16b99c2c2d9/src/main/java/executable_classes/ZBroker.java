package executable_classes;

public class ZBroker {
    private static components.ZBroker zBroker;
    private static volatile boolean flag = true;

    private static void stop() {
        zBroker.stop();
        System.out.println("Done");
        flag = false;
    }

    public static void main(String[] args) {
        zBroker = new components.ZBroker();

        zBroker.start();
        Runtime.getRuntime().addShutdownHook(new Thread(ZBroker::stop));

        // This makes Briareos run forever.
        // To stop it, send a SIGINT (Ctrl + C) or SIGTERM (kill -15) signal
        // When it receives one of those signals, it stops the zbroker and exits
        while (flag) Thread.onSpinWait();
    }
}
