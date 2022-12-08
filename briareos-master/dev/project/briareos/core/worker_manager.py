
from briareos.utils import logger
from briareos.core import distributed_zmq

from threading import Thread

import zmq
import docker
import time
import uuid

USAGE_MSG = "REPORT USAGE"
START_NEW_INSTANCE_MSG = "NEW INSTANCE"
STOP_INSTANCE_MSG = "STOP INSTANCE"

# TODO containers: limit CPU and RAM
# TODO thread lock
# TODO choose a cluster to start new instance


class WorkerManager:
    metric_interval = 15  #TODO config
    lower_bound = 10
    upper_bound = 70

    def __init__(self, zbroker, worker_manager_config):
        self._zbroker = zbroker
        self._context = zmq.Context()
        self._init_sockets(worker_manager_config)
        self.interval = int(worker_manager_config.get("interval"))  # TODO config and default value
        self.worker_stats = {}
        self.connected_zworkers = []  # TODO connected_*z*workers (distributed_zmq.py)
        self.average_cpu = None
        self.average_memory = None
        self.already_requested = False
        self.n_workers = 0
        self.clusters = {}

    def _init_sockets(self, worker_manager_config):
        ip = worker_manager_config.get("ip")
        port = worker_manager_config.get("port")
        sink_port = worker_manager_config.get("sink_port")

        if ip is None:
            ip = "*"

        self.publisher_address = distributed_zmq.get_address(ip, port)
        self.sink_address = distributed_zmq.get_address(ip, sink_port)
        self._publisher = self._context.socket(zmq.PUB)
        self._sink = self._context.socket(zmq.PULL)

    def start(self):
        logger.subinfo("Worker Manager address: '%s' | '%s'" % (self.publisher_address, self.sink_address))
        self._publisher.bind(self.publisher_address)
        self._sink.bind(self.sink_address)

        run_publisher_thread = Thread(target=self.run_publisher)
        run_publisher_thread.daemon = True
        run_publisher_thread.start()

        run_sink_thread = Thread(target=self.run_sink)
        run_sink_thread.daemon = True
        run_sink_thread.start()

        run_thread = Thread(target=self.run)
        run_thread.daemon = True
        run_thread.start()

    # TODO stop

    def run_publisher(self):
        while True:
            self._publisher.send(USAGE_MSG)
            time.sleep(self.interval+0.01)
            # self._remove_disconnected_workers()

    def run_sink(self):
        while True:
            cluster_id = self._sink.recv()
            stats = self._sink.recv_json()
            self._process_usage_stats(cluster_id, stats)


    def run(self):
        while True:
            # logger.debug(self.connected_zworkers)
            try:
                self._sliding_windows()
            except:
                pass

            if self.average_cpu is not None and self.average_memory is not None:
                logger.debug("Metrics: avg_cpu=%s avg_memory=%s" % (self.average_cpu, self.average_memory))
                if self.average_cpu >= self.upper_bound or self.average_memory >= self.upper_bound:
                    if not self.already_requested:
                        print("Trying to start worker")
                        
                        cluster_id = self.choose_cluster_id()
                        self.clusters[cluster_id] += 1
                        self._publisher.send(START_NEW_INSTANCE_MSG + " " + cluster_id)
                        self.already_requested = True
                        

                    # self._publisher.send(cluster_id)
                elif self.average_cpu <= self.lower_bound or self.average_memory <= self.lower_bound:
                    if self.n_workers > 1:
                        '''
                        pass
                        print("Trying to stop worker")
                        self._publisher.send(STOP_INSTANCE_MSG)
                        self.n_workers -= 1
                        #self._publisher.send(cluster_id)
                        '''

            time.sleep(self.interval)
            logger.debug("Tasks: %s | workers: %s" % (self._zbroker.broker.n_tasks, self.n_workers))

    def choose_cluster_id(self):
        '''
        clusters = {}
        for k in self.worker_stats:
            cluster_id = self.worker_stats[k][0]["cluster"]
            try:
                clusters[cluster_id] += 1
            except:
                clusters[cluster_id] = 1
        '''

        cluster_id = ""
        min_workers = 0xffffffff
        for c in self.clusters:
            if self.clusters[c] < min_workers:
                cluster_id = c
                min_workers = self.clusters[c]
        print(cluster_id),
        print(min_workers)
        return cluster_id


    def _sliding_windows(self):
        cpu_values = []
        memory_values = []
        worker_stats_to_remove = []

        current_time = time.time()
        for worker_id in self.worker_stats:
            stats = self.worker_stats[worker_id]
            for stat in stats:
                stat_time = stat["time"]
                if int(current_time - stat_time) <= self.metric_interval:
                    cpu_values.append(stat["cpu"])
                    memory_values.append(stat["memory"])
                else:
                    worker_stats_to_remove.append(worker_id)

        for worker_id in worker_stats_to_remove:
            del self.worker_stats[worker_id]

        self.average_cpu = average(cpu_values)
        self.average_memory = average(memory_values)

    def _process_usage_stats(self, cluster_id, stats):
        for zworker_id in stats:
            cpu_usage = stats[zworker_id]["cpu"]
            memory_usage = stats[zworker_id]["memory"]
            if zworker_id not in self.connected_zworkers:
                self.connected_zworkers.append(zworker_id)

            self._update_worker_stats(cluster_id, zworker_id, cpu_usage, memory_usage)

    def _update_worker_stats(self, cluster_id, zworker_id, cpu_usage, memory_usage):
        if self.worker_stats.get(zworker_id) is None:
            self.worker_stats[zworker_id] = []

        if self.clusters.get(cluster_id) is None:
            self.clusters[cluster_id] = 1

        self.worker_stats[zworker_id].append({"cluster": cluster_id, "cpu": cpu_usage,
                                              "memory": memory_usage, "time": time.time()})

    def _remove_disconnected_workers(self):
        worker_stats_to_remove = []
        for worker_id in self.worker_stats:
            if worker_id not in self.connected_zworkers:
                worker_stats_to_remove.append(worker_id)

        for worker_id in worker_stats_to_remove:
            del self.worker_stats[worker_id]

        self.connected_zworkers = []  # TODO sync or remove this...


