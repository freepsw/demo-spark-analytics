# Stage 4. Use cloud service for processing real time big data like dataproc, pubsub at Stage3
- Cloud에서 제공하는 실시간 대용량 빅데이터 처리 기술을 활용하여 서비스를 안정적으로 제공
- 전체 서비스 중에서 대량의 데이터를 처리하는 영역인 Apache Kafka와 Apache Spark 영역을 GCP 서비스로 대체

## Stage4의 주요 내용 (스타트업이 비즈니스에 집중할 수 있도록 Cloud Service를 활용해 보자)
### 현재 스타트업의 고민(문제)
- 자체적으로 구축한 빅데이터 오픈소스를 안정적으로 운영하기 위해서는 많은 인프라비용과 전문인력이 필요하다. 
- 하지만, 초기 스타트업은 이러한 비용을 초기에 투자하기 어려운 경우가 많다. 
    - 만약 특별한 상황으로 사용자가 급격하게 줄면? (
        - 미리 구해한 하드웨어 비용이 낭비되고, 전문 인력에 대한 비용도 꾸준히 소비됨
    - 만약 예상한 규모 이상으로 서비스/사용자가 급격하게 늘어나면? 
        - 적시에 하드웨어를 다시 구매하지 못하면, 증가하는 사용자를 처리하지 못하여 서비스 장애 또는 서비스 접속오류 발생
        - 늘어난 오픈소스 소프트웨어의 운영 복잡성으로 서비스 안정화에 더 많은 인력 필요. 
- 초기 스타트업은 핵심 비즈니스에 집중해야 한다.. 

### 해결방안 
- Cloud Service를 활용하여 하드웨어 동적 할당 및 복잡한 오픈소스 운영 비용 감소


## Technical Changes (using gcp cloud service)
#### Managed Service인 PubSub, DataProc를 활용하여 빅데이터 시스템 운영 최소화
- Apache Kafka를 대체하여 메세지 큐 서비스인 PubSub을 활용
- Apache Spark를 대체하여 실시간 대용량 처리를 위한 DataProc 활용
- Cloud의 사용량 기반 서비스 활용
    - 사용량에 따라 동적으로 클러스터를 할당하여 사용한 만큼만 비용 사용
    - PubSub, DataProc 모두 사용자가 클러스터 확장에 대한 고민없이, 필요한 만큼 자동으로 인프라를 할당
#### ELK Stack version 업그레이드 (to V7.10.1)
- 최신 버전(7.10.1)으로 오픈소스를 업그레이드 하여, 성능 및 보안성 등이 추가된 기능을 활용한다. 

### - Software 구성도
![stage4 architecture](https://github.com/freepsw/demo-spark-analytics/blob/master/resources/images/stage4-1.png)


## [STEP 0] 1단계 Apache Spark를 DataProc로 대체 & ELK 업그레이드 준비
- 한번에 Cloud로 전환하는 것보다, 우선적으로 필요한 것을 먼저 cloud로 전환하여 안정성을 검증하고
- 이후 필요한 서비스를 cloud로 전환한다. 
- Apache Spark는 시간이 지날수록 많은 운영비용(인력, 인프라)이 추가되므로, 1단계 전환 대상으로 선정한다.
- 그리고, 기존 ELK stack의 버전(2.4.0)을 최신 버전으로 업그레이드

### 초기 설정
- Stage1에서 이미 했다면 다음 명령어는 생략 가능
```
> sudo yum install -y java

# console에 JAVA_HOME 설정
> export JAVA_HOME=$(alternatives --display java | grep current | sed 's/link currently points to //' | sed 's|/bin/java||')
> echo $JAVA_HOME
/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.275.b01-0.el7_9.x86_64/jr

# user shell에 JAVA_HOME 설정
> vi ~/.bash_profile
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.275.b01-0.el7_9.x86_64/jr

> source ~/.bash_profile


# Download git project 
> cd ~
> sudo yum install -y wget git
> git clone https://github.com/freepsw/demo-spark-analytics.git
> cd demo-spark-analytics
> mkdir sw
```


## [STEP 1] Install ELK Stack (Elasticsearch + Logstash + Kibana)
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
> wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-7.10.1-linux-x86_64.tar.gz
> tar -xzf elasticsearch-7.10.1-linux-x86_64.tar.gz
```
- config 설정 
    - 외부 접속 허용(network.host) : server와 client가 다른 ip가 있을 경우, 외부에서 접속할 수 있도록 설정을 추가해야함.
    - master host 설정 (cluster.initial_master_nodes) : Master Node의 후보를 명시하여, Master Node 다운시 새로운 Master로 선출한다.
        - 
```
> cd ~/demo-spark-analytics/sw/elasticsearch-7.10.1
> vi config/elasticsearch.yml
# bind ip to connect from client  (lan이 여러개 있을 경우 외부에서 접속할 ip를 지정할 수 있음.)
# bind all ip server have "0.0.0.0"

network.host: 0.0.0.0   #(":" 다음에 스페이스를 추가해야 함.)

# Master Node의 후보 서버 목록을 적어준다. (여기서는 1대 이므로 본인의 IP만)
# ip를 입력하면 
cluster.initial_master_nodes: ["서버이름"]
```

#### Error 발생 (cluster.initial_master_nodes에 IP를 입력한 경우)
- 에러 로그 유형
    - skipping cluster bootstrapping as local node does not match bootstrap requirements: [34.64.85.55]
    - master not discovered yet, this node has not previously joined a bootstrapped (v7+) cluster, and [cluster.initial_master_nodes] is empty on this node
- 해결
    - cluster.initial_master_nodes: ["node name"] 입력 

#### run elasticsearch 
```
> cd ~/demo-spark-analytics/sw/elasticsearch-7.10.1
> bin/elasticsearch

# 아래와 같은 에러가 발생함. 
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

# 적용을 위해 콘솔을 닫고 다시 연결한다. (console 재접속)
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

#### Elasticsearch UI로 접속하기 
- 1) 웹브라우저에서 접속 확인 
    - http://VM외부IP:9200
- 2) Elasticsearch용 시각화 plugin(elasticsearch head) 설치 (구글 크롬 브라우저)
    - https://chrome.google.com/webstore/detail/elasticsearch-head/ffmkiejjmecolpfloofpjologoblkegm
    - "Chrome에 추가" 클릭
    - 추가된 Plugin 클릭하여 접속 > "Elasticsearch 설치된 IP입력" > Connect 버튼 클릭


