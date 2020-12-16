# [Example] DataProc + Pub/Sub + Apache spark streaming
- https://cloud.google.com/solutions/using-apache-spark-dstreams-with-dataproc-and-pubsub?authuser=1

## [STEP 0] Gcloud 설정
```
> sudo tee -a /etc/yum.repos.d/google-cloud-sdk.repo << EOM
[google-cloud-sdk]
name=Google Cloud SDK
baseurl=https://packages.cloud.google.com/yum/repos/cloud-sdk-el7-x86_64
enabled=1
gpgcheck=1
repo_gpgcheck=1
gpgkey=https://packages.cloud.google.com/yum/doc/yum-key.gpg
       https://packages.cloud.google.com/yum/doc/rpm-package-key.gpg
EOM

> sudo yum install -y google-cloud-sdk

> gcloud --version
Google Cloud SDK 320.0.0
alpha 2020.12.04
beta 2020.12.04
bq 2.0.64
core 2020.12.04
gsutil 4.55
kubectl 1.17.14

> gcloud init
# 아래 항목에서 [2] Log in with a new account 선택 
.......
Choose the account you would like to use to perform operations for
this configuration:
 [1] 455258827586-compute@developer.gserviceaccount.com
 [2] Log in with a new account
Please enter your numeric choice: 2

Your credentials may be visible to others with access to this
virtual machine. Are you sure you want to authenticate with
your personal account?

# 아래에서 Y 입력
Do you want to continue (Y/n)?   Y

# 아래 출력된 링크로 웹 브라우저에서 접속
Go to the following link in your browser:

    https://accounts.google.com/o/oauth2/auth?response_type=code&client_id=32555940559.apps.googleusercontent.com&redirect_uri=urn%3Aietf%3Awg%3Aoauth%3A2.0%3Aoob&scope=openid+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.email+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fcloud-platform+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fappengine.admin+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fcompute+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Faccounts.reauth&state=vJ5TtWcbjBzCMKL3ffkhCaCptt2Fea&code_challenge=S5aY4D7CmMMCUGty_6nvxtprdzxEaY_hl_Jt_JLZzDY&prompt=consent&code_challenge_method=S256&access_type=offline

# 접속후 구글 계정을 선택하고, 화면에 표시되는 Code를 복사하여 아래에 붙여넣기 
Enter verification code: 4/1AY0e-g7_v-EyHSMwSTjIyPrAW6JdeW6n8tebv1EolWx0q_B9wiGzEEpYJlw

Enter verification code: 4/1AY0e-g7_v-EyHSMwSTjIyPrAW6JdeW6n8tebv1EolWx0q_B9wiGzEEpYJlw
You are logged in as: [frexxxxw@xxxx.com].

# GCP 프로젝트를 선택한다. 
Pick cloud project to use:
 [1] ds-ai-platform
 [2] Create a new project
Please enter numeric choice or text value (must exactly match list
item):  1

# 디폴트로 지정되는 리전을 지정한다. (옵션)
Do you want to configure a default Compute Region and Zone? (Y/n)? Y

# 출력되는 리전의 번호 중에서 원하는 리전을 선택한다. ([33] asia-northeast1-c 선택)
 [29] asia-southeast1-b
 [30] asia-southeast1-a
 [31] asia-southeast1-c
 [32] asia-northeast1-b
 [33] asia-northeast1-c
 [34] asia-northeast1-a
Please enter numeric choice or text value (must exactly match list
item): 33

# Default region/zone을 변경하려는 경우 (서울로 변경)
> gcloud config set compute/zone asia-northeast3-c 
> gcloud config get-value compute/zone
asia-northeast3-c

# 설치 완료 및 테스트
> gcloud config get-value project
ds-ai-platform
```

- gcloud로 다른 계정으로 로그인 하는 경우
```
> gcloud auth login
> gcloud config get-value project
my-old-project

> gcloud config set project my-new-project
> gcloud compute instances list
```

