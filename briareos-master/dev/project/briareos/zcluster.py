from briareos.core import worker_manager

from briareos.utils import logger
from briareos.utils import loader
from briareos.common import *


class ZCluster:
    def __init__(self, cluster_config_path=ZCLUSTER_CONFIG_PATH):
        logger.info("Initializing Briareos Z-Cluster")
        self.config = loader.import_zcluster_config(cluster_config_path)
        self.cluster = worker_manager.Cluster(self.config)

    def start(self):
        self.cluster.start()
        logger.info("Z-Cluster ID: %s" % self.cluster.id)
        logger.success("Briareos Z-Cluster is running")

    def stop(self):
        logger.info("Stopping Briareos Z-Cluster")
        self.cluster.stop()