### Install and run a kibana 
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

#### Kibana 에러 시 기존 index 삭제 후 재시작
```
curl -XDELETE http://localhost:9200/.kibana
curl -XDELETE 'http://localhost:9200/.kibana*'
curl -XDELETE http://localhost:9200/.kibana_2
curl -XDELETE http://localhost:9200/.kibana_1
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
# 실행까지 시간이 소요된다. (아래 메세지가 출력되면 정상 실행된 것으로 확인)
.........
The stdin plugin is now waiting for input:
[2020-12-20T08:20:58,728][INFO ][logstash.agent           ] Pipelines running {:count=>1, :running_pipelines=>[:main], :non_running_pipelines=>[]}
[2020-12-20T08:20:59,146][INFO ][logstash.agent           ] Successfully started Logstash API endpoint {:port=>9600}
mytest  <-- 메세지 입력 후 아래와 같이 출력되면 정상적으로 설치된 것
{
       "message" => "mytest",
      "@version" => "1",
          "host" => "freepsw-test",
    "@timestamp" => 2020-12-14T10:51:12.408Z
}
```

## [STEP 2] Run apache kafka cluster and redis 
### 2.1 Download apache kafka 
```
> cd ~/demo-spark-analytics/sw
> wget http://apache.mirror.cdnetworks.com/kafka/2.4.1/kafka_2.11-2.4.1.tgz
> tar xvf kafka_2.11-2.4.1.tgz
> cd ~/demo-spark-analytics/sw/kafka_2.11-2.4.1
```
- edit kafka config (server.config)
    - 외부에서 apache kafka 접속할 수 있도록 설정
    - 아래 "서버IP"를 kafka가 실행중인 서버 IP로 변경한다.
    - Host name으로 설정하려는 경우, 외부에서 접속 가능한 host명이어야 한다. (DNS에 등록된 hostname)
    - 즉, 외부에서 kafka에 접속 할 수 있는 정보를 입력해야 함.
```
> cd ~/demo-spark-analytics/sw/kafka_2.11-2.4.1
> vi config/server.properties
advertised.listeners=PLAINTEXT://서버IP:9092 
```

