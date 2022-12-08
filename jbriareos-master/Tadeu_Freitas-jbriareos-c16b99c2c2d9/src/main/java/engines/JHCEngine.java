package engines;

import core.JDSInterface;
import core.JMSInterface;
import core.Pipeline;
import org.json.simple.JSONObject;
import utils.BPacket;
import utils.Common;
import utils.Loader;

import java.util.LinkedList;
import java.util.Map;

public class JHCEngine extends Engine {
    public JHCEngine(JSONObject componentConfig, JDSInterface jdsInterface) {
        super(EngineType.JHCEngine, componentConfig, jdsInterface, null);
    }

    public JHCEngine(JSONObject componentConfig, JDSInterface jdsInterface, JMSInterface jmsItf) {
        super(EngineType.JHCEngine, componentConfig, jdsInterface, jmsItf);
    }

    private boolean loadPipeline(JSONObject pipelineConfig, LinkedList<Long> ports) {
        LinkedList<Pipeline> pipelines = Loader.importPipelines(pipelineConfig, ports);
        if (pipelines == null || pipelines.isEmpty())
            return false;

        this.pipelineList.addAll(pipelines);
        for (Pipeline pipeline : pipelines)
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

            LinkedList<Long> ports = Loader.getPorts(pipeline);
            if (this.loadPipeline(pipeline, ports))
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

            LinkedList<Long> ports = Loader.getPorts(pipelineConfig);
            if (this.loadPipeline(pipelineConfig, ports))
                numPipelines++;
            else
                System.out.println("Couldn't load the pipeline " + pipelineName);
        }

        return numPipelines;
    }

    @Override
    public ProcessResult process(byte[] packetPayload, String queueId) {
        Pipeline pipeline = this.queueMap.get(Integer.parseInt(queueId));

        switch (pipeline.mode) {
            case InlineLocal:
                return new ProcessResult(this.runInline(packetPayload, pipeline), null);
            case Distributed:
                return new ProcessResult(null, this.runDistributed(packetPayload, pipeline));
        }

        return new ProcessResult(null, pipeline.defaultVerdict);
    }

    private BPacket runInline(byte[] packetPayload, Pipeline pipeline) {
        return pipeline.run(packetPayload, this.jmsItf);
    }

    private Common.Verdict runDistributed(byte[] packetPayload, Pipeline pipeline) {
        this.jdsInterface.process(packetPayload, pipeline.name);

        return pipeline.defaultVerdict;
    }
}
