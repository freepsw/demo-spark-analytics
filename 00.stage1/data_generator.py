#-*- coding: utf-8 -*-
import time
import random

r_fname = "tracks.csv"
w_fname = "tracks_live.csv"

rf = open(r_fname)
wf = open(w_fname, "a+")

try:
	num_lines = sum(1 for line in rf)
	print(num_lines)
	#num_lines = 10

	rf.seek(0)
	lines = 0
	while (1):
		line = rf.readline()
		wf.write(line)
		wf.flush()

		# sleep for weighted time period
		stime = random.choice([1, 1, 1, 0.5, 0.5, 0.8, 0.3, 2, 0.1, 3])
		print(stime)
		time.sleep(stime)
		lines += 1

		# exit if read all lines
		if(lines == num_lines):
			break
		# if(lines == num_lines):
		# 	rf.seek(0)
finally:
	rf.close()
	wf.close()
	print "close file"