#### run zookeeper
```
> cd ~/demo-spark-analytics/sw/kafka_2.11-2.4.1
> bin/zookeeper-server-start.sh config/zookeeper.properties
```

#### run kafka
```
> cd ~/demo-spark-analytics/sw/kafka_2.11-2.4.1
> bin/kafka-server-start.sh config/server.properties
```

#### create a topic (realtime)
- 실습에 사용할 topic을 생성한다. 
```
> cd ~/demo-spark-analytics/sw/kafka_2.11-2.4.1
> bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic realtime4
# check created topic "realtime4"
> bin/kafka-topics.sh --list --zookeeper localhost:2181
realtime4
```



### 2.2 Run redis 
#### download redis and compile
```
> cd ~/demo-spark-analytics/sw
> wget http://download.redis.io/releases/redis-3.0.7.tar.gz
> tar -xzf redis-3.0.7.tar.gz
> cd redis-3.0.7
> sudo yum -y install gcc-c++
> make
```

- (아래 명령어는 오류 발생시 실행) "zmalloc.h:51:31: error: jemalloc/jemalloc.h: No such file or directory"에러 발생시
```
> make distclean
> make
```

#### run redis server
```
> cd ~/demo-spark-analytics/sw/redis-3.0.7
> src/redis-server
```

### 2.3 Run import_customer_info.py (read customer info and insert into redis)
- Stage2에서 이미 진행한 내용 (Stage4가 처음인 경우에만 실행)
- 고객의 상세정보를 redis에 입력하는 명령어
```
> sudo yum install -y python-setuptools
> sudo easy_install pip
> sudo pip install redis
> sudo yum install -y numpy

> cd ~/demo-spark-analytics/00.stage2
> python import_customer_info.py
```

- redis에 정상적으로 저장되었는지 확인
```
> cd ~/demo-spark-analytics/sw/redis-3.0.7
> src/redis-cli
127.0.0.1:6379> hgetall 2 #사용자 id 2번에 대한 정보를 조회
 1) "gender"
 2) "0"
 3) "name"
 4) "Paula Peltier"
 5) "age"
 6) "30"
 7) "zip"
 8) "66216"
 9) "Address"
10) "10084 Easy Gate Bend"
11) "Status"
12) "1"
13) "SignDate"
14) "01/13/2013"
15) "Campaign"
16) "4"
17) "Level"
18) "0"
19) "LinkedWithApps"
20) "1"
```


### 2.4 Run predict_ml_libsvm.py to classify the customer using ml model
#### install apache spark 
```
> cd ~/demo-spark-analytics/sw/
> wget http://d3kbcqa49mib13.cloudfront.net/spark-2.0.1-bin-hadoop2.7.tgz
> tar -xvf spark-2.0.1-bin-hadoop2.7.tgz
> cd spark-2.0.1-bin-hadoop2.7

# slave 설정
> cp conf/slaves.template conf/slaves
localhost //현재  별도의 slave node가 없으므로 localhost를 slave node로 사용

# spark master 설정
# 현재 demo에서는 별도로 변경할 설정이 없다. (실제 적용시 다양한 설정 값 적용)
> cp conf/spark-env.sh.template conf/spark-env.sh
```

#### run predict_ml_libsvm.py
```
# PySpark 실행에 필요한 환경설정 
> vi ~/.bash_profile
# 아래 내용을 추가
export SPARK_HOME=~/demo-spark-analytics/sw/spark-2.0.1-bin-hadoop2.7
export PYTHONPATH=$SPARK_HOME/python/:$SPARK_HOME/python/lib/py4j-0.10.3-src.zip:$PYTHONPATH
export PATH=$PATH:$SPARK_HOME/bin
> source ~/.bash_profile

> cd ~/demo-spark-analytics/00.stage3
> python predict_ml_libsvm.py
# 아래 메세지가 보이면 정상
all: 5000 training size: 3484, test size 1516
LBFGS error: 0.0105540897098
```
- 5000건 데이터 중에 3,484 건은 학습데이터로 이용하고, 1,516 건은 검증용으로 활용
- 1,516건을 학습된 모델로 검증한 결과, 에러율리 0.01(정확도 99%)로 나타남.

#### check the binary classification model result 
```
> cd ~/demo-spark-analytics/sw/redis-3.0.7
> src/redis-cli
127.0.0.1:6379> get pred_event:2 #사용자 id 2번은 광고 대상이 아님으로 분류
"0"
```

