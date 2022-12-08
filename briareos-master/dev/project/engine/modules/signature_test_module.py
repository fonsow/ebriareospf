# Pipeline Module

from briareos.utils.modules import *

PATH = "signatures.txt"

# TODO signature check example module, yara rules...


class SignatureTestModule(Module):
    name = "Signature Test Module",
    description = "Testing distributed computing"

    def __init__(self):
        # load from file
        with open(PATH) as f:
            self.signatures = f.read().splitlines()

    def process(self, packet):
        data = packet.get_application_data()
        if data is None:
            packet.accept()
            return None

        # result = signature_check(self.signatures, packet[Raw].load)
        result = None

        print("Result %s" % result)
        if result:
            packet.drop()
        else:
            packet.accept()