```
> gcloud services enable \
    dataproc.googleapis.com \
    pubsub.googleapis.com \
    cloudfunctions.googleapis.com \
    datastore.googleapis.com
Operation "operations/acf.653a6d8d-9829-4ef4-8d47-05b54f25decf" finished successfully.

```

## [STEP 1] Create DataStore
- DataStore를 따로 생성하지 않고, AppEngine API를 통해서 DataStore의 Entity를 생성한다. 
    - 참고 : https://cloud.google.com/appengine/docs/standard/java/datastore/creating-entities?hl=ko
- 죽 Java/Scala의 Entity 클래스를 생성하면, 이를 DataStore의 Entity의 항목으로 함께 저장하는 방식이다. 
- 여기서는 Spark Code의 DataStoreConverter 클래스를 보면 확인이 가능하다. 
    - 내부 흐름을 보면
    - convertToEntity --> convertToDatastore --> saveRDDtoDataStore로 저장됨. 
    - DataStoreConverter.scala
```scala
object DataStoreConverter {

  private def convertToDatastore(keyFactory: KeyFactory,
                                 record: Popularity): FullEntity[IncompleteKey] =
    FullEntity.newBuilder(keyFactory.newKey())
      .set("name", record.tag)
      .set("occurrences", record.amount)
      .build()

  // 수집된 데이터를 Entity로 변경하고, 
  // 이를 다시 DataStore로 변경하는 작업을 수행. 
  private[demo] def convertToEntity(hashtags: Array[Popularity],
                                    keyFactory: String => KeyFactory): FullEntity[IncompleteKey] = {
    val hashtagKeyFactory: KeyFactory = keyFactory("Hashtag")

    val listValue = hashtags.foldLeft[ListValue.Builder](ListValue.newBuilder())(
      (listValue, hashTag) => listValue.addValue(convertToDatastore(hashtagKeyFactory, hashTag))
    )

    val rowKeyFactory: KeyFactory = keyFactory("TrendingHashtags")

    FullEntity.newBuilder(rowKeyFactory.newKey())
      .set("datetime", Timestamp.now())
      .set("hashtags", listValue.build())
      .build()
  }
  ....생략 
}
```

```
# App Engine을 생성
> gcloud app create --region=asia-northeast3
```

## [STEP 2] Create Pub/Sub topic and subscription
```
> git clone https://github.com/GoogleCloudPlatform/dataproc-pubsub-spark-streaming
> cd dataproc-pubsub-spark-streaming
> export REPO_ROOT=$PWD

# Create pub/sub topic
> gcloud pubsub topics create tweets
> gcloud pubsub subscriptions create tweets-subscription --topic=tweets

```

## [STEP 3] Create DataProc 
### Cretea a service account and iam role
```
# Create service account 
> export SERVICE_ACCOUNT_NAME="dataproc-service-account"
> gcloud iam service-accounts create $SERVICE_ACCOUNT_NAME
Created service account [dataproc-service-account].


# Add an iam role to service account for dataproc
> export PROJECT=$(gcloud info --format='value(config.project)')
> gcloud projects add-iam-policy-binding $PROJECT \
    --role roles/dataproc.worker \
    --member="serviceAccount:$SERVICE_ACCOUNT_NAME@$PROJECT.iam.gserviceaccount.com"
Updated IAM policy for project [ds-ai-platform].
bindings:
- members:
  - serviceAccount:455258827586@cloudbuild.gserviceaccount.com
  role: roles/appengine.appAdmin
  ....
  role: roles/viewer
etag: BwW2kwpLE0k=
version: 1

# Add an iam role to service account for datastore
> gcloud projects add-iam-policy-binding $PROJECT \
    --role roles/datastore.user \
    --member="serviceAccount:$SERVICE_ACCOUNT_NAME@$PROJECT.iam.gserviceaccount.com"

# Add an iam role to service account for pub/sub
> gcloud beta pubsub subscriptions add-iam-policy-binding \
    tweets-subscription \
    --role roles/pubsub.subscriber \
    --member="serviceAccount:$SERVICE_ACCOUNT_NAME@$PROJECT.iam.gserviceaccount.com"
```    

