from briareos.zclient import ZClient
from briareos.utils import logger
from briareos.common import *

from threading import Thread
import time
import Queue


class BdsInterface:
    def __init__(self, client_config_path=ZCLIENT_CONFIG_PATH):
        self.zclient = ZClient(client_config_path)
        self.queue = Queue.Queue()

    def start(self):
        logger.info("Starting BDS Interface")
        self.zclient.start()
        run_thread = Thread(target=self.run)
        run_thread.daemon = True
        run_thread.start()
        logger.success("BDS is ready")

    def stop(self):
        logger.subinfo("Stopping Distributed System...")
        try:
            self.zclient.stop()
        except AssertionError:
            pass

    def run(self):
        while True:
            data, pipeline_name = self.queue.get()
            self.zclient.process(data, pipeline_name)
            self.queue.task_done()
            time.sleep(0.001)

    def process(self, data, pipeline_name):
        self.queue.put((data, pipeline_name))


class ParallelConsumer:
    def __init__(self, bhc):
        self._bhc = bhc
        self.queue = Queue.Queue()
        self.n_tasks = 0

    def start(self):
        t = Thread(target=self.run_task_counter)
        t.daemon = True
        t.start()
        run_thread = Thread(target=self.run)
        run_thread.daemon = True
        run_thread.start()

    def run(self):
        while True:
            data, queue_id = self.queue.get()
            pipeline = self._bhc.processing_engine.queue_map[queue_id]
            pipeline.run(data)
            self.queue.task_done()
            self.n_tasks += 1
            time.sleep(0.001)

    def run_task_counter(self):
        return
        while True:
            #logger.debug("Tasks: %s" % self.n_tasks)
            time.sleep(5)

    def process(self, data, queue_id):
        #self.queue.put((data, pipeline))
        #pipeline.run(data)
        self.queue.put((data, queue_id))
