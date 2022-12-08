from briareos.utils import loader
from briareos.utils import logger
from briareos.utils.modules import *

import os
import signal
import traceback
from threading import Thread
import Queue
import time

remove_me = []

class Engine:
    def __init__(self, parent, pipelines_root_path=PIPELINES_ROOT_PATH,
                 modules_root_path=MODULES_ROOT_PATH):
        self._parent = parent
        self.pipelines = {}
        self.pipeline_list = []
        self.pipelines_root_path = pipelines_root_path
        self._parallel_queue = Queue.Queue()
        self.n_tasks = 0
        sys.path.append(modules_root_path)

    def start(self):
        logger.info("Starting Processing Engine...")
        self._load_pipelines()

    def stop(self):
        logger.subinfo("Stopping Processing Engine...")
        for pipeline in self.pipeline_list:
            pipeline.stop()

    def _load_pipelines(self):
        pass

    def load_pipeline(self, path, empty=None):
        return False


class BhcEngine(Engine):
    def __init__(self, bhc, pipelines_root_path=PIPELINES_ROOT_PATH,
                 modules_root_path=MODULES_ROOT_PATH):
        Engine.__init__(self, bhc, pipelines_root_path, modules_root_path)
        self._bhc = bhc
        self.queue_map = {}

    def _load_pipelines(self):
        pipelines = self._bhc.config.get("pipelines")
        n_pipelines = 0

        for pipeline in pipelines:
            try:
                path = "%s%s.json" % (self.pipelines_root_path, pipeline.get("name"))
                ports = loader.get_ports(pipeline)

                logger.subinfo("Loading pipeline: '%s'" % path)
                if self.load_pipeline(path, ports):
                    n_pipelines += 1
            except:
                logger.warning(traceback.format_exc())

        if n_pipelines > 0:
            logger.info("%s pipelines loaded" % n_pipelines)
            logger.success("Processing engine is running")
        else:
            logger.error("Stopping BHC: No pipelines loaded")
            os.kill(os.getpid(), signal.SIGTERM)

            # TODO test if multiple pipelines are working in the same port!

    def load_pipeline(self, path, ports=None):
        pipelines = loader.import_pipeline(path, self._bhc, ports)

        if pipelines is None:
            return False

        self.pipeline_list += pipelines

        # TODO improve this code?
        for pipeline in pipelines:
            if self.pipelines.get(pipeline.port) is None:
                self.pipelines[pipeline.port] = {}

            if self.pipelines[pipeline.port].get(pipeline.name) is None:
                self.pipelines[pipeline.port][pipeline.name] = {}

            if self.pipelines[pipeline.port][pipeline.name].get(pipeline.type) is None:
                self.pipelines[pipeline.port][pipeline.name][pipeline.type] = []

            self.pipelines[pipeline.port][pipeline.name][pipeline.type].append(pipeline)
        return True

    def process(self, packet_payload, queue_id):
        pipeline = self.queue_map[queue_id]
        try:
            self.n_tasks += 1
            return self.run_map[pipeline.mode](self, packet_payload, pipeline, queue_id)
        except:
            logger.warning(traceback.format_exc())
            return pipeline.default_verdict

    def run_inline(self, packet_payload, pipeline, queue_id):
        return pipeline.run(packet_payload)

    def run_parallel(self, packet_payload, pipeline, queue_id):
        self._bhc.parallel_consumer.process(packet_payload, queue_id)
        return pipeline.default_verdict

    def run_distributed(self, packet_payload, pipeline, queue_id):
        self._bhc.bds_interface.process(packet_payload, pipeline.name)
        return pipeline.default_verdict

    run_map = {INLINE_LOCAL_PROCESSING_MODE: run_inline,
               PARALLEL_PROCESSING_MODE: run_parallel,
               DISTRIBUTED_PROCESSING_MODE: run_distributed}


class WorkerEngine(Engine):
    def __init__(self, zworker, pipelines_root_path=PIPELINES_ROOT_PATH,
                 modules_root_path=MODULES_ROOT_PATH):
        Engine.__init__(self, zworker, pipelines_root_path, modules_root_path)
        self._zworker = zworker

    def _load_pipelines(self):
        pipelines = self._parent.config.get("pipelines")
        n_pipelines = 0

        for pipeline in pipelines:
            try:
                path = "%s%s.json" % (self.pipelines_root_path, pipeline.get("name"))

                logger.subinfo("Loading pipeline: '%s'" % path)
                if self.load_pipeline(path):
                    n_pipelines += 1
            except:
                logger.warning(traceback.format_exc())

        if n_pipelines > 0:
            logger.info("%s pipelines loaded" % n_pipelines)
            logger.success("Processing engine is running")
        else:
            logger.error("Stopping Z-Worker: No pipelines loaded")
            os.kill(os.getpid(), signal.SIGTERM)

    def load_pipeline(self, path, empty=None):
        pipeline = loader.import_pipeline(path, self._zworker)

        if pipeline is None:
            return False

        self.pipeline_list += pipeline
        pipeline = pipeline[0]
        self.pipelines[pipeline.name] = pipeline
        return True

    def process(self, packet_payload, pipeline_name):
        result = self.pipelines[pipeline_name].run(packet_payload)
        #print("Pipeline result %s" % result)
        return str(result) # TODO
