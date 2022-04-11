package io.skiper.driver

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import scala.collection.mutable
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.scheduler.{StreamingListener, StreamingListenerBatchCompleted, StreamingListenerBatchStarted, StreamingListenerBatchSubmitted}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.elasticsearch.spark.rdd.EsSpark
import com.redis._
import org.apache.kafka.common.serialization.StringDeserializer

object Stage3StreamingDriver {
  def main(args: Array[String]) {

    //[STEP 1] create spark streaming session
    // Create the context with a 1 second batch size
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("Stage3_Streaming")
    sparkConf.set("es.index.auto.create", "true");
    sparkConf.set("es.nodes", "localhost")
    val ssc = new StreamingContext(sparkConf, Seconds(2))

    addStreamListener(ssc)

    // [STEP 1]. Create Kafka Receiver and receive message from kafka broker
    val host_server = "localhost" // apache kafka, elasticsearch, redis가 설치된 서버의 IP
    val kafka_broker = host_server+":9092"
    val topics = "realtime"
    val topicsSet = topics.split(",").toSet
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> kafka_broker,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "realtime-group4",
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (true: java.lang.Boolean)
    )

//    val Array(zkQuorum, group, topics, numThreads) = Array("localhost:2181" ,"realtime-group1", "realtime", "2")
//    ssc.checkpoint("checkpoint")
//    val topicMap    = topics.split(",").map((_, numThreads.toInt)).toMap
//    val numReceiver = 1

    // parallel receiver per partition
    val kafkaStreams = (1 to 1).map { i =>
      KafkaUtils.createDirectStream[String, String](
        ssc,
        LocationStrategies.PreferConsistent,
        ConsumerStrategies.Subscribe[String, String](topicsSet, kafkaParams))
    }
    val messages = ssc.union(kafkaStreams)
    val lines = messages.map(_.value)
    // [STEP 2]. parser message and join customer info from redis
    // original msg = ["event_id","customer_id","track_id","datetime","ismobile","listening_zip_code"]
    val columnList  = List("@timestamp", "customer_id","track_id","ismobile","listening_zip_code", "name", "age", "gender", "zip", "Address", "SignDate", "Status", "Level", "Campaign", "LinkedWithApps", "SendEvent")
    val wordList    = lines.mapPartitions(iter => {
      val r = new RedisClient("localhost", 6379)
      iter.toList.map(s => {
        val listMap = new mutable.LinkedHashMap[String, Any]()
        val split   = s.split(",")

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

        // 광고 대상 사용자인지 체크하고, 광고 대상자라면 광고 메세지를 보낸다.
        // 광고 여부 확인
        val pred_key = s"pred_event:${split(1).trim}"
        val pred = r.get(pred_key).get.toInt
        // 광고 대상이 맞다면, 광고를 보내라는 신호를 redis에 전송
        if(pred == 1) {
          r.sadd("1_day_event_users", split(1).trim)
          println(s"insert into redis ${pred_key}  : ${split(1).trim}")
          //elasticsearch user 정보에 추가 (광고를 보낸 이력)
        }
        listMap.put(columnList(15), pred.toInt)
        listMap
      }).iterator
    })

    //[STEP 3]. Write to ElasticSearch
    wordList.foreachRDD(rdd => {
      rdd.foreach(s => s.foreach(x => println(x.toString)))
      EsSpark.saveToEs(rdd, "ba_realtime3/stage3")
    })

    ssc.start()
    ssc.awaitTermination()
  }

  // get current time
  def getTimestamp(): Timestamp = {
    val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    new Timestamp(Calendar.getInstance().getTime().getTime)
  }

  // spark stream listener interface
  def addStreamListener(ssc: StreamingContext): Unit = {
    ssc.addStreamingListener(new StreamingListener() {
      override def onBatchSubmitted(batchSubmitted: StreamingListenerBatchSubmitted): Unit = {
        super.onBatchSubmitted(batchSubmitted)
        val batchTime = batchSubmitted.batchInfo.batchTime
        println("[batchSubmitted] " + batchTime.milliseconds)
      }

      override def onBatchStarted(batchStarted: StreamingListenerBatchStarted): Unit = {
        super.onBatchStarted(batchStarted)
      }

      override def onBatchCompleted(batchCompleted: StreamingListenerBatchCompleted): Unit = {
        super.onBatchCompleted(batchCompleted)
      }
    })
  }
}
