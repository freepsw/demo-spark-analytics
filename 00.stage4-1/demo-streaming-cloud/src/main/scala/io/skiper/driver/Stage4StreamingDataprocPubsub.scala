package io.skiper.driver

import java.nio.charset.StandardCharsets
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar

import com.redis.RedisClient
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.pubsub.{PubsubUtils, SparkGCPCredentials}
import org.apache.spark.streaming.scheduler.{StreamingListener, StreamingListenerBatchCompleted, StreamingListenerBatchStarted, StreamingListenerBatchSubmitted}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.elasticsearch.spark.rdd.EsSpark

import scala.collection.mutable

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
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("Stage42_Streaming")

    // 2) DataProc를 사용하는 경우 setMaster를 지정하지 않음.
    //val sparkConf = new SparkConf().setAppName("Stage42_Streaming")
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
      EsSpark.saveToEs(rdd, "ba_realtime42/stage42")
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
