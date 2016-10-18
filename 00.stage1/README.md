# Stage 1. Simple realtime visualization

## 1) data generator (data_generator.py)
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

### run python
```
cd ~/demo-spark-analytics/00.stage1
python data_generator.py
```


## 2) logstash (logstash_stage1.conf)
- tracks_live.csv 파일을 읽어서온 후, 필드별로 type을 지정하고 elasticsearch에 저장한다. 

### configuration (collect logs and save to ES, logstash_stage1.conf)

```
input {  
  file {
    path => "<PATH>/demo-spark-analytics/00.stage1/tracks_live.csv"
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
    locale => en
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
    document_type => "stage1"
  }
}
```

- input
 * path : 읽어올 파일이 절대 경로를 입력한다. (새로운 내용이 파일에 입력되면 즉시 해당 내용을 읽어온다. tail -f 와 동일한 기능)
 * start_position : 처음 파일을 읽어올때 기존 내용을 전부 읽을 경우(beginning), 마지막 내용만 읽어올 경우(end, default)

- filter
 * csv
  - csv파일의 내용을 명시한 field명으로 매핑하여 저장(elasticsearch에 저장될 field명)
  - seperator : 구분
 * date
  - match : 지정한 field(지정한 date format을 가진)를 timestamp로 지정. (만약 아래 target이 별도로 지정되지 않는 경우.)
  - target : 위에서 매핑한 date를 elasticsearch의 기본 timestamp로 사용하지 않고, datetime으로 저장함. (만약 target이 없으면 datetime을 timestamp로 사용)
  - locale : log에 저장된 날짜 type이 영어권 지역인 경우, 지역에 맞는 parsing locale을 지정해야 한다.
 * mutate
  - convert : 해당 field의 type을 지정해 준다. (integer로 해야 kibana등에서 sum/avg등의 연산이 가능함)

- output
 * hosts : elasticsearch가 실행중인 서버의 ip
 * index : elasticsearch의 index (없으면 자동으로 생성됨)
 * document_type : elasticsearch의 document_type (없으면 자동으로 생성됨)

#### run logstash
```
logstash -f logstash_stage1.conf
```


## 3) kibana
####- index 추가 

####- discovery에서 입력되는 count 확인
 * 각 record의 상세 데이터 확인
 * 조회할 기간 변경 
 * filter를 이용해서 검색어를 입력하여 원하는 데이터만 조회
 * 화면에 표시될 필드 선택하여 조회
 * 위의 bar 그래프에서 원하는 영역을 drag하여 drill down 조

####- visualization 메뉴를 이용하여 시각화 
 * pie chart 선택 (전체 날짜 선택)
  - spilit chart -> missing value가 있는 경우 그래프에서 삭제 (-모양의 돗보기 사용), top n개만 보여지도록 설정
  - split chart (하나의 항목별로 다른 항목의 비중 조회), 예를 들면 서울지역에서 남/여 비율
  - 2개의 split의 순서를 조정(increase priority, 화살표 버튼)
  - save current visualization as visualization component (this can be used for dashboard later)
  - 
 * line chart (시간별 추세 확인 용도)
  - 초기에 데이터를 split하지 않으면 x축이 all, y축이 count로 전체 데이터의 건수가 하나의 포인트로 표시된다. 
  - X-Axis > split > date histogram -> x축의 date column 선택 -> interval은 auto로 조회
  - 위에서는 단순히 record 건수만 표시되므로, 이를 특정 field의 값을 표시하도록 하자 (이때, time interval이 1 day인 경우 1일 동안의 값을 어떻게 보여주지 선택)
  - Y-Axis의 aggregation에서 field명과 선택된 field에 대한 sum, average등을 선택(string type의 field는 선택하지 못함)
  - X-Axis > split line -> Sub aggregation(Terms) -> 특정 field를 선택하면 해당 field의 상위 n개에 대하여 line이 추가된다. (priority를 높여서 그래서 재생 -> missing value 제거 )
  - 여기서의 line은 average score만 y축으로 보여주므로, 해당 기간(x축 한 눈금)에 얼마나 많은 정보(field의 값 중에서 unique한 값이 count, 예를 들면 해당 기간동한 방문한 unique user id의 갯수를 표현)가 있는지 표현하고 싶을 수 있다. 
  - 이때  Y-Axis에서 dot-size를 이용하여 line의 node별로 dot의 크기를 다르게 설정함.
  - Data/Option메뉴에서 Option을 보면 그래프를 어떻게 보여줄 지에 대한 상세 설정이 있음. (hide line등)

* map

* metrics
 - 처음 metrics를 클릭하면 전체 record의 count가 출력된다.
 - fields에서 count하고자 하는 field를 선택하고, aggregation에서 count/unique count/sum/average 등의 집계방식을 선택할 수 있다. 
 - metric 추가도 위와 같은 방식으로 가능함.