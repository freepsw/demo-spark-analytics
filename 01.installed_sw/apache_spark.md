# Apache spark

## Why spark ?
>
- 유사한 straming sw(storm, flink, samza 등)들과 실시간 분산처리 성능은 유사하고,
- 또한 데이터의 유실을 방지하는 exactly once도 유사하게 지원한다.
- Apache spark의 장점은 실시간 처리와 함께 다양한 plugin(Graphx, SQL, MLlib)을 제공하여 원하는 제품을 쉽게 확장할 수 있는데 그 장점이 있다. (Apache Flink도 일부 유사함)
- 또한 수많은 commiter & contrubuter들의 참여 및 기업에서의 적용/투자를 통하여 제품의 안정성 및 성능이 지속적으로 검증 및 개선된다는 장점도 있다
- 이는 Open source sw를 도입하고자 하는 기업의 관점에서는 가장 중요한 항목중에 하나이다.
>

## Basic Concept
### Spark overview
- http://spark.apache.org/docs/latest/

### Deploy mode(cluster) overview
![spark cluster mode]
(http://spark.apache.org/docs/latest/img/cluster-overview.png) 

### Spark streaming overview
![spark streaming]
(http://spark.apache.org/docs/latest/img/streaming-arch.png) 

## Install guide (spark-2.0.1-bin-hadoop2.7)
> 
- master가 될 컴퓨터에 spark 2.0.0 버전을 다운받아서 압축을 푼다
 conf/spark-env.sh 에
- SPARK_MASTER_IP 에 master ip 기입
- conf/slaves 에 slave ip 혹은 호스트 명을 기입 - 한줄에 하나씩
- spark 폴더 전체를 다른 slave에 복사. 앞서 설명한 bash script를 사용하면 쉽다.
- master에서 .sbin/start-all.sh 실행
- http://master_ip:8080/ 에 접속하면 spark web ui를 볼 수 있다.
>

### 1. download and install
```
> ~/demo-spark-analytics/sw/
> wget http://d3kbcqa49mib13.cloudfront.net/spark-2.0.1-bin-hadoop2.7.tgz
> tar -xvf spark-2.0.1-bin-hadoop2.7.tgz
> cd spark-2.0.1-bin-hadoop2.7
```

### 2. run spark master
#### set spark configuration
- spark environment
```
> cd ~/demo-spark-analytics/sw/spark-2.0.1-bin-hadoop2.7/

# slave 설정
> cp conf/slaves.template conf/slaves
# localhost //현재  별도의 slave node가 없으므로 localhost를 slave node로 사용

# spark master 설정
# 현재 demo에서는 별도로 변경할 설정이 없다. (실제 적용시 다양한 설정값 적용)
> cp conf/spark-env.sh.template conf/spark-env.sh
```

#### run spark master
```
> sbin/start-all.sh
```

- Error (at mac os)
localhost: ssh: connect to host localhost port 22: Connection refused
- solution
 * System > Remote Login  : turn on "Remote Login"

#### open spark master web-ui with web browser
localhsot:8080

## spark guide
### - spark streaming guide : http://spark.apache.org/docs/latest/

### - spark-submit option
http://spark.apache.org/docs/latest/submitting-applications.html

   

## spark libraries     

### - elasticsearch for spark
https://www.elastic.co/guide/en/elasticsearch/hadoop/master/spark.html

### - redis scala client library
https://github.com/debasishg/scala-redis