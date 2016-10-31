# redis
- In memory cache, NoSQL key-value data store.

## Why redis?

## Basic concept

## redis data types
http://redis.io/topics/data-types-intro

## 1. Install (redis 3.0.7)
```
> cd ~/demo-spark-analytics/sw
> wget http://download.redis.io/releases/redis-3.0.7.tar.gz
> tar -xzf redis-3.0.7.tar.gz
> cd redis-3.0.7
> make
```

## 2. run 
```
> src/redis-server
```


## 3. test
```
> cd ~/demo-spark-analytics/sw/redis-3.0.7
> src/redis-cli
redis> set foo bar
OK
redis> get foo
"bar"
```

## etc
- hashmap example
```
redis> HSET myhash field1 "Hello"
(integer) 1
redis> HSET myhash field2 "World"
(integer) 1
redis> HGETALL myhash
1) "field1"
2) "Hello"
3) "field2"
4) "World"

redis> HGET myhash field1
"Hello"
```

- Redis 활용 방안에 따른 아키텍처 (2014)
http://www.kosta.or.kr/mail/2014/download/Track2-8_2014Architect.pdf
