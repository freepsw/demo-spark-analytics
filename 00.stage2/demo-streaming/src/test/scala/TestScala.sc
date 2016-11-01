val m = Map( "a" -> 2, "b" -> 3 )
//val m1 = m.transform((key, value) => key + value) //Map[String, String](a -> a2, b -> b3)
//m1
//
//val m2 = m.mapValues(_ => "hi")
//m2
//
//
//val m3 = m.mapValues(_ + "hi")
//m3

val s = "dldfd gdf dd "
val s1 = s.trim



def changeValue(a: Int):(Int, Int, Int) = {

  println("test")
  return (a,1, 3)
}

def changeValue_1(a: Int):(String, String, String) = {

  println("test")
  return ("1", "music", "item")
}

val m4 = m.mapValues(a => (a,1))
m4.foreach(x => {
  println(x._1, x._2)
})

val m5 = m.mapValues(a => changeValue(a))
val m5_1 = m5.map(x => x)
m5_1.foreach(x => {
  println("prt " , x.toString)
})

val m6 = m.mapValues(a => changeValue_1(a))

