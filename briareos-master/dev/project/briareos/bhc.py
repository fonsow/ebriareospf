from briareos.core import processing
from briareos.core import network
from briareos.core import computing
from briareos.core import management
from briareos.utils import logger
from briareos.utils import loader


class BHC:
    def __init__(self):
        logger.info("Initializing BHC")
        self.config = loader.import_bhc_config()
        self.processing_engine = processing.BhcEngine(self)
        self.bds_interface = computing.BdsInterface()
        self.parallel_consumer = computing.ParallelConsumer(self)
        # TODO start distributed system if there is at least one pipeline in distributed mode
        self.bms_interface = management.BmsInterface()
        self.interceptor = network.Interceptor(self)

    def start(self):
        self.processing_engine.start()
        self.bds_interface.start()
        self.parallel_consumer.start()
        # self.bms_interface.start()
        self.interceptor.start()
        logger.success("BHC is running")

    def stop(self):
        print("")
        logger.info("Stopping BHC...")
        self.processing_engine.stop()
        self.bds_interface.stop()
        self.interceptor.stop()
