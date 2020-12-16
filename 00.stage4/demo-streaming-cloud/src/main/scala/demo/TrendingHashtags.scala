/*
 Copyright Google Inc. 2018
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package demo

import java.nio.charset.StandardCharsets

import com.google.cloud.datastore._
import demo.DataStoreConverter.saveRDDtoDataStore
import demo.HashTagsStreaming.processTrendingHashTags
import org.apache.spark.SparkConf
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.pubsub.{PubsubUtils, SparkGCPCredentials}
import org.apache.spark.streaming.{Seconds, StreamingContext}


object TrendingHashtags {

  def createContext(projectID: String, windowLength: String, slidingInterval: String, checkpointDirectory: String)
    : StreamingContext = {

    // [START stream_setup]
//    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("TrendingHashtags")
    val sparkConf = new SparkConf().setAppName("TrendingHashtags")
    val ssc = new StreamingContext(sparkConf, Seconds(slidingInterval.toInt))

    // Set the checkpoint directory
//    val yarnTags = sparkConf.get("spark.yarn.tags")
//    val jobId = yarnTags.split(",").filter(_.startsWith("dataproc_job")).head
//    ssc.checkpoint(checkpointDirectory + '/' + jobId)
    
    // Create stream
    val messagesStream: DStream[String] = PubsubUtils
      .createStream(
        ssc,
        projectID,
        None,
        "tweets-subscription",  // Cloud Pub/Sub subscription for incoming tweets
        SparkGCPCredentials.builder.build(), StorageLevel.MEMORY_AND_DISK_SER_2)
      .map(message => new String(message.getData(), StandardCharsets.UTF_8))
    // [END stream_setup]

    //process the stream
    processTrendingHashTags(messagesStream,
      windowLength.toInt,
      slidingInterval.toInt,
      10,
      //decoupled handler that saves each separate result for processed to datastore
      saveRDDtoDataStore(_, windowLength.toInt)
    )
    
	ssc
  }

  def main(args: Array[String]): Unit = {
    if (args.length != 5) {
      System.err.println(
        """
          | Usage: TrendingHashtags <projectID> <windowLength> <slidingInterval> <totalRunningTime>
          |
          |     <projectID>: ID of Google Cloud project
          |     <windowLength>: The duration of the window, in seconds
          |     <slidingInterval>: The interval at which the window calculation is performed, in seconds
          |     <totalRunningTime>: Total running time for the application, in minutes. If 0, runs indefinitely until termination.
          |     <checkpointDirectory>: Directory used to store RDD checkpoint data
          |
        """.stripMargin)
      System.exit(1)
    }

    val Seq(projectID, windowLength, slidingInterval, totalRunningTime, checkpointDirectory) = args.toSeq

    // Create Spark context
    val ssc = StreamingContext.getOrCreate(checkpointDirectory,
      () => createContext(projectID, windowLength, slidingInterval, checkpointDirectory))

    // Start streaming until we receive an explicit termination
    ssc.start()

    if (totalRunningTime.toInt == 0) {
      ssc.awaitTermination()
    }
    else {
      ssc.awaitTerminationOrTimeout(1000 * 60 * totalRunningTime.toInt)
    }
  }

}
