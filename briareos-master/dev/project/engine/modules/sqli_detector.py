# Pipeline Module

from briareos.utils.modules import *

from http_object import HttpObject


class SQLInjectionDetector(Module):
    name = "SQL Injection Detector"
    description = ""
    input_type = HttpObject
    output_type = str

    def process(self, packet, http_object):
        if "hack" in http_object.url:
            packet.drop()
            print("Dropping packet: %s" % http_object.url)
            packet.block_ip_address()

        # TODO httpObject.url = escape(http_object.url) # -> automatic packet payload?

        return "SQLi Detector output"
