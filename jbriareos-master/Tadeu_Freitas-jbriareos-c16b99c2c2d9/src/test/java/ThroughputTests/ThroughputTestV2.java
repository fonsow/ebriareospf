package ThroughputTests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ThroughputTestV2 {
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
        while (!line.contains("Total transferred:"))
            line = reader.readLine();

        String[] parts = line.split("\\s+");
        double total = Double.parseDouble(parts[2]);

        while (!line.contains("Transfer rate:"))
            line = reader.readLine();

        parts = line.split("\\s+");
        double rate = Double.parseDouble(parts[2]);

        return new Stats(concurrency, total, rate);
    }

    private static class Stats {
        int concurrency;
        double total;
        double rate;

        Stats(int concurrency, double total, double rate) {
            this.concurrency = concurrency;
            this.total = total;
            this.rate = rate;
        }

        @Override
        public String toString() {
            return "(total = " + total + ", rate = " + rate + ")";
        }
    }

    private static double meanOfTotals(LinkedList<Stats> list) {
        if (list.isEmpty())
            return 0;

        double sum = 0;
        for (Stats item : list) {
            sum += item.total;
        }

        return sum / list.size();
    }

    private static double meanOfRates(LinkedList<Stats> list) {
        if (list.isEmpty())
            return 0;

        double sum = 0;
        for (Stats item : list) {
            sum += item.rate;
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

            Stats stats = new Stats(i, meanOfTotals(results), meanOfRates(results));
            finalResults.addLast(stats);
        }

        System.out.println("Final results:");
        System.out.println(finalResults);
    }
}
