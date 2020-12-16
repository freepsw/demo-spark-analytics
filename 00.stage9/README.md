# Stage 4. Stage3 + Monitoring pipeline process using jmx & elk stack (작성중...)
- logstash -> kafka -> spark streaming으로 전달되는 데이터의 흐름을 직관적으로 파악할 수 있는 모니터링 시스템 구성


## Stage 4의 주요 내용
### 시스템의 안정적인 운영 및 장애대처를 위한 모니터링 체계 필요
#### Problem
 - 아래와 같이 각 오픈소스별로 각각의 모니터링 화면을 일부 제공
 - 데이터의 전체 흐름을 모니터링 할 수 있는 방법이 없음
 - 이로 인하여, 장애 발생시 한번에 원인을 파악하기 어려우며, 각각의 로그 및 UI를 확인해야 함. (장애대응 속도 저하)

- logstash : 별도 모니터링 UI 없음
- Apache kafka : 다양한 오픈소스 및 상용 도구 존재, Confluent Control Center가 가장 활용성 높음
  * check_kafka.pl (무료))
  * KafkaOffsetMonitor : offset 정보를 모니터링하는 웹 ui (무료))
  * Burrow. : consumer 상태 모니터링만 제공 (무료))
  * Kafka-Manager. : ui기반의 모니터링 도구 (무료)
  * kafkat. : command line 기반의 관리 도구 (무료)
  * Confluent Control Center : 가장 많은 모티너링 서비스 제공 (무로)
  * datadog : 상용서비스, 클라우스 기반 모니터링 UI제공 (다양한 오픈소스 지원, 외부 접속이 안되는 환경에서는 사용불가)
- Apache Spark : Spark 자체 Web UI제공

#### Solution
- 데이터의 흐름이 있는 시스템에서는 전체 단계 중에 어떤 단계에서 문제가 발생하고 있는지 확인하는 것이 중요
- 따라서, 각 오픈소스의 모니터링 지표 중 성능(처리량 등)에 영향을 미치는 지표를 선정하고,
- 이를 통합하여 하나의 연속적인 흐름으로 파악할 수 있는 통합 모니터링 UI가 필요함

#### How?
- JVM 기반의 오픈소스는 JMX metrics 정보를 외부로 전송할 수 있는 기능을 제공한다.
- 이 JMX metrics에 다양한 성능 및 jvm 자원상황등에 대한 정보를 포함한다.
- 이번 단계에서는 logstash jmx plugin을 이용하여,
- logstash-kafka-spark에서 jmx metrics를 수집하고,
- elasticsearch에 저장 후, kibana를 이용하여 자체 통합 모니터링 UI를 구성할 것이다.

## [STEP 0] Stop the processes to adapt jmx configuration
- stage3에서 동작 중인 프로세스(오픈소스)를 jmx 적용을 위하여 중지함 .(logstash, kafka, spark)
```
## stop data geneartor python process
- 아래 프로세스 검색을 통해 pid 조회후 kill -9 pid로 중지 또는 Ctrl+C
> ps -ef | grep python

## stop logstash
- 아래 프로세스 검색을 통해 pid 조회후 kill -9 pid로 중지 또는 Ctrl+C
> ps -ef | grep logstash

## stop apache spark streaming
> sbin/stop-all.sh

## stop apache kafka
> bin/kafka-server-stop.sh

## stop apache zookeeper
> bin/zookeeper-server-stop.sh
```  


## [STEP 1] Set JMX port on logstash + kafka + spark  and restart all

### 1). Set JMX port on Apache Kafka and run
- Apache Kafka는 별도의 환경파일 수정없이
- 실행시점에 JMX Port를 지정할 수 있다.

#### Run apache Zookeeper
```
> cd ~/demo-spark-analytics/sw/kafka_2.11-0.10.1.0
> env JMX_PORT=9998 bin/zookeeper-server-start.sh config/zookeeper.properties
```

#### Run apache Kafka
```
> cd ~/demo-spark-analytics/sw/kafka_2.11-0.10.1.0
> vi bin/kafka-run-class.sh
- 아래 hostname을 추가한다.
# JMX settings
if [ -z "$KAFKA_JMX_OPTS" ]; then
  KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false  -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=172.16.118.128"
fi
```

- JMX_PORT를 지정하여 Apach kafka를 실행
```
> cd ~/demo-spark-analytics/sw/kafka_2.11-0.10.1.0
> env JMX_PORT=9999 bin/kafka-server-start.sh config/server.properties
```

### 2). Set JMX port on Apache Spark and run
- 먼저 jmxsink가 가능하도록 spark config(metrics.properties)를 수정한다.
- 아래 항목의 주석을 제거하여 활성화 한다.

