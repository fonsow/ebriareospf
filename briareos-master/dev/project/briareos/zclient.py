from briareos.utils import loader
from briareos.common import *

from briareos.core import distributed_zmq


class ZClient:
    def __init__(self, client_config_path=ZCLIENT_CONFIG_PATH):
        self.config = loader.import_zclient_config(client_config_path)
        self.client = distributed_zmq.Client(self, self.config)

    def start(self):
        self.client.start()

    def stop(self):
        self.client.stop()

    def process(self, packet_payload, pipeline_name):
        return self.client.process(packet_payload, pipeline_name)
