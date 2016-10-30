# Apache kafka

## Why kafka ?
>
- kafka는 기존 message queue에서 사용하는 용도(messaging, website activity tracking, log aggregation, operational metrics, stream processing)로 주로 활용되고 있다.
- 그럼 왜 kafka를 써야하는 걸까?
- 대부분의 message queue의 한계(fault tolerance, memory 용량에 따른 throughtput)를 극복할 수 있는 용도
- disk기반으로 message 유실을 없애고, broker-partition을 활용하여 100K+/sec 성능을 충분히 보장
>

## Basic Concept
https://kafka.apache.org/intro

### Kafka API 
- Producer, Consumer, Stream, Connector
![apache kafka api] (https://kafka.apache.org/images/kafka-apis.png)

### Quick start
https://kafka.apache.org/quickstart


## 1. Install (kafka_2.11-0.10.1.0)
```
> cd ~/demo-spark-analytics/sw
> wget http://apache.mirror.cdnetworks.com/kafka/0.10.1.0/kafka_2.11-0.10.1.0.tgz 
> tar -xzf kafka_2.11-0.10.1.0.tgz
> cd kafka_2.11-0.10.1.0
```

## 2. run zookeeper
```
> bin/zookeeper-server-start.sh config/zookeeper.properties
```

## 3. run kafka
```
> cd ~/demo-spark-analytics/sw/kafka_2.11-0.10.1.0
> bin/kafka-server-start.sh config/server.properties
```


## 4. create topic for stage 2 (realtime)
```
> cd ~/demo-spark-analytics/sw/kafka_2.11-0.10.1.0
> bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic realtime
# check created topic "realtime"
> bin/kafka-topics.sh --list --zookeeper localhost:2181
realtime
```

## 5. new console-consumer
```
> bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic realtime
```

## 6. monitoring
```
# describe topic detail
> bin/kafka-topics.sh --describe --zookeeper localhost:2181 --topic realtime

# current group name
> bin/kafka-run-class.sh kafka.admin.ConsumerGroupCommand --zookeeper 127.0.0.1:2181 --list
realtime-group1

# detail info about topic
> bin/kafka-consumer-groups.sh --zookeeper localhost:2181 --describe --group realtime-group1

# zookeeper client
> bin/zookeeper-shell.sh localhost:2181
```

## 7. delete topic 
```
> bin/kafka-topics.sh --delete --zookeeper localhost:2181 --topic <topic_name>
```

## stop kafka
```
bin/kafka-server-stop.sh
```