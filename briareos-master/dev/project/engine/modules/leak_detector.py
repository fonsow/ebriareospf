# Pipeline Module

from briareos.utils.modules import *
from briareos.utils import logger

import psutil

# Options: block and log, replace and log, allow exploit and save exploit?

REPLACE = 0
BLOCK = 1
ALLOW_EXPLOIT = 2

MODE = BLOCK
INCLUDE_FORKS = True
STRUCT_FORMAT = "<Q"  # 64 bits little-endian
MIN_LEAK_LENGTH = 2


class LeakDetector(Module):
    name = "Address Leak Detector"
    description = "Prevents 0day exploits by blocking leaks of memory addresses"
    input_type = tuple
    output_type = tuple

    # TODO replace - API
    # TODO Extra mode, let the exploit run but save it for fun and profit! -> relacionar Input/Output
    # TODO get canary leaks https://www.elttam.com.au/blog/playing-with-canaries/
    # TODO more efficient
    # TODO check if exploit is complete
    
    def process(self, packet, process_info):
        pid, app_name = process_info

        data = packet.get_application_data()
        if data is None:
            packet.accept()
            return process_info

        process = psutil.Process(pid=pid)
        children = process.children(recursive=True)

        for proc in [process] + children:
            maps = get_memory_maps(proc.pid)
            rev_data = data[::-1]

            for memory_map in maps:
                pattern = memory_map[1]
                if pattern in rev_data:
                    if MODE == BLOCK:
                        logger.warning("Leak detected %s (%s) -> Dropping packet" % (memory_map[3], memory_map[4]))
                        # exploitDetected()
                        packet.drop(final=True)
                        return process_info
                    elif MODE == REPLACE:
                        logger.warning("Leak detected %s -> Replacing leak data" % memory_map[3])
                        return process_info
                    elif MODE == ALLOW_EXPLOIT:
                        logger.warning("Leak detected %s -> Waiting for exploit to complete" % memory_map[3])
                        # exploitDetected()
                        break

        return process_info


def get_pattern(a, b):
    pattern = ""
    a = a.replace("\x00", "")
    b = b.replace("\x00", "")
    for i in range(min(len(a), len(b))):
        if a[i] == b[i]:
            pattern += a[i]
        else:
            break
    return pattern


def get_memory_maps(pid):
    maps = []

    with open("/proc/%s/maps" % pid) as f:
        lines = f.readlines()
        for line in lines:
            line = line.strip()
            l = line.split()

            l_addr = l[0].split("-")
            address_range = (int(l_addr[0], 16), int(l_addr[1], 16))
            address_range_hex = (hex(int(l_addr[0], 16)), hex(int(l_addr[1], 16)))
            pat_start_addr = struct.pack(STRUCT_FORMAT, address_range[0])[::-1]
            pat_end_addr = struct.pack(STRUCT_FORMAT, address_range[1])[::-1]

            pattern = get_pattern(pat_start_addr, pat_end_addr)

            if len(pattern) >= MIN_LEAK_LENGTH:
                permissions = l[1]
                if len(l) == 6:
                    name = l[5]
                else:
                    name = "Unknown"
                maps.append((address_range, pattern, permissions, name, address_range_hex))

    return maps
