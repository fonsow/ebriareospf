from briareos.common import *
from briareos.utils import logger
import briareos.core.pipeline
from briareos.utils.modules import *

import json
import importlib
import sys


# TODO better config, access config from all modules?, etc


def config_error(info=None):
    if info is None:
        msg = "Malformed config file"
    else:
        msg = "%s" % info
    logger.error(msg)
    sys.exit(1)


# BHC Config Parsing
# TODO check if config file is malformed
# TODO allow custom config path
def import_bhc_config():
    if not os.path.isfile(BHC_CONFIG_PATH):
        config_error("Stopping BHC: %s is missing" % BHC_CONFIG_PATH)

    f = open(BHC_CONFIG_PATH)
    s = f.read()
    f.close()

    try:
        config_object = json.loads(s)
        if config_object.get("pipelines") is None:
            config_error("pipeline config is missing")
        return config_object
    except:
        config_error()


def import_zclient_config(client_config_path):
    if not os.path.isfile(client_config_path):
        config_error("Stopping Z-Client: %s is missing" % client_config_path)

    f = open(client_config_path)
    s = f.read()
    f.close()

    try:
        config_object = json.loads(s)
        # TODO more checks
        if config_object.get("client") is None:
            config_error("Client config is missing")
        return config_object.get("client")
    except:
        config_error()


def import_zworker_config(worker_config_path):
    if not os.path.isfile(worker_config_path):
        config_error("Stopping Z-Worker: %s is missing" % worker_config_path)

    f = open(worker_config_path)
    s = f.read()
    f.close()

    try:
        config_object = json.loads(s)
        # TODO more checks
        if config_object.get("worker") is None:
            config_error("Worker config is missing")
        return config_object.get("worker")
    except:
        config_error()


def import_zbroker_config(broker_config_path):
    if not os.path.isfile(broker_config_path):
        config_error("Stopping Z-Broker: %s is missing" % broker_config_path)

    f = open(broker_config_path)
    s = f.read()
    f.close()

    try:
        config_object = json.loads(s)
        if config_object.get("broker") is None:
            config_error("Broker config is missing")
        return config_object.get("broker")
    except:
        config_error()


def import_zcluster_config(cluster_config_path):
    if not os.path.isfile(cluster_config_path):
        config_error("Stopping Z-Cluster: %s is missing" % cluster_config_path)

    f = open(cluster_config_path)
    s = f.read()
    f.close()

    try:
        config_object = json.loads(s)
        if config_object.get("cluster") is None:
            config_error("Cluster config is missing")
        return config_object.get("cluster")
    except:
        config_error()


# Pipeline Parsing
def import_pipeline(file_path, bhc, ports=None):
    try:
        f = open(file_path)
        s = f.read()
        f.close()
    except IOError:
        logger.subwarning("Pipeline file not found: '%s'" % file_path)
        return None

    try:
        pipeline_object = json.loads(s)
    except ValueError:
        logger.subwarning("Invalid json file" % file_path)
        return None

    modules_array = pipeline_object.get("modules")
    if modules_array is None:
        logger.subwarning("No modules specified")
        return None

    pipeline_type = INPUT_PIPELINE
    s_pipeline_type = pipeline_object.get("type")
    if s_pipeline_type is not None:
        if s_pipeline_type.lower() == "output":
            pipeline_type = OUTPUT_PIPELINE

    protocol = pipeline_object.get("protocol")
    if protocol is not None:
        protocol = protocol.lower()

    name = pipeline_object.get("name")
    if name is None:
        name = "Unknown Pipeline"

    mode = INLINE_LOCAL_PROCESSING_MODE
    s_mode = pipeline_object.get("mode")
    if s_mode is None:
        s_mode = pipeline_object.get("modes")

    if s_mode is not None:
        if type(s_mode) == str:
            s_mode = s_mode.lower()
        if "parallel" in s_mode:
            mode = PARALLEL_PROCESSING_MODE
        elif "distributed" in s_mode:
            mode = DISTRIBUTED_PROCESSING_MODE

    interface = pipeline_object.get("interface")

    source_ip = pipeline_object.get("source_ip")
    # TODO support --src-range and -s
    # TODO support port range?

    s_verdict = pipeline_object.get("verdict")

    default_verdict = VERDICT_ACCEPT
    if s_verdict is not None:
        if s_verdict.lower() == "drop":
            default_verdict = VERDICT_DROP

    s_modules = []
    s_connections = []

    for bhc_module in modules_array:
        current_module = bhc_module.get("name")
        if current_module not in s_modules:
            s_modules.append(current_module)

        next_module = bhc_module.get("next")
        if next_module is None:
            break

        if isinstance(next_module, list):
            for m in next_module:
                s_connections.append((current_module, m))
                if m not in s_modules:
                    s_modules.append(m)
        else:
            s_connections.append((current_module, next_module))
            if next_module not in s_modules:
                s_modules.append(next_module)

    try:
        modules = map(lambda x: importlib.import_module(x), s_modules)
        connections = map(lambda x: (x[0], x[1]), s_connections)
    except ImportError as exception:
        logger.subwarning("Error while loading %s: %s" % (name, exception))
        return None

    for bhc_module in modules:
        # TODO if not extends Module?
        # if not hasattr(module, "Module"):
        #    return None, 5
        module_class = get_subclass(bhc_module, Module)

        if not hasattr(module_class, "process"):
            logger.subwarning("Invalid module (%s): process function not found" % module_class)
            return None

        if not hasattr(module_class, "name"):
            module_class.name = str(module_class)

        if hasattr(module_class, "io"):
            if type(module_class.io) == list or type(module_class.io) == tuple:
                if len(module_class.io) == 2:
                    module_class.input_type = module_class.io[0]
                    module_class.output_type = module_class.io[1]

        if not hasattr(module_class, "input_type"):
            module_class.input_type = None
            if hasattr(module_class, "input_mode"):
                if module_class.input_mode == MULTIPLE_INPUT_MODE:
                    module_class.input_type = list

        if not hasattr(module_class, "output_type"):
            module_class.output_type = None

        if not hasattr(module_class, "input_mode"):
            module_class.input_mode = SINGLE_INPUT_MODE

    for i in range(len(modules)):
        module_class = get_subclass(modules[i], Module)
        logger.subsubinfo("Initializing module: %s" % module_class.name)
        modules[i] = module_class()

    pipelines = []

    # TODO improve this code
    # Worker pipeline
    if ports is None:
        ports = [None]

    # BHC pipeline
    for port in ports:
        pipeline = briareos.core.pipeline.Pipeline(pipeline_type, protocol, name, port, mode,
                                                   interface, source_ip, default_verdict)
        pipeline.add_modules(modules)

        for connection in connections:
            result, io_types = pipeline.connect_modules(connection[0], connection[1])
            if not result:
                logger.subwarning("Input/output types don't match: %s (%s) -> %s (%s)" %
                                  (connection[0], io_types[1], connection[1], io_types[0]))
                return None

        if not pipeline.is_valid():
            logger.subwarning("Invalid pipeline")
            return None

        pipelines.append(pipeline)

    return pipelines


def get_ports(pipeline):
    ports = pipeline.get("port")

    if ports is None:
        ports = pipeline.get("ports")

    if ports is not None:
        if type(ports) == int:
            ports = [ports]
        elif type(ports) == str:
            ports = [int(ports)]

        return ports
    return None


def get_subclass(bhc_module, base_class):
    obj = None
    for name in dir(bhc_module):
        obj = getattr(bhc_module, name)
        try:
            if issubclass(obj, base_class) and base_class != obj:
                return obj
        except TypeError:
            pass
    return obj