## [STEP 3] Gcloud 설정
- gcp의 cloud 서비스를 명령어로 생성/실행 할 수 있는 gcloud라는 도구를 설치하여
- gcp 계정과 연결한다. 

### gcloud 설치 
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
Google Cloud SDK 321.0.0
alpha 2020.12.11
beta 2020.12.11
bq 2.0.64
core 2020.12.11
gsutil 4.57

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
You are logged in as: [frexxxxw@xxxx.com].

# GCP 프로젝트를 선택한다. 
Pick cloud project to use:
 [1] omega-byte-286705
 [2] Create a new project
Please enter numeric choice or text value (must exactly match list
item):  1

# 디폴트로 지정되는 리전을 지정한다. (옵션)
Do you want to configure a default Compute Region and Zone? (Y/n)? Y

# 출력되는 리전의 번호 중에서 원하는 리전을 선택한다. (서울로 선택 52번)
# https://cloud.google.com/compute/docs/regions-zones 참고
 [44] asia-east2-a
 [45] asia-east2-b
 [46] asia-east2-c
 [47] asia-northeast2-a
 [48] asia-northeast2-b
 [49] asia-northeast2-c
 [50] asia-northeast3-a
 [51] asia-northeast3-b
 [52] asia-northeast3-c
Please enter numeric choice or text value (must exactly match list
item): 52

# Default region/zone을 변경하려는 경우 (서울로 변경)
> gcloud config set compute/zone asia-northeast3-c 
> gcloud config get-value compute/zone
asia-northeast3-c

# 설치 완료 및 테스트
> gcloud config get-value project
omega-byte-286705
```

- (참고)  gcloud로 다른 계정으로 로그인 하는 경우
```
> gcloud auth login
> gcloud config get-value project
my-old-project

> gcloud config set project my-new-project
>  gcloud compute instances list
NAME      ZONE               MACHINE_TYPE   PREEMPTIBLE  INTERNAL_IP  EXTERNAL_IP   STATUS
mytest1   asia-northeast3-a  e2-standard-2               10.178.0.3   34.64.85.55   RUNNING
```

### gcloud로 사용할 gcp service 활성화 
- GCP의 다양한 서비스를 활용하기 위해서는 해당 서비스를 활성화(enable) 해야한다. 
    - 실습에 필요한 dataproc 서비스 활성화
```
> gcloud services enable \
    dataproc.googleapis.com
Operation "operations/acf.653a6d8d-9829-4ef4-8d47-05b54f25decf" finished successfully.

```


## [STEP 4] Create DataProc 
### Cretea a service account and iam role
- GCP에 가입하면 본인의 계정이 생성된다. 
- 여기서 생성하는 service account는 GCP에서 사용할 서비스에 접근 권한을 가지는 별도의 서비스를 의미한다.
- 이렇게 service account를 별도로 생성하는 이유는 
    - 내가 생성한 모든 GCP 서비스에 대한 접근을 세분화하여 관리하기 위함이다. 
    - 예를 들어 이번 실습에서 생성할 DataProc의 접근 할 수 있는 권한을 특정 service account에만 부여하여,
    - 다른 용도로 생성한 service account에서 접근할 수 없도록(서비스를 임의로 삭제, 중지 하는 등) 권한을 제어한다.
- Create service account 
```
> export SERVICE_ACCOUNT_NAME="dataproc-service-account"

> gcloud iam service-accounts create $SERVICE_ACCOUNT_NAME
Created service account [dataproc-service-account].

# Add an iam role to service account for dataproc
> export PROJECT=$(gcloud info --format='value(config.project)')

> gcloud projects add-iam-policy-binding $PROJECT \
    --role roles/dataproc.worker \
    --member="serviceAccount:$SERVICE_ACCOUNT_NAME@$PROJECT.iam.gserviceaccount.com"
