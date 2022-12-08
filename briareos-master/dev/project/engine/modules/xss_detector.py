# Pipeline Module

from briareos.utils.modules import *

from http_object import HttpObject


class XSSDetector(Module):
    name = "XSS Detector"
    description = ""
    input_type = HttpObject
    output_type = str

    def process(self, packet, http_object):
        return "XSS Detector output"
