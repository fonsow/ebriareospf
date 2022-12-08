from briareos.utils.modules import *
from briareos.utils import logger

from pygraph.algorithms import cycles
from pygraph.classes.digraph import digraph


# TODO ver porque e que se uma pipeline tiver um erro o trafego vai para a seguinte?

class Pipeline:
    def __init__(self, pipeline_type, protocol=None, name="", port=None, mode=INLINE_LOCAL_PROCESSING_MODE,
                 interface=None, source_ip=None, default_verdict=VERDICT_ACCEPT):
        self.type = pipeline_type
        self.name = name
        self.port = port
        self.mode = mode
        self.interface = interface
        self.source_ip = source_ip
        self.packet_class = BPacket

        self.protocol = protocol
        if self.protocol == "tcp":
            self.packet_class = TcpPacket

        # TODO udp, icmp

        self.graph = digraph()
        self.root_node = None
        self.default_verdict = default_verdict
        self.nodes = []

    def stop(self):
        for node in self.nodes:
            try:
                node.module.cleanup()
            except AttributeError:
                pass

    def add_modules(self, modules):
        for bhc_module in modules:
            self.nodes.append(Node(bhc_module))
        self._add_nodes(self.nodes)

    def connect_modules(self, source_module, target_module):
        source_node = None
        target_node = None
        for i in range(len(self.nodes)):
            if self.nodes[i].module.__module__ == source_module:
                source_node = self.nodes[i]
            elif self.nodes[i].module.__module__ == target_module:
                target_node = self.nodes[i]

        if source_node is not None and target_node is not None:
            if target_node.module.input_mode == SINGLE_INPUT_MODE:
                output_type = source_node.module.output_type
                input_type = target_node.module.input_type
                if input_type != output_type:
                    return False, (input_type, output_type)
            self._add_connection(source_node, target_node)
        return True, None

    def _add_nodes(self, nodes):
        if self.root_node is None:
            self.root_node = nodes[0]
        self.graph.add_nodes(nodes)

    def _add_connection(self, source_node, target_node):
        self.graph.add_edge((source_node, target_node))

    # TODO multithreading @ same depth
    def run(self, data):
        #self.root_node.process(data, data)
        #return BPacket(data, self.default_verdict)

        #packet = self.packet_class(data, self.default_verdict)
        packet = BPacket(data, self.default_verdict)

        queue = [(self.root_node, packet)]
        output = None

        while len(queue):
            current_node, obj = queue.pop(0)
            # depth = q.get("depth")

            if current_node.input_mode == SINGLE_INPUT_MODE:
                # logger.info("Processing: %s -> INPUT: %s | Depth: %s" % (current_node.module.name, "obj", 0))
                output = current_node.process(packet, obj)
            elif current_node.input_mode == MULTIPLE_INPUT_MODE:
                current_node.inputs.append(obj)
                if len(current_node.inputs) == \
                        len(self.graph.incidents(current_node)):
                    #logger.info("Processing: %s -> INPUT: %s | Depth: %s" %
                     #(current_node.module.name, "obj", 0))
                    output = current_node.process(packet,
                                                  current_node.inputs)
                    current_node.inputs = []

            if packet.is_final_verdict:
                return packet

            for neighbor_node in self.graph.neighbors(current_node):
                queue.append((neighbor_node, output)) # depth+1

        return packet

    def is_valid(self):
        return self._is_acyclic()

    def _is_acyclic(self):
        return cycles.find_cycle(self.graph) == []

    def __str__(self):
        s_type = "input"
        if self.type == OUTPUT_PIPELINE:
            s_type = "output"
        return "Pipeline name: %s, Type: %s, Port: %s, Protocol: %s, Interface: %s" \
               % (self.name, s_type, self.port, self.protocol, self.interface)


class Node:
    def __init__(self, bhc_module):
        self.module = bhc_module
        self.input_mode = bhc_module.input_mode
        self.inputs = []

    def process(self, packet, obj):
        try:
            return self.module.process(packet, obj)
        except TypeError:
            return self.module.process(packet)