```    

### Cretea a dataproc cluster
- DataProc를 생성하여 Apache Spark cluster를 쉽게 구성한다. 
- 아래 옵션 외에도 다양한 생성 옵션을 제공
    - 참고 : https://cloud.google.com/sdk/gcloud/reference/dataproc/clusters/create

- 아래에서 별도로 지정하지 않았지만, default로 설정되는 값은
- worker node : 2개 
    - --num-workers : 최소 2개 이상 지정 해야함.
- Machine Type
    - Default : n1-standard-4(4core, 15GB Mem) type
    - 무료 계정은 cpu 12core가 최대 
        - 따라서 master(4core), worker(4core) * 2대로 지정하면 
        - 다른 vm instance를 실행할 수 없게 된다. 
        - n1-standard-2 이하로 조정하여 설정 필요
- Disk : 100GB
- SSD : 기본은 지정되지 않으나, 아래 명령어로 할당 가능 (개수로 할당, 1개당 375G )
  --num-master-local-ssds=1 \
  --num-worker-local-ssds=1 \
- scopes : dataproc에서 접근 가능한 gcp 서비스를 명시한다. (이번 실습에서는 pubsub에 접속하지 않지만, 다음 실습용으로 추가하여 생성)

```
> gcloud dataproc clusters create demo-cluster \
    --worker-machine-type=n1-standard-1 \
    --region=asia-northeast3 \
    --zone=asia-northeast3-c\
    --scopes=pubsub \
    --image-version=1.2 \
    --service-account="$SERVICE_ACCOUNT_NAME@$PROJECT.iam.gserviceaccount.com"
```


## [STEP 5]  Run sample spark job
- Maven에서 Java 애플리케이션을 runnable jar 파일로 만드는 방법은 아래와 같이 대략 3가지 방법이 있다.
    - maven-jar-plugin : src/main/java, src/main/resources 만 포함한다.
    - maven-assembling-plugin: depdendencies jar 를 파일들을 함께 모듈화 한다.
    - maven-shade-plugin: depdendencies jar 를 파일을 함께 모듈화 하고 중복되는 클래스가 있을경우 relocate
- https://warpgate3.tistory.com/entry/Maven-Shade 참고

### 5.1 Pom.xml 설정 관련 (maven-shade-plugin 활용)
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
                            <mainClass>io.skiper.driver.Stage4StreamingDataprocKafka</mainClass>
<!--                  <mainClass>io.skiper.driver.Stage4StreamingDataprocPubsub</mainClass>-->
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

### 5.2 Spark Job 생성
- GCP DataProc(spark cluseter)에서 실행시킬 job을 코딩하여 컴파일한다. 
- spark cluster에서 실행 가능한 jar파일로 생성한다.
- SparConf.SetMaster 지정 옵션
    - Master를 local[*]로 지정 : Spark Job을 localhost에서만 실행 (즉, 병렬처리하지 않음)
        - local : 1개 쓰레드만 사용
        - local[2] : 2개 쓰레드를 사용. (core 갯수만큼 지정하는 것이 효율적)
        - local[*] : 서버에 있는 최대한 많은 core를 사용하도록 설정
        - 모든 Log가 한군데 존재하여, 바로 출력되어 확인 가능
    - Master를 지정하지 않음
        - GCP의 DataProc의 Cluster를 활용하여 처리함
        - DataProc Master에서 작업에 필요한 자원을 여러 노드(서버)에 할당
        - 즉, 데이터를 분할하여 여러대 서버에서 처리함.
        - 실제 데이터를 처리하는 서버가 다른 곳에 있으므로, 작업 로그가 출력되지 않음
        - Driver에서 실행되는 작업만 출력됨

- Stage4StreamingDataprocKafka.scala 파일의 주요 내용
```java
object Stage4StreamingDataprocKafka {
  def main(args: Array[String]) {

    val host_server = "서버의 외부 IP" // apache kafka, elasticsearch, redis가 설치된 서버의 IP 
    val kafka_broker = host_server+":9092"
    //[STEP 1] create spark streaming session

    // Create the context with a 1 second batch size
    // 1) Local Node에서만 실행 하는 경우 "local[2]"를 지정하거나, spark master url을 입력
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("Stage41_Streaming")

    // 2) DataProc를 사용하는 경우 setMaster를 지정하지 않음. (Log를 바로 확인하기 어려움)
    //val sparkConf = new SparkConf().setAppName("Stage41_Streaming")
    
    sparkConf.set("es.index.auto.create", "true");
    sparkConf.set("es.nodes", host_server)
    sparkConf.set("es.port", "9200")
    // 외부에서 ES에 접속할 경우 아래 설정을 추가 (localhost에서 접속시에는 불필요)
    sparkConf.set("spark.es.nodes.wan.only","true")

    val ssc = new StreamingContext(sparkConf, Seconds(2))

    addStreamListener(ssc)

    // [STEP 1]. Create Kafka Receiver and receive message from kafka broker
    // Create direct kafka stream with brokers and topics
    val topics = "realtime4"
    val topicsSet = topics.split(",").toSet
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> kafka_broker,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "realtime-group4",
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (true: java.lang.Boolean)
    )

