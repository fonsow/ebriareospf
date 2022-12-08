from briareos.utils import logger

from threading import Thread

import zmq
import uuid
import time


# TODO zmq SSL


class Client:
    def __init__(self, zclient, client_config):
        self._zclient = zclient
        self.id = uuid.uuid1()
        self._context = zmq.Context()
        self._init_sockets(client_config)

    def _init_sockets(self, client_config):
        broker_config = client_config.get("broker")
        self.broker_address = get_address(broker_config.get("ip"), broker_config.get("port"))
        self._connection = self._context.socket(zmq.DEALER)
        # self._connection.setsockopt(zmq.LINGER, 0)
        self._connection.identity = "%s" % self.id

    def start(self):
        logger.info("Connecting to Z-Broker: '%s'" % self.broker_address)
        self._connection.connect(self.broker_address)

    def stop(self):
        self._connection.close()
        self._context.term()

    def process(self, data, extra_param):
        self._connection.send(data, zmq.SNDMORE)
        self._connection.send_string(extra_param)

        # TODO "sink" to receive results?


class Worker:
    ready_msg = "WORKER READY"

    def __init__(self, zworker, worker_config):
        self._zworker = zworker
        self.id = uuid.uuid1()
        self._context = zmq.Context()
        self._init_sockets(worker_config)

    def _init_sockets(self, worker_config):
        broker_config = worker_config.get("broker")
        self.broker_address = get_address(broker_config.get("ip"), broker_config.get("port"))
        self._connection = self._context.socket(zmq.REQ)
        self._connection.setsockopt(zmq.LINGER, 0)
        self._connection.identity = "%s" % self.id

    def start(self):
        logger.subinfo("Connecting to Z-Broker: '%s'" % self.broker_address)
        self._connection.connect(self.broker_address)
        run_thread = Thread(target=self.run)
        run_thread.daemon = True
        run_thread.start()

    def stop(self):
        self._connection.close()
        self._context.term()

    def run(self):
        self._connection.send(Worker.ready_msg)

        while True:
            try:
                address = self._connection.recv()
            except zmq.ContextTerminated:
                return

            data = self._connection.recv()
            extra_param = self._connection.recv()
            #time.sleep(1)  # TODO remove this -> testing purposes only!
            result = self._zworker.process(data, extra_param)

            #print(result)
            self._connection.send(address, zmq.SNDMORE)
            self._connection.send(result)


class Broker:
    def __init__(self, zbroker, broker_config):
        self._zbroker = zbroker
        self.id = uuid.uuid1()
        self._context = zmq.Context()
        self._poller = None
        self._init_sockets(broker_config)
        self.available_workers = []
        self.connected_workers = []
        self.n_tasks = 0

    def _init_sockets(self, broker_config):
        frontend_config = broker_config.get("frontend")
        backend_config = broker_config.get("backend")
        frontend_ip = frontend_config.get("ip")
        frontend_port = frontend_config.get("port")
        backend_ip = backend_config.get("ip")
        backend_port = backend_config.get("port")

        if frontend_ip is None:
            frontend_ip = "*"
        if backend_ip is None:
            backend_ip = "*"

        self.frontend_address = get_address(frontend_ip, frontend_port)
        self.backend_address = get_address(backend_ip, backend_port)
        self._frontend = self._context.socket(zmq.ROUTER)
        self._frontend.setsockopt(zmq.LINGER, 0)
        self._backend = self._context.socket(zmq.ROUTER)
        self._backend.setsockopt(zmq.LINGER, 0)

    def start(self):
        logger.info("Starting Z-Broker")
        logger.subinfo("Frontend address: '%s'" % self.frontend_address)
        logger.subinfo("Backend address: '%s'" % self.backend_address)
        self._frontend.bind(self.frontend_address)
        self._backend.bind(self.backend_address)
        self._poller = zmq.Poller()
        self._poller.register(self._backend, zmq.POLLIN)
        self._poller.register(self._frontend, zmq.POLLIN)

        broker_thread = Thread(target=self.run)
        broker_thread.daemon = True
        broker_thread.start()

    def stop(self):
        self._backend.close()
        self._frontend.close()
        self._context.term()
        print("Tasks: %s" % self.n_tasks)


    def run(self):
        while True:
            sockets = dict(self._poller.poll())
            if self._backend in sockets \
                    and sockets[self._backend] == zmq.POLLIN:
                worker_id = self._backend.recv()
                self.available_workers.append(worker_id)
                if worker_id not in self.connected_workers:
                    self.connected_workers.append(worker_id)

                self._backend.recv()  # empty
                client_id = self._backend.recv()

                if client_id == Worker.ready_msg:
                    logger.info("New worker: %s" % worker_id)
                    self._zbroker.worker_manager.already_requested = False
                    self._zbroker.worker_manager.n_workers += 1
                else:
                    reply = self._backend.recv()
                    self.n_tasks += 1
                    # self._frontend.send(client_id, zmq.SNDMORE) # TODO send to handler
                    # self._frontend.send(reply)

            if self.available_workers:
                if self._frontend in sockets and sockets[self._frontend] == zmq.POLLIN:
                    client_id = self._frontend.recv()
                    data = self._frontend.recv()
                    extra_param = self._frontend.recv()

                    worker_id = self.available_workers.pop()

                    #logger.debug("Sending new task to worker %s" % worker_id)
                    self._backend.send(worker_id, zmq.SNDMORE)
                    self._backend.send("", zmq.SNDMORE)
                    self._backend.send(client_id, zmq.SNDMORE)
                    self._backend.send(data, zmq.SNDMORE)
                    self._backend.send(extra_param)


def get_address(ip, port):
    return "tcp://%s:%s" % (ip, port)
