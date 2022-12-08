package utils;

import engine_modules.EngineModule;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

public class Digraph {
    LinkedList<Node> nodes;
    public Node rootNode;
    public HashMap<UUID, LinkedList<UUID>> neighbourMap;
    public HashMap<UUID, LinkedList<UUID>> incidentMap;

    public Digraph() {
        this.nodes = new LinkedList<>();
        this.rootNode = null;
        this.neighbourMap = new HashMap<>();
        this.incidentMap = new HashMap<>();
    }

    public Node getRootNode() {
        return this.rootNode;
    }

    public void createRootNode(EngineModule module) {
        this.rootNode = new Node(module);
        this.nodes.addLast(this.rootNode);
        this.neighbourMap.put(this.rootNode.nodeId, new LinkedList<>());
        this.incidentMap.put(this.rootNode.nodeId, new LinkedList<>());
    }

    public void addNode(Node node) {
        this.nodes.addLast(node);
        this.neighbourMap.put(node.nodeId, new LinkedList<>());
        this.incidentMap.put(node.nodeId, new LinkedList<>());
    }

    public void addConnection(UUID srcNodeId, UUID dstNodeId) {
        LinkedList<UUID> src_neighbours = this.neighbourMap.get(srcNodeId);
        if (src_neighbours == null) {
            src_neighbours = new LinkedList<>();
        }

        src_neighbours.addLast(dstNodeId);
        this.neighbourMap.put(srcNodeId, src_neighbours);

        LinkedList<UUID> dst_incidents = this.incidentMap.get(dstNodeId);
        if (dst_incidents == null) {
            dst_incidents = new LinkedList<>();
        }

        dst_incidents.addLast(srcNodeId);
        this.incidentMap.put(dstNodeId, src_neighbours);
    }

    public void addConnection(Node srcNode, Node dstNode) {
        this.addConnection(srcNode.nodeId, dstNode.nodeId);
    }

    public LinkedList<UUID> getIncidents(UUID nodeId) {
        return this.incidentMap.get(nodeId);
    }

    public LinkedList<UUID> getNeighbours(UUID nodeId) {
        return this.neighbourMap.get(nodeId);
    }

    public Node getNode(UUID nodeId) {
        for (Node node : this.nodes)
            if (node.nodeId.equals(nodeId))
                return node;

        return null;
    }

    public LinkedList<Node> getNodes() {
        return this.nodes;
    }
}
