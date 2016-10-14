# 1. Logstash (Collect)

## logstash Quick guide
 - https://www.elastic.co/guide/en/logstash/current/getting-started-with-logstash.html

- logstash component
![logstash ]
(https://www.elastic.co/guide/en/logstash/current/static/images/basic_logstash_pipeline.png)

- filter plugins
 - https://www.elastic.co/guide/en/logstash/current/filter-plugins.html

## install logstash 2.4  and run

- 설치
```
wget https://download.elastic.co/logstash/logstash/logstash-2.4.0.tar.gz
tar xvf logstash-2.4.0.tar.gz
```


- set logstash path to $path
```
vi ~/.bash_profile
export PATH=$PATH:/Users/skiper/work/DevTools/elk/logstash-2.4.0/bin
```


- logstash의 정상동작 확인.
```
/home/rts/elk/logstash-2.4.0/bin/logstash -e 'input { stdin { } } output { stdout {} }'
// 아래와 같은 메세지가  stdin 입력을 받을 준비가 됨.
Settings: Default pipeline workers: 1
Pipeline main started
// 메세지 입력 후 엔터
hello logstash
//아래와 같은 메세지가 출력되며 정상
2016-10-21T01:22:14.405+0000 0.0.0.0 hello logstash
//Ctrl + D로 종료
```



## simple example
