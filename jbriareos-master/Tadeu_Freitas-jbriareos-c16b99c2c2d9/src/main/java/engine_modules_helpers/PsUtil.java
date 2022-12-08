package engine_modules_helpers;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class PsUtil {
    public static class ConnectionAppInfo {
        public long pid;
        public String appName;

        public ConnectionAppInfo(long pid, String appName) {
            this.pid = pid;
            this.appName = appName;
        }
    }

    private static Process executeNetStatCommand(String protocol) throws Exception {
        // -- Execute "netstat -antp", using awk to parse the relevant columns --
        // Check if the protocol matches, in the first column,
        // printing the fourth, fifth and seventh column
        // (srcAddress, dstAddress and pid/appName, respectively)
        String command = "netstat -antp | awk '{if($1 == \"" + protocol + "\") printf(\"%s %s %s\\n\", $4, $5, $7)}'";

        ProcessBuilder processBuilder = new ProcessBuilder();
        return processBuilder.command("bash", "-c", command).start();
    }

    public static ConnectionAppInfo getPidFromNetConnections
            (String protocol, String srcIp, int srcPort, String dstIp, int dstPort) {
        try {
            Process process = executeNetStatCommand(protocol);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            while (line != null) {
                String[] parts = line.split(" ");
                String src = srcIp + ":" + srcPort;
                String dst = dstIp + ":" + dstPort;

                // Check if the src and dst address match
                if (src.equals(parts[0]) && dst.equals(parts[1])) {
                    String[] appInfo = parts[2].split("/");
                    long pid = Long.parseLong(appInfo[0]);
                    String appName = appInfo[1];
                    return new ConnectionAppInfo(pid, appName);
                }

                line = reader.readLine();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
