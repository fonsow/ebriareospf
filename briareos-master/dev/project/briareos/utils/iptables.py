import os

CHAIN_INPUT = "INPUT"
CHAIN_OUTPUT = "OUTPUT"

SUPPORTED_PROTOCOLS = ("tcp", "udp", "icmp", "all")
ANY_INTERFACE = ("", "any", "all")

# TODO allow other nfqueues in the system (other software!)
# -> list nfqueues and set first queueID = first empty slot


def create_nfqueue_rule(queue_id, chain, port, protocol=None, interface=None, source_ip=None):
    protocol_option = ""
    port_option = ""

    if protocol is not None:
        if protocol in SUPPORTED_PROTOCOLS:
            protocol_option = "-p %s" % protocol
            if protocol == "tcp" or protocol == "udp":
                if chain == CHAIN_INPUT:
                    port_option = "--dport %s" % port
                elif chain == CHAIN_OUTPUT:
                    port_option = "--sport %s" % port

    interface_option = ""
    if interface is not None:
        if interface not in ANY_INTERFACE:
            if chain == CHAIN_INPUT:
                interface_option = "-i %s" % interface
            elif chain == CHAIN_OUTPUT:
                interface_option = "-o %s" % interface

    source_ip_option = ""
    if source_ip is not None:
        source_ip_option = "-s %s" % source_ip

    command = "iptables -I %s %s %s %s %s -j NFQUEUE --queue-num %s --queue-bypass" % (chain,
                                                                                       interface_option,
                                                                                       source_ip_option,
                                                                                       protocol_option,
                                                                                       port_option, queue_id)

    # print(command)
    return os.system(command) == 0


# TODO permanent rule default == True?
def block_ip_address(ip_address, protocol=None, port=None, permanent=True):
    protocol_option = ""
    port_option = ""

    if protocol is not None:
        if protocol in SUPPORTED_PROTOCOLS:
            protocol_option = "-p %s" % protocol
            if port is not None:
                port_option = "--dport %s" % port

    command = "iptables -I INPUT 1 -s %s %s %s -j DROP" % (ip_address, protocol_option, port_option)
    return os.system(command)


# TODO don't delete all rules, use python iptables and delete matching rules (except permanent rules)
def clean_rules():
    os.system("sudo iptables -F INPUT")
    os.system("sudo iptables -F OUTPUT")