### Cretea a dataproc cluster
```
# dataproc가 pub/sub에서 데이터를 읽어오고, datastore로 저장할 수 있는 권한을 부여한 dataproc cluster 생성
# 아래에서 별도로 지정하지 않았지만, default로 설정되는 값은
# worker node : 2개 (n1-standard-4 type)
# Disk : 100GB
# SSD : 기본은 지정되지 않으나, 아래 명령어로 할당 가능 (개수로 할당, 1개당 375G )
#  --num-master-local-ssds=1 \
#  --num-worker-local-ssds=1 \

> gcloud dataproc clusters create demo-cluster \
    --region=asia-northeast3 \
    --zone=asia-northeast3-c\
    --scopes=pubsub,datastore \
    --image-version=1.2 \
    --service-account="$SERVICE_ACCOUNT_NAME@$PROJECT.iam.gserviceaccount.com"
Waiting on operation [projects/ds-ai-platform/regions/asia-northeast3/operations/b64a99a7-e28c-37be-a8aa-37e47beb4e55].
Waiting for cluster creation operation...
WARNING: For PD-Standard without local SSDs, we strongly recommend provisioning 1TB or larger to ensure consistently high I/O performance. See https://cloud.google.com/compute/docs/disks/performance for information on disk I/O performance.
Waiting for cluster creation operation...done.
Created [https://dataproc.googleapis.com/v1/projects/ds-ai-platform/regions/asia-northeast3/clusters/demo-cluster] Cluster placed in zone [asia-northeast3-c].

```


## [STEP 4]  Run sample spark job
- Maven에서 Java 애플리케이션을 runnable jar 파일로 만드는 방법은 아래와 같이 대략 3가지 방법이 있다.
    - maven-jar-plugin : src/main/java, src/main/resources 만 포함한다.
    - maven-assembling-plugin: depdendencies jar 를 파일들을 함께 모듈화 한다.
    - maven-shade-plugin: depdendencies jar 를 파일을 함께 모듈화 하고 중복되는 클래스가 있을경우 relocate
- https://warpgate3.tistory.com/entry/Maven-Shade 참고

### Pom.xml 설정 관련 (maven-shade-plugin 활용)
- Jar 생성시 의존관계가 있는 모든 library를 추가하는 설정
    - 자바 어플리케이션의 모든 패키지와, 그에 의존관계에 있는 패키지 라이브러리까지 모두 하나의 'jar' 에 담겨져 있는 것
    - http://asuraiv.blogspot.com/2016/01/maven-shade-plugin-1-resource.html 참고
- 기본 설정 <Configuration>
    - 1. <execution>에서 package 페이지를 통해서 shade를 직접 실행 할 수 있도록 설정
        - 즉, mvn package를 실행하면, shade:shade를 실행하도록 하여,
        - 모든 의존관계가 있는 library를 포함하여 jar파일을 target/ 디렉토리 아래에 생성한다. 
    - 2. <transformers> 에서 ManifestResourcesTransformer를 이용하여 기본으로 실행할 class를 지정한다. 
        - 기존에는 Manifest 파일에서 실행 가능한 jar를 생성할 때 지정하는 옵션
        - Maniest.txt 파일에 "Main-Class: demo.TrendingHashtags"를 지정하는 것과 동일한 설정 
        - 즉, java -jar ~.jar 실행시 별도로 main class를 지정하지 않아도 내부적으로 Main-Class의 main을 실행함
    - 3. <relocations>
        - jar 파일내의 특정 패키지 구조를 변경한다. 
        - 여기서는 com 패키지를 repackaged.com으로 구조를 변경하고, 
        - com을 사용하는 모든 클래스들이 변경된 패키지를 사용하도록 변경한다.
            - 즉, 실행환경에서 동일한 라이브러리가 버전만 다르게 존재하는 경우, 
            - 내가 원하지 않는 버전의 라이브러리가 실행되는 경우가 발생(버전만 다를 뿐 패키지 명은 동일하기 때문에 오류 유발)
            - 이를 위해서 내가 사용하는 라이브러리의 패키지 명을 다른 이름으로 변경해서, 
            - 명확하게 필요한 라이브러리를 호출하도록 한다. 
        - https://javacan.tistory.com/entry/mavenshadeplugin 참고 

