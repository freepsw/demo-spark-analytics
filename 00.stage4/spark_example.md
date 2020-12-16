## [STEP 2] Install spark 
```
> cd ~/demo-spark-analytics/sw/
> wget https://downloads.apache.org/spark/spark-2.4.7/spark-2.4.7-bin-hadoop2.7.tgz
> tar xvf spark-2.4.7-bin-hadoop2.7.tgz
> cd spark-2.4.7-bin-hadoop2.7

# slave 설정
> cp conf/slaves.template conf/slaves
localhost //현재  별도의 slave node가 없으므로 localhost를 slave node로 사용

# spark master 설정
# 현재 demo에서는 별도로 변경할 설정이 없다. (실제 적용시 다양한 설정 값 적용)
> cp conf/spark-env.sh.template conf/spark-env.sh

> vi ~/.bash_profile
# 마지막 line에 아래 내용을 추가한다.
export SPARK_HOME=~/demo-spark-analytics/sw/spark-2.4.7-bin-hadoop2.7
export PATH=$PATH:$SPARK_HOME/bin
export PYTHONPATH=$SPARK_HOME/python/:$SPARK_HOME/python/lib/py4j-0.10.7-src.zip:$PYTHONPATH
```

### 소매 데이터 중 날짜별(by-day) 데이터 사용
- 샘플 : https://github.com/FVBros/Spark-The-Definitive-Guide/blob/master/data/retail-data/by-day/2010-12-01.csv
> wget https://github.com/FVBros/Spark-The-Definitive-Guide/tree/master/data/retail-data

```scala
val staticDataFrame = spark.read.format("csv").option("header", "true").option("inferSchema", "true").load("/home/freepsw/Spark-The-Definitive-Guide/data/retail-data/by-day/*.csv")

staticDataFrame.createOrReplaceTempView("retail_data")
val staticSchema = staticDataFrame.schema

import org.apache.spark.sql.functions.{window, column, desc, col}
staticDataFrame.selectExpr(
    "CustomerId",
    "(UnitPrice * Quantity) as total_cost",
    "InvoiceDate").groupBy(
    col("CustomerId"), window(col("InvoiceDate"), "1 day")).sum("total_cost").show(5)

```