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

import com.google.cloud.Timestamp
import com.google.cloud.datastore._
import demo.HashTagsStreaming.Popularity

object DataStoreConverter {

  private def convertToDatastore(keyFactory: KeyFactory,
                                 record: Popularity): FullEntity[IncompleteKey] =
    FullEntity.newBuilder(keyFactory.newKey())
      .set("name", record.tag)
      .set("occurrences", record.amount)
      .build()

  // [START convert_identity] 
  private[demo] def convertToEntity(hashtags: Array[Popularity],
                                    keyFactory: String => KeyFactory): FullEntity[IncompleteKey] = {
    val hashtagKeyFactory: KeyFactory = keyFactory("Hashtag")

    val listValue = hashtags.foldLeft[ListValue.Builder](ListValue.newBuilder())(
      (listValue, hashTag) => listValue.addValue(convertToDatastore(hashtagKeyFactory, hashTag))
    )

    val rowKeyFactory: KeyFactory = keyFactory("TrendingHashtags")

    FullEntity.newBuilder(rowKeyFactory.newKey())
      .set("datetime", Timestamp.now())
      .set("hashtags", listValue.build())
      .build()
  }
  // [END convert_identity]

  def saveRDDtoDataStore(tags: Array[Popularity],
                         windowLength: Int): Unit = {
    val datastore: Datastore = DatastoreOptions.getDefaultInstance().getService()
    val keyFactoryBuilder = (s: String) => datastore.newKeyFactory().setKind(s)

    val entity: FullEntity[IncompleteKey] = convertToEntity(tags, keyFactoryBuilder)

    datastore.add(entity)

    // Display some info in the job's logs
    println("\n-------------------------")
    println(s"Window ending ${Timestamp.now()} for the past ${windowLength} seconds\n")
    if (tags.length == 0) {
      println("No trending hashtags in this window.")
    }
    else {
      println("Trending hashtags in this window:")
      tags.foreach(hashtag => println(s"${hashtag.tag}, ${hashtag.amount}"))
    }
  }
}
