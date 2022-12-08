# Pipeline Module

from briareos.utils.modules import *

HONEYPOT_PORT = 8001

# TODO


class Honeypot(Module):
    name = "Packet Modification Module"
    description = "How to perform packet modifications"

    def process(self, packet):
        print(hexdump(IP(str(packet.pkt))))

        packet.pkt[IP].dst = "10.0.2.15"
        packet.pkt[TCP].dport = 8001
        del packet.pkt[TCP].chksum
        del packet.pkt[IP].payload.chksum
        del packet.pkt[IP].chksum
        del packet.pkt.chksum

        honeypotPacket = packet.pkt.copy()

        '''
        if Raw in packet[TCP]:
            data = str(packet[TCP].payload)
            packet[TCP].remove_payload()
            data = data.replace("hack", "1337")
            packet[TCP].add_payload(data)

        del packet[TCP].chksum
        del packet[IP].payload.chksum
        del packet[IP].chksum
        del packet[IP].len

        action.modifyPacket(str(packet))
        '''

        #action.modifyPacket(str(honeypotPacket))

        #print(hexdump(IP(str(honeypotPacket))))
        return "Output"
