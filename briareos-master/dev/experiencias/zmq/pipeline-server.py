import zmq
import time
import random

context = zmq.Context()

sender = context.socket(zmq.PUSH)
sender.bind("tcp://*:5557")

raw_input()

sender.send("0")

total_msec = 0

for i in range(100):
	workload = random.randint(1, 100)
	total_msec += workload
	sender.send(str(workload))

print("Expected cost: %s" % total_msec)
time.sleep(20)
