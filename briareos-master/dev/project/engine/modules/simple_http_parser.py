# Pipeline Module
# Requirements: http-parser
# TODO instalar pipeline como package, com requirements etc, pipeline manager?

from briareos.utils.modules import *

from http_parser import pyparser
from http_object import HttpObject


class HttpParser(Module):
    name = "Simple HTTP Parser"
    description = "Parses application data and returns a Http object"

    io = (str, HttpObject)

    def process(self, packet, data):
        parser = pyparser.HttpParser()
        parser.execute(data, len(data))

        method = parser.get_method()
        status_code = parser.get_status_code()
        url = parser.get_url()
        headers = dict(parser.get_headers())
        body = "".join(parser._body)

        http_object = HttpObject(method, status_code, url, headers, body)
        return http_object
