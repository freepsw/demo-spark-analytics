# Stage 4-1. Stage4 + GCP PubSub
- Apache Kafka를 pubsub으로 대체하는 실습

## Technical Changes (using gcp cloud service)
#### Stage4의 기본 내용은 유지
- Logstash --> Pubsub 전달 코드 추가
- Apache Spark Code에서 Kafka 대신 Pubsub에서 데이터 수신하도록 수정 

## Stage4-1의 주요 내용 (스타트업이 비즈니스에 집중할 수 있도록 Cloud Service를 활용해 보자)
![stage4-1 architecture](https://github.com/freepsw/demo-spark-analytics/blob/master/resources/images/stage4-2.png)

## [STEP 0] Stage4의 내용은 그대로 유지



## [STEP 1] Install ELK Stack (Elasticsearch + Logstash + Kibana)

## [STEP 2] Run redis 

## [STEP 3] Gcloud 설정

## [STEP 4] Create DataProc 
gcloud dataproc clusters create demo-cluster \
    --region=asia-northeast3 \
    --zone=asia-northeast3-c\
    --scopes=pubsub,datastore \
    --image-version=1.2 \
    --service-account="$SERVICE_ACCOUNT_NAME@$PROJECT.iam.gserviceaccount.com"


## [STEP 5] Create Pub/Sub topic and subscription
- pubsub 서비스 api를 시용할 수 있도록 서비스를 활성화 한다. 
```
> gcloud services enable pubsub.googleapis.com
```

- topic & subscription 생성
```
> gcloud pubsub topics create realtime
Created topic [projects/omega-byte-286705/topics/realtime].

> gcloud pubsub subscriptions create realtime-subscription --topic=realtime
Created subscription [projects/omega-byte-286705/subscriptions/realtime-subscription].
```

- Add an iam role to service account for pub/sub
```
> export PROJECT=$(gcloud info --format='value(config.project)')
> export SERVICE_ACCOUNT_NAME="dataproc-service-account"

# topic에 메세지를 전달 할 수 있는 권한 부여
> gcloud beta pubsub topics add-iam-policy-binding \
  realtime \
  --role roles/pubsub.publisher \
  --member="serviceAccount:$SERVICE_ACCOUNT_NAME@$PROJECT.iam.gserviceaccount.com"

# topic의 메세지를 구독(subscription) 할 수 있는 권한 부여 
> gcloud beta pubsub subscriptions add-iam-policy-binding \
    realtime-subscription \
    --role roles/pubsub.subscriber \
    --member="serviceAccount:$SERVICE_ACCOUNT_NAME@$PROJECT.iam.gserviceaccount.com"


```



## [STEP 6]  Compile and run sample spark job
### 6.1 Compile spark job 
#### Spark Job 코드 이해
- GCP DataProc(spark cluseter)에서 실행시킬 job을 코딩하여 컴파일한다. 
- spark cluster에서 실행 가능한 jar파일로 생성한다.
- Stage4StreamingDataprocPubsub.scala 파일의 주요 내용
```java
object Stage4StreamingDataprocPubsub {
  def main(args: Array[String]) {
    if (args.length != 1) {
      System.err.println(
        """
          | Usage: Stage4StreamingDataprocPubsub <projectID>
          |
          |     <projectID>: ID of Google Cloud project
          |
        """.stripMargin)
      System.exit(1)
    }
    val Seq(projectID) = args.toSeq

    val host_server = "서버의 외부 IP" // apache kafka, elasticsearch, redis가 설치된 서버의 IP 
    val kafka_broker = host_server+":9092"
    //[STEP 1] create spark streaming session
    // Create the context with a 1 second batch size
    // 1) Local Node에서만 실행 하는 경우 "local[2]"를 지정하거나, spark master url을 입
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("Stage41_Streaming")

    // 2) DataProc를 사용하는 경우 setMaster를 지정하지 않음.
    //val sparkConf = new SparkConf().setAppName("Stage41_Streaming")
    sparkConf.set("es.index.auto.create", "true");
    sparkConf.set("es.nodes", host_server)
    sparkConf.set("es.port", "9200")
    // 외부에서 ES에 접속할 경우 아래 설정을 추가 (localhost에서 접속시에는 불필요)
    sparkConf.set("spark.es.nodes.wan.only","true")

    val ssc = new StreamingContext(sparkConf, Seconds(2))
    addStreamListener(ssc)

    // [STEP 1]. Create PubSub Receiver and receive message from kafka broker
    val messagesStream: DStream[String] = PubsubUtils
      .createStream(
        ssc,
        projectID,
        None,
        "realtime-subscription",  // Cloud Pub/Sub subscription for incoming tweets
        SparkGCPCredentials.builder.build(), StorageLevel.MEMORY_AND_DISK_SER_2)
      .map(message => new String(message.getData(), StandardCharsets.UTF_8))

    // [STEP 2]. parser message and join customer info from redis
    // original msg = ["event_id","customer_id","track_id","datetime","ismobile","listening_zip_code"]
    val columnList  = List("@timestamp", "customer_id","track_id","ismobile","listening_zip_code", "name", "age", "gender", "zip", "Address", "SignDate", "Status", "Level", "Campaign", "LinkedWithApps")
//    val lines = messages.map(_.value)
    val lines = messagesStream
    println(lines.toString)

    val wordList    = lines.mapPartitions(iter => {
      val r = new RedisClient(host_server, 6379)
      iter.toList.map(s => {
        val listMap = new mutable.LinkedHashMap[String, Any]()
        val split   = s.split(",")
        //println(s)
        //println(split(0))

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
        listMap.toString()
        listMap
      }).iterator
    })

    //[STEP 4]. Write to ElasticSearch
    wordList.foreachRDD(rdd => {
      rdd.foreach(s => s.foreach(x => println(x.toString)))
      EsSpark.saveToEs(rdd, "ba_realtime41/stage41")
    })

    ssc.start()
    ssc.awaitTermination()
  }
}
```

#### PubSub 실행을 위한 Spark code 수정 ( IP 변경)
```
> cd ~/demo-spark-analytics/00.stage4-1/demo-streaming-cloud/
> vi src/main/scala/io/skiper/driver/Stage4StreamingDataprocPubsub.scala
# 아래 IP를 본인의 apache kafka/redis/elasticsearch가 설치된 IP로 변경한다. 
    val host_server = "IP입력"
```
#### Main Class 설정 변경 
- 이번에 실행할 Spark job의 main class 명을 지정해 준다. (io.skiper.driver.Stage4StreamingDataprocPubSub)
- pom.xml
```xml
              <transformers>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                  <mainClass>io.skiper.driver.Stage4StreamingDataprocPubsub</mainClass>
                </transformer>
              </transformers>
```

#### compile spark job
```
> cd ~/demo-spark-analytics/00.stage4-1/demo-streaming-cloud/
> mvn clean package
> ls -alh  target
# demo-streaming-cloud-1.0-SNAPSHOT.jar파일이 original 대비 크기가 증가한 것을 볼 수 있다.
-rw-rw-r--. 1 freepsw.09 freepsw.09  61K 12월 20 09:51 original-demo-streaming-cloud-1.0-SNAPSHOT.jar
-rw-rw-r--. 1 freepsw.09 freepsw.09 111M 12월 20 09:52 demo-streaming-cloud-1.0-SNAPSHOT.jar

```

### 6.2 Submit spark job to dataroc
```
> cd ~/demo-spark-analytics/00.stage4-1/demo-streaming-cloud/
> export PROJECT=$(gcloud info --format='value(config.project)')
> export JAR="demo-streaming-cloud-1.0-SNAPSHOT.jar"
> export SPARK_PROPERTIES="spark.dynamicAllocation.enabled=false,spark.streaming.receiver.writeAheadLog.enabled=true"

# DataProc에서 Pubsub에 접근하기 위해서는 project id가 필요함. 
# ARGEUMETNS로 project id를 전달한다.
> export ARGUMENTS="$PROJECT"

> gcloud dataproc jobs submit spark \
--cluster demo-cluster \
--region asia-northeast3  \
--async \
--jar target/$JAR \
--max-failures-per-hour 10 \
--properties $SPARK_PROPERTIES \
-- $ARGUMENTS

- 위에서 생성한 job이 정상 동작하는지 확인 
```
> gcloud dataproc jobs list --region=asia-northeast3 --state-filter=active
JOB_ID                            TYPE   STATUS
446ca40670bf4c55be0e690710882a20  spark  RUNNING
```
-  아래의 jobs에 JOB_ID를 입력하여 웹브라우저로 접속하여, 실행한 job이 정상 실행 중인지 확인한다. 
    - https://console.cloud.google.com/dataproc/jobs/223a633aeb514c91818534ad89adc39d?region=asia-northeast3
```



## [STEP 7] Collect the log data using logstash and send to the gcp pubsub
### 7.1 Download the credential key file(json) of gcp service account
- Google Cloud Console > IAM 및 관리자 > 서비스 계정
- "dataproc-service-account@프로젝트-id-286705.iam.gserviceaccount.com" > 작업 선택
- "키 만들기" 클릭 
- 자동으로 로컬 PC/Notebook의 "Download" 디렉토리에 "project-id-xxxx.json"파일이 생성됨.

### 7.2 Sent a downloaded credential key file to the vm instance
- VM Instance > 연결(ssh) > "브라우저에서 창열기" 클릭
- 오픈된 브라우저 창에서  우측 상단 설정(톱니바퀴 모양 아이콘) 클릭
- "파일 업로드" 클릭 > 다운로드 받은 credential file(json) 선택 
- 업로드 완료 메세지 확인 후, 아래 명령어로 업로드된 파일 확인 가능
```
> ls
projectid-2xxxx-9ada3ee4266b.json
```

### 7.3 Run logstash 
- PubSub으로 데이터를 전송하기 위한 logstash 설정
- 위에서 생성한 pubsub의 topic(realtime)으로 전송
- 수정항목
  - input file path : rts --> 본인의 계정명으로 변경
  - google_pubsub > project_id : 본인의 gcp project id로 변경
  - google_pubsub > json_key_file : 다운받은 credential key로 변경
```
> cd ~/demo-spark-analytics/00.stage4-2
> vi logstash_stage4-2.conf
```
```yaml
input {
  file {
    path => "/home/rts/demo-spark-analytics/00.stage1/tracks_live.csv"
  }
}

output {
  stdout {
    codec => rubydebug{ }
  }

  google_pubsub {
    # Required attributes (본인의 gcp project id를 입력)
    project_id => "my_project" 
    topic => "realtime"

    # Optional if you're using app default credentials
    json_key_file => "/home/rts/projectid-2xxxx-9ada3ee4266b.json"
  }
}

```

- install gcp pubsub output plugin
```
> cd ~/demo-spark-analytics/sw/logstash-7.10.1/
> bin/logstash-plugin install logstash-output-google_pubsub
```


- run logstash
```
> cd ~/demo-spark-analytics/00.stage4-2
> ~/demo-spark-analytics/sw/logstash-7.10.1/bin/logstash -f logstash_stage4-2.conf
```

### Generate steaming data using data-generator.py
```
> cd ~/demo-spark-analytics/00.stage1
> python data_generator.py
```

## [STEP 6]  최종 처리 결과 확인
### DataProc 로그 확인 
- 아래의 jobs에 JOB_ID를 입력하여 웹브라우저로 접속한다. 
https://console.cloud.google.com/dataproc/jobs/446ca40670bf4c55be0e690710882a20?region=asia-northeast3
- 로그에서 정상적으로 출력되는 것을 확인
```
 map = Map(@timestamp -> 2020-12-16 14:25:18.027, customer_id -> 392, track_id -> 29, ismobile -> 0, listening_zip_code -> 74428, name -> Melissa Thornton, age -> 23, gender -> 1, zip -> 85646, Address -> 79994 Hazy Goat Flats, SignDate -> 02/25/2013, Status -> 0, Level -> 1, Campaign -> 3, LinkedWithApps -> 0)
(@timestamp,2020-12-16 14:25:18.027)
(customer_id,392)
(track_id,29)
(ismobile,0)
(listening_zip_code,74428)
(name,Melissa Thornton)
(age,23)
(gender,1)
(zip,85646)
(Address,79994 Hazy Goat Flats)
(SignDate,02/25/2013)
(Status,0)
(Level,1)
(Campaign,3)
(LinkedWithApps,0)
```
- 여기서 master를 local[*]로 지정하면, 로그가 정상적으로 출력됨 
    - 왜냐하면, worker가 driver에서 실행되므로 driver의 로그를 바로 화면에 출력
- 만약 master를 지정하지 않았다면, 위와 같은 로그가 출력되지 않음.
    - 왜냐하면, woker가 다른 노드에서 실행되므로 driver에서 로그를 출력할 수 없음.
    - 따라서 디버깅 용도로 실행하려면 new SparkConf().setMaster("local[2]")로 지정해서 실행해야 함.


### Hadoop Cluster Web UI 정보 확인 
- DataProc은 오픈소스 Hadoop/Spark를 쉽게 사용하도록 지원하는 서비스이다. 
- 따라서 오픈소스 hadoop에서 제공하는 web ui에도 접근이 가능한다. 
- 브라우저에서 웹으로 접속하려면 IP/Port를 알아야 한다. 
    - IP 확인 : COMPUTE > Compute Engine > VM Instances 접속
        - cluster명(여기서는 demo-cluster-m)을 확인하고, 외부 IP를 확인 
    - PORT 확인
        - 8088은 Hadoop을 위한 포트
        - 9870은 HDFS를 위한 포트
        - 19888은 Hadoop 데이터 노드의 Jobhistory 정보
- 원하는 정보를 보기 위해서 브라우저에 IP:PORT를 입력하여 접속한다. 
- https://jeongchul.tistory.com/589 참고

## [STEP 7]  GCP 자원 해제
```
export SERVICE_ACCOUNT_NAME="dataproc-service-account"
gcloud dataproc jobs kill 446ca40670bf4c55be0e690710882a20 --region=asia-northeast3 --quiet
gcloud dataproc clusters delete demo-cluster --quiet --region=asia-northeast3
gcloud pubsub topics delete tweets --quiet
gcloud pubsub subscriptions delete tweets-subscription --quiet 
gcloud iam service-accounts delete $SERVICE_ACCOUNT_NAME@$PROJECT.iam.gserviceaccount.com --quiet --region=asia-northeast3
```


## [ETC]
### DataProc의 동적 확장
```
> gcloud dataproc clusters update example-cluster --num-workers 4
```