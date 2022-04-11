# Stage 1. Simple realtime visualization
- 사용자의 접속로그를 logstsh로 수집하여 Elasticsearch로 저장한 후, kibana를 이용하여 빠르게 시각화

### What are customer doing?
 * 시간별 사용량 추이를 어떠한가? 그 중 mobile 접속자는 어느정도 되는가?
 * 가장 많이 접속하는 지역은 어디인가?
 * 지역별로 어떤 사용자들이 접속하는가? (customer_id만 조회가능)
 * 사용자들이 언제 어떤 음악을 듣는가? (customer_id와 track_id만 조회가능)

### software 구성도
![stage1 architecture](https://github.com/freepsw/demo-spark-analytics/blob/master/resources/images/stage1.png)


## [Prerequisite] Install system library
- 실습에 필요한 라이브러리 설치
#### Java 설치 및 JAVA_HOME 설정
```
> sudo yum install -y java

# 현재 OS 설정이 한글인지 영어인지 확인한다. 
> alternatives --display java

# 아래와 같이 출력되면 한글임. 
슬레이브 unpack200.1.gz: /usr/share/man/man1/unpack200-java-1.8.0-openjdk-1.8.0.312.b07-1.el7_9.x86_64.1.gz
현재 '최고' 버전은 /usr/lib/jvm/java-1.8.0-openjdk-1.8.0.312.b07-1.el7_9.x86_64/jre/bin/java입니다.

### 한글인 경우 
> alternatives --display java | grep '현재 /'| sed "s/현재 //" | sed 's|/bin/java로 링크되어 있습니다||'
> export JAVA_HOME=$(alternatives --display java | grep '현재 /'| sed "s/현재 //" | sed 's|/bin/java로 링크되어 있습니다||')

### 영문인 경우
> alternatives --display java | grep current | sed 's/link currently points to //' | sed 's|/bin/java||' | sed 's/^ //g'
> export JAVA_HOME=$(alternatives --display java | grep current | sed 's/link currently points to //' | sed 's|/bin/java||' | sed 's/^ //g')

# 제대로 java 경로가 설정되었는지 확인
> echo $JAVA_HOME
> echo "export JAVA_HOME=$JAVA_HOME" >> ~/.bash_profile
> source ~/.bash_profile
```

### Download git project
```
> cd ~
> sudo yum install -y wget git
> git clone https://github.com/freepsw/demo-spark-analytics.git
> cd demo-spark-analytics
> mkdir sw
```

## [STEP 1] Install ELK Stack (Elasticsearch)
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
> cd ~/demo-spark-analytics/sw/
> wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-7.10.2-linux-x86_64.tar.gz
> tar -xzf elasticsearch-7.10.2-linux-x86_64.tar.gz
```

### config 설정 
    - 외부 접속 허용(network.host) : server와 client가 다른 ip가 있을 경우, 외부에서 접속할 수 있도록 설정을 추가해야함.
    - master host 설정 (cluster.initial_master_nodes) : Master Node의 후보를 명시하여, Master Node 다운시 새로운 Master로 선출한다.
```
> cd ~/demo-spark-analytics/sw/elasticsearch-7.10.2
> vi config/elasticsearch.yml
# bind ip to connect from client  (lan이 여러개 있을 경우 외부에서 접속할 ip를 지정할 수 있음.)
# bind all ip server have "0.0.0.0"

network.host: 0.0.0.0   #(":" 다음에 스페이스를 추가해야 함.)

# Master Node의 후보 서버 목록을 적어준다. (여기서는 1대 이므로 본인의 IP만)
cluster.initial_master_nodes: ["서버이름"]

## 위의 설정에서 IP를 입력하면, 아래 오류 발생
    - skipping cluster bootstrapping as local node does not match bootstrap requirements: [34.64.85.xx]
    - master not discovered yet, this node has not previously joined a bootstrapped (v7+) cluster, and [cluster.initial_master_nodes] is empty on this node
```

#### Error 발생 (cluster.initial_master_nodes에 IP를 입력한 경우)
- 에러 로그 유형
    - skipping cluster bootstrapping as local node does not match bootstrap requirements: [34.64.85.55]
    - master not discovered yet, this node has not previously joined a bootstrapped (v7+) cluster, and [cluster.initial_master_nodes] is empty on this node
- 해결
    - cluster.initial_master_nodes: ["서버이름"] 입력 

#### run elasticsearch 
```
> cd ~/demo-spark-analytics/sw/elasticsearch-7.10.2
> bin/elasticsearch

# 아래와 같은 에러가 발생함. 
ERROR: [3] bootstrap checks failed
[1]: max file descriptors [4096] for elasticsearch process is too low, increase to at least [65535]
[2]: max virtual memory areas vm.max_map_count [65530] is too low, increase to at least [262144]
ERROR: Elasticsearch did not exit normally - check the logs at /home/freepsw/apps/elasticsearch-7.10.2/logs/elasticsearch.log
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

### elasticsearch 실행
- 1) Foreground 실행
```
> cd ~/demo-spark-analytics/sw/elasticsearch-7.10.2
> bin/elasticsearch
......
[2020-12-14T10:18:18,803][INFO ][o.e.l.LicenseService     ] [freepsw-test] license [944a4695-3ec0-41f1-b3f8-5752b71c759e] mode [basic] - valid
[2020-12-14T10:18:18,806][INFO ][o.e.x.s.s.SecurityStatusChangeListener] [freepsw-test] Active license is now [BASIC]; Security is disabled
```
- 2) background(daemon)으로 실행시 명령어 (실행된 프로세스의 pid 값을 elastic_pid 파일에 기록)
```
> cd ~/apps/elasticsearch-7.10.2
> ./bin/elasticsearch -d -p elastic_pid

### daemon으로 실행시 서비스 종료하려면 아래 명령어 실행 (기록된 pid 값을 읽어와서 프로세스 종료)
> pkill -F elastic_pid
```

