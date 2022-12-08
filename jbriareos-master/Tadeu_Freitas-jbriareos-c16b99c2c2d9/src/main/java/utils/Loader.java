package utils;

import core.Pipeline;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.LinkedList;

public class Loader {
    public static JSONObject importJhcConfig() {
        JSONParser parser = new JSONParser();
        try {
            return (JSONObject) parser.parse(new FileReader(Common.JHC_CONFIG_PATH));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }

    public static JSONObject importJmsConfig() {
        JSONParser parser = new JSONParser();
        try {
            JSONObject obj = (JSONObject) parser.parse(new FileReader(Common.JMS_CONFIG_PATH));
            return (JSONObject) obj.get("jms");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }

    public static JSONObject importZBrokerConfig() {
        JSONParser parser = new JSONParser();
        try {
            JSONObject obj = (JSONObject) parser.parse(new FileReader(Common.ZBROKER_CONFIG_PATH));
            return (JSONObject) obj.get("broker");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }

    public static JSONObject importZClientConfig() {
        JSONParser parser = new JSONParser();
        try {
            JSONObject obj = (JSONObject) parser.parse(new FileReader(Common.ZCLIENT_CONFIG_PATH));
            return (JSONObject) obj.get("client");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }

    public static JSONObject importZClusterConfig() {
        JSONParser parser = new JSONParser();
        try {
            JSONObject obj = (JSONObject) parser.parse(new FileReader(Common.ZCLUSTER_CONFIG_PATH));
            return (JSONObject) obj.get("cluster");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }

    public static JSONObject importZWorkerConfig() {
        JSONParser parser = new JSONParser();
        try {
            JSONObject obj = (JSONObject) parser.parse(new FileReader(Common.ZWORKER_CONFIG_PATH));
            return (JSONObject) obj.get("worker");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }

    public static JSONObject loadPipelineJSON(String pipelinePath) {
        JSONParser parser = new JSONParser();
        try {
            return (JSONObject) parser.parse(new FileReader(pipelinePath));
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private static class Connection {
        String srcMod;
        String dstMode;

        public Connection(String srcMod, String dstMode) {
            this.srcMod = srcMod;
            this.dstMode = dstMode;
        }
    }

    public static LinkedList<Pipeline> importPipelines(JSONObject obj) {
        return importPipelines(obj, new LinkedList<>());
    }

    public static LinkedList<Pipeline> importPipelines(JSONObject obj, LinkedList<Long> ports) {
        LinkedList<Pipeline> pipelines = new LinkedList<>();

        try {
            String name = ((String) obj.get("name")).toLowerCase();
            System.out.println("    Loading pipeline '" + name + "'");

            JSONArray moduleArray = (JSONArray) obj.get("modules");
            if (moduleArray == null || moduleArray.isEmpty()) {
                System.out.println("There are no modules in the pipeline " + obj.get("name"));
                return null;
            }

            Common.PipelineType pipelineType = Common.PipelineType.InputPipeline;
            Object pipelineTypeObj = obj.get("type");
            if (pipelineTypeObj != null) {
                String sPipelineType = (String) pipelineTypeObj;
                if (sPipelineType.toLowerCase().equals("output"))
                    pipelineType = Common.PipelineType.OutputPipeline;
            }

            String protocol = ((String) obj.get("protocol")).toLowerCase();

            Common.ProcessingMode mode = Common.ProcessingMode.InlineLocal;
            Object modeObj = obj.get("mode");
            if (modeObj != null) {
                 String sMode = (String) modeObj;
                 if (sMode.toLowerCase().equals("distributed"))
                     mode = Common.ProcessingMode.Distributed;
            }

            String itf = ((String) obj.get("interface"));

            String srcIp = ((String) obj.get("source_ip"));

            Common.Verdict defaultVerdict = Common.Verdict.Accept;
            Object verdictObj = obj.get("verdict");
            if (verdictObj != null) {
                String sVerdict = ((String) verdictObj).toLowerCase();
                if (sVerdict.equals("drop"))
                    defaultVerdict = Common.Verdict.Drop;
            }

            LinkedList<String> modules = new LinkedList<>();
            LinkedList<Connection> connections = new LinkedList<>();

            for (Object jhcMod : moduleArray) {
                String currentMod = (String) ((JSONObject) jhcMod).get("name");

                if (!modules.contains(currentMod))
                    modules.addLast(currentMod);

                Object nextMod = ((JSONObject) jhcMod).get("next");
                if (nextMod == null)
                    break;

                if (nextMod instanceof JSONArray) {
                    for (Object mod : (JSONArray) nextMod) {
                        String nextModName = (String) mod;
                        connections.addLast(new Connection(currentMod, nextModName));

                        if (!modules.contains(nextModName))
                            modules.addLast(nextModName);
                    }
                } else {
                    String nextModName = (String) nextMod;
                    connections.addLast(new Connection(currentMod, nextModName));

                    if (!modules.contains(nextModName))
                        modules.addLast(nextModName);
                }
            }

            if (ports.isEmpty()) {
                Pipeline pipeline = new Pipeline(pipelineType, name, null, mode,
                        itf, srcIp, protocol, defaultVerdict);
                pipeline.addModules(modules);

                for (Connection connection : connections)
                    if (!pipeline.connectModules(connection.srcMod, connection.dstMode))
                        return new LinkedList<>();

                pipelines.addLast(pipeline);
            } else for (long port : ports) {
                Pipeline pipeline = new Pipeline(pipelineType, name, port, mode,
                        itf, srcIp, protocol, defaultVerdict);
                pipeline.addModules(modules);

                for (Connection connection : connections)
                    if (!pipeline.connectModules(connection.srcMod, connection.dstMode))
                        return new LinkedList<>();

                pipelines.addLast(pipeline);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return pipelines;
    }

    public static LinkedList<Long> getPorts(JSONObject pipeline) {
        LinkedList<Long> ports = new LinkedList<>();

        Object portsObj = pipeline.get("port");
        if (portsObj == null)
            portsObj = pipeline.get("ports");

        if (portsObj != null) {
            if (portsObj instanceof Long) {
                ports.addLast((Long) portsObj);
            } else if (portsObj instanceof String) {
                ports.addLast(Long.parseLong((String) portsObj));
            } else /* It is a JSONArray object, with multiple Longs */ {
                for (Object port : (JSONArray) portsObj) {
                    ports.addLast((Long) port);
                }
            }
        }

        return ports;
    }
}