- pom.xml
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>2.4.3</version>
            <executions>
                <!-- 1. mvn package 설정 -->    
                <execution>
                    <phase>package</phase>
                    <goals>
                    <goal>shade</goal>
                    </goals>
                    <configuration>
                    <!-- 2. Jar 파일의 기본 실행 Class 지정 -->    
                        <transformers>
                            <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                            <mainClass>demo.TrendingHashtags</mainClass>
                            </transformer>
                        </transformers>

                    <!-- 3. Jar 파일의 패키지 구조를 변경한다. com => repackaged.com -->    
                        <relocations>
                            <relocation>
                            <pattern>com</pattern>
                            <shadedPattern>repackaged.com</shadedPattern>
                            <includes>
                                <include>com.google.protobuf.**</include>
                                <include>com.google.common.**</include>
                            </includes>
                            </relocation>
                        </relocations>
                    </configuration>
                </execution>
            </executions>
      </plugin>
    </plugins>
  </build>
```

### Compile and run spark job
```
# jdk 1.8이 사전에 설치되어 있어야 함. 
> sudo yum install -y git maven
> sudo update-java-alternatives -s java-1.8.0-openjdk-amd64 && export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/jre

> git clone https://github.com/GoogleCloudPlatform/dataproc-pubsub-spark-streaming
> cd dataproc-pubsub-spark-streaming/spark
> mvn clean package
> ls -alh  target
-rw-rw-r--. 1 freepsw freepsw 32K 12월 15 12:54 original-spark-streaming-pubsub-demo-1.0-SNAPSHOT.jar
-rw-rw-r--. 1 freepsw freepsw 17M 12월 15 12:55 spark-streaming-pubsub-demo-1.0-SNAPSHOT.jar

```





# Submit spark job to dataroc
```
> export PROJECT=$(gcloud info --format='value(config.project)')
> export JAR="spark-streaming-pubsub-demo-1.0-SNAPSHOT.jar"
> export SPARK_PROPERTIES="spark.dynamicAllocation.enabled=false,spark.streaming.receiver.writeAheadLog.enabled=true"
> export ARGUMENTS="$PROJECT 60 20 60 hdfs:///user/spark/checkpoint"

> gcloud dataproc jobs submit spark \
--cluster demo-cluster \
--region asia-northeast3  \
--async \
--jar target/$JAR \
--max-failures-per-hour 10 \
--properties $SPARK_PROPERTIES \
-- $ARGUMENTS

# 아래와 같이 정상적으로 작업이 할당됨. 
Job [446ca40670bf4c55be0e690710882a20] submitted.
jobUuid: 592f937e-2310-31f2-8d91-992196c6ba3e
placement:
  clusterName: demo-cluster
  clusterUuid: aa8b54c0-0b08-4a5d-adae-644d159a2f65
reference:
  jobId: 446ca40670bf4c55be0e690710882a20
  projectId: ds-ai-platform
scheduling:
  maxFailuresPerHour: 10
sparkJob:
  args:
  - ds-ai-platform
  - '60'
  - '20'
  - '60'
  - hdfs:///user/spark/checkpoint
  mainJarFileUri: gs://dataproc-staging-asia-northeast3-455258827586-owsdz48p/google-cloud-dataproc-metainfo/aa8b54c0-0b08-4a5d-adae-644d159a2f65/jobs/446ca40670bf4c55be0e690710882a20/staging/spark-streaming-pubsub-demo-1.0-SNAPSHOT.jar
  properties:
    spark.dynamicAllocation.enabled: 'false'
    spark.streaming.receiver.writeAheadLog.enabled: 'true'
