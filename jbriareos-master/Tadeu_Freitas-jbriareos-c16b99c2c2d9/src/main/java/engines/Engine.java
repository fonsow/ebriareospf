package engines;

import core.JDSInterface;
import core.JMSInterface;
import core.Pipeline;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utils.BPacket;
import utils.Common;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

public abstract class Engine {
    public EngineType engineType;
    public LinkedList<Pipeline> pipelineList;
    public HashMap<Integer, Pipeline> queueMap;
    JSONObject componentConfig;
    JDSInterface jdsInterface;
    HashMap<String, Pipeline> pipelineMap;
    UUID engineId;

    JMSInterface jmsItf;

    public Engine(EngineType engineType, JSONObject componentConfig, JDSInterface jdsInterface,
                  JMSInterface jmsItf) {
        this.engineType = engineType;
        this.pipelineList = new LinkedList<>();
        this.queueMap = new HashMap<>();
        this.componentConfig = componentConfig;
        this.jdsInterface = jdsInterface;
        this.pipelineMap = new HashMap<>();
        this.engineId = UUID.randomUUID();

        this.jmsItf = jmsItf;
    }

    public static class ProcessResult {
        public BPacket packet;

        public Common.Verdict verdict;

        public ProcessResult(BPacket packet, Common.Verdict verdict) {
            this.packet = packet;
            this.verdict = verdict;
        }
        @Override
        public String toString() {
            if (this.packet != null)
                return this.packet.toString();
            else
                return this.verdict.name();
        }

    }

    public void start() {
        if (this.jmsItf != null)
            this.jmsItf.start();

        if (this.engineType == EngineType.JHCEngine) {
            if (jmsItf != null) {
                System.out.println("Requesting rules saved on JMS...");
                this.jmsItf.loadRules();
            }

            this.jdsInterface.start();
        }

        System.out.println("Loading pipelines...");
        if (this.jmsItf != null)
            this.loadPipelines();
        else
            this.loadLocalPipelines();
    }

    public void stop() {
        System.out.println("Stopping the engine...");

        if (this.jdsInterface != null)
            this.jdsInterface.stop();

        if (this.jmsItf != null)
            this.jmsItf.stop();
    }

    @SuppressWarnings("unchecked")
    void loadPipelines() {
        JSONObject pipelineConfigs = new JSONObject();

        Object obj = this.componentConfig.get("pipelines");
        checkObjIsJsonArray(obj);

        JSONArray pipelines = (JSONArray) obj;
        JSONArray request = new JSONArray();
        for (Object pipelineObj : pipelines) {
            JSONObject pipeline = (JSONObject) pipelineObj;
            String pipelineName = checkPipelineName(pipeline);

            request.add(pipelineName);

            JSONObject pipelineConfig = new JSONObject();
            pipelineConfig.putAll(pipeline);
            pipelineConfigs.put(pipelineName, pipelineConfig);
        }

        JSONObject responseConfigs = this.jmsItf.requestPipelines(request);
        int numPipelines;
        if (responseConfigs.isEmpty()) {
            System.out.println("Couldn't import pipeline configurations from the JMS.");
            System.out.println("Trying to create pipelines with the local configurations...");
            numPipelines = initPipelines(pipelineConfigs);
        } else
            numPipelines = initPipelines(pipelineConfigs, responseConfigs);

        if (numPipelines > 0)
            System.out.println(numPipelines + " pipelines loaded\n");
        else {
            System.out.println("No pipelines were loaded. Exiting...");
            System.exit(-1);
        }
    }

    @SuppressWarnings("unchecked")
    void loadLocalPipelines() {
        JSONObject pipelineConfigs = new JSONObject();
        Object obj = this.componentConfig.get("pipelines");
        checkObjIsJsonArray(obj);

        JSONArray pipelines = (JSONArray) obj;
        for (Object pipelineObj : pipelines) {
            JSONObject pipeline = (JSONObject) pipelineObj;
            String pipelineName = checkPipelineName(pipeline);

            JSONObject pipelineConfig = new JSONObject();
            pipelineConfig.putAll(pipeline);
            pipelineConfigs.put(pipelineName, pipelineConfig);
        }

        int numPipelines = initPipelines(pipelineConfigs);
        if (numPipelines > 0)
            System.out.println(numPipelines + " pipelines loaded\n");
        else {
            System.out.println("No pipelines were loaded. Exiting...");
            System.exit(-1);
        }
    }

    private String checkPipelineName(JSONObject pipeline) {
        String pipelineName = (String) pipeline.get("name");
        if (pipelineName == null || pipelineName.isEmpty()) {
            System.out.println("No pipeline name was provided...");
            System.out.println("Please provide the pipeline's filename, if connected to the JMS");
            System.out.println("Otherwise, simply provide any pipeline name");
            System.exit(-1);
        }

        return pipelineName;
    }

    private void checkObjIsJsonArray(Object obj) {
        if (!(obj instanceof JSONArray)) {
            System.out.println("Pipelines weren't defined as a list, as they are supposed to. Exiting...");
            System.exit(-1);
        }
    }

    abstract int initPipelines(JSONObject pipelineConfigs);
    abstract int initPipelines(JSONObject pipelineConfigs, JSONObject responseConfigs);
    public abstract ProcessResult process(byte[] packetPayload, String data);
}
