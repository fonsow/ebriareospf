package executable_classes;

public class ZWorker {
    private static components.ZWorker zWorker;
    private static volatile boolean flag = true;

    private static void stop() {
        zWorker.stop();
        System.out.println("Done");
        flag = false;
    }

    public static void main(String[] args) {
        zWorker = new components.ZWorker();

        zWorker.start();
        Runtime.getRuntime().addShutdownHook(new Thread(ZWorker::stop));

        // This makes Briareos run forever.
        // To stop it, send a SIGINT (Ctrl + C) or SIGTERM (kill -15) signal
        // When it receives one of those signals, it stops the zworker and exits
        while (flag) Thread.onSpinWait();
    }
}