status:
  state: PENDING
  stateStartTime: '2020-12-15T13:07:04.803Z'

# 위에서 생성한 job이 정상 동작함.
> gcloud dataproc jobs list --region=asia-northeast3 --state-filter=active
JOB_ID                            TYPE   STATUS
446ca40670bf4c55be0e690710882a20  spark  RUNNING

# 아래의 jobs에 JOB_ID를 입력하여 웹브라우저로 접속한다. 
https://console.cloud.google.com/dataproc/jobs/446ca40670bf4c55be0e690710882a20?region=asia-northeast3

# spark streaming이 pub/sub에서 데이터를 가져오려고 시도하지만, 데이터가 없다는 로그가 출력된다. 
20/12/15 13:17:37 WARN org.apache.spark.streaming.scheduler.ReceiverTracker: Error reported by receiver for stream 0: Failed to pull messages - java.lang.NullPointerException
at scala.collection.convert.Wrappers$JListWrapper.iterator(Wrappers.scala:88)
at scala.collection.IterableLike$class.foreach(IterableLike.scala:72)
at scala.collection.AbstractIterable.foreach(Iterable.scala:54)
at scala.collection.generic.Growable$class.$plus$plus$eq(Growable.scala:59)
at scala.collection.mutable.ListBuffer.$plus$plus$eq(ListBuffer.scala:183)
at scala.collection.mutable.ListBuffer.$plus$plus$eq(ListBuffer.scala:45)
at scala.collection.TraversableLike$class.to(TraversableLike.scala:590)
at scala.collection.AbstractTraversable.to(Traversable.scala:104)
at scala.collection.TraversableOnce$class.toList(TraversableOnce.scala:294)
at scala.collection.AbstractTraversable.toList(Traversable.scala:104)
at org.apache.spark.streaming.pubsub.PubsubReceiver.receive(PubsubInputDStream.scala:259)
at org.apache.spark.streaming.pubsub.PubsubReceiver$$anon$1.run(PubsubInputDStream.scala:247)
-------------------------
Window ending 2020-12-15T13:17:40.168000000Z for the past 60 seconds

No trending hashtags in this window.
```

#### (참고) run on intellij 
- Check point에 주석을 추가하고, sparkconf에 master 정보도 추가해야 로컬에서 실행이 가능함. 
```
val sparkConf = new SparkConf().setMaster("local[2]").setAppName("TrendingHashtags")
// Set the checkpoint directory
// val yarnTags = sparkConf.get("spark.yarn.tags")
// val jobId = yarnTags.split(",").filter(_.startsWith("dataproc_job")).head
// ssc.checkpoint(checkpointDirectory + '/' + jobId)
```
- 그리고 실행을 해도 아래와 같은 에러가 발생함. 
- 주요 원인은 GCP의 서비에 접근하기 위한 권한(서비스 계정)이 없어서 Pub/Sub에 연결하지 못하는 에러
- 그래서 처음에 dataproc cluster를 생성할 때, Pub/Sub에 접근할 수 있는 권한을 부여하는 부분을 추가함. 
- 결과적으로 intellij에서 테스트를 못해보고, 바로 dataproc에서 실행하면서 테스트를 해야함. 
    - 이 부분은 개발자에게 굉장히 부담이 되는 상황. (디버깅도 못해보고 매번 spark-submit을 한 후 log로 문제를 파악해야 하는데...)
    - 다른 방법이 있는데 내가 모르는 것일수 도 있으니, 나중에 다시 확인해 보는 걸로. 
```
20/12/15 21:38:44 WARN ReceiverTracker: Error reported by receiver for stream 0: Failed to pull messages - java.io.IOException: The Application Default Credentials are not available. They are available if running on Google App Engine, Google Compute Engine, or Google Cloud Shell. Otherwise, the environment variable GOOGLE_APPLICATION_CREDENTIALS must be defined pointing to a file defining the credentials. See https://developers.google.com/accounts/docs/application-default-credentials for more information.

