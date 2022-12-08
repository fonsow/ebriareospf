package ThroughputTests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ThroughputTest {
    static boolean echoRequest = false;

    private static Stats test(int concurrency) throws IOException {
        String command;
        if (!echoRequest)
            command = "ab -n 50000 -c " + concurrency + " http://localhost:4000/";
        else
            command = "ab -p post.ok.data -n 50000 -c " + concurrency + " http://localhost:4000/echo";

        ProcessBuilder processBuilder = new ProcessBuilder();
        Process process = processBuilder.command("bash", "-c", command).start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        while (!line.contains("Total:"))
            line = reader.readLine();

        String[] parts = line.split("\\s+");

        return new Stats(concurrency, Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }

    private static class Stats {
        int concurrency;
        double mean;
        double std;

        Stats(int concurrency, double mean, double std) {
            this.concurrency = concurrency;
            this.mean = mean;
            this.std = std;
        }

        @Override
        public String toString() {
            return "(mean = " + mean + ", std = " + std + ")";
        }
    }

    private static double meanOfMeans(LinkedList<Stats> list) {
        if (list.isEmpty())
            return 0;

        double sum = 0;
        for (Stats item : list) {
            sum += item.mean;
        }

        return sum / list.size();
    }

    private static double meanOfStd(LinkedList<Stats> list) {
        if (list.isEmpty())
            return 0;

        double sum = 0;
        for (Stats item : list) {
            sum += item.std;
        }

        return sum / list.size();
    }

    private static LinkedList<Stats> finalResults = new LinkedList<>();

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Use arguments");
            return;
        }

        if (args[0].equals("test")) {
            test(0);
        } else if(args[0].equals("echo-test"))
            echoRequest = true;

        List<Integer> concurrencies = Arrays.asList(100, 200, 300, 400, 500);
        for (int i : concurrencies) {
            LinkedList<Stats> results = new LinkedList<>();
            System.out.println("Testing " + i + " concurrent packets\n");

            for (int j = 0; j < 10; j++) {
                results.addLast(test(i));
            }

            System.out.println("Results:");
            System.out.println(results);
            System.out.println();

            Stats stats = new Stats(i, meanOfMeans(results), meanOfStd(results));
            finalResults.addLast(stats);
        }

        System.out.println("Final results:");
        System.out.println(finalResults);
    }
}
