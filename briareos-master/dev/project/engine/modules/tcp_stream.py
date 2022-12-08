from briareos.utils.modules import *


class TCPStream(Module):
    name = "TCP Stream Module"
    description = "Demo"
    author = "Andre Baptista"
    version = "1.0.0"
    email = "amccbaptista@gmail.com"
    license = "GPLv3"

    def process(self, packet):
        if packet.is_new_connection():
            print("New connection")
        elif packet.is_connection_closed():
            print("Connection closed")
