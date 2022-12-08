from briareos.common import *
from briareos.utils import iptables

from scapy.layers.inet import *

# move to network utils?
class BPacket:
    def __init__(self, payload, verdict=VERDICT_ACCEPT, parse_scapy=False):

        self.pkt = None
        self.verdict = verdict
        self.is_final_verdict = False
        self.payload = payload
        self.new_payload = False
        self.protocol = None
        self._parsed = False
        if parse_scapy:
            self._parse_scapy()

    # Actions
    def accept(self, final=True):
        self.is_final_verdict = final
        self.verdict = VERDICT_ACCEPT

    def drop(self, final=True):
        self.is_final_verdict = final
        self.verdict = VERDICT_DROP

    def set_payload(self, payload):
        self.new_payload = True
        self.payload = payload
        # TODO TCP, etc

    # Scapy
    def _parse_scapy(self):
        self.pkt = IP(self.payload)
        self._parsed = True

    def get_application_data(self):
        if not self._parsed:
            self._parse_scapy()
        try:
            return self.pkt[Raw].load
        except:
            return None

    def get_source_ip(self):
        if not self._parsed:
            self._parse_scapy()
        return self.pkt[IP].src

    def get_dest_ip(self):
        if not self._parsed:
            self._parse_scapy()
        return self.pkt[IP].dst

    def block_ip_address(self):
        iptables.block_ip_address(self.get_source_ip())

class TcpPacket(BPacket):
    # TODO flags

    FIN = 0x01
    SYN = 0x02
    RST = 0x04
    PSH = 0x08
    ACK = 0x10
    URG = 0x20
    ECE = 0x40
    CWR = 0x80

    def __init__(self, data, verdict=VERDICT_ACCEPT):
        BPacket.__init__(self, data, verdict, parse_scapy=True)
        self.protocol = "tcp"

    def is_new_connection(self):
        flags = self.pkt[TCP].flags
        return flags & self.SYN

    def is_connection_closed(self):
        flags = self.pkt[TCP].flags
        return flags & self.FIN

    def get_source_port(self):
        return self.pkt[TCP].sport

    def get_dest_port(self):
        return self.pkt[TCP].dport

    # TODO move this to utils and pass object (?)
    def block_ip_address(self, all_connections=False):
        if all_connections:
            return iptables.block_ip_address(self.get_source_ip())
        else:
            iptables.block_ip_address(self.get_source_ip(), self.protocol, self.get_dest_port())

# unused
def get_packet_layers(packet):
    layers = []
    counter = 0
    while True:
        layer = packet.getlayer(counter)
        if layer != None:
            print layer.name
            layers.append(layer.name)
        else:
            break
        counter += 1
    print(layers)


def block_ip_address():
    iptables.block_ip_address()

# TODO udp packet, etc


# TODO funcionalidades da classe?
class Module:
    def __init__(self):
        pass