```
> cd ~/demo-spark-analytics/sw/spark-2.0.1-bin-hadoop2.7
> vi conf/metrics.properties
# Enable JmxSink for all instances by class name
*.sink.jmx.class=org.apache.spark.metrics.sink.JmxSink

# Enable JvmSource for instance master, worker, driver and executor
master.source.jvm.class=org.apache.spark.metrics.source.JvmSource
worker.source.jvm.class=org.apache.spark.metrics.source.JvmSource
driver.source.jvm.class=org.apache.spark.metrics.source.JvmSource
executor.source.jvm.class=org.apache.spark.metrics.source.JvmSource
```

- 두번 째로 sprk-submit 시에 driver와 executor가 구동되는 jvm에 jmx port를 오픈하도록 한다.
```
> cd ~/demo-spark-analytics/00.stage3
> vi run_spark_streaming_s3.sh
# 아래 --conf 항목을 추가한다. (driver 와 executor를 위한 옵션)
spark-submit \
        --class io.skiper.driver.Stage3StreamingDriver \
        --master spark://localhost:7077 \
        --name "demo-spark-analysis-s3" \
        --deploy-mode client \
        --driver-memory 1g \
        --executor-memory 1g \
        --total-executor-cores 2 \
        --executor-cores 2 \
        --conf "spark.driver.extraJavaOptions=-Dcom.sun.management.jmxremot -Dcom.sun.management.jmxremote.port=8090 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=172.16.118.128" \
        --conf "spark.executor.extraJavaOptions=-Dcom.sun.management.jmxremot -Dcom.sun.management.jmxremote.port=8091 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=172.16.118.128" \
        ../00.stage2/demo-streaming/target/demo-streaming-1.0-SNAPSHOT-jar-with-dependencies.jar
```
##### Run spark
```
> cd ~/demo-spark-analytics/00.stage4
> ./run_spark_streaming_s4.sh
```



### 3). Set JMX port on logstash and run
- logstash 설정파일에 jmx metric을 전송하기 위한 옵션설정
```
> cd ~/demo-spark-analytics/sw/logstash-2.4.0/
> vi bin/logstash.lib.sh
- 아래 JAVA_OPTS 항목을 추가
if [ "$LS_JAVA_OPTS" ] ; then
  # The client set the variable LS_JAVA_OPTS, choosing his own
  # set of java opts.
  JAVA_OPTS="$JAVA_OPTS $LS_JAVA_OPTS"
  JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote"
  JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.port=9010"
  JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.local.only=false"
  JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.authenticate=false"
  JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.ssl=false"
  JAVA_OPTS="$JAVA_OPTS -Djava.rmi.server.hostname=현재서버의IP"
fi
```
- Djava.rmi.server.hostname (logstash가 실행되고 있는 서버의 IP)
- 위 옵션을 설정하지 않아도 잘 되는 경우가 있던데... 어떤차이인지 확인 필요.

#### Run logstash
```
> cd ~/demo-spark-analytics/00.stage2
> logstash -f logstash_stage2.conf
```




## [STEP 2] Check jmx metrics using jconsole
- JMX Metrics를 조회할 수 있는 다양한 도구들이 있다.
- 여기서는 jconsole을 이용하여 원격으로 jmx metrics를 확인해 본다. (java를 설치하면 자동으로 설치됨)
- jconsole의 실행은 현재 노트북 또는 pc에서 실행한다.

### jconsole 실행
- terminal에서 아래의 명령어 실행
```
> jconsole
```

- 접속할 "Remote Process" IP:PORT를 입력한다.
  - IP는 logstash, spark, kafka가 설치된 서버의 IP를 의미
  - PORT는 jmx설정에서 지정한 port를 의미

-> 그림


### Logstash JMX metrics 확인
-> 그림

### Apache Kafka JMX metrics 확인
--> 그림

### Apache Spark JMX metrics 확인
--> 그림


## [STEP 3] Collect jmx metrics using logstah jmx-input-plugin
- Jmx metrics가 제대로 전송되는지 확인했다면,
- 지금부터는 logstash를 이용하여 remote에서 jmx metrics를 수집해보자.
- 최종적인 목적은
  - logstash로 수집하고
  - elasticsearch로 저장한 후
  - kibana로 시각화하여
- 데이터의 흐름을 한번에 확인하고, 이를 통해 장애탐지/운영 효율화를 하기 위함이다.


### 1). Install logstash jmx plugins
- logstash의 jmx 수집을 위해서는 별도이 plugin이 필요하다.

```
# online install
> cd ~/demo-spark-analytics/sw/logstash-2.4.0/
> bin/logstash-plugin install logstash-input-jmx

# offline install
# export plugins to zip
> bin/logstash-plugin prepare-offline-pack logstash-input-jmx

# import zip to logstash plugins
> bin/logstash-plugin install file:///home/rts/logstash-offline-plugins-5.4.1.zip
```
