# Stage 2. Stage1 + distributed processing using apache spark
- logstash에서  kafka로 저장하고, 이를 spark에서 실시간 분산처리 후 ES에 저장 
 * logstash > kafka > spark streaming > ES/redis

## Stage 2의 주요 내용 
### Stage1의 한계
 * Stage1에서는 실시간 Data가 많아지게 될 경우, 하나의 logstash로는 대량의 데이터 처리가 어려운 상황이다.
 * 또한 customer_id, track_id 이외의 구체적인 정보가 없어서 세분화된 분석을 하기 어렵다 (예를 들면 남성이 가장 좋아하는 음악은?) 
 * 매번 ES전체 table을 조회하여 데이터를 시각화하게 되어, 성능상의 부하가 예상된다.

### Technical changes (support huge data processing using spark)
 * logstash의 biz logic(filter)을 단순화하여 최대한 많은 양을 전송하는 용도로 활용한다.
 * 그리고 kafka를 이용하여 대량의 데이터를 빠르고, 안전하게 저장 및 전달하는 Message queue로 활용한다.
 * Spark streaming은 kafka에서 받아온 데이터를 실시간 분산처리하여 대상 DB(ES or others)에 병렬로 저장한다. 
  - 필요한 통계정보(최근 30분간 접속통계 등을 5분단위로 저장 등) 및  복잡한 biz logic지원
 * redis는 spark streaming에서 customer/music id를 빠르게 join하기 위한 memory cache역할을 한다.

## STEP 1) install apache kafka, redis, apache spark
### install apache kafka

### edit kafka config (server.config)
- 실습을 위해서 topic을 delete한 후 재생성할 수 있도록 설정
```
> cd ~/demo-spark-analytics/sw/kafka_2.11-0.10.1.0
> vi config/server.properties
# Switch to enable topic deletion or not, default value is false
delete.topic.enable=true
```

### install redis

### install apahche spark


## STEP 2) run zookeeper and kafka and redis
### run zookeeper 
```
> cd ~/demo-spark-analytics/sw/kafka_2.11-0.10.1.0
> bin/zookeeper-server-start.sh config/zookeeper.properties
```

### run kafka
```
> cd ~/demo-spark-analytics/sw/kafka_2.11-0.10.1.0
> bin/kafka-server-start.sh config/server.properties
```

### create topic for stage 2 (realtime) 
- logstash will use this topic to send user log
```
> cd ~/demo-spark-analytics/sw/kafka_2.11-0.10.1.0
> bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic realtime
# check created topic "realtime"
> bin/kafka-topics.sh --list --zookeeper localhost:2181
realtime
```

### run redis


## STEP 3) import customer info to redis
### install python redis package
```
> pip install redis
```


## STEP 4) run logstash (read logs --> kafka)
### open configurations
```
> cd ~/demo-spark-analytics/00.stage2
> vi logstash_stage2.conf
```

### edit configuration
-  output
 * codec : logs에서 읽은 문자열을 그대로 kafka로 저장
 * bootstrap_servers : kafka server(broker)의 ip:port
 * topic_id : kafka에서 생성한 topic 명 (spark에서 동일 topic명으로 데이터를 읽음)

```javascript
input {  
  file {
    path => "/Users/skiper/work/DevTools/github/demo-spark-analytics/00.stage1/tracks_live.csv"
    sincedb_path => "/dev/null"
    start_position => "beginning"
  }
}

output {
  stdout {
    codec => rubydebug{ }
  }
  
  kafka {
    codec => plain {
      format => "%{message}"
    }
    bootstrap_servers => "localhost:9092"
    topic_id => "realtime"
  }
}
```

### prepare test data set
```
> cd ~/demo-spark-analytics/00.stage1
> vi tracks_live.csv
# paste this message
0,48,453,"2014-10-23 03:26:20",0,"72132"
1,1081,19,"2014-10-15 18:32:14",1,"17307"
2,532,36,"2014-12-10 15:33:16",1,"66216"
```

### run
```
> cd ~/demo-spark-analytics/00.stage2
> logstash -f logstash_stage2.conf
# 아래와 같은 메세지가 출력되면 정상
Settings: Default pipeline workers: 8
Pipeline main started
{
               "message" => "0,48,453,\"2014-10-23 03:26:20\",0,\"72132\"",
              "@version" => "1",
            "@timestamp" => "2016-10-25T08:58:09.796Z",
                  "path" => "/Users/skiper/work/DevTools/github/demo-spark-analytics/00.stage1/tracks_live.csv",
                  "host" => "skcc11n00142",
              "event_id" => "0",
           "customer_id" => "48",
              "track_id" => "453",
              "datetime" => "2014-10-23 03:26:20",
              "ismobile" => "0",
    "listening_zip_code" => "72132"
}
.....
```

### read message from kafka using kafka-console_consumer
```
> cd ~/demo-spark-analytics/sw/kafka_2.11-0.10.1.0
> bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic realtime
# 아래와 같은 메세지가 출력되면 정상
0,48,453,"2014-10-23 03:26:20",0,"72132"
1,1081,19,"2014-10-15 18:32:14",1,"17307"
2,532,36,"2014-12-10 15:33:16",1,"66216
```


## STEP 4) run apache spark streaming application
### create spark application
- create scala project using maven [link](https://github.com/freepsw/java_scala)

### implement application logic 
 * read from kafka
 * parse messages by line and save to variable
 * mapping a user info using db with customer id
 * mapping a music info using db with tracks id
 * aggregating 

### code description
- full source code [link]




### Open source
 * redis : customer info, music info를 빠르게 조회하기 위한 cache memory
  - customer/music 정보를 주기적으로 업데이트 하여 최신정보를 유지한다.
 * logstash : output plugin을 kafka로 변경한다.
 * kafka : install and run (create topic with 3 partition), 1 broker
 * spark : 1 node(client mode), save to ES/redis
  - customerid, trackid와 상세정보를 join(redis)하여 ES에 저장한다.
  - 최근 30시간 동안 남자/여자가 가장 많이 들은 top 10 music
  - 
 * customerid, trackid와 상세정보를 join(redis)하여 데이터를 추가한다. -> ES
- compute a summary profile for each user
 * 특정기간(아침, 점심, 저녁)동안 각 사용자들이 들은 음악의 평균값 (언제 가장 많이 듣는가?)
 * 전체 사용자들이 들은 전체 음악 목록 (중복 제거한 unique값)
 * 모바일 기기에서 들은 전체 음악 목록(중복 제거한 unique) 
- 특정 시간(30분) 이내에 같은 곡을 3번 이상 들은 사용자는 해당곡을 관심 list로 등록 -> Redis, ES