#### elasticsearch 정상 동작 확인
```
> curl -X GET "localhost:9200/?pretty"
```

#### Elasticsearch UI로 접속하기 
- 1) 웹브라우저에서 접속 확인 
    - http://VM외부IP:9200
- 2) Elasticsearch용 시각화 plugin(elasticsearch head) 설치 (구글 크롬 브라우저)
    - https://chrome.google.com/webstore/detail/elasticsearch-head/ffmkiejjmecolpfloofpjologoblkegm
    - "Chrome에 추가" 클릭
    - 추가된 Plugin 클릭하여 접속 > "Elasticsearch 설치된 IP입력" > Connect 버튼 클릭



## [STEP 2] Install and run Kibana
### Download the kibana 
```
> cd ~/demo-spark-analytics/sw/
> curl -O https://artifacts.elastic.co/downloads/kibana/kibana-7.10.2-linux-x86_64.tar.gz
> tar -xzf kibana-7.10.2-linux-x86_64.tar.gz
> cd ~/demo-spark-analytics/sw/kibana-7.10.2-linux-x86_64/

# 외부 접속 가능하도록 설정 값 변경 
# 외부의 어떤 IP에서도 접속 가능하도록 0.0.0.0으로 변경 (운영환경에서는 특정 ip대역만 지정하여 보안강화)
> vi config/kibana.yml
server.host: "0.0.0.0"
```

### Run the kibana 
- 2 가지 방식으로 실행 가능 
```
> cd ~/demo-spark-analytics/sw/kibana-7.10.2-linux-x86_64

## 1) foreground 실행
> bin/kibana
.....
  log   [10:40:10.296] [info][server][Kibana][http] http server running at http://localhost:5601
  log   [10:40:12.690] [warning][plugins][reporting] Enabling the Chromium sandbox provides an additional layer of protection

## 2) background 실행
> nohup bin/kibana &
```

### [ERRO 유형 1 ] Port 5601 is already in use. Another instance of Kibana may be running!
- kibana가 현재 실행중. (설정이나 재시작이 필욯지 않다면 현재 실해 중인 kibana 사용)
- 재시작이 필요하다면, 아래와 같이 process id를 찾아서 해당 process를 종료한 후, kibana 재시작
```
## 마지막 칼럼의 16998이 process id (process id는 개인별로 다르게 표시됨)
> netstat -anp | grep 5601
tcp        0      0 0.0.0.0:5601            0.0.0.0:*               LISTEN      16998/bin/../node/b

## process 종료
> kill -9 16998
```

#### [ERRO 유형 2] Kibana 에러 시 기존 index 삭제 후 재시작
```
curl -XDELETE http://localhost:9200/.kibana
curl -XDELETE 'http://localhost:9200/.kibana*'
curl -XDELETE http://localhost:9200/.kibana_2
curl -XDELETE http://localhost:9200/.kibana_1
```


