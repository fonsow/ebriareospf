import zmq
import time

context = zmq.Context()

receiver = context.socket(zmq.PULL)
receiver.bind("tcp://*:5558")

s = receiver.recv()

print("Go!")
tstart = time.time()
total_msec = 0

for task_nbr in range(100):
	s = receiver.recv()
	print(task_nbr),
	print(s)

tend = time.time()
print("Total time: %s" % ((tend-tstart)*1000))
