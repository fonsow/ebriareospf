import zmq
import time

context = zmq.Context()

receiver = context.socket(zmq.PULL)
receiver.connect("tcp://localhost:5557")

sender = context.socket(zmq.PUSH)
sender.connect("tcp://localhost:5558")

while True:
	s = receiver.recv()
	
	time.sleep(int(s)*0.001)
	print(".")
	sender.send("result")
