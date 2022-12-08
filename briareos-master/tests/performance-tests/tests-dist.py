import subprocess
import numpy

n_reqs = 5000
conc = 50

def average(l):
    if not l:
        return None
    return sum(l) / float(len(l))

def test(n_reqs, conc):
	result = subprocess.check_output("ab -n %s -c %s http://127.0.0.1/ | grep received" % (n_reqs, conc), shell=True)
	l = result.split()
	val = l[2]
	if "Kbytes" not in l[3]:
		print("!!")
		sys.exit()
	return float(val)

final_results = {}

for conc in [100]:
	results = []
	print("Testing %s" % conc)
	for i in range(0, 1):
		results.append(test(n_reqs, conc))
	print("Results for %s" % conc),
	print(results)
	final_results[conc] = {}
	final_results[conc]["mean"] = numpy.mean(results)
	final_results[conc]["std"] = numpy.std(results)

print(final_results)
