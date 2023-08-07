#!/usr/bin/python3
from bcc import BPF
import socket
import os
from time import sleep
from pyroute2 import IPRoute
import argparse
import socket
import fcntl
import struct
import ctypes
import binascii

ETH_ALEN = 6

class EthernetHeader(ctypes.Structure):
    _fields_ = [("src_mac", ctypes.c_uint8 * ETH_ALEN),
                ("dst_mac", ctypes.c_uint8 * ETH_ALEN)]

# Create an instance of the EthernetHeader
eth_header = EthernetHeader()

bpf = BPF(src_file="tcp_tracer.c")

def get_interface_ip(interface):
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        interface_ip = socket.inet_ntoa(fcntl.ioctl(
            sock.fileno(),
            0x8915,  # SIOCGIFADDR
            struct.pack('256s', interface.encode())[:40]
        )[20:24])
        return interface_ip
    except OSError:
        return None

# Packets sent from the host to container are egress
# Packets sent from containers to the host are ingress
parser = argparse.ArgumentParser(description='TCP Operation Script')
parser.add_argument('-I', '--interface', required=True, help='Network interface name (e.g., eth0)')

args = parser.parse_args()
interface = args.interface

ip = get_interface_ip(interface)

#XDP will be the first program hit when a packet is received ingress
fx = bpf.load_func("xdp", BPF.XDP)
#If the xdp() program drops ping packets, they won't get as far as TC ingress
BPF.attach_xdp(interface, fx, 0)

def callback(ctx, data, size):
    packet = bpf["packets"].event(data)
    result = bytes(packet.src_mac)
    src_mac = ':'.join(format(byte, '02x') for byte in result)
    result = bytes(packet.dst_mac)
    dst_mac = ':'.join(format(byte, '02x') for byte in result)
    print("TYPE=%d;SRC_MAC=%s;DST_MAC=%s" % (packet.type, src_mac, dst_mac))



bpf["packets"].open_perf_buffer(callback)
try:
    print("Listening on IP:", ip)
    #bpf.trace_print()
    while True:
        bpf.perf_buffer_poll()
except KeyboardInterrupt:
    print("\n Unloading")
exit()