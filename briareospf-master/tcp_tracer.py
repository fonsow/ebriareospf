#!/usr/bin/python3
from bcc import BPF
import socket
import os
from time import sleep
from pyroute2 import IPRoute

b = BPF(src_file="tcp_tracer.c")

# Packets sent from the host to container are egress
# Packets sent from containers to the host are ingress
interface = "wlo1"

# XDP will be the first program hit when a packet is received ingress
fx = b.load_func("xdp", BPF.XDP)
# If the xdp() program drops ping packets, they won't get as far as TC ingress
BPF.attach_xdp(interface, fx, 0)

ipr = IPRoute()
links = ipr.link_lookup(ifname=interface)
idx = links[0]

try:
    ipr.tc("add", "ingress", idx, "ffff:")
except:
    print("qdisc ingress already exists")


# TC. Choose one program: drop all packets, just drop ping requests, or respond
# to ping requests
fi = b.load_func("tc", BPF.SCHED_CLS)

ipr.tc("add-filter", "bpf", idx, ":1", fd=fi.fd,
        name=fi.name, parent="ffff:", action="ok", classid=1, da=True)

# Remove with sudo tc qdisc del dev wlo1 parent -ffff

# Read data from socket filter 
try:
  #packet_str = os.read(fx, 4096)
  #print("Userspace got data: %x", packet_str)
  b.trace_print()
except KeyboardInterrupt:
    print("\n Unloading")
    ipr.tc("del", "ingress", idx, "ffff:")
    
exit()