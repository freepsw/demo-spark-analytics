#-*- coding: utf-8 -*-
import time
from random import random
from bisect import bisect

def weighted_choice(choices):
    values, weights = zip(*choices)
    total = 0
    cum_weights = []
    for w in weights:
        total += w
        cum_weights.append(total)
    x = random() * total
    i = bisect(cum_weights, x)
    return values[i]

# choice = weighted_choice([(1,50), (0.5,20), (2,20), (0.1,5), (5,5)])
# print choice



r_fname = "tracks.csv"
w_fname = "tracks_live.csv"

rf = open(r_fname)
wf = open(w_fname, "a+")

try:
	num_lines = sum(1 for line in rf)
	print(num_lines)

	rf.seek(0)
	lines = 0
	while (1):
		line = rf.readline()
		wf.write(line)
		wf.flush()

		stime = weighted_choice([(1,30), (0.5,20), (2,20), (0.8,10), (0.3,10), (0.1,5), (3,5)])
		print(stime)
		time.sleep(stime)
		lines += 1

		if(lines == num_lines):
			break
		# if(lines == num_lines):
		# 	rf.seek(0)
finally:
	rf.close()
	wf.close()
	print "close file"