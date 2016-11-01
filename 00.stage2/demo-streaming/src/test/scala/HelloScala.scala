/**
  * Created by skiper on 2016. 10. 25..
  */
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar

import scala.util.{Failure, Success, Try}

import com.redis._

object HelloScala {
  def main(args: Array[String]) : Unit = {
    println("Helo Scala")
//    testTime()
    testRedis()
  }

  def testRedis() = {
    val r = new RedisClient("localhost", 6379)
//    val cust_info = r.hmget[String, String]("1", "age", "name")
    // get, sadd
    //val s1 = "3164"
    val s1 = "3174"
    val s2 = s"pred_event:$s1"
    val s2_1 = r.get(s2).get.toInt
    println(s"pred_event = $s2_1")

    if(r.get(s2).get.toInt == 1) {
      r.sadd("1_day_event_users", s1)

    }

    // sadd, sismember
    r.sadd("1_day_event_users", 1)
    r.sadd("1_day_event_users", 3)
    r.sadd("1_day_event_users", 5)

    val s3 = r.sismember("1_day_event_users",1)
    println(s"1_day_event_users 1 : ${s3}")
    val s4 = r.sismember("1_day_event_users",s1)
    println(s"1_day_event_users 3162 : ${s3}")



//    val s = "1, 1081,19, 2014-10-15 18:32:14,1, 17307"
//    val split   = s.split(",")
//    println(s"cust_id =${split(1).trim}=")
//
//
//    val cust_info = r.hmget[String, String](split(1).trim, "name", "age", "gender", "zip", "Address", "SignDate", "Status", "Level", "Campaign", "LinkedWithApps")
//    println(cust_info)
//    println(cust_info.get) //convert Some to Map
//    println(cust_info.get("age"))
//    println(cust_info.get("LinkedWithApps"))
  }


  def testTime() = {
    val str = getTimestamp("2014-10-20 02:24:55").get
    println(str)

    val s = "\"test\""
    println(s)
    val s1 = s.replace("\"", "")
    println(s1)

    val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    println(format.format(Calendar.getInstance().getTime()))
    val ts = new Timestamp(Calendar.getInstance().getTime().getTime)
    print(ts)
  }


  def getTimestamp(s: String) : Option[Timestamp] = s match {
    case "" => None
    case _ => {
      val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")//2
      println(new Timestamp(format.parse(s).getTime))

      Try(new Timestamp(format.parse(s).getTime)) match {
        case Success(t) => Some(t)
        case Failure(_) => None
      }
    }
  }
}
