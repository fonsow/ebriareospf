package JMSTest;

import core.DistributedZMQ;
import core.Pipeline;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import utils.Loader;

import java.util.LinkedList;
import java.util.Map;

public class PipelineDistTest {

    static JSONObject componentConfig = new JSONObject();
    static String jmsAddress;

    @SuppressWarnings("unchecked")
    private static void componentConfigInit() {
        JSONArray pipelines = new JSONArray();
        JSONObject pipeline = new JSONObject();

        pipeline.put("name", "new_simple_web_app_firewall");
        pipeline.put("port", (long) 4000);
        pipeline.put("protocol", "TCP");
        pipeline.put("interface", "any");
        pipeline.put("mode", "distributed");

        pipelines.add(pipeline);

        componentConfig.put("pipelines", pipelines);

        JSONObject jms = new JSONObject();
        jms.put("ip", "127.0.0.1");
        jms.put("port", (long) 11336);

        componentConfig.put("jms", jms);
    }

    private static void jmsConnectionInit() {
        JSONObject config = (JSONObject) componentConfig.get("jms");
        String ip = (String) config.get("ip");
        long port = (Long) config.get("port");
        jmsAddress = DistributedZMQ.formAddress(ip, port);
    }

    private static JSONObject requestPipelines(JSONArray request) {
        ZContext context = new ZContext();
        ZMQ.Socket req = context.createSocket(SocketType.REQ);
        req.connect(jmsAddress);

        req.send(request.toJSONString());
        String responseString = req.recvStr();

        JSONParser parser = new JSONParser();
        try {
            return (JSONObject) parser.parse(responseString);
        } catch (ParseException ignored) {
            System.out.println(responseString);
            return new JSONObject();
        }
    }

    private static boolean newLoadPipeline(JSONObject pipelineConfig, LinkedList<Long> ports) {
        LinkedList<Pipeline> pipelines = Loader.importPipelines(pipelineConfig, ports);
        return !pipelines.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        componentConfigInit();
        jmsConnectionInit();

        int numPipelines = 0;
        JSONObject pipelineConfigs = new JSONObject();

        Object obj = componentConfig.get("pipelines");
        if (!(obj instanceof JSONArray)) {
            System.out.println("Pipelines weren't defined as a list, as they are supposed to");
            return;
        }

        JSONArray pipelines = (JSONArray) obj;
        JSONArray request = new JSONArray();
        for (Object pipelineObj : pipelines) {
            JSONObject pipeline = (JSONObject) pipelineObj;
            String pipelineName = (String) pipeline.get("name");
            request.add(pipelineName);

            JSONObject pipelineConfig = new JSONObject();
            pipelineConfig.putAll(pipeline);
            pipelineConfigs.put(pipelineName, pipelineConfig);
        }

        JSONObject responseConfigs = requestPipelines(request);
        if (responseConfigs.isEmpty()) {
            System.out.println("No pipelines were loaded. Exiting...");
            System.exit(-1);
        }

        for (Object pipelineObj : responseConfigs.entrySet()) {
            Map.Entry<String, JSONObject> pipelineEntry = (Map.Entry<String, JSONObject>) pipelineObj;

            String pipelineName = pipelineEntry.getKey();
            JSONObject pipeline = pipelineEntry.getValue();

            JSONObject pipelineConfig = (JSONObject) pipelineConfigs.get(pipelineName);
            pipelineConfig.putAll(pipeline);
            LinkedList<Long> ports = Loader.getPorts(pipelineConfig);
            if (newLoadPipeline(pipelineConfig, ports))
                numPipelines++;
            else
                System.out.println("Couldn't load the pipeline " + pipelineName);
        }

        if (numPipelines > 0)
            System.out.println(numPipelines + " pipelines loaded\n");
        else {
            System.out.println("No pipelines were loaded. Exiting...");
            System.exit(-1);
        }
    }
}