## [STEP 3] Install the logstash 
## Install the logstash 
```
> cd ~/demo-spark-analytics/sw/
> curl -OL https://artifacts.elastic.co/downloads/logstash/logstash-oss-7.10.2-linux-x86_64.tar.gz
> tar xvf logstash-oss-7.10.2-linux-x86_64.tar.gz
> rm -rf logstash-oss-7.10.2-linux-x86_64.tar.gz
> cd ~/logstash-7.10.2
```
### Test the logstash 
```
> bin/logstash -e 'input { stdin { } } output { stdout {} }'
# 실행까지 시간이 소요된다. (아래 메세지가 출력되면 정상 실행된 것으로 확인)
.........
The stdin plugin is now waiting for input:
[2022-03-20T08:20:58,728][INFO ][logstash.agent           ] Pipelines running {:count=>1, :running_pipelines=>[:main], :non_running_pipelines=>[]}
[2022-03-20T08:20:59,146][INFO ][logstash.agent           ] Successfully started Logstash API endpoint {:port=>9600}
mytest  <-- 메세지 입력 후 아래와 같이 출력되면 정상적으로 설치된 것
{
       "message" => "mytest",
      "@version" => "1",
          "host" => "freepsw-test",
    "@timestamp" => 2022-03-20T08:20:59.408Z
}
```

## [STEP 4] run logstash (logstash_stage1.conf)
- tracks_live.csv 파일을 읽어서온 후, 필드별로 type을 지정하고 elasticsearch에 저장한다.

### configuration (collect logs and save to ES)
- "demo-spark-analytics/00.stage1/logstash_stage1.conf"
- <PATH> 부분을 각자의 경로로 수정한다.

```javascript
> vi ~/demo-spark-analytics/00.stage1/logstash_stage1.conf
input {  
  file {
    path => "/home/rts/demo-spark-analytics/00.stage1/tracks_live.csv"
    sincedb_path => "/dev/null"
    start_position => "beginning"
  }
}

filter {
  csv {
    columns => ["event_id","customer_id","track_id","datetime","ismobile","listening_zip_code"]
    separator => ","
  }

  date {
    match => [ "datetime", "YYYY-MM-dd HH:mm:ss"]
    target => "datetime"
  }

  mutate {
    convert => { "ismobile" => "integer" }
  }
}

output {
  stdout {
    codec => rubydebug{ }
  }

  elasticsearch {
    hosts => "http://localhost:9200"
    index => "ba_realtime"
  }
}
```
### logstash 설정 정보 
- input
  - path : 읽어올 파일이 절대 경로를 입력한다. (새로운 내용이 파일에 입력되면 즉시 해당 내용을 읽어온다. tail -f 와 동일한 기능)
  - start_position : 처음 파일을 읽어올때 기존 내용을 전부 읽을 경우(beginning), 마지막 내용만 읽어올 경우(end, default)

- filter
  - csv
  > - csv파일의 내용을 명시한 field명으로 매핑하여 저장(elasticsearch에 저장될 field명)
  > - seperator : 구분
  - date
  > - match : 지정한 field(지정한 date format을 가진)를 timestamp로 지정. (만약 아래 target이 별도로 지정되지 않는 경우.)
  > - target : 위에서 매핑한 date를 elasticsearch의 기본 timestamp로 사용하지 않고, datetime으로 저장함. (만약 target이 없으면 datetime을 timestamp로 사용)
  > - locale : log에 저장된 날짜 type이 영어권 지역인 경우, 지역에 맞는 parsing locale을 지정해야 한다.
  - mutate
  - convert : 해당 field의 type을 지정해 준다. (integer로 해야 kibana등에서 sum/avg등의 연산이 가능함)

- output
  - hosts : elasticsearch가 실행중인 서버의 ip
  - index : elasticsearch의 index (없으면 자동으로 생성됨)
  - document_type : elasticsearch의 document_type (없으면 자동으로 생성됨)

### run logstash
```
> cd ~/demo-spark-analytics/00.stage1
> ~/demo-spark-analytics/sw/logstash-7.10.2/bin/logstash -f logstash_stage1.conf
```

