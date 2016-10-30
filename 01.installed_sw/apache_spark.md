# Apache spark

## Why spark ?


## spark-submit option
http://spark.apache.org/docs/latest/submitting-applications.html


## start spark master
### set spark configuration
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

### run spark master
```
> sbin/start-all.sh
```

- Error (at mac os)
localhost: ssh: connect to host localhost port 22: Connection refused
- solution
 * System > Remote Login  : turn on "Remote Login"

### open spark master web-ui with web browser
localhsot:8080

   

## libraries     

### - elasticsearch for spark
https://www.elastic.co/guide/en/elasticsearch/hadoop/master/spark.html

### - redis scala client library
https://github.com/debasishg/scala-redis