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


## STEP 1) make training datat set(features) from user log
- 

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