## [STEP 5] Run data generator (data_generator.py)
### source code
- 실시간으로 데이터가 유입될 수 있도록 data generator에서 특정 file에 write (random time period)
- 이는 실시간으로 사용자들이 접속하는 log를 재연하기 위한 용도로 사용.
- tracks.csv -> data generator(date를 현재 시간으로 조정) -> tracks_live.log
- 1,000,000건이 저장된 tracks.csv에서 파일을 읽어서, 랜덤한 시간 간격으로 tracks_live.csv에 쓴다.
- data_generator.py
```python
#-*- coding: utf-8 -*-
import time
import random

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

    # sleep for weighted time period
    stime = random.choice([1, 1, 1, 0.5, 0.5, 0.8, 0.3, 2, 0.1, 3])
    time.sleep(stime)
    lines += 1

    # exit if read all lines
    if(lines == num_lines):
      break

finally:
  rf.close()
  wf.close()
  print "close file"
```

### run data_generator.py
```
> cd ~/demo-spark-analytics/00.stage1
> python data_generator.py
```


## [STEP 6] check result
### elasticsearch query를 활용한 데이터 처리 상태 확인 
- curl로 확인하는 코드 추가 !!!!!!
```javascript
//open with web browser
http://localhost:9200/ba_realtime/_count

// 아래와 같이 count가 증가하면 정상적으로 ES에 입력되고 있음을 확인
{
"count": 16,
  "_shards": {
    "total": 5,
    "successful": 5,
    "failed": 0
  }
}
```

### 크롬 브라우저의 plugin(elasticsearch hea) 활용한 데이터 처리 상태 확인 
- 추가된 Plugin 클릭하여 접속 > "Elasticsearch 설치된 IP입력" > Connect 버튼 클릭




## [STEP 7] visualize collected data using kibana
- kibana를 이용한 시각화 가이드는 아래의 link에 있는 ppt파일을 참고
- https://github.com/freepsw/demo-spark-analytics/blob/master/00.stage1/kibana_visualization_guide_stage1.pptx

####- index 추가
 * http://localhost:5601/ 접속
 * [Settings] > [Indeices]로 이동
 * index name 입력 (kibana에서 시각화할 index)
 * timestamp 선택 (데이터를 시각화할 기본 timestamp)

####- Discover
 * discovery에서 입력되는 count 확인
 * 각 record의 상세 데이터 확인
 * 조회할 기간 변경
 * filter를 이용해서 검색어를 입력하여 원하는 데이터만 조회
 * 화면에 표시될 필드 선택하여 조회
 * 위의 bar 그래프에서 원하는 영역을 drag하여 drill down 조회
 * 사용자가 시각화한 포맷을 저장한다. "save"아이콘 활용

####- visualization 메뉴를 이용하여 시각화
 * [Visualize] > [Pie Chart]
  - 가장 많이 접속하는 지역과, 해당지역에서 접속하는 사용자 비율
  - Split Sliices > Terms > listening_zip_code
  - Split Slices > Terms > customer_id
  - Options > View options > check Donut
  - Save as "BA_S1: pie_chart Customer count per region"

 * [Visualize] > [Line Chart]
  - X-Axis > Aggregation (Date Histogram) > Field(@timestamp) > Interval(Auto)
  - Y-Axis > Aggregation (Sum) > Field(ismobile)
  - Dot Size > Aggregation(Unique Count) > Field(customer_id)

 * [Visualize] > [Metrics]

 * [Visualize] > [Bar chart]

#### - Dashboard 생성
 * [Dashboard] > [+] button
 * [Visualize]메뉴에서 저장한 chart를 선택하여 자신만의 dashboard를 생성한다.
 * 생성한 dashboard를 저장한다.
 * 이후 다른 kibana web에서 dashboard를 보고싶다면 export하여 json파일로 저장한다.

#### - github에 포함된 json파일을 이용하여 dashboard 생성
 * [Setting] > [Objects] 메뉴에서 "import"를 클릭
 * ~/demo-spark-analytics/00.stage1 아래에 있는 json 파일을 선택
 * kibana_dashboard_s1.json, kibana_search_s1.json, kibana_visualization_s1.json
 * [Dashboard] > [+] button을 클릭하여 import한 시각화 객체를 불러온다. (또는 Dashboard 객체)














## [STEP 1] install elasticsearch and kibana

