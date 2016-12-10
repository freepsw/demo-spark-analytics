# -*- coding: utf-8 -*-
from pyspark import SparkContext, SparkConf
from pyspark.mllib.linalg import SparseVector
from pyspark.mllib.util import MLUtils
from pyspark.mllib.classification import LogisticRegressionWithLBFGS
import redis
from pyspark.mllib.classification import LogisticRegressionWithSGD

conf = SparkConf().setAppName('Stage3_AdPredictor').setMaster("local[1]")
sc = SparkContext(conf=conf)

data = MLUtils.loadLibSVMFile(sc, 'features.txt')

# split the data into training, and test
train, test = data.randomSplit([0.7, 0.3], seed = 0)
tr_count = train.count()
te_count = test.count()

# train LBFGS model on the training data
model_1 = LogisticRegressionWithLBFGS.train(train)

# evaluate the model on test data
results_1 = test.map(lambda p: (p.label, model_1.predict(p.features)))

# calculate the error
err_1 = results_1.filter(lambda (v, p): v != p).count() / float(te_count)

# train SGD model on the training data
model_2 = LogisticRegressionWithSGD.train(train)

# evaluate the model on test data
results_2 = test.map(lambda p: (p.label, model_2.predict(p.features)))

# calculate the error
err_2 = results_2.filter(lambda (v, p): v != p).count() / float(te_count)

print "Results"
print "-------"
print "training size: %d, test size %d" % (tr_count, te_count)
print "LBFGS error: %s" % str(err_1)
print "SGD error: %s" % str(err_2)

