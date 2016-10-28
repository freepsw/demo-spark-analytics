package io.skiper.driver

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.storage.StorageLevel
import org.apache.log4j.{Level, Logger};
import org.apache.spark.streaming.scheduler.{StreamingListener, StreamingListenerBatchCompleted, StreamingListenerBatchStarted, StreamingListenerBatchSubmitted}
/**
  * Created by skiper on 2016. 10. 25..
  */
object StageStreaming {
  def main(args: Array[String]) {

    // Create the context with a 1 second batch size
    val sparkConf = new SparkConf().setMaster("local[1]").setAppName("Simple-Streaming")
    val ssc = new StreamingContext(sparkConf, Seconds(2))
    addStreamListener(ssc)

    // Create Kafka Receiver
    val Array(zkQuorum, group, topics, numThreads) = Array("localhost:2181" ,"realtime-group1", "realtime", "2")
    //ssc.checkpoint("checkpoint")
    val topicMap = topics.split(",").map((_, numThreads.toInt)).toMap
    val numReceiver = 1

    val lines = KafkaUtils.createStream(ssc, zkQuorum, group, topicMap).map(_._2)
    val words = lines.flatMap(_.split(" "))
    val wordCounts = words.map(x => (x, 1)).reduceByKey(_ + _)
    wordCounts.print()
    
    ssc.start()
    ssc.awaitTermination()
  }
  
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

        var total_delay = batchCompleted.batchInfo.totalDelay
        var process_delay = batchCompleted.batchInfo.processingDelay

        //check Black List IP
        if (true) {
          //redis
          //redis.putLog("mykey", batchStartTime.toString)
        }


        //        accumulableExecuteLogs.value.map(s => {
        //          //println("accumulated execution map : " + s._1 + " / " + s._2)
        //          val key = flow.id + ":" + s._1 + ":" + formatter.format(batchStartTime) + ":" + formatter.format(batchEndTime)
        //          val lastExe = accumulableLastExecuted.value
        //          redisLogger.putLog(key, lastExe(s._1) + "," + s._2)
        //        })
        //        accumulableExecuteLogs.value.clear()
      }

    })
  }
}
