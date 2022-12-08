from briareos.utils.modules import *

from procmon import ProcMon
import proc


class PidFetcher(Module):
    name = "PID Fetcher"
    description = "Returns the PID of a connection"
    author = "Andre Baptista"
    version = "1.0"
    license = "GPLv3"

    output_type = tuple
    # io = (None, tuple)

    def __init__(self):
        self.procmon = ProcMon()
        if ProcMon.is_ftrace_available():
            self.procmon.enable()
            self.procmon.start()

    def cleanup(self):
        self.procmon.disable()

    def process(self, packet):
        source_port = packet.get_source_port()
        dest_port = packet.get_dest_port()
        src_ip = packet.get_source_ip()
        dest_ip = packet.get_dest_ip()

        pid, app_name = \
            proc.get_pid_by_connection(self.procmon, src_ip,
                                                   source_port, dest_ip,
                                                   dest_port, packet.protocol)

        return pid, app_name
