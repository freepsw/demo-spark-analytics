# -*- coding: utf-8 -*-
from pyspark import SparkContext, SparkConf
from pyspark.mllib.linalg import SparseVector
from pyspark.mllib.util import MLUtils
from pyspark.mllib.classification import LogisticRegressionWithLBFGS
import redis
from pyspark.mllib.classification import LogisticRegressionWithSGD

conf = SparkConf().setAppName('Stage3_AdPredictor').setMaster("local[2]")
sc = SparkContext(conf=conf)
r = redis.Redis('localhost')
r.set("key1", "value1")

data = MLUtils.loadLibSVMFile(sc, 'features.txt', minPartitions=1)

# 1. train data와 학습모델 검증을 위한 test data set을 나눈다
# split the data into training, and test
# all, noall = data.randomSplit([1.0, 0.0], seed = 0)
all = (data)
train, test = data.randomSplit([0.7, 0.3], seed = 0)
tr_count = train.count()
te_count = test.count()
cust_id = 0

# 2. train LBFGS model on the training data
model_1 = LogisticRegressionWithLBFGS.train(train)

# 2-1. evaluate the model on test data
results_1 = test.map(lambda p: (p.label, model_1.predict(p.features)))

# 2-2. calculate the error
err_1 = results_1.filter(lambda x: x[0] != x[1]).count() / float(te_count)

def predict_all_user():
    r = redis.Redis('localhost')
    fname = "features.txt"
    rf = open(fname)

    try:
        num_lines = sum(1 for line in rf)
        rf.seek(0)
        lines = 0
        while (1):
            line = rf.readline()
            vec = make_SparseVector(line)
            pred = model_1.predict(vec)

            print("cust_id = %s : pred = %d" % (lines, pred))

            # insert into redis (cust_id, predict_val)
            key = "pred_event:" + str(lines)
            r.set(key, str(pred))

            lines += 1
            if (lines == num_lines):
                break
    finally:
        rf.close()
        print ("close file")


def make_SparseVector(line):
    # s0 = "0 1:0.00 2:0.00 3:0.00 4:1.00 5:0.00 6:1.00"
    s = line[2:]
    print(s)
    s1 = s.split()

    dic_key = [0, 1, 2, 3, 4, 5]
    dic_val = []
    for v in s1:
        dic_val.append(float(v.split(':')[1]))

    dic = dict(zip(dic_key, dic_val))
    vec = SparseVector(len(dic), dic)
    return vec

#3. predict all user using trained ml model
predict_all_user()


print ("\nall: %d training size: %d, test size %d" % (all.count(), tr_count, te_count))
print ("LBFGS error: %s" % (str(err_1)))




# # 3. train SGD model on the training data
# model_2 = LogisticRegressionWithSGD.train(train)
#
# # 3-1. evaluate the model on test data
# results_2 = test.map(lambda p: (p.label, model_2.predict(p.features)))
#
# # 3-2. calculate the error
# err_2 = results_2.filter(lambda (v, p): v != p).count() / float(te_count)
#
# print "Results"
# print "-------"
# print "training size: %d, test size %d" % (tr_count, te_count)
# print "LBFGS error: %s" % str(err_1)
# print "SGD error: %s" % str(err_2)




# temp function
# def insert_to_redis(p):
#     global cust_id
#     r = redis.Redis('localhost')
#
#     key = "pred_event:" + str(cust_id)
#     # print(key)
#     # print("\n")
#     r.set(key, str(p))
#
#     cust_id = cust_id + 1
#     return (cust_id, 1)
#
#
# result_pred = results_2.map(lambda (v, p): insert_to_redis(p))
# def predict_lr_lbfgs(p):
#     global cust_id
#
#     # print "custid: %d y = %s : %s" % (cust_id, p.label, model_1.predict(p.features))
#     # print "feature : %s" % p.features
#     # print type(p.features)
#
#     r = redis.Redis('localhost')
#     key = "pred_event:" + str(cust_id)
#     r.set(key, str(model_1.predict(p.features)))
#     cust_id = cust_id + 1
#
#     return model_1.predict(p.features)
# # 2-2.  전체 사용자를 대상으로 Event 클릭 여부를 예측한다.
# results_2 = all.map(lambda p: (p.label,  predict_lr_lbfgs(p)))
