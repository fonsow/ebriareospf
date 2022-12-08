package core;

import engine_modules.EngineModule;
import engine_modules_helpers.IOData;
import engine_modules_helpers.IOType;
import engine_modules_helpers.ModuleIO;
import utils.BPacket;
import utils.Common;
import utils.Digraph;
import utils.Node;

import java.util.*;

public class Pipeline {
    public Common.PipelineType pipelineType;
    public String name;
    public Long port;
    public Common.ProcessingMode mode;
    public String itf;
    public String srcIp;
    public String protocol;
    public Common.Verdict defaultVerdict;
    Digraph graph;

    public Pipeline(Common.PipelineType pipelineType, String name, Long port, Common.ProcessingMode mode, String itf,
                    String srcIp, String protocol, Common.Verdict defaultVerdict) {
        this.pipelineType = pipelineType;
        this.name = name;
        this.port = port;
        this.mode = mode;
        this.itf = itf;
        this.srcIp = srcIp;
        this.protocol = protocol;
        this.defaultVerdict = defaultVerdict;
        this.graph = new Digraph();
    }

    private LinkedList<EngineModule> parseModules(LinkedList<String> modules) {
        LinkedList<EngineModule> engineModules = new LinkedList<>();

        for (String module : modules) {
            engineModules.addLast(EngineModule.createModule(module));
        }

        return engineModules;
    }

    public void addModules(LinkedList<String> modules) {
        LinkedList<EngineModule> engineModules = this.parseModules(modules);

        if (this.graph.rootNode == null) {
            this.graph.createRootNode(engineModules.removeFirst());
        }

        for (EngineModule module : engineModules) {
            this.graph.addNode(new Node(module));
        }
    }

    public boolean connectModules(String srcModString, String dstModString) {
        UUID srcNodeId = null;
        UUID dstNodeId = null;
        EngineModule srcMod = EngineModule.createModule(srcModString);
        EngineModule dstMod = EngineModule.createModule(dstModString);

        if (dstMod.getInputMode() == Common.InputMode.SingleInput) {
            if (srcMod.getOutputType() != dstMod.getInputType()) {
                System.out.println("The output type of " + srcModString + " doesn't match" +
                        "the input type of " + dstModString);
                return false;
            }
        }

        for (Node node : this.graph.getNodes()) {
            if (node.getModType().equals(srcMod.getModType()))
                srcNodeId = node.nodeId;
            else if (node.getModType().equals(dstMod.getModType()))
                dstNodeId = node.nodeId;
        }

        if (srcNodeId != null && dstNodeId != null)
            this.addConnection(srcNodeId, dstNodeId);

        return true;
    }

    private void addConnection(UUID srcNodeId, UUID dstNodeId) {
        this.graph.addConnection(srcNodeId, dstNodeId);
    }

    private static class QueueObject {
        UUID nodeId;
        ModuleIO moduleIO;

        QueueObject(UUID nodeId, ModuleIO moduleIO) {
            this.nodeId = nodeId;
            this.moduleIO = moduleIO;
        }
    }

    public BPacket run(byte[] data, JMSInterface jmsItf) {
        BPacket packet = new BPacket(data, this.defaultVerdict);
        LinkedList<QueueObject> queue = new LinkedList<>();

        queue.addLast(new QueueObject(this.graph.rootNode.nodeId, new ModuleIO()));
        ModuleIO output = null;
        HashMap<UUID, LinkedList<ModuleIO>> inputMap = new HashMap<>();
        while (!queue.isEmpty()) {
            QueueObject object = queue.removeFirst();
            UUID currentNodeId = object.nodeId;
            ModuleIO input = object.moduleIO;
            Node currentNode = this.graph.getNode(currentNodeId);

            switch (currentNode.getModInputMode()) {
                case SingleInput:
                    output = currentNode.process(packet, input, jmsItf);
                    break;
                case MultipleInput:
                    LinkedList<ModuleIO> inputs = inputMap.get(currentNodeId);
                    if (inputs == null)
                        inputs = new LinkedList<>();

                    inputs.addLast(input);
                    inputMap.put(currentNodeId, inputs);

                    if (inputs.size() == this.graph.getIncidents(currentNodeId).size()) {
                        IOData ioData = new IOData.IOList(inputs);

                        ModuleIO inputData = new ModuleIO(IOType.List, ioData);

                        output = currentNode.process(packet, inputData, jmsItf);

                        inputMap.remove(currentNodeId);
                    }

                    break;
            }

            if (packet.isFinalVerdict) {
                return packet;
            }

            for (UUID nodeId : this.graph.getNeighbours(currentNodeId)) {
                queue.addLast(new QueueObject(nodeId, output));
            }
        }

        return packet;
    }
}