    val kafkaStreams = (1 to 1).map { i =>
      KafkaUtils.createDirectStream[String, String](
        ssc,
        LocationStrategies.PreferConsistent,
        ConsumerStrategies.Subscribe[String, String](topicsSet, kafkaParams))
    }
    val messages = ssc.union(kafkaStreams)

    // [STEP 2]. parser message and join customer info from redis
    // original msg = ["event_id","customer_id","track_id","datetime","ismobile","listening_zip_code"]
    val columnList  = List("@timestamp", "customer_id","track_id","ismobile","listening_zip_code", "name", "age", "gender", "zip", "Address", "SignDate", "Status", "Level", "Campaign", "LinkedWithApps")
    val lines = messages.map(_.value)

    val wordList    = lines.mapPartitions(iter => {
      val r = new RedisClient(host_server, 6379)
      iter.toList.map(s => {
        val listMap = new mutable.LinkedHashMap[String, Any]()
        val split   = s.split(",")
        //        println(s)
        //        println(split(0))

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
      EsSpark.saveToEs(rdd, "ba_realtime4/stage4")
    })

    ssc.start()
    ssc.awaitTermination()
  }
}
```

- 위 코드에서 
```
> cd ~/demo-spark-analytics/00.stage4-1/demo-streaming-cloud/
> vi src/main/scala/io/skiper/driver/Stage4StreamingDataprocKafka.scala
# 아래 IP를 본인의 apache kafka/redis/elasticsearch가 설치된 IP로 변경한다. 
    val host_server = "IP입력"
```

### 5.3 Compile and run spark job
```
# jdk 1.8이 사전에 설치되어 있어야 함. 
> sudo yum install -y git maven
# (옵션-생략가능 JAVA_HOME 설정이 되어 있는 경우)
> sudo update-java-alternatives -s java-1.8.0-openjdk-amd64 && export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/jre

> cd ~/demo-spark-analytics/00.stage4-1/demo-streaming-cloud/
> mvn clean package
> ls -alh  target
# demo-streaming-cloud-1.0-SNAPSHOT.jar파일이 original 대비 크기가 증가한 것을 볼 수 있다.
-rw-rw-r--. 1 freepsw.09 freepsw.09  61K 12월 20 09:51 original-demo-streaming-cloud-1.0-SNAPSHOT.jar
-rw-rw-r--. 1 freepsw.09 freepsw.09 111M 12월 20 09:52 demo-streaming-cloud-1.0-SNAPSHOT.jar

```

### 5.4 Submit spark job to dataroc
```
> cd ~/demo-spark-analytics/00.stage4-1/demo-streaming-cloud/
> export PROJECT=$(gcloud info --format='value(config.project)')
> export JAR="demo-streaming-cloud-1.0-SNAPSHOT.jar"
> export SPARK_PROPERTIES="spark.dynamicAllocation.enabled=false,spark.streaming.receiver.writeAheadLog.enabled=true"

> gcloud dataproc jobs submit spark \
--cluster demo-cluster \
--region asia-northeast3  \
--async \
--jar target/$JAR \
--max-failures-per-hour 10 \
--properties $SPARK_PROPERTIES 

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
```

- 위에서 생성한 job이 정상 동작함.
```
> gcloud dataproc jobs list --region=asia-northeast3 --state-filter=active
JOB_ID                            TYPE   STATUS
446ca40670bf4c55be0e690710882a20  spark  RUNNING
```

-  아래의 jobs에 JOB_ID를 입력하여 웹브라우저로 접속하여, 실행한 job이 정상 실행 중인지 확인한다. 
    - https://console.cloud.google.com/dataproc/jobs/446ca40670bf4c55be0e690710882a20?region=asia-northeast3



## [STEP 6] Collect the log data using logstash 
### Run logstash 
- kafka topic을 realtime4로 변경
```
> cd ~/demo-spark-analytics/00.stage4-1
> vi logstash_stage4-1.conf
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