class Cluster:
    docker_image_name = "zworker:latest"
    cpu_quota = 10000  # TODO config
    max_memory = 512  # TODO config

    def __init__(self, zreporter_config):
        self.id = uuid.uuid1()
        self._context = zmq.Context()
        self._init_sockets(zreporter_config)
        self._docker_client = docker.from_env()
        self.containers = []

    def _init_sockets(self, zreporter_config):
        worker_manager_config = zreporter_config.get("worker_manager")
        self.worker_manager_address = distributed_zmq.get_address(worker_manager_config.get("ip"), worker_manager_config.get("port"))
        self.sink_address = distributed_zmq.get_address(worker_manager_config.get("ip"), worker_manager_config.get("sink_port"))
        self._publisher = self._context.socket(zmq.SUB)
        self._publisher.setsockopt(zmq.SUBSCRIBE, "")
        self._sink = self._context.socket(zmq.PUSH)
        self.n_instances = 0

    def start(self):
	#n_workers = 2
	#for i in range(n_workers):
	#        self.start_new_instance()
        self.start_new_instance()

        logger.info("Connecting to Worker Manager: '%s' | '%s'" % (self.worker_manager_address, self.sink_address))
        self._publisher.connect(self.worker_manager_address)
        self._sink.connect(self.sink_address)
        run_thread = Thread(target=self.run)
        run_thread.daemon = True
        run_thread.start()

    def stop(self):
        self.stop_all_instances()

    def start_new_instance(self):
        logger.info("Starting new Z-Worker instance")
        self.n_instances += 1
        self.containers.append(self._docker_client.containers.run(Cluster.docker_image_name, detach=True,
                                                                  network_mode="host", cpu_quota=120000))
        # TODO network/config

    def stop_instance(self):
        if self.containers:
            logger.info("Stopping Z-Worker instance")
            self.containers[0].stop()
            self.containers.pop()

    def stop_all_instances(self):
        logger.info("Stopping all Z-Worker instances. Please wait...")
        for instance in self.containers:
            instance.stop()

    def run(self):
        while True:
            data = self._publisher.recv()
            print("Recv %s" % data)
            if data == USAGE_MSG:
                usage_stats = self.get_usage_stats()
                self._sink.send("%s" % self.id)
                self._sink.send_json(usage_stats)
            elif START_NEW_INSTANCE_MSG in data:
                cluster_id = data.split()[2]
                print(cluster_id),
                print(self.id)
                print(str(self.id) == cluster_id)
                if str(self.id) == cluster_id:
                    print("Ok %s" % self.n_instances)
                    if self.n_instances < 5:
                        logger.debug("Starting new Z-Worker instance")
                        self.start_new_instance()
            elif data == STOP_INSTANCE_MSG:
                logger.debug("Stopping one Z-Worker instance")
                self.stop_instance()

    def get_usage_stats(self):
        usage_stats = {}
        for container in self.containers:
            container_stats = container.stats(stream=False)
            container_id = container_stats["id"]
            memory_usage = self.get_memory_usage(container_stats)
            cpu_usage = self.get_cpu_usage(container_stats)
            usage_stats[container_id] = {"memory": memory_usage, "cpu": cpu_usage}
        return usage_stats

    @staticmethod
    def get_cpu_usage(container_stats):
        try:
            cpu_percent = 0.0
            precpu_stats = container_stats["precpu_stats"]
            cpu_stats = container_stats["cpu_stats"]
            cpu_total_usage = cpu_stats["cpu_usage"]["total_usage"]
            precpu_total_usage = precpu_stats["cpu_usage"]["total_usage"]
            system_cpu_usage = cpu_stats["system_cpu_usage"]
            presystem_cpu_usage = precpu_stats["system_cpu_usage"]
            percpu_usage = cpu_stats["cpu_usage"]["percpu_usage"]

            cpu_delta = cpu_total_usage - precpu_total_usage
            system_delta = system_cpu_usage - presystem_cpu_usage

            if cpu_delta > 0 and system_delta > 0:
                cpu_percent = (cpu_delta*1.0 / system_delta) * len(percpu_usage) * 100.0

            return cpu_percent
        except:
            return 0

    @staticmethod
    def get_memory_usage(container_stats):
        try:
            memory_stats = container_stats["memory_stats"]
            usage = memory_stats["usage"]
            limit = memory_stats["limit"]
            return usage*100.0/limit
        except:
            return 0


# TODO move this to utils?
def average(l):
    if not l:
        return None
    return sum(l) / float(len(l))
