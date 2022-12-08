# Pipeline Module

from briareos.utils.modules import *


class IncidentProcessor(Module):
    name = "Incident Processor"
    description = ""

    input_mode = MULTIPLE_INPUT_MODE

    def process(self, packet, data):
        pass
