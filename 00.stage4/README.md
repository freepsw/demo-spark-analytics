# Stage 4. Use cloud service for processing real time big data like dataproc, pubsub at Stage3
- Cloud에서 제공하는 실시간 대용량 빅데이터 처리 기술을 활용하여 서비스를 안정적으로 제공
- 전체 서비스 중에서 대량의 데이터를 처리하는 영역인 Apache Kafka와 Apache Spark 영역을 GCP 서비스로 대체

## Stage4의 주요 내용 (스타트업이 비즈니스에 집중할 수 있도록 Cloud Service를 활용해 보자)
- 아키텍처 그림 추가 
### 현재 스타트업의 고민(문제)
- 자체적으로 구축한 빅데이터 오픈소스를 안정적으로 운영하기 위해서는 많은 인프라비용과 전문인력이 필요하다. 
- 하지만, 초기 스타트업은 이러한 비용을 초기에 투자하기 어려운 경우가 많다. 
    - 만약 특별한 상황으로 사용자가 급격하게 줄면? (
        - 미리 구해한 하드웨어 비용이 낭비되고, 전문 인력에 대한 비용도 꾸준히 소비됨
    - 만약 예상한 규모 이상으로 서비스/사용자가 급격하게 늘어나면? 
        - 적시에 하드웨어를 다시 구매하지 못하면, 증가하는 사용자를 처리하지 못하여 서비스 장애 또는 서비스 접속오류 발생
        - 늘어난 오픈소스 소프트웨어의 운영 복잡성으로 서비스 안정화에 더 많은 인력 필요. 
- 초기 스타트업은 핵심 비즈니스에 집중해야 한다.. 
#### 어떻게 해결할 수 있을까?

### 해결방안 
- Cloud Service를 활용하여 하드웨어 동적 할당 및 복잡한 오픈소스 운영 비용 감소
#### Managed Service인 PubSub, DataProc를 활용하여 빅데이터 시스템 운영 최소화
- Apache Kafka를 대체하여 메세지 큐 서비스인 PubSub을 활용
- Apache Spark를 대체하여 실시간 대용량 처리를 위한 DataProc 활용
#### Cloud의 사용량 기반 서비스 활용
- 사용량에 따라 동적으로 클러스터를 할당하여 사용한 만큼만 비용 사용
- PubSub, DataProc 모두 사용자가 클러스터 확장에 대한 고민없이, 필요한 만큼 자동으로 인프라를 할당


## [STEP 1] Create a topic using the GCP PubSub 
- Apache Kafka를 대체할 메세지 큐 서비스인 PubSub을 활용하여 Topic을 생성
- Topic 명은 realtime 



## [STEP 2] Install spark 
```
> cd ~/demo-spark-analytics/sw/
> wget https://downloads.apache.org/spark/spark-2.4.7/spark-2.4.7-bin-hadoop2.7.tgz
> tar xvf spark-2.4.7-bin-hadoop2.7.tgz
> cd spark-2.4.7-bin-hadoop2.7

# slave 설정
> cp conf/slaves.template conf/slaves
localhost //현재  별도의 slave node가 없으므로 localhost를 slave node로 사용

# spark master 설정
# 현재 demo에서는 별도로 변경할 설정이 없다. (실제 적용시 다양한 설정 값 적용)
> cp conf/spark-env.sh.template conf/spark-env.sh

> vi ~/.bash_profile
# 마지막 line에 아래 내용을 추가한다.
export SPARK_HOME=~/demo-spark-analytics/sw/spark-2.4.7-bin-hadoop2.7
export PATH=$PATH:$SPARK_HOME/bin
export PYTHONPATH=$SPARK_HOME/python/:$SPARK_HOME/python/lib/py4j-0.10.7-src.zip:$PYTHONPATH
```

### 소매 데이터 중 날짜별(by-day) 데이터 사용
- 샘플 : https://github.com/FVBros/Spark-The-Definitive-Guide/blob/master/data/retail-data/by-day/2010-12-01.csv
> wget https://github.com/FVBros/Spark-The-Definitive-Guide/tree/master/data/retail-data

```scala
val staticDataFrame = spark.read.format("csv").option("header", "true").option("inferSchema", "true").load("/home/freepsw/Spark-The-Definitive-Guide/data/retail-data/by-day/*.csv")

staticDataFrame.createOrReplaceTempView("retail_data")
val staticSchema = staticDataFrame.schema

import org.apache.spark.sql.functions.{window, column, desc, col}
staticDataFrame.selectExpr(
    "CustomerId",
    "(UnitPrice * Quantity) as total_cost",
    "InvoiceDate").groupBy(
    col("CustomerId"), window(col("InvoiceDate"), "1 day")).sum("total_cost").show(5)

```




## [STEP 3] Install ELK Stack (Elasticsearch + Logstash + Kibana)
- Elasticsearch를 비즈니에서 활용시 주의사항 (OSS버전 vs Default)
    - OSS는 elasticsearch를 이용하여 별도의 제품/솔루션으로 판매할 목적인 경우에 활용
    - Basic은 기업 내부에서는 무료로 사용가능 
        - 즉 OSS 버전을 기반으로 elastic사에서 추가기능(ML, SIEM등)을 무료로 제공하는 것
    - 정리하면, OSS는 누구나 활용 가능한 오픈소스
        - 이를 이용해 별도의 제품을 만들어도 가능함.
        - elastic사도 OSS를 이용해서 basic 제품을 개발하고, 이를 무료로 제공함. 
        - 하지만, basic 버전의 소유권은 elastic사에 귀속됨(무로지만, 이를 이용해 비즈니스/사업을 하면 안됨)
    - http://kimjmin.net/2020/06/2020-06-elastic-devrel/

### Install an Elasticsearch 
- https://www.elastic.co/guide/en/elastic-stack/current/installing-elastic-stack.html 참고

```
> cd ~/demo-spark-analytics/sw

# download
> wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-7.10.1-linux-x86_64.tar.gz

> tar -xzf elasticsearch-7.10.1-linux-x86_64.tar.gz

# install plugin
> cd elasticsearch-7.10.1/
> bin/plugin install mobz/elasticsearch-head
```

- config 설정 
    - 외부 접속 허용(network.host) : server와 client가 다른 ip가 있을 경우, 외부에서 접속할 수 있도록 설정을 추가해야함.
    - master host 설정 (cluster.initial_master_nodes) : Master Node의 후보를 명시하여, Master Node 다운시 새로운 Master로 선출한다.
```
> cd ~/demo-spark-analytics/sw/elasticsearch-7.10.1
> vi config/elasticsearch.yml
# bind ip to connect from client  (lan이 여러개 있을 경우 외부에서 접속할 ip를 지정할 수 있음.)
# bind all ip server have "0.0.0.0"
network.host: 0.0.0.0   (":" 다음에 스페이스를 추가해야 함.)

cluster.initial_master_nodes: ["서버이름 또는 IP"]
```

- run elasticsearch 
```
> cd ~/demo-spark-analytics/sw/elasticsearch-7.10.1
> bin/elasticsearch

ERROR: [3] bootstrap checks failed
[1]: max file descriptors [4096] for elasticsearch process is too low, increase to at least [65535]
[2]: max virtual memory areas vm.max_map_count [65530] is too low, increase to at least [262144]
ERROR: Elasticsearch did not exit normally - check the logs at /home/freepsw/demo-spark-analytics/sw/elasticsearch-7.10.1/logs/elasticsearch.log
[2020-12-14T08:16:54,358][INFO ][o.e.n.Node               ] [freepsw-test] stopping ...
[2020-12-14T08:16:54,395][INFO ][o.e.n.Node               ] [freepsw-test] stopped
[2020-12-14T08:16:54,395][INFO ][o.e.n.Node               ] [freepsw-test] closing ...
[2020-12-14T08:16:54,431][INFO ][o.e.n.Node               ] [freepsw-test] closed
```
- Elasticsearch를 실행하기 위해서 필요한 OS 설정이 충족되지 못하여 발생하는 오류 (이를 해결하기 위한 설정 변경)
#### 오류1) File Descriptor 오류 해결
- file descriptor 갯수를 증가시켜야 한다.
- 에러 : [1]: max file descriptors [4096] for elasticsearch process is too low, increase to at least [65536]
- https://www.elastic.co/guide/en/elasticsearch/reference/current/setting-system-settings.html#limits.conf
```
> sudo vi /etc/security/limits.conf
# 아래 내용 추가 
* hard nofile 70000
* soft nofile 70000
root hard nofile 70000
root soft nofile 70000

# 적용을 위해 콘솔을 닫고 다시 연결한다.
# 적용되었는지 확인
> ulimit -a
core file size          (blocks, -c) 0
data seg size           (kbytes, -d) unlimited
scheduling priority             (-e) 0
file size               (blocks, -f) unlimited
pending signals                 (-i) 59450
max locked memory       (kbytes, -l) 64
max memory size         (kbytes, -m) unlimited
open files                      (-n) 70000  #--> 정상적으로 적용됨을 확인함
```

#### 오류2) virtual memory error 해결
- 시스템의 nmap count를 증가기켜야 한다.
- 에러 : [2]: max virtual memory areas vm.max_map_count [65530] is too low, increase to at least [262144]
- https://www.elastic.co/guide/en/elasticsearch/reference/current/vm-max-map-count.html
```
# 0) 현재 설정 값 확인
> cat /proc/sys/vm/max_map_count
65530

# 아래 3가지 방법 중 1가지를 선택하여 적용 가능
# 1-1) 현재 서버상태에서만 적용하는 방식
> sudo sysctl -w vm.max_map_count=262144

# 1-2) 영구적으로 적용 (서버 재부팅시 자동 적용)
> sudo vi /etc/sysctl.conf

# 아래 내용 추가
vm.max_map_count = 262144

# 1-3) 또는 아래 명령어 실행 
> echo vm.max_map_count=262144 | sudo tee -a /etc/sysctl.conf


# 3) 시스템에 적용하여 변경된 값을 확인
> sudo sysctl -p
vm.max_map_count = 262144
```

- rerun elasticsearch 
```
> cd ~/demo-spark-analytics/sw/elasticsearch-7.10.1
> bin/elasticsearch
......
[2020-12-14T10:18:18,803][INFO ][o.e.l.LicenseService     ] [freepsw-test] license [944a4695-3ec0-41f1-b3f8-5752b71c759e] mode [basic] - valid
[2020-12-14T10:18:18,806][INFO ][o.e.x.s.s.SecurityStatusChangeListener] [freepsw-test] Active license is now [BASIC]; Security is disabled
```

### Install a kibana 
```
> cd ~/demo-spark-analytics/sw
> curl -O https://artifacts.elastic.co/downloads/kibana/kibana-7.10.1-linux-x86_64.tar.gz
> tar -xzf kibana-7.10.1-linux-x86_64.tar.gz
> cd kibana-7.10.1-linux-x86_64/

# 외부 접속 가능하도록 설정 값 변경 
# 외부의 어떤 IP에서도 접속 가능하도록 0.0.0.0으로 변경 (운영환경에서는 특정 ip대역만 지정하여 보안강화)
> vi config/kibana.yml
server.host: "0.0.0.0"


> cd ~/demo-spark-analytics/sw/kibana-7.10.1-linux-x86_64/
> bin/kibana
.....
  log   [10:40:10.296] [info][server][Kibana][http] http server running at http://localhost:5601
  log   [10:40:12.690] [warning][plugins][reporting] Enabling the Chromium sandbox provides an additional layer of protection
```

### Install a logstash 
```
> cd ~/demo-spark-analytics/sw
> wget https://artifacts.elastic.co/downloads/logstash/logstash-7.10.1-linux-x86_64.tar.gz
> tar xvf logstash-7.10.1-linux-x86_64.tar.gz
> cd logstash-7.10.1
```
- Test a logstash 
```
> bin/logstash -e 'input { stdin { } } output { stdout {} }'
.........
The stdin plugin is now waiting for input:
[2020-12-14T10:51:08,038][INFO ][logstash.agent           ] Pipelines running {:count=>1, :running_pipelines=>[:main], :non_running_pipelines=>[]}
mytest  <-- 메세지 입력 후 아래와 같이 출력되면 정상적으로 설치된 것
{
       "message" => "mytest",
      "@version" => "1",
          "host" => "freepsw-test",
    "@timestamp" => 2020-12-14T10:51:12.408Z
}
```

### Run stage1
```
> ~/demo-spark-analytics/sw/logstash-7.10.1/bin/logstash -f logstash_stage1.conf
```
