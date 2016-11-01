# -*- coding: utf-8 -*-
# import random
import numpy as np
from pyspark import SparkContext, SparkConf
from pyspark.mllib.linalg import SparseVector

def type_set():
    vec = SparseVector(6, [0, 1, 2, 3, 4, 5], [0.23, 0.18, 0.26, 0.34, 0.87, 0.98])
    vec1 = SparseVector(6, {1: 0.28, 2: 0.22, 3: 0.23, 4: 0.27, 5: 0.56, 6: 0.97})
    print(vec)
    print(vec1)

    l1 = [0, 1]

    feature = "1:0.28 2:0.22 3:0.23 4:0.27 5:0.56 6:0.97"
    print(set(feature.split()))
    # vec2 = SparseVector(6, feature)
    # print(vec2)

    word_dict = dict()
    word_dict["foo"] = set()
    word_dict["foo"].add("baz")
    word_dict["foo"].add("bang")
    print(word_dict)

    set_1 = {1: 0.28}
    print(set_1)

    print(set_1)

    set2 = {1: 0.28, 2: 0.22, 3: 0.23, 4: 0.27, 5: 0.56, 6: 0.97}
    print(set2)

    test = {1, 2, 3}
    print(test)

def dic_fromkey():
    s = {0, 0, 2, 3, 4}
    s1 = dict.fromkeys(s, [1,2,4])
    print(s1)


    d = {1:0.28}
    d.update({2:32})
    d[3] = 0.58

    print(d)

def split_str():
    s = '1:0.28 2:0.22 3:0.23 4:0.27 5:0.56 6:0.97'
    s1 = s.split()
    print("count %d" % (len(s1)))
    print(s[0] , s[1] , s[2], s[3], s[4])
    a = "Life is too short"
    print(a.split())
    print(a[1])
    print(len(a))

def make_SparseVector():
    s0 = "0 1:0.00 2:0.00 3:0.00 4:1.00 5:0.00 6:1.00"
    s = s0[2:]
    print(s)

    # s = '1:0.28 2:0.22 3:0.23 4:0.27 5:0.56 6:0.97'
    s1 = s.split()
    # print(s1[1].split(':')[1])
    # v = s1[1].split(':')[1]

    dic_key = [0, 1, 2, 3, 4, 5]
    dic_val = []
    for v in s1:
        dic_val.append(float(v.split(':')[1]))

    print(dic_val)
    dic = dict(zip(dic_key, dic_val))
    print(dic)
    print(len(dic))

    vec = SparseVector(len(dic), dic)
    print(vec)


    # dic = {dic_key:v[1].split(':')[1] for v in s}
    # print(dic)

def pySpark_rdd():
    conf = SparkConf().setAppName('Stage3_AdPredictor').setMaster("local[1]")
    sc = SparkContext(conf=conf)

    mat = np.arange(15)
    print(mat)

    rdd = sc.parallelize(mat)
    print(type(rdd))


def file_read():
    fname = "features.txt"
    rf = open(fname)

    try:
        num_lines = sum(1 for line in rf)
        print(num_lines)

        # rf.seek(0)
        lines = 0

        line = rf.readline()
        # print("teset", line)
        while (line):
            line = rf.readline()
            print(line)
            lines += 1

            if (lines == num_lines):
                break
    finally:
        rf.close()
        print "close file"


# type_set()

dic_fromkey()

split_str()

make_SparseVector()

