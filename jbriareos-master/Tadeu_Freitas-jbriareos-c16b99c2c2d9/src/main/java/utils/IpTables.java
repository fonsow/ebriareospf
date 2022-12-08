package utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class IpTables {
    public static final String INPUT_CHAIN = "INPUT";
    public static final String OUTPUT_CHAIN = "OUTPUT";

    private static final List<String> SUPPORTED_PROTOCOLS = Arrays.asList("tcp", "udp", "all");

    public static final List<String> ANY_INTERFACE = Arrays.asList("", "any", "all");

    /**
     * Forms an iptable rule from the options present in the JSON.
     * From all the options, only the iptable chain and the action/jump are mandatory.
     * The source and destination address and port, protocol and interface are all optional.
     * All other options are disregarded.
     *
     * @param ruleOptions The JSON with the rule options
     * @return An iptable command, used to create the iptable rule matching the options
     */
    public static String formRuleFromJSON(JSONObject ruleOptions) throws Exception {
        String chain = (String) ruleOptions.get("chain");
        String action = (String) ruleOptions.get("action");
        if (chain == null || action == null)
            throw new Exception("Both 'chain' and 'action' have to be present in the rule's options for it to be valid");


        chain = "-I " + ruleOptions.get("chain") + " ";
        action = "-j " + ruleOptions.get("action");

        String table = "";
        String ruleTable = (String) ruleOptions.get("table");
        if (ruleTable != null)
            table = "-t " + ruleTable + " ";

        String protocol = "";
        String ruleProtocol = (String) ruleOptions.get("protocol");
        if (ruleProtocol != null)
            protocol = "-p " + ruleProtocol + " ";

        String itf = "";
        String ruleItf = (String) ruleOptions.get("itf");
        if (ruleItf != null)
            if (chain.equals(INPUT_CHAIN))
                itf = "-i " + ruleItf + " ";
            else if (chain.equals(OUTPUT_CHAIN))
                itf = "-o " + ruleItf + " ";

        String srcIp = "";
        String ruleSrcIp = (String) ruleOptions.get("srcIp");
        if (ruleSrcIp != null)
            srcIp = "-s " + ruleSrcIp + " ";

        String srcPort = "";
        String ruleSrcPort = (String) ruleOptions.get("srcPort");
        if (ruleSrcPort != null)
            srcPort = "--sport " + ruleSrcPort + " ";

        String dstIp = "";
        String ruleDstIp = (String) ruleOptions.get("dstIp");
        if (ruleDstIp != null)
            dstIp = "-d " + ruleDstIp + " ";

        String dstPort = "";
        String ruleDstPort = (String) ruleOptions.get("dstPort");
        if (ruleDstPort != null)
            dstPort = "--dport " + ruleDstPort + " ";

        return "iptables " + table + chain + srcIp + srcPort
                + dstIp + dstPort + protocol + itf + action;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject formJSONfromRuleArgs(String table, String chain, String protocol, String itf,
                                                  String srcIp, String srcPort, String dstIp, String dstPort,
                                                  String action) {
        JSONObject object = new JSONObject();
        object.put("chain", chain);
        object.put("action", action);

        if (table != null)
            object.put("table", table);
        if (protocol != null)
            object.put("protocol", protocol);
        if (itf != null)
            object.put("itf", itf);
        if (srcIp != null)
            object.put("srcIp", srcIp);
        if (srcPort != null)
            object.put("srcPort", srcPort);
        if (dstIp != null)
            object.put("srcIp", dstIp);
        if (dstPort != null)
            object.put("dstPort", dstPort);

        return object;
    }

    /**
     * Consists of multiple calls to the method with the same name which only handles one JSONObject at a time
     * @param rules The JSONArray with multiple JSONObject which represent the rules' options
     */
    public static void createRulesFromJSON(JSONArray rules) {
        for (Object obj : rules) {
            JSONObject ruleOptions = (JSONObject) obj;

            try {
                createRulesFromJSON(ruleOptions);
            } catch (IOException ignored) {
                try {
                    System.out.println("Couldn't create a rule:");
                    System.out.println("    " + formRuleFromJSON(ruleOptions));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Creates an iptable rule from the options present in the JSON
     * @param ruleOptions The JSON with the options
     * @throws IOException If it wasn't possible to create a new rule
     */
    public static void createRulesFromJSON(JSONObject ruleOptions) throws IOException {
        try {
            String rule = formRuleFromJSON(ruleOptions);
            createNewRule(rule);
        } catch (IOException ioException) {
            throw ioException;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean createNFQueueRule(int queueId, String chain, Long port, String protocol,
                                    String itf, String sourceIp) {

        String portStr = "*";
        if (port != null)
            portStr = port.toString();

        String protocolOption = "";
        String portOption = "";
        if (protocol != null) {
            if (SUPPORTED_PROTOCOLS.contains(protocol)) {
                protocolOption = "-p " + protocol;
                if (protocol.equals("tcp") || protocol.equals("udp")) {
                    if (chain.equals(INPUT_CHAIN))
                        portOption = "--dport " + portStr;
                    else if (chain.equals(OUTPUT_CHAIN))
                        portOption = "--sport " + portStr;
                }
            }
        }

        String itfOption = "";
        if (itf != null) {
           if (!ANY_INTERFACE.contains(itf)) {
                if (chain.equals(INPUT_CHAIN)) {
                    itfOption = "-i " + itf;
                } else if (chain.equals(OUTPUT_CHAIN))  {
                    itfOption = "-o " + itf;
                }
            }
        }

        String sourceIpOption = "";
        if (sourceIp != null) {
            sourceIpOption = "-s " + sourceIp;
        }

        String command = "iptables -I " + chain + " " + itfOption + " " + sourceIpOption + " "
                + protocolOption + " " + portOption + " -j NFQUEUE --queue-num " + queueId + " --queue-bypass";

        try {
            createNewRule(command);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    @SuppressWarnings("unused")
    public static void initMasquerade() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", "iptables -t nat -A POSTROUTING -j MASQUERADE");
        processBuilder.start();
    }

    /**
     * Creates a new iptable rule, based on the command received
     *
     * @param ruleCommand The new rule's iptable command to run
     */
    public static void createNewRule(String ruleCommand) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", ruleCommand);
        processBuilder.start();
    }

    // TODO: Do not delete all rules
    public static void cleanRules() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", "iptables -t nat -F PREROUTING");
        try {
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        processBuilder.command("bash", "-c", "iptables -t nat -F POSTROUTING");
        try {
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        processBuilder.command("bash", "-c", "iptables -F INPUT");
        try {
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        processBuilder.command("bash", "-c", "iptables -F OUTPUT");
        try {
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
