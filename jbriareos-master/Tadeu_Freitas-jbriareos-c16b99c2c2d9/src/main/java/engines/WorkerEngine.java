package engines;

import core.JMSInterface;
import core.Pipeline;
import org.json.simple.JSONObject;
import utils.BPacket;
import utils.Loader;

import java.util.LinkedList;
import java.util.Map;

public class WorkerEngine extends Engine {
    public WorkerEngine(JSONObject componentConfig) {
        super(EngineType.WorkerEngine, componentConfig, null, null);
    }

    public WorkerEngine(JSONObject componentConfig, JMSInterface jmsItf) {
        super(EngineType.WorkerEngine, componentConfig, null, jmsItf);
    }

    private boolean loadPipeline(JSONObject pipelineConfig) {
        LinkedList<Pipeline> pipelines = Loader.importPipelines(pipelineConfig);

        if (pipelines == null || pipelines.isEmpty())
            return false;

        this.pipelineList.addAll(pipelines);
        Pipeline pipeline = pipelines.getFirst();
        this.pipelineMap.put(pipeline.name, pipeline);

        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    int initPipelines(JSONObject pipelineConfigs) {
        int numPipelines = 0;

        for (Object pipelineObj : pipelineConfigs.entrySet()) {
            Map.Entry<String, JSONObject> pipelineEntry = (Map.Entry<String, JSONObject>) pipelineObj;

            String pipelineName = pipelineEntry.getKey();
            JSONObject pipeline = pipelineEntry.getValue();

            if (this.loadPipeline(pipeline))
                numPipelines++;
            else
                System.out.println("Couldn't load the pipeline " + pipelineName);
        }

        return numPipelines;
    }

    @Override
    @SuppressWarnings("unchecked")
    int initPipelines(JSONObject pipelineConfigs, JSONObject responseConfigs) {
        int numPipelines = 0;

        for (Object pipelineObj : responseConfigs.entrySet()) {
            Map.Entry<String, JSONObject> pipelineEntry = (Map.Entry<String, JSONObject>) pipelineObj;

            String pipelineName = pipelineEntry.getKey();
            JSONObject pipeline = pipelineEntry.getValue();

            JSONObject pipelineConfig = (JSONObject) pipelineConfigs.get(pipelineName);
            pipelineConfig.putAll(pipeline);

            if (this.loadPipeline(pipelineConfig))
                numPipelines++;
            else
                System.out.println("Couldn't load the pipeline " + pipelineName);
        }

        return numPipelines;
    }

    @Override
    public ProcessResult process(byte[] packetPayload, String pipelineName) {
        BPacket packet = this.pipelineMap.get(pipelineName).run(packetPayload, this.jmsItf);

        return new ProcessResult(packet, null);
    }
}
