from briareos.core import distributed_zmq
from briareos.core import worker_manager

from briareos.utils import logger
from briareos.utils import loader
from briareos.common import *

from threading import Thread


class ZBroker:
    def __init__(self, broker_config_path=ZBROKER_CONFIG_PATH):
        logger.info("Initializing Briareos Z-Broker")
        self.config = loader.import_zbroker_config(broker_config_path)
        self.broker = distributed_zmq.Broker(self, self.config)
        self.worker_manager = worker_manager.WorkerManager(self, self.config.get("worker_manager"))

    def start(self):
        self.worker_manager.start()
        self.broker.start()
        logger.info("Z-Broker ID: %s" % self.broker.id)
        logger.success("Briareos Z-Broker is running")

    def stop(self):
        logger.info("Stopping Briareos Z-Broker")
        self.broker.stop()