  kafka {
    codec => plain {
      format => "%{message}"
    }
    bootstrap_servers => "localhost:9092"
    topic_id => "realtime4"
  }
}

```

- run logstash 
```
> cd ~/demo-spark-analytics/00.stage4-1
> ~/demo-spark-analytics/sw/logstash-7.10.1/bin/logstash -f logstash_stage4-1.conf
```

- check received message from kafka using kafka-console_consumer
    - logstash에서 kafka로 정상적으로 메세지가 전송되고 있는지 모니터링
    - 아래의 kafka-console-consumer 명령어를 통해 전송되는 메세지를 확인
```
> cd ~/demo-spark-analytics/sw/kafka_2.11-2.4.1
> bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic realtime4
# logstash에서 정상적으로 메세지를 보내면, 아래와 같은 메세지가 출력될 것임.
0,48,453,"2014-10-23 03:26:20",0,"72132"
1,1081,19,"2014-10-15 18:32:14",1,"17307"
2,532,36,"2014-12-10 15:33:16",1,"66216
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
    - http://<demo-cluster-m의 IP>:8088
    - http://<demo-cluster-m의 IP>:9870
    - http://<demo-cluster-m의 IP>:19888

- https://jeongchul.tistory.com/589 참고

## [STEP 7]  GCP 자원 해제
```
> export SERVICE_ACCOUNT_NAME="dataproc-service-account"

> gcloud dataproc jobs list --region=asia-northeast3 --state-filter=active
JOB_ID                            TYPE   STATUS
446ca40670bf4c55be0e690710882a20  spark  RUNNING

> gcloud dataproc jobs kill 446ca40670bf4c55be0e690710882a20 --region=asia-northeast3 --quiet
> gcloud dataproc clusters delete demo-cluster --quiet --region=asia-northeast3
> gcloud pubsub topics delete realtime --quiet
> gcloud pubsub subscriptions delete realtime-subscription --quiet 
> gcloud iam service-accounts delete $SERVICE_ACCOUNT_NAME@$PROJECT.iam.gserviceaccount.com --quiet --region=asia-northeast3
```


## [ETC]
### 1. DataProc의 동적 확장
```
> gcloud dataproc clusters update example-cluster --num-workers 4
```

### 2. run on intellij (원격 로컬서버에서 실행시)
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

#### [Error] ItelliJ 에서 Run 실행시 오류 및 해결
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

#### [Solve] IntelliJ 환경설정
- GCP에서는 모든 서비스에 접근하기 위해서는 권한이 필요함.
- 이 권한을 service account를 통해서 허용을 할 수 있으므로, 
- Service Account를 먼저 생성하고, 
- 이 service account를 인증하는 인증서를 생성해야 한다. 
- 이후 Intellij와 같은 외부 서비스에서 GCP에 접근하기 위해서는 생성된 인증서을 통해서 가능하다. 
    - 인증서는 GCP 사용권한을 가지므로, 안전하게 보관해야 함.

##### 1) Service Account 생성하기
- IAM에서 service account 생성 
##### 2) 인증서 생성 및 다운로드하기
- json 파일로 다운로드 

##### 3) IntelliJ에 인증서정보를 환경변수로 설정하기. 
- Run > Edit Configuration 메뉴 클릭
- Configuratio 탭에서 Environment variables 확인
- 환경변수 추가
    - Name : GOOGLE_APPLICATION_CREDENTIALS
    - Value : 다운로드 받은 인증서 파일의 경로


##### 4) (옵션) GCP Project ID를 프로그램 실행 argument로 전달하기
- Run > Edit Configuration 메뉴 클릭
- Configuratio 탭에서 "Program arguments" 선택
- gcp project id 입력

##### 5) (옵션) 실행 시 오류 
- 에러 메세지 
```
Error: A JNI error has occurred, please check your installation and try again
Exception in thread "main" java.lang.NoClassDefFoundError: org/apache/spark/streaming/StreamingContext
	at java.lang.Class.getDeclaredMethods0(Native Method)
```
- 해결방안 
    - IntelliJ의 Edit Run Configuration >  'Include dependencies with "Provided" scope' 체크
    - 참고
        - https://stackoverflow.com/questions/36437814/-how-to-work-efficiently-with-sbt-spark-and-provided-dependencies?rq=1

