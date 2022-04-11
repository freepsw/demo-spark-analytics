# -*- coding: utf-8 -*-
from __future__ import division
from pyspark import SparkContext, SparkConf
from pyspark.mllib.stat import Statistics
from operator import add
# import csv

conf = SparkConf().setAppName('Stage3_ListenerSummarizer').setMaster("local[1]")
sc = SparkContext(conf=conf)
sc.setLogLevel("INFO")

trackfile = sc.textFile('../00.stage1/tracks_live.csv')
clicksfile = sc.textFile('clicks.csv')
trainfile = open('features1.txt', 'w')

def make_tracks_kv(str):
    l = str.split(",")
    # key = l[1] (customer_id)
    return [l[1], [[int(l[2]), l[3], int(l[4]), l[5]]]]

def clicks_summary(str):
    print(str)
    l = str.split(",")
    custid = l[1]
    adv = l[2]
    if (adv == "ADV_REDUCED_1DAY"):
        return (custid, 1)

def compute_stats_byuser(tracks):
    mcount = morn = aft = eve = night = 0
    tracklist = []
    for t in tracks:
        trackid, dtime, mobile, zip = t
        if trackid not in tracklist:
            tracklist.append(trackid)

    # dtime = "2014-10-15 18:32:14"
    d,t = dtime.split(" ")
    hourofday = int(t.split(":")[0])

    mcount += mobile
    if (hourofday < 5):
        night += 1
    elif (hourofday < 12):
        morn += 1
    elif (hourofday < 17):
        aft += 1
    elif (hourofday < 22):
        eve += 1
    else:
        night += 1

    return [len(tracklist), morn, aft, eve, night, mcount]

# 사용자가 "ADV_REDUCED_1DAY"를 클릭했는지 확인하여 횟수를 1 증가
# line : clicks.csv 1 line
# which : "ADV_REDUCED_1DAY"
def user_clicked(line, which):
    eid, custid, adclicked, ltime = line.split(",")
    if (which in adclicked):
        return (custid, 1)
    else:
        return (custid, 0)

def test(a):
    print(a[0])
    print(a[1])
    return a[1]
# 1. tracks.csv(최근 사용자들의 음악 청취 이력 log)
#   spark에서 처리할 수 있는 데이터 구조(RDD)로 변환 (make a k,v RDD out of the input data)
#   customer_id 별로 데이터를 grouping 한다. (reduceByKey)
tbycust = trackfile.map(lambda line: make_tracks_kv(line)).reduceByKey(lambda a,b: a + b)

# 2. customer 별로 음악을 청취한 시간대 별 요약정보를 생성한다.
#   [len(tracklist), morn, aft, eve, night, mcount]
#   사용자가 몇개의 곡을 어떤 시간대에 음악을 듣는지 분석 (night, morn, aft, eve  4개의 시간대로 구분)
#   청취한 노래 개수 mobile로 접속한 횟수,
#   compute profile for each user
custdata = tbycust.mapValues(lambda a: compute_stats_byuser(a))

# 3. customer 별로 음악을 청취한 시간대 별 통계정보를 생성한다
# compute aggregate stats for entire track history
# aggdata = Statistics.colStats(custdata.map(lambda x: x[1]))
# aggdata = Statistics.colStats(custdata.map(lambda x: test(x)))
# {'adata:unique_tracks': str(aggdata.mean()[0]),
#  'adata:morn_tracks': str(aggdata.mean()[1]),
#  'adata:aft_tracks': str(aggdata.mean()[2]),
#  'adata:eve_tracks': str(aggdata.mean()[3]),
#  'adata:night_tracks': str(aggdata.mean()[4]),
#  'adata:mobile_tracks': str(aggdata.mean()[5])})

# 3. 사용자가 "ADV_REDUCED_1DAY"를 클릭한 수를 합산한다.
# distill the clicks down to a smaller data set that is faster
clickdata = clicksfile.map(lambda line:
        user_clicked(line, "ADV_REDUCED_1DAY")).reduceByKey(add)
# 4. 사용자 id를 기준으로 정렬.
sortedclicks = clickdata.sortByKey()

# 5. 사용자 id별로 id, morn_cnt, aft_cnt, eve_cnt, night_cnt, mobile_cnt, unique_cnt
#    위과 같은 포맷의 file을 생성하여, training시 feature data set으로 활용한다
#    write the individual user profiles
c = 0
entries = []
for k, v in custdata.collect():
    unique, morn, aft, eve, night, mobile = v
    tot = float(morn + aft + eve + night)
    c += 1

    # 사용자가 "ADV_REDUCED_1DAY" 이벤트를 클릭했는지 여부 확인 (몇번을 클릭했는지는 고려하지 않는다)
    # see if this user clicked on a 1-day special reduced Gold rate
    clicked = 1 if sortedclicks.lookup(k)[0] > 0 else 0

    #print unique,morn,aft,eve,night,mobile,tot
    training_row = [
                morn / tot,
                aft / tot,
                eve / tot,
                night / tot,
                mobile / tot,
                unique / tot ]
    trainfile.write(b"%d" % clicked)

    # the libSVM format wants features to start with 1
    for i in range(1, len(training_row) + 1):
        trainfile.write(" %d:%.2f" % (i, training_row[i - 1]))
    trainfile.write(b"\n")

    # (optional) so we can watch the output
    trainfile.flush()


print ("averages1:  unique: %d morning: %d afternoon: %d evening: %d night: %d mobile: %d" % \
    (unique, morn, aft, eve, night, mobile))
print ("done")
