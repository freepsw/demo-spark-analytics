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

import demo.HashTagsStreaming._
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest._

class HashTagsStreamingSpec extends WordSpec with MustMatchers with BeforeAndAfter {

  private var sc: SparkContext = _
  private var ssc: StreamingContext = _

  before {
    val conf = new SparkConf().setAppName("unit-testing").setMaster("local")
    ssc = new StreamingContext(conf, Seconds(1))
    sc = ssc.sparkContext
  }

  after {
    if (ssc != null) {
      ssc.stop()
    }
  }


  def getPopularTagsTestHelper(input: List[String], expected: List[Popularity]) = {
    val inputRDD: RDD[String] = sc.parallelize(input)
    val res: Array[Popularity] = extractTrendingTags(inputRDD).collect()
    res must have size expected.size
    res.map(_.tag).toList must contain theSameElementsInOrderAs expected.map(_.tag).toList
    res.map(_.amount).toList must contain theSameElementsInOrderAs expected.map(_.amount).toList
  }

  "getPopularTags op" should {
    "extract and sorts tags for single rdd" in {
      getPopularTagsTestHelper(List("#t1 #t2 #t3", "#t1 #t2", "#t1 #t3", "#t1 #t3 #t4"),
        List(("t1", 4), ("t3", 3), ("t2", 2), ("t4", 1)).map(r => Popularity(r._1, r._2)))
    }

    "sort lexicographically in case of equal occurrences" in {
      getPopularTagsTestHelper(List("#t1 #t2", "#t2 #t1", "#t1", "#t2"),
        List(("t1", 3), ("t2", 3)).map(r => Popularity(r._1, r._2)))
    }

    "bring to lowercase" in {
      getPopularTagsTestHelper(List("#tag1 #tag2", "#Tag1", "#tag1", "#tAG2"),
        List(("tag1", 3), ("tag2", 2)).map(r => Popularity(r._1, r._2)))
    }

    "remove # only from the beginning of the hashtag" in {
      getPopularTagsTestHelper(List("#t1 #t2", "#t#1", "#t2#"),
        List(("t#1", 1), ("t1", 1), ("t2", 1), ("t2#", 1)).map(r => Popularity(r._1, r._2)))
    }

    "remove empty hashtags and punctuations" in {
      getPopularTagsTestHelper(List("#t1  #t2, # #!?", "#t1? ##t2!"),
        List(("t1", 2), ("#t2", 1), ("t2", 1)).map(r => Popularity(r._1, r._2)))
    }

    "ignores non-tags" in {
      getPopularTagsTestHelper(List("#t1  #t2, #t3 t3", "#t3"),
        List(("t3", 2), ("t1", 1), ("t2", 1)).map(r => Popularity(r._1, r._2)))
    }

  }

}
