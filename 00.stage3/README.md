# Stage 3. Stage1 + Stage2 + spark mlLib
- 회사의 매출향상을 위해 사용자들의 최근 광고 click 기록을 기반으로 요금제(FREE, SILVER, GOLD) 업그레드를 광고를 할 대상을 분류 
- 분류된 사용자들 만을 대상으로 music을 듣기 바로 전에 광고 영상을 제공 

### - Software 구성도
![stage3 architecture] (https://github.com/freepsw/demo-spark-analytics/blob/master/resources/images/stage3.png)

## Stage 3의 주요 내용 
### scenario
- customer는 3가지 멤버십을 가지고 있다. 

>
- Free -- the base service which is free to subscribe, but has limits (for example, limited number of tracks, more ads, etc.)
- Silver -- an upgraded level of service which has associated revenue
- Gold -- the highest level of service
>

- 회계년도가 다가오면서 계획된 매출을 달성하기 위하여 CEO는 매출 향상을 위한 방안 제시를 지시함
- Marketing team에서는 음악이 시작되기 바로전에 고객에게 광고(ondeay 멤버십 upgrade to gold discount(50%) event)를 보여주는 것을 제안
- 그렇다고 모든 고객들이 접속할 때 마다 광고를 제공하는 것은 광고사업자에게 제공하는 비용이 너무 높아짐. 
- 최근 고객들이 유사한 event 광고를 클릭한 패턴을 기반으로 event에 관심이 높을 것 같은 고객을 분류하고, 해당 고객에게만 event 광고를 하면 비용이 절약됨.
- 그럼 어떻게? (중요한 요건)
 - The decision needs to be fast. 
  * 광고는 고객이 음악을 듣기 바로 전에 play되어야 한다.
  * "멤버십 upgrade 50% discount event에 참여하시겠습니까?"라는 문구로 고객관심 유
 - It needs to take into account the latest information.
  * 몇주 또는 몇개월 전의 고객 데이터는 고객의 성향이나 트렌드를 반영하지 못하므로, 최근 고객의 action을 기반으로 분류되어야 한다.

### use machine learning classification algorithm (SVM)
- 아래의 data set을 이용할 것이다. 
  * indicating which ad was played to the user and whether or not they clicked on it 
  * 여기서 중요한 field는 customer가 어떤 광고(adClicked)를 클릭했고, 그때의 시간이 있을 것이다. 
  * 그 외에도 사용자가 성별, 나이, 주거지에 따라 다양한 변수들이 예측을 더 정확하게 할 수 있지만, 이번 실습에서는 간단하게 분류할 예정이다. (실습의 단순함을 위하여...)

EventID | CustID | AdClicked | Localtime
------------ | ------------- | ------------- | ------------- 
0 | 109 | ADV_FREE_REFERRAL | 2014-12-18 08:15:16

## STEP 1) install python and packages
### install python
- 현재 demo용으로 사용하는 vm장비에는 이미 python2.7이 설치되어 있다.
- 만약 centos를 사용하고, 아직 설치되어 있지 않다면 [link](https://www.lesstif.com/pages/viewpage.action?pageId=30705072)참고

### install python packages
- package들 역시 demo용 vm에는 이미 설치되어 있다. 
- 만약 설치가 되어있지 않다면, 아래의 명령어로 설치
```
> sudo pip install python 
> sudo pip install numpy
> sudo yum install python-devel #psutil 설치에 필요한 lib가 있음
> sudo pip install psutil 
```
- psutil은 "(shuffle.py:58: UserWarning: Please install psutil to have better support with spilling))"와 같은 warning을 방지하기 위해 설치 (pyspark ml 실행시 발생)

## STEP 2) make training datat set(features) from user log
### run python script(create_features_for_ml.py)
```
> cd ~/demo-spark-analytics/00.stage3
> python create_features_for_ml.py
ERROR No module named pyspark 발생
```

- No module name pyspark 에러 수정
```
> vi ~/.bash_profile
아래 내용을 추가
export SPARK_HOME=~/demo-spark-analytics/sw/spark-2.0.1-bin-hadoop2.7
export PYTHONPATH=$SPARK_HOME/python/:$SPARK_HOME/python/lib/py4j-0.10.3-src.zip:$PYTHONPATH
> source ~/.bash_profile
```

### 다시 실행 run python script(create_features_for_ml.py)
```
> python create_features_for_ml.py
RROR PythonRDD: Error while sending iterator
java.net.SocketTimeoutException: Accept timed out 
또 위와 같은 에러가 발생한다....
```
- 에러가 발생되면서 종료되었지만,
- features1.txt 파일을 보면, 정상적으로 결과가 write되고 있었다.
- google에서 검색한 결과 대부분 메모리를 과다하게 사용하면서 위와 같은 에러가 발생한다고 함.
- 이번 demo에서는 해당 코드가 정상적으로 구동한다는 것까지만 확인하고,
- 실제 feature.txt는 기존에 있는 파일을 활용한다.


### python code에 대한 설명
- 1. Spark context 생성 및 필요한 파일들을 로딩한다.
- 2. tracks.csv(최근 사용자들의 음악 청취 이력 log)을 읽어와 spark에서 처리할 수 있는 데이터 구조(RDD)로 변환
- 3. customer 별로 음악을 청취한 시간대 별 요약정보를 생성한다.
- 4. 사용자별로 "ADV_REDUCED_1DAY"를 클릭한 수를 합산하고, 사용자 ID기준으로 정렬
- 5. 사용자 id별로 id, morn_cnt, aft_cnt, eve_cnt, night_cnt, mobile_cnt, unique_cnt 포맷을 생성하여 머신러닝에서 사용할 포맷으로 features.txt 파일에 저장.

```python
# 1. Spark context 생성 및 필요한 파일들을 로딩한다.
conf = SparkConf().setAppName('Stage3_ListenerSummarizer').setMaster("local[1]")
sc = SparkContext(conf=conf)
sc.setLogLevel("INFO")
trackfile = sc.textFile('../00.stage1/tracks_live.csv')
clicksfile = sc.textFile('clicks.csv')
trainfile = open('features1.txt', 'wb')

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
    d, t = dtime.split(" ")
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
    print a[0]
    print a[1]
    return a[1]

# 2. tracks.csv(최근 사용자들의 음악 청취 이력 log)
#   spark에서 처리할 수 있는 데이터 구조(RDD)로 변환 (make a k,v RDD out of the input data)
#   customer_id 별로 데이터를 grouping 한다. (reduceByKey)
tbycust = trackfile.map(lambda line: make_tracks_kv(line)).reduceByKey(lambda a,b: a + b)

# 3. customer 별로 음악을 청취한 시간대 별 요약정보를 생성한다.
#   [len(tracklist), morn, aft, eve, night, mcount]
#   사용자가 몇개의 곡을 어떤 시간대에 음악을 듣는지 분석 (night, morn, aft, eve  4개의 시간대로 구분)
#   청취한 노래 개수 mobile로 접속한 횟수,
#   compute profile for each user
custdata = tbycust.mapValues(lambda a: compute_stats_byuser(a))

# 4. 사용자가 "ADV_REDUCED_1DAY"를 클릭한 수를 합산한다.
# distill the clicks down to a smaller data set that is faster
clickdata = clicksfile.map(lambda line:
        user_clicked(line, "ADV_REDUCED_1DAY")).reduceByKey(add)

# 사용자 id를 기준으로 정렬.
sortedclicks = clickdata.sortByKey()

# 5. 사용자 id별로 id, morn_cnt, aft_cnt, eve_cnt,회night_cnt, mobile_cnt, unique_cnt
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
    trainfile.write("%d" % clicked)

    # the libSVM format wants features to start with 1
    for i in range(1, len(training_row) + 1):
        trainfile.write(" %d:%.2f" % (i, training_row[i - 1]))
    trainfile.write("\n")

    # (optional) so we can watch the output
    trainfile.flush()

print "averages1:  unique: %d morning: %d afternoon: %d evening: %d night: %d mobile: %d" % \
    (unique, morn, aft, eve, night, mobile)
print "done"
```


## STEP 3) feature를 이용하여 머신러닝 알고리즘(SVM)으로 학습하고, 사용자 분류하
### run python script(predict_ml_libsvm.py)
```
> cd ~/demo-spark-analytics/00.stage3
> python predict_ml_libsvm.py
아래 메세지가 보이면 정상
all: 5000 training size: 3484, test size 1516
LBFGS error: 0.0105540897098
```
- 5000건 데이터 중에 3,484 건은 학습데이터로 이용하고, 1,516 건은 검증용으로 활용
- 1,516건을 학습된 모델로 검증한 결과, 에러율리 0.01(정확도 99%)로 나타남.

### python code에 대한 설명
- 1. features.txt에서 읽어온 데이터를 70:30으로 분류 (학습: 검증)
 * features.txt의 data가 이미 SVM 학습을 위한 포맷으로 구성됨.
 * 3500 : 1500 으로 분류되어야 하는데.. 3486 : 1515으로 분류됨(확인필요)
- 2. LogisticRegressionWithLBFGS 함수로 학습데이터로 모델을 training
- 3. training된 모델을 이용하여, test 데이터를 검증
- 4. 학습된 모델을 이용하여 분류한 사용자 정보를 redis에 저장
 * redis key = pred_event:사용자id
 * redis value = 1(광고를 보여줄 사용자), 0(광고를 보여주지 않을 사용자)
 * spark streaming에서 해당 정보를 이용하여 광고 표출여부를 판단.
- 참고 : LibSVM https://www.csie.ntu.edu.tw/~cjlin/libsvm/
```python
conf = SparkConf().setAppName('Stage3_AdPredictor').setMaster("local[1]")
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
err_1 = results_1.filter(lambda (v, p): v != p).count() / float(te_count)

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
        print "close file"

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

print "\nall: %d training size: %d, test size %d" % (all.count(), tr_count, te_count)
print "LBFGS error: %s" % (str(err_1))
```

- 1 day upgrade 이벤트 광고를 클릭할 사용자를 분류하였는지 redis에서 확인
```
> cd ~/demo-spark-analytics/sw/redis-3.0.7
> src/redis-cli
127.0.0.1:6379> get pred_event:2 #사용자 id 2번은 광고 대상이 아님으로 분류
"0"
```

## STEP 4) spark streaming 기능 추가 (사용자 접속시 광고이벤트 발생)
### spark code 변경

- Stage3StreamingDriver.scala 파일에서 변경된 부분만 보
- elasticsearch에 저장할 field(columnList)에 광고를 발송했는지 여부를 보여주는 "SendEvent"가 추가됨
- "predict_ml_libsvm.py"에서 redis에 저장한 키(pred_event:사용자 id)로 광고표추 대상인자 확인하고, 맞는 경우에는 광고를 전송하라는 flag를 redis에 전달한다. (key는 1_day_event_users)
- 실제 광고를 내보내는 역할은 별도의 광고대행사 또는 시스템에서 처리하게 되고, spark-streaming에서는 빠르게 사용자에 대한 광고표출 여부만 판단하도록 한다.

```scala
    // [STEP 2]. parser message and join customer info from redis
    // original msg = ["event_id","customer_id","track_id","datetime","ismobile","listening_zip_code"]
    val columnList  = List("@timestamp", "customer_id","track_id","ismobile","listening_zip_code", "name", "age", "gender", "zip", "Address", "SignDate", "Status", "Level", "Campaign", "LinkedWithApps", "SendEvent")
    val wordList    = lines.mapPartitions(iter => {
      val r = new RedisClient("localhost", 6379)
      iter.toList.map(s => {
        val listMap = new mutable.LinkedHashMap[String, Any]()
        val split   = s.split(",")

        listMap.put(columnList(0), getTimestamp()) //timestamp
        listMap.put(columnList(1), split(1).trim) //customer_id
        listMap.put(columnList(2), split(2).trim) //track_id
        listMap.put(columnList(3), split(4).trim.toInt) //ismobile
        listMap.put(columnList(4), split(5).trim.replace("\"", "")) //listening_zip_code

        // get customer info from redis
        val cust = r.hmget(split(1).trim, "name", "age", "gender", "zip", "Address", "SignDate", "Status", "Level", "Campaign", "LinkedWithApps")

        // extract detail info and map with elasticsearch field
        listMap.put(columnList(5), cust.get("name"))
        listMap.put(columnList(6), cust.get("age").toInt)
        listMap.put(columnList(7), cust.get("gender"))
        listMap.put(columnList(8), cust.get("zip"))
        listMap.put(columnList(9), cust.get("Address"))
        listMap.put(columnList(10), cust.get("SignDate"))
        listMap.put(columnList(11), cust.get("Status"))
        listMap.put(columnList(12), cust.get("Level"))
        listMap.put(columnList(13), cust.get("Campaign"))
        listMap.put(columnList(14), cust.get("LinkedWithApps"))

        println(s" map = ${listMap.toString()}")

        // 광고 대상 사용자인지 체크하고, 광고 대상자라면 광고 메세지를 보낸다.
        // 광고 여부 확인
        val pred_key = s"pred_event:${split(1).trim}"
        val pred = r.get(pred_key).get.toInt
        // 광고 대상이 맞다면, 광고를 보내라는 신호를 redis에 전송
        if(pred == 1) {
          r.sadd("1_day_event_users", split(1).trim)
          println(s"insert into redis ${pred_key}  : ${split(1).trim}")
          //elasticsearch user 정보에 추가 (광고를 보낸 이력)
        }
        listMap.put(columnList(15), pred.toInt)
        listMap
      }).iterator
    })

```

### compile with scala ide(eclipse) or compile with maven command line
### run spark streaming
- spark-submit을 통해 spark application을 실행시킨다. 
```
> cd ~/demo-spark-analytics/00.stage3
> ./run_spark_streaming_s3.sh
```

## STEP 5) run apache kafka, redis, apache spark + stage1(elasticsearch & kibana)
### 기존에 구동한 sw가 동작중이라면 별도의 작업이 필요없음.
### 새롭게 구동해야 한다면 아래의 절차를 따름
- 1. elasticsearch 실행
- 2. kibana 실행 
- 3. zookeeper 실행 
- 4. kafka server 실행 
- 5. redis server 실행 
- 6. logstash 실행 (00.stage2의 logstash conf로 구동)
- 7. data_generator 실행 

## STEP 6) kibana를 이용한 데이터 시각화
- 개인별 실습 과제 
 - 추가된 필드인 SendEvent를 이용한 Visualization chart를 생성.
 - dashboard를 구성하고, 이를 json으로 export  




