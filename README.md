# Demo about realtime analytics of user behavior
- This project is designed based on mapR Tech blog.
- https://www.mapr.com/blog/real-time-user-profiles-spark-drill-and-mapr-db


## Part 1. Demo scenario
### 1). User Story
- Music streaming site를 제공하는 회사에서 수많은 사용자들의 행위(behavior)를 조회하고자 함.
- 하루 중 가장 많이 사용하는지? 남성/여성 및 연령의 비율은? Mobile/PC의 비율은? 접속지역은?
- 이러한 정보를 다양한 chart, map으로 시각화하여 실시간으로 보고자 함.


- 또한 실시간으로 수집된 사용자 정보를 활용하여 마케팅 대상을 분류할 수 있는 머신러닝 모델 학습
- 학습된 모델을 이용해 사용자별로 마케팅 적용대상을 분류하여 접속시에 광고를 전달하고자 함.
- 이를 위해 많은 사용자들의 log를 실시간으로 수집하여 분산처리 및 시각화하는 기술/시간/자원 필요.

### 2). Data Model
- Individual customers listening to individual tracks

   ![Image of tracks table] (https://www.mapr.com/sites/default/files/blogimages/blog_RealTimeUser-table1.png)

- Customer information

   ![Image of customers table] (https://www.mapr.com/sites/default/files/blogimages/blog_RealTimeUser-table2.png)

- Previous ad clicks : indicating which ad was played to the user and whether or not they clicked on it (clicks table)

```
 EventID, CustID, AdClicked,           Localtime
 0,       109,    "ADV_FREE_REFERRAL", "2014-12-18 08:15:16"
```

- Customer behaviors (live table) : summary data about listening habits, for example what time(s) of day were they listening, how much listening on a mobile device, and how many unique tracks they played

### 3). Define demo level
#### Stage 1. Simple realtime visualization
- 사용자의 접속로그를 logstsh로 수집하여 Elasticsearch로 저장한 후, kibana를 이용하여 빠르게 시각화


#### Stage 2. Stage1 + distributed processing with apache spark
- logstash에서  kafka로 저장하고, 이를 spark에서 실시간 집계를 수행 -> ES
- customerid, trackid와 상세정보를 join하여 데이터를 추가한다. -> ES
- 특정 시간(30분) 이내에 같은 곡을 3번 이상 들은 사용자는 해당곡을 관심 list로 등록 -> Redis, ES


#### Stage 3. Stage 1~2 + classify user by mllib(logistic regression)
- 특정 등급(Gold)이하의 사용자에게 이벤트 광고
- (display ad about one day gold grade upgrade offer)
- 주기적으로 ml model(logistic regression)을 학습하고, 에러율을 체크하여 모델 사용여부를 판단
- 에러가 일정수준(에를 들어 10% 이하)이면 해당 사용자를 광고할 타겟으로 분류
- Stage2의 spark streaming에 광고타겟 사용자일 경우 SMS전송 기능 추가 (Redis로 저장 -> SMS 서버에서 발송)


#### Stage 4. Stage 1~3 + advanced visualization for web page with c3.js or slamData
- nodejs를 이용하여 직접 실시간 데이터의 시각화


### Part 2. Install necessary software (mac)
#### 1) logstash
##### - install
 - [link](https://github.com/freepsw/demo-spark-analytics/tree/master/01.logstash)

#### 2) elasticsearch
##### - install



## Part 2. Implementing Stage 1

### 1) data generator (data_generator.py)
- 
- 실시간으로 데이터가 유입될 수 있도록 data generator에서 특정 file에 write (random time period)
- 이는 실시간으로 사용자들이 접속하는 log를 재연하기 위한 용도로 사용.
- tracks.csv -> data generator(date를 현재 시간으로 조정) -> tracks_live.log
- 1,000,000건이 저장된 tracks.csv에서 파일을 읽어서, 랜덤한 시간 간격으로 tracks_live.csv에 쓴다.
- data_generator.py
```python
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

r_fname = "tracks.csv"
w_fname = "tracks_live.csv"

rf = open(r_fname)
wf = open(w_fname, "a+")

try:
    num_lines = sum(1 for line in rf)
    rf.seek(0)
    lines = 0
    while (1):
        line = rf.readline()
        wf.write(line)
        wf.flush()


        stime = weighted_choice([(1,30), (0.5,20), (2,20), (0.8,10), (0.3,10), (0.1,5), (3,5)])

        time.sleep(stime)
        lines += 1

        if(lines == num_lines):
            break
finally:
    rf.close()
    wf.close()
    print "close file"
```

### 2) logstash


#### configuration (collect logs and save to ES, logstash_stage1.conf)
 -  input 
  -  data_generator.py가 write하는 tracks_live.csv에 내용이 추가되면 읽어서 ES에 저장한다.
 - filtersssss 한ㄴ다. ㅇ이이알ㅇㄹ라라링ㄹ링링ㄹ잉링이ㅣ이


### kibana
####- index 추가 

####- discovery에서 입력되는 count 확인
 * 각 record의 상세 데이터 확인
 * 조회할 기간 변경 
 * filter를 이용해서 검색어를 입력하여 원하는 데이터만 조회
 * 화면에 표시될 필드 선택하여 조회
 * 위의 bar 그래프에서 원하는 영역을 drag하여 drill down 조

####- visualization 메뉴를 이용하여 시각화 
 * pie chart 선택 (전체 날짜 선택)
  - spilit chart -> missing value가 있는 경우 그래프에서 삭제 (-모양의 돗보기 사용), top n개만 보여지도록 설정
  - split chart (하나의 항목별로 다른 항목의 비중 조회), 예를 들면 서울지역에서 남/여 비율
  - 2개의 split의 순서를 조정(increase priority, 화살표 버튼)
  - save current visualization as visualization component (this can be used for dashboard later)
 * line chart (시간별 추세 확인 용도)
  - 초기에 데이터를 split하지 않으면 x축이 all, y축이 count로 전체 데이터의 건수가 하나의 포인트로 표시된다. 
  - X-Axis > split > date histogram -> x축의 date column 선택 -> interval은 auto로 조회
  - 위에서는 단순히 record 건수만 표시되므로, 이를 특정 field의 값을 표시하도록 하자 (이때, time interval이 1 day인 경우 1일 동안의 값을 어떻게 보여주지 선택)
  - Y-Axis의 aggregation에서 field명과 선택된 field에 대한 sum, average등을 선택(string type의 field는 선택하지 못함)
  - X-Axis > split line -> Sub aggregation(Terms) -> 특정 field를 선택하면 해당 field의 상위 n개에 대하여 line이 추가된다. (priority를 높여서 그래서 재생 )