```

#### [Error 1] ItelliJ 에서 Run 실행시 오류 및 해결
- Run TrendingHashtags 실행시 오류 메세지
```
Error: A JNI error has occurred, please check your installation and try again
Exception in thread "main" java.lang.NoClassDefFoundError: org/apache/spark/streaming/StreamingContext
....
Caused by: java.lang.ClassNotFoundException: org.apache.spark.streaming.StreamingContext
```

- 해결방안 
    - 참고 : - 참고 : https://stackoverflow.com/questions/36437814/how-to-work-efficiently-with-sbt-spark-and-provided-dependencies?rq=1
    - IntelliJ의 Edit Run Configuration >  'Include dependencies with "Provided" scope' 체크



## [STEP 5]  Send twitter message to pub/sub
### python에서 pubsub에 접근 할 수 있는 service account 인증키 생성 및 할당
```
# GCP IDENTITY & SECURITY > Access > Service Account > 생성하기 클릭
# service account 명 입력 > 서비스계정 권한(Project - Owner) > 생성
# 생성된 Service account 목록에서 "Create Key" 클릭 > Json 선택하여 파일을 로컬로 다운로드.
# 아래 명령어로 다운로드 받은 파일을 VM Instance로 전달 
> scp -i ~/.ssh/my-key ds-ai-platform-16ec2a0de4b3.json freepsw@34.xx.xx.xx:~/
> export GOOGLE_APPLICATION_CREDENTIALS="/home/freepsw/ds-ai-platform-16ec2a0de4b3.json"
```

### python code 실행
```
> cd tweet-generator
> sudo pip install virtualenv

> virtualenv venv
> source venv/bin/activate
> pip install -r requirements.txt
> export PROJECT=$(gcloud info --format='value(config.project)')
> python tweet-generator.py $PROJECT 15 1000 &
```


## [STEP 6]  최종 처리 결과 확인
### DataProc 로그 확인 
- 아래의 jobs에 JOB_ID를 입력하여 웹브라우저로 접속한다. 
https://console.cloud.google.com/dataproc/jobs/446ca40670bf4c55be0e690710882a20?region=asia-northeast3
- 로그에서 정상적으로 twee
```
[Stage 386:====================================>                (271 + 1) / 398]
[Stage 386:=========================================>           (310 + 1) / 398]
[Stage 386:==============================================>      (351 + 1) / 398]
                                                                                

[Stage 389:================================================>    (366 + 1) / 398]
                                                                                
-------------------------
Window ending 2020-12-15T13:47:02.553000000Z for the past 60 seconds

Trending hashtags in this window:
particularly, 8
picture, 7
author, 6
condition, 6
decision, 6
husband, 6
knowledge, 6
network, 6
officer, 6
option, 6
```


### DataStore 로그 확인
- DATABASES > DataStore > Enitties 크릭
- Kind의 "TrendingHashtags"에 데이터가 수집되고 있음. 
```
Key
TrendingHashtags id:4788953613860864
Key literal
Key(TrendingHashtags, 4788953613860864)
URL-safe key
ahB2fmRzLWFpLXBsYXRmb3Jtch0LEhBUcmVuZGluZ0hhc2h0YWdzGICAgLjy8MAIDA
datetime
2020-12-15 (22:48:02.360) JST
hashtags
[{"name":"without","occurrences":"9"},{"name":"career","occurrences":"7"},{"name":"figure","occurrences":"7"},{"occurrences":"7","name":"information"},{"name":"traditional","occurrences":"7"},{"name":"brother","occurrences":"6"},{"occurrences":"6","name":"father"},{"occurrences":"6","name":"finish"},{"occurrences":"6","name":"hospital"},{"occurrences":"6","name":"knowledge"}]
```

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

