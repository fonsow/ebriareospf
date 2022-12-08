import zmq
import random
import time

context = zmq.Context()
socket = context.socket(zmq.PUB)
socket.bind("tcp://*:5555")

while True:
	time.sleep(1)
	print("wut")
	socket.send("World %s" % random.randint(1, 1000))
