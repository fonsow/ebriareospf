# Pipeline Module

from briareos.utils.modules import *
from briareos.utils import logger


# It can lead to false positives if the original program calls system commands
class ShellDetector(Module):
    name = "Shell Detector"
    description = "Prevents 0day exploits by detecting shells."
    input_type = tuple
    output_type = None

    def process(self, packet, process_info):
        pid, app_name = process_info

        if app_name == "/bin/sh" or app_name == "/bin/bash":
            logger.warning("Shell detected > Dropping connection")
            # exploitDetected()
            packet.drop()
            return None
            # TODO terminate connection! further input still accepted

        return None
