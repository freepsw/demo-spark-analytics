# Elasticsearch

## Basic concept (Getting Started)
- https://www.elastic.co/guide/en/elasticsearch/reference/current/getting-started.html

## 1. Install 
### elasticsearch 2.4 설치 및 실행

```
mkdir ~/demo-spark-analytics/sw
cd ~/demo-spark-analytics/sw

# download 
wget https://download.elastic.co/elasticsearch/release/org/elasticsearch/distribution/tar/elasticsearch/2.4.0/elasticsearch-2.4.0.tar.gz
tar xvf elasticsearch-2.4.0.tar.gz

# install plugin 
cd elasticsearch-2.4.0
bin/plugin install mobz/elasticsearch-head
```

## 2. configuration 
- server와 client가 다른 ip가 있을 경우, 외부에서 접속할 수 있도록 설정을 추가해야함.
```
cd ~/demo-spark-analytics/sw/elasticsearch-2.4.0
vi config/elasticsearch.yml
# bind ip to connect from client  (lan이 여러개 있을 경우 외부에서 접속할 ip를 지정할 수 있음.)
# bind all ip server have "0.0.0.0"
 network.host: 0.0.0.0 
```

## 3. run
```
cd ~/demo-spark-analytics/sw/elasticsearch-2.4.0
bin/elasticsearch

# 아래와 같은 메세지가 보이면 정상
[2016-10-20 08:54:06,906][INFO ][node                     ] [Haven] started
[2016-10-20 08:54:07,142][INFO ][gateway                  ] [Haven] recovered [5] indices into cluster_state
[2016-10-20 08:54:08,433][INFO ][cluster.routing.allocation] [Haven] Cluster health status changed from [RED] to [YELLOW] (reason: [shards started [[.kibana][0]] ...]).
```

## 4. open with web browser
```
# open web browser
http://localhost:9200/

#정상동작 확인 (Web browser에서 아래 주소 입력하면 결과 json 확인)
http://localhost:9200/

{
"name": "Haven",
"cluster_name": "elasticsearch",
"version": {
"number": "2.4.0",
"build_hash": "ce9f0c7394dee074091dd1bc4e9469251181fc55",
"build_timestamp": "2016-08-29T09:14:17Z",
"build_snapshot": false,
"lucene_version": "5.5.2"
},
"tagline": "You Know, for Search"
}

# plug-in 확인
 
http://localhost:9200/_plugin/head/
```
- 5.5 이후에는 plugin이 kibana에 통합될 예정임.
