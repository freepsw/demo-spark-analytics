# Step by step demo about realtime analytics of user behavior
- This project is designed based on mapR Tech blog.
- https://www.mapr.com/blog/real-time-user-profiles-spark-drill-and-mapr-db


## PART 1. About demo scenario
### 1). User Story
- Music streaming site를 제공하는 회사에서 수많은 사용자들의 행위(behavior)를 조회하고자 함.
- 하루 중 가장 많이 사용하는지? 남성/여성 및 연령의 비율은? Mobile/PC의 비율은? 접속지역은?
- 이러한 정보를 다양한 chart, map으로 시각화하여 실시간으로 보고자 함  

- 또한 실시간으로 수집된 사용자 정보를 활용하여 마케팅 대상을 분류할 수 있는 머신러닝 모델 학습
- 학습된 모델을 이용해 사용자별로 마케팅 적용대상을 분류하여 접속시에 광고를 전달하고자 함.
- 이를 위해 많은 사용자들의 log를 실시간으로 수집하여 분산처리 및 시각화하는 기술/시간/자원 필요.

### 2). Data Model
####- Individual customers listening to individual tracks (tracks.csv)

   ![Image of tracks table](https://www.mapr.com/sites/default/files/blogimages/blog_RealTimeUser-table1.png)
 - 어떤 고객이 어떤 track(음악)을 들었는지 알려주거나, 모바일에서 접속했는지, 실제 음악을 들은 지역은 어디인지 알수 있는 정보.
 - The event, customer and track IDs tell us what occurred (a customer listened to a certain track), while the other fields tell us some associated information, like whether the customer was listening on a mobile device and a guess about their location while they were listening.


####- Customer information (cust.csv)
   ![Image of customers table](https://www.mapr.com/sites/default/files/blogimages/blog_RealTimeUser-table2.png)
- The fields are defined as follows:
  - Customer ID: a unique identifier for that customer Name, gender, address, zip: the customer’s associated information  
  - Sign date: the date of addition to the service  
  - Status: indicates whether or not the account is active (0 = closed, 1 = active)
  - Level: indicates what level of service -- 0, 1, 2 for Free, Silver and Gold, respectively  
  - Campaign: indicates the campaign under which the user joined, defined as the following (fictional) campaigns driven by our (also fictional) marketing team:
   * NONE - no campaign  
   * 30DAYFREE - a ‘30 days free’ trial offer  
   * SUPERBOWL - a Superbowl-related program  
   * RETAILSTORE - an offer originating in brick-and-mortar retail stores  
   * WEBOFFER - an offer for web-originated customers  
>

####- Previous ad clicks(clicks.csv)
- indicating which ad was played to the user and whether or not they clicked on it

EventID | CustID | AdClicked | Localtime
------------ | ------------- | ------------- | -------------
0 | 109 | ADV_FREE_REFERRAL | 2014-12-18 08:15:16


####- Music information (music.csv)

TrackId | Title | Artist | Length
------------ | ------------- | ------------- | -------------
0 | Caught Up In You | .38 Special | 200

####- Customer behaviors (live table) : summary data about listening habits, for example what time(s) of day were they listening, how much listening on a mobile device, and how many unique tracks they played

### 3). 단계별 구현 시나리오
#### Stage 1. Simple realtime visualization
- 사용자의 접속로그를 logstsh로 수집하여 Elasticsearch로 저장한 후, kibana를 이용하여 빠르게 시각화
- What are customer doing?
 * 시간별 사용량 추이를 어떠한가? 그 중 mobile 접속자는 어느정도 되는가?
 * 가장 많이 접속하는 지역은 어디인가?
 * 지역별로 어떤 사용자들이 접속하는가? (customer_id만 조회가능)
 * 사용자들이 언제 어떤 음악을 듣는가? (customer_id와 track_id만 조회가능)

#### Stage 2. Stage1 + distributed processing using apache spark
- logstash에서  kafka로 저장하고, 이를 spark에서 실시간 분산처리 -> ES
- customerid, trackid와 상세정보를 join(redis)하여 데이터를 추가한다. -> ES
- 특정 시간(30분) 이내에 같은 곡을 3번 이상 들은 사용자는 해당곡을 관심 list로 등록 -> Redis, ES


#### Stage 3. Stage 1~2 + classify user by mllib(logistic regression)
- 특정 등급(Gold)이하의 사용자에게 이벤트 광고
- (display ad about one day gold grade upgrade offer)
- 주기적으로 ml model(logistic regression)을 학습하고, 에러율을 체크하여 모델 사용여부를 판단
- 에러가 일정수준(에를 들어 10% 이하)이면 해당 사용자를 광고할 타겟으로 분류
- Stage2의 spark streaming에 광고타겟 사용자일 경우 SMS전송 기능 추가 (Redis로 저장 -> SMS 서버에서 발송)


#### Stage 4. Stage 1~3 + replace open source sw with public cloud service (dataproc, pubsub)
- Cloud에서 제공하는 실시간 대용량 빅데이터 처리 기술을 활용하여 서비스를 안정적으로 제공
- 전체 서비스 중에서 대량의 데이터를 처리하는 영역인 Apache Kafka와 Apache Spark 영역을 GCP 서비스로 대체


## Part 2. Project settings

### 1) download this demo project
- github에서 demo project를 다운받고, 해당 프로젝트 폴더로 이동한다.

```
> cd ~
> git clone https://github.com/freepsw/demo-spark-analytics.git
> cd demo-spark-analytics
> mkdir sw
```

### 2) demo에 필요한 open source
#### - development tools (library, pacakge ..) [link](https://github.com/freepsw/demo-spark-analytics/blob/master/01.installed_sw/centos68-min.md)
- centos 6.8 minimal
- java 1.8+
- python 2.7

#### - logstash [link](https://github.com/freepsw/demo-spark-analytics/blob/master/01.installed_sw/logstash.md)

#### - elasticsearch [link](https://github.com/freepsw/demo-spark-analytics/blob/master/01.installed_sw/elasticsearch.md)

#### - kibana [link](https://github.com/freepsw/demo-spark-analytics/blob/master/01.installed_sw/kibana.md)

#### - apache kafka [link](https://github.com/freepsw/demo-spark-analytics/blob/master/01.installed_sw/apache_kafka.md)

#### - apache spark [link](https://github.com/freepsw/demo-spark-analytics/blob/master/01.installed_sw/apache_spark.md)

#### - redis [link](https://github.com/freepsw/demo-spark-analytics/blob/master/01.installed_sw/redis.md)



## Part 3. Implementing demo project
### Stage 1 Demo scenario & implementation guide
![stage1 architecture](https://github.com/freepsw/demo-spark-analytics/blob/master/resources/images/stage1.png)
- https://github.com/freepsw/demo-spark-analytics/tree/master/00.stage1

### Stage 2 Demo scenario & implementation guide
![stage2 architecture](https://github.com/freepsw/demo-spark-analytics/blob/master/resources/images/stage2.png)
- https://github.com/freepsw/demo-spark-analytics/tree/master/00.stage2

### Stage 3 Demo scenario & implementation guide
![stage3 architecture](https://github.com/freepsw/demo-spark-analytics/blob/master/resources/images/stage3.png)
https://github.com/freepsw/demo-spark-analytics/tree/master/00.stage3


### Stage 4-1 Demo scenario & implementation guide
![stage4 architecture](https://github.com/freepsw/demo-spark-analytics/blob/master/resources/images/stage4-1.png)
https://github.com/freepsw/demo-spark-analytics/tree/master/00.stage4-1

### Stage 4-1 Demo scenario & implementation guide
![stage4-1 architecture](https://github.com/freepsw/demo-spark-analytics/blob/master/resources/images/stage4-2.png)
https://github.com/freepsw/demo-spark-analytics/tree/master/00.stage4-2


## ETC 

### Git version 관리

```
# tag 목록 확인 
> git tag

# git tag 추가 (현재 master branch의 버전을 tag로 생성)
> git tag -a v2.0.0 -m "2022.04 stage4 cloud data pipeline" 

# github에 tag 정보 업데이트 
> git push  origin master v2.0.0 
```

