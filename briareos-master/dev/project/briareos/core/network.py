from briareos.utils import iptables
from briareos.common import *
from briareos.utils import logger

from netfilterqueue import NetfilterQueue
from threading import Thread


class Interceptor:
    def __init__(self, bhc):
        self._bhc = bhc
        self._processing_engine = bhc.processing_engine
        self._current_queue_id = 0
        self._dispatchers = {}
        self._nfqueues = []

    def start(self):
        logger.info("Starting Interceptor...")
        self._configure_iptables()
        self._bind_queues()
        logger.success("Interceptor is running")

    def stop(self):
        logger.subinfo("Stopping Interceptor...")
        iptables.clean_rules()

        for nf_queue in self._nfqueues:
            nf_queue.unbind()

    def _bind_queues(self):
        logger.info("Binding queues...")

        for queue_id in self._processing_engine.queue_map:
            nfqueue = NetfilterQueue()
            self._dispatchers[queue_id] = Dispatcher(queue_id, self._handler)
            nfqueue.bind(queue_id, self._dispatchers[queue_id].run)
            thread = Thread(target=self._start_interception, args=(nfqueue,))
            thread.daemon = True
            thread.start()
            self._nfqueues.append(nfqueue)

        logger.subinfo("%s queues binded" % len(self._processing_engine.queue_map))

    @staticmethod
    def _start_interception(nfqueue):
        nfqueue.run()

    def _handler(self, nfpacket, queue_id):
        # TODO BUG pacotes entram em varias queues!
        # TODO -> iptables...
        # logger.debug(queue_id)

        result = self._processing_engine.process(nfpacket.get_payload(), queue_id)

        # TODO return none em vez de verdict no modo paralelo
        try:
            packet = result
            if packet.verdict == VERDICT_ACCEPT:
                if packet.new_payload:
                    nfpacket.set_payload(packet.payload)
                nfpacket.accept()
            elif packet.verdict == VERDICT_DROP:
                # TODO mark em vez de drop para nao serem mais analisados pacotes iguais?
                # TODO exemplo do dropping packet port 80 com curl
                nfpacket.drop()
        except AttributeError:
            if result == VERDICT_DROP:
                nfpacket.drop()
            nfpacket.accept()

    def _get_current_queue_id(self):
        queue_id = self._current_queue_id
        if queue_id > MAX_QUEUE_ID:
            return -1
        self._current_queue_id += 1
        return queue_id

    def _configure_iptables(self):
        logger.info("Configuring iptables...")

        for pipeline in self._processing_engine.pipeline_list:
            queue_id = self._get_current_queue_id()
            if queue_id == -1:
                logger.warning("Maximum number of queues reached")
                return

            chain = iptables.CHAIN_INPUT
            if pipeline.type == OUTPUT_PIPELINE:
                chain = iptables.CHAIN_OUTPUT

            if iptables.create_nfqueue_rule(queue_id, chain, pipeline.port, pipeline.protocol,
                                            pipeline.interface, pipeline.source_ip):
                self._processing_engine.queue_map[queue_id] = pipeline
            else:
                logger.warning("Error configuring iptables for pipeline: %s" % pipeline.name)

        # for key in self._processing_engine.queue_map:
        #     logger.debug("%s: %s" % (key, self._processing_engine.queue_map[key]))

class Dispatcher:
    def __init__(self, queue_id, handler):
        self.queue_id = queue_id
        self.handler = handler

    def run(self, nfpacket):
        self.handler(nfpacket, self.queue_id)
