# Stage 3. Stage1 + Stage2 + spark mlLib
- 회사의 매출향상을 위해 사용자들의 최근 광고 click 기록을 기반으로 요금제(FREE, SILVER, GOLD) 업그레드를 광고를 할 대상을 분류 
- 분류된 사용자들 만을 대상으로 music을 듣기 바로 전에 광고 영상을 제공 

## Stage 2의 주요 내용 
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
> ssudo pip install psutil 
```
- psutil은 "(shuffle.py:58: UserWarning: Please install psutil to have better support with spilling))"와 같은 warning을 방지하기 위해 설치 (pyspark ml 실행시 발생)

## STEP 2) make training datat set(features) from user log
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

Technical changes (support huge data processing using spark)
 * logstash의 biz logic(filter)을 단순화하여 최대한 많은 양을 전송하는 용도로 활용한다.
 * 그리고 kafka를 이용하여 대량의 데이터를 빠르고, 안전하게 저장 및 전달하는 Message queue로 활용한다.
 * Spark streaming은 kafka에서 받아온 데이터를 실시간 분산처리하여 대상 DB(ES or others)에 병렬로 저장한다. 
  - 필요한 통계정보(최근 30분간 접속통계 등을 5분단위로 저장 등) 및  복잡한 biz logic지원
 * redis는 spark streaming에서 customer/music id를 빠르게 join하기 위한 memory cache역할을 한다.


install python 
install numpy
sudo pip install psutil (shuffle.py:58: UserWarning: Please install psutil to have better support with spilling))

train을 위한 feature 데이터 생성
rt_profile_dash.py 


## STEP 2) train a svm model using libsvm library

LibSVM : https://www.csie.ntu.edu.tw/~cjlin/libsvm/