- elasticsearh [link](https://github.com/freepsw/demo-spark-analytics/blob/master/01.installed_sw/elasticsearch.md)

```
> cd ~/demo-spark-analytics/sw

# download
> wget https://download.elastic.co/elasticsearch/release/org/elasticsearch/distribution/tar/elasticsearch/2.4.0/elasticsearch-2.4.0.tar.gz
> tar xvf elasticsearch-2.4.0.tar.gz

# install plugin
> cd elasticsearch-2.4.0
> bin/plugin install mobz/elasticsearch-head
```

- config : server와 client가 다른 ip가 있을 경우, 외부에서 접속할 수 있도록 설정을 추가해야함.
```
> cd ~/demo-spark-analytics/sw/elasticsearch-2.4.0
> vi config/elasticsearch.yml
# bind ip to connect from client  (lan이 여러개 있을 경우 외부에서 접속할 ip를 지정할 수 있음.)
# bind all ip server have "0.0.0.0"
network.host: 0.0.0.0   (":" 다음에 스페이스를 추가해야 함.)
```


- kibana [link](https://github.com/freepsw/demo-spark-analytics/blob/master/01.installed_sw/kibana.md)

```
> cd ~/demo-spark-analytics/sw
> wget https://download.elastic.co/kibana/kibana/kibana-4.6.2-linux-x86_64.tar.gz
> tar xvf kibana-4.6.2-linux-x86_64.tar.gz
```

- logstash [link](https://github.com/freepsw/demo-spark-analytics/tree/master/01.logstash)

```
> cd ~/demo-spark-analytics/sw
> wget https://download.elastic.co/logstash/logstash/logstash-2.4.0.tar.gz
> tar xvf logstash-2.4.0.tar.gz
```

 - set logstash path to $path
```
> vi ~/.bash_profile
# 마지막 line에 추가
export PATH=$PATH:~/demo-spark-analytics/sw/logstash-2.4.0/bin

> source ~/.bash_profile
```


## [STEP 2] run elasticsearch and kibana
```
> cd ~/demo-spark-analytics/sw/elasticsearch-2.4.0
> bin/elasticsearch

> cd ~/demo-spark-analytics/sw/kibana-4.6.2-linux-x86_64
> bin/kibana
```

- open with web browser
  - open elasticsearch : http://localhost:9200/_plugin/head/
  - open kibana : http://localhost:5601


## [STEP 3] run logstash (logstash_stage1.conf)
- tracks_live.csv 파일을 읽어서온 후, 필드별로 type을 지정하고 elasticsearch에 저장한다.

### configuration (collect logs and save to ES)
- "demo-spark-analytics/00.stage1/logstash_stage1.conf"
- <PATH> 부분을 각자의 경로로 수정한다.

```javascript
> vi ~/demo-spark-analytics/00.stage1/logstash_stage1.conf
input {  
  file {
    path => "/home/rts/demo-spark-analytics/00.stage1/tracks_live.csv"
    sincedb_path => "/dev/null"
    start_position => "beginning"
  }
}

filter {
  csv {
    columns => ["event_id","customer_id","track_id","datetime","ismobile","listening_zip_code"]
    separator => ","
  }

  date {
    match => [ "datetime", "YYYY-MM-dd HH:mm:ss"]
    target => "datetime"
  }

  mutate {
    convert => { "ismobile" => "integer" }
  }
}

output {
  stdout {
    codec => rubydebug{ }
  }

  elasticsearch {
    hosts => "http://localhost:9200"
    index => "ba_realtime"
  }
}
```

- input
  - path : 읽어올 파일이 절대 경로를 입력한다. (새로운 내용이 파일에 입력되면 즉시 해당 내용을 읽어온다. tail -f 와 동일한 기능)
  - start_position : 처음 파일을 읽어올때 기존 내용을 전부 읽을 경우(beginning), 마지막 내용만 읽어올 경우(end, default)

- filter
  - csv
  > - csv파일의 내용을 명시한 field명으로 매핑하여 저장(elasticsearch에 저장될 field명)
  > - seperator : 구분
  - date
  > - match : 지정한 field(지정한 date format을 가진)를 timestamp로 지정. (만약 아래 target이 별도로 지정되지 않는 경우.)
  > - target : 위에서 매핑한 date를 elasticsearch의 기본 timestamp로 사용하지 않고, datetime으로 저장함. (만약 target이 없으면 datetime을 timestamp로 사용)
  > - locale : log에 저장된 날짜 type이 영어권 지역인 경우, 지역에 맞는 parsing locale을 지정해야 한다.
  - mutate
  - convert : 해당 field의 type을 지정해 준다. (integer로 해야 kibana등에서 sum/avg등의 연산이 가능함)

- output
  - hosts : elasticsearch가 실행중인 서버의 ip
  - index : elasticsearch의 index (없으면 자동으로 생성됨)
  - document_type : elasticsearch의 document_type (없으면 자동으로 생성됨)

#### run logstash
```
> cd ~/demo-spark-analytics/00.stage1
> ~/demo-spark-analytics/sw/logstash-2.4.0/bin/logstash -f logstash_stage1.conf
```

#### check result
- check using elasticsearch query
```javascript
//open with web browser
http://localhost:9200/ba_realtime/_count

// 아래와 같이 count가 증가하면 정상적으로 ES에 입력되고 있음을 확인
{
"count": 16,
  "_shards": {
    "total": 5,
    "successful": 5,
    "failed": 0
  }
}
```

- 크롬 브라우저에 접속하여 확인
```
//open with web brower
http://localhost:9200/_plugin/head/
// Overview menu에 생성한 index와 document type이 존재하는지 확인
```

## [STEP 4] run data generator (data_generator.py)
### source code
- 실시간으로 데이터가 유입될 수 있도록 data generator에서 특정 file에 write (random time period)
- 이는 실시간으로 사용자들이 접속하는 log를 재연하기 위한 용도로 사용.
- tracks.csv -> data generator(date를 현재 시간으로 조정) -> tracks_live.log
- 1,000,000건이 저장된 tracks.csv에서 파일을 읽어서, 랜덤한 시간 간격으로 tracks_live.csv에 쓴다.
- data_generator.py
```python
#-*- coding: utf-8 -*-
import time
import random

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

    # sleep for weighted time period
    stime = random.choice([1, 1, 1, 0.5, 0.5, 0.8, 0.3, 2, 0.1, 3])
    time.sleep(stime)
    lines += 1

    # exit if read all lines
    if(lines == num_lines):
      break

finally:
  rf.close()
  wf.close()
  print "close file"
```

### run data_generator.py
```
> cd ~/demo-spark-analytics/00.stage1
> python data_generator.py
```


## [STEP 5] visualize collected data using kibana
- kibana를 이용한 시각화 가이드는 아래의 link에 있는 ppt파일을 참고
- https://github.com/freepsw/demo-spark-analytics/blob/master/00.stage1/kibana_visualization_guide_stage1.pptx

####- index 추가
 * http://localhost:5601/ 접속
 * [Settings] > [Indeices]로 이동
 * index name 입력 (kibana에서 시각화할 index)
 * timestamp 선택 (데이터를 시각화할 기본 timestamp)

####- Discover
 * discovery에서 입력되는 count 확인
 * 각 record의 상세 데이터 확인
 * 조회할 기간 변경
 * filter를 이용해서 검색어를 입력하여 원하는 데이터만 조회
 * 화면에 표시될 필드 선택하여 조회
 * 위의 bar 그래프에서 원하는 영역을 drag하여 drill down 조회
 * 사용자가 시각화한 포맷을 저장한다. "save"아이콘 활용

####- visualization 메뉴를 이용하여 시각화
 * [Visualize] > [Pie Chart]
  - 가장 많이 접속하는 지역과, 해당지역에서 접속하는 사용자 비율
  - Split Sliices > Terms > listening_zip_code
  - Split Slices > Terms > customer_id
  - Options > View options > check Donut
  - Save as "BA_S1: pie_chart Customer count per region"

 * [Visualize] > [Line Chart]
  - X-Axis > Aggregation (Date Histogram) > Field(@timestamp) > Interval(Auto)
  - Y-Axis > Aggregation (Sum) > Field(ismobile)
  - Dot Size > Aggregation(Unique Count) > Field(customer_id)

 * [Visualize] > [Metrics]

 * [Visualize] > [Bar chart]

#### - Dashboard 생성
 * [Dashboard] > [+] button
 * [Visualize]메뉴에서 저장한 chart를 선택하여 자신만의 dashboard를 생성한다.
 * 생성한 dashboard를 저장한다.
 * 이후 다른 kibana web에서 dashboard를 보고싶다면 export하여 json파일로 저장한다.

#### - github에 포함된 json파일을 이용하여 dashboard 생성
 * [Setting] > [Objects] 메뉴에서 "import"를 클릭
 * ~/demo-spark-analytics/00.stage1 아래에 있는 json 파일을 선택
 * kibana_dashboard_s1.json, kibana_search_s1.json, kibana_visualization_s1.json
 * [Dashboard] > [+] button을 클릭하여 import한 시각화 객체를 불러온다. (또는 Dashboard 객체)
