# -*- coding: utf-8 -*-
from pyspark import SparkContext, SparkConf
from pyspark.mllib.util import MLUtils
from pyspark.mllib.classification import LogisticRegressionWithLBFGS
import redis
from pyspark.mllib.classification import LogisticRegressionWithSGD

conf = SparkConf().setAppName('Stage3_AdPredictor').setMaster("local[1]")
sc = SparkContext(conf=conf)
r = redis.Redis('localhost')
r.set("key1", "value1")

data = MLUtils.loadLibSVMFile(sc, 'features.txt')

# 1. train data와 학습모델 검증을 위한 test data set을 나눈다
# split the data into training, and test
all, noall = data.randomSplit([1.0, 0.0], seed = 0)
train, test = data.randomSplit([0.7, 0.3], seed = 1)
tr_count = train.count()
te_count = test.count()
cust_id = 0
# 2. train LBFGS model on the training data
model_1 = LogisticRegressionWithLBFGS.train(train)

def predict_lr_lbfgs(p):
    global cust_id

    print "custid: %d y = %s : %s" % (cust_id, p.label, model_1.predict(p.features))
    print "feature : %s\n" % p.features
    # cust_id = cust_id + 1

    return model_1.predict(p.features)


# 2-1. evaluate the model on test data
results_1 = test.map(lambda p: (p.label, model_1.predict(p.features)))

# 2-2.  전체 사용자를 대상으로 Event 클릭 여부를 예측한다.
results_2 = all.map(lambda p: (p.label,  predict_lr_lbfgs(p)))

def insert_to_redis(p):
    global cust_id
    r = redis.Redis('localhost')

    key = "pred_event:" + str(cust_id)
    print(key)
    r.set(key, str(p))

    cust_id = cust_id + 1
    return (cust_id, 1)

def insert_to_redis2(p,r):
    global cust_id
    key = "pred_event:" + str(cust_id)
    print(key)
    r.set(key, str(p))

    cust_id = cust_id + 1
    return (cust_id, 1)

result_pred = results_2.map(lambda (v, p): insert_to_redis(p))

# results_2.mappartitions(insert_to_redis2)
# 2-2. calculate the error
err_1 = results_1.filter(lambda (v, p): v != p).count() / float(te_count)
err_2 = results_2.filter(lambda (v, p): v != p).count() / float(all.count())

result_pred.filter(lambda a: a).count()


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

print "all: %d training size: %d, test size %d" % (all.count(), tr_count, te_count)
print "LBFGS error: %s / %s" % (str(err_1), str(err_2))