package executable_classes;

public class JMS {
    private static components.JMS jms;
    private static volatile boolean flag = true;

    private static void stop() {
        jms.stop();
        System.out.println("Done");
        flag = false;
    }

    public static void main(String[] args) {
        if (!System.getProperty("user.name").equals("root")) {
            System.out.println("You need to have be root to execute Briareos.\n Exiting...\n");
            System.exit(1);
        }

        jms = new components.JMS();
        jms.start();
        Runtime.getRuntime().addShutdownHook(new Thread(JMS::stop));

        // This makes Briareos run forever.
        // To stop it, send a SIGINT (Ctrl + C) or SIGTERM (kill -15) signal
        // When it receives one of those signals, it stops the jms and exits
        while (flag) Thread.onSpinWait();
    }
}
