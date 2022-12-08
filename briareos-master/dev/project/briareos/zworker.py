from briareos.core import distributed_zmq
from briareos.core import processing

from briareos.utils import logger
from briareos.utils import loader
from briareos.common import *

import psutil
import uuid


class ZWorker:
    def __init__(self, worker_config_path=ZWORKER_CONFIG_PATH):
        logger.info("Initializing Briareos Z-Worker")
        self.id = uuid.uuid1()
        self.config = loader.import_zworker_config(worker_config_path)
        self.processing_engine = processing.WorkerEngine(self)
        self.workers = []
        self._init_workers()

    def _init_workers(self):
        max_workers = self.get_max_workers()

        for i in range(max_workers):
            self.workers.append(distributed_zmq.Worker(self, self.config))

    def start(self):
        self.processing_engine.start()

        workers_info = "subworkers"
        max_workers = len(self.workers)
        if max_workers == 1:
            workers_info = "subworker"

        logger.info("Starting %s %s" % (max_workers, workers_info))
        for worker in self.workers:
            worker.start()
            logger.subinfo("Subworker ID: %s" % worker.id)

        logger.info("Z-Worker ID: %s" % self.id)
        logger.success("Briareos Z-Worker is running")

    def stop(self):
        logger.info("Stopping Z-Worker...")
        logger.subinfo("Stopping %s workers..." % len(self.workers))
        for worker in self.workers:
            worker.stop()
        self.processing_engine.stop()

    def process(self, packet_payload, pipeline_name):
        return self.processing_engine.process(packet_payload, pipeline_name)

    @staticmethod
    def get_max_workers():
        return 1
        return psutil.cpu_count